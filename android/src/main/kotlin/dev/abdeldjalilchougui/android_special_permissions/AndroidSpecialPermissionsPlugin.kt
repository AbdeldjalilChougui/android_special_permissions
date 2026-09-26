package dev.abdeldjalilchougui.android_special_permissions

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.lifecycle.HiddenLifecycleReference
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry

/** Flutter plugin exposing Android special-access permissions. */
class AndroidSpecialPermissionsPlugin :
    FlutterPlugin,
    MethodCallHandler,
    ActivityAware,
    PluginRegistry.ActivityResultListener {

    private class PendingRequest(
        val permission: SpecialPermission,
        val serviceClassName: String?,
        val result: Result,
    )

    private class PendingRole(val role: Role, val result: Result)

    private var channel: MethodChannel? = null
    private var appContext: Context? = null
    private var activityBinding: ActivityPluginBinding? = null
    private var lifecycle: Lifecycle? = null
    private val waiter = ResumeWaiter<PendingRequest>()
    private var pendingRole: PendingRole? = null

    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> waiter.onPaused()
            Lifecycle.Event.ON_RESUME -> waiter.onResumed()?.let { completeRequest(it) }
            else -> Unit
        }
    }

    // region FlutterPlugin

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        appContext = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, CHANNEL).also { it.setMethodCallHandler(this) }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
        channel = null
        appContext = null
    }

    // endregion

    // region ActivityAware

    override fun onAttachedToActivity(binding: ActivityPluginBinding) = attach(binding)

    override fun onDetachedFromActivityForConfigChanges() = detach(failPending = false)

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) = attach(binding)

    override fun onDetachedFromActivity() = detach(failPending = true)

    private fun attach(binding: ActivityPluginBinding) {
        activityBinding = binding
        binding.addActivityResultListener(this)
        lifecycle = (binding.lifecycle as? HiddenLifecycleReference)?.lifecycle
        lifecycle?.addObserver(lifecycleObserver)
    }

    private fun detach(failPending: Boolean) {
        lifecycle?.removeObserver(lifecycleObserver)
        lifecycle = null
        activityBinding?.removeActivityResultListener(this)
        activityBinding = null
        if (failPending) {
            waiter.cancel()?.result?.let { activityGone(it) }
            pendingRole?.result?.let { activityGone(it) }
            pendingRole = null
        }
    }

    private val activity: Activity? get() = activityBinding?.activity

    // endregion

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "status" -> handleStatus(call, result)
            "openSettings" -> handleOpenSettings(call, result)
            "request" -> handleRequest(call, result)
            "openAppSettings" -> handleOpenAppSettings(result)
            "roleStatus" -> handleRoleStatus(call, result)
            "requestRole" -> handleRequestRole(call, result)
            else -> result.notImplemented()
        }
    }

    private fun handleStatus(call: MethodCall, result: Result) {
        val permission = permissionArg(call, result) ?: return
        val serviceClassName = serviceClassArg(call, permission, required = true, result) ?: return
        val context = appContext ?: return activityGone(result)
        result.success(PermissionChecker(context).status(permission, serviceClassName.value).wireName)
    }

    private fun handleOpenSettings(call: MethodCall, result: Result) {
        val permission = permissionArg(call, result) ?: return
        if (permission == SpecialPermission.BACKGROUND_LOCATION) return backgroundLocationUnsupported(result)
        val serviceClassName = serviceClassArg(call, permission, required = false, result) ?: return
        val activity = activity ?: return activityGone(result)
        val candidates = SettingsIntents.candidates(
            permission,
            Build.VERSION.SDK_INT,
            activity.packageName,
            serviceClassName.value,
            SettingsIntents.Purpose.OPEN_SETTINGS,
            accessibilityDetailsAllowed = canOpenAccessibilityDetails(activity),
        )
        result.success(launch(activity, candidates))
    }

    private fun handleRequest(call: MethodCall, result: Result) {
        val permission = permissionArg(call, result) ?: return
        if (permission == SpecialPermission.BACKGROUND_LOCATION) return backgroundLocationUnsupported(result)
        val serviceClassName = serviceClassArg(call, permission, required = true, result) ?: return
        val activity = activity ?: return activityGone(result)
        if (isBusy()) return busy(result)

        val checker = PermissionChecker(activity)
        val current = checker.status(permission, serviceClassName.value)
        if (current != PermissionStatus.DENIED) return result.success(current.wireName)

        val candidates = SettingsIntents.candidates(
            permission,
            Build.VERSION.SDK_INT,
            activity.packageName,
            serviceClassName.value,
            SettingsIntents.Purpose.REQUEST,
            batteryDialogDeclared = permission == SpecialPermission.IGNORE_BATTERY_OPTIMIZATIONS &&
                checker.declares(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS),
            accessibilityDetailsAllowed = canOpenAccessibilityDetails(activity),
        )
        waiter.start(PendingRequest(permission, serviceClassName.value, result))
        if (!launch(activity, candidates)) {
            waiter.cancel()
            result.success(current.wireName)
        }
    }

    private fun handleOpenAppSettings(result: Result) {
        val activity = activity ?: return activityGone(result)
        result.success(launch(activity, SettingsIntents.appDetails(activity.packageName)))
    }

    private fun handleRoleStatus(call: MethodCall, result: Result) {
        val role = roleArg(call, result) ?: return
        val context = appContext ?: return activityGone(result)
        result.success(PermissionChecker(context).roleStatus(role).wireName)
    }

    @SuppressLint("NewApi")
    private fun handleRequestRole(call: MethodCall, result: Result) {
        val role = roleArg(call, result) ?: return
        val activity = activity ?: return activityGone(result)
        if (isBusy()) return busy(result)
        val current = PermissionChecker(activity).roleStatus(role)
        if (current != PermissionStatus.DENIED) return result.success(current.wireName)
        val roleManager = activity.getSystemService(RoleManager::class.java)
            ?: return result.success(PermissionStatus.NOT_APPLICABLE.wireName)
        val intent = roleManager.createRequestRoleIntent(role.roleName)
        pendingRole = PendingRole(role, result)
        try {
            activity.startActivityForResult(intent, ROLE_REQUEST_CODE)
        } catch (e: Exception) {
            pendingRole = null
            result.success(current.wireName)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != ROLE_REQUEST_CODE) return false
        val pending = pendingRole ?: return true
        pendingRole = null
        val context = activity ?: appContext
        if (context == null) {
            activityGone(pending.result)
        } else {
            pending.result.success(PermissionChecker(context).roleStatus(pending.role).wireName)
        }
        return true
    }

    private fun completeRequest(request: PendingRequest) {
        val context = activity ?: appContext ?: return activityGone(request.result)
        request.result.success(PermissionChecker(context).status(request.permission, request.serviceClassName).wireName)
    }

    /** Starts the first candidate that resolves and launches; false if none did. */
    private fun launch(activity: Activity, candidates: List<IntentSpec>): Boolean {
        val pm = activity.packageManager
        return SettingsIntents.choose(candidates) { spec ->
            val intent = spec.toIntent()
            intent.resolveActivity(pm) != null && try {
                activity.startActivity(intent)
                true
            } catch (e: Exception) {
                // Resolvable but refused (e.g. a protected details page): try the next screen.
                false
            }
        } != null
    }

    private fun IntentSpec.toIntent(): Intent = Intent(action).also { intent ->
        data?.let { intent.data = Uri.parse(it) }
        extras.forEach { (key, value) -> intent.putExtra(key, value) }
    }

    private fun canOpenAccessibilityDetails(activity: Activity): Boolean =
        ContextCompat.checkSelfPermission(activity, SettingsIntents.PERMISSION_OPEN_ACCESSIBILITY_DETAILS_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    private fun isBusy() = waiter.isWaiting || pendingRole != null

    // region argument parsing and errors

    /** Wraps a nullable value so "absent" can be told apart from "invalid". */
    private class Arg(val value: String?)

    private fun permissionArg(call: MethodCall, result: Result): SpecialPermission? {
        val name = call.argument<String>("permission")
        return SpecialPermission.fromWire(name) ?: run {
            result.error(INVALID_ARGUMENT, "Unknown permission: $name", null)
            null
        }
    }

    private fun roleArg(call: MethodCall, result: Result): Role? {
        val name = call.argument<String>("role")
        return Role.fromWire(name) ?: run {
            result.error(INVALID_ARGUMENT, "Unknown role: $name", null)
            null
        }
    }

    private fun serviceClassArg(
        call: MethodCall,
        permission: SpecialPermission,
        required: Boolean,
        result: Result,
    ): Arg? {
        val name = call.argument<String>("serviceClassName")?.takeIf { it.isNotBlank() }
        if (name == null && required && permission == SpecialPermission.ACCESSIBILITY_SERVICE) {
            result.error(INVALID_ARGUMENT, "serviceClassName is required for ${permission.wireName}", null)
            return null
        }
        return Arg(name)
    }

    private fun activityGone(result: Result) {
        try {
            result.error(ACTIVITY_NOT_ATTACHED, "The plugin is not attached to an Activity", null)
        } catch (e: Exception) {
            // The engine may already be gone; nothing left to notify.
        }
    }

    private fun busy(result: Result) =
        result.error(REQUEST_IN_PROGRESS, "Another request is waiting for the user", null)

    private fun backgroundLocationUnsupported(result: Result) = result.error(
        NOT_SUPPORTED,
        "Background location is a runtime permission: request it with permission_handler " +
            "after the foreground location permission is granted",
        null,
    )

    // endregion

    companion object {
        const val CHANNEL = "dev.abdeldjalilchougui/android_special_permissions"
        private const val ROLE_REQUEST_CODE = 0x5E7A
        private const val ACTIVITY_NOT_ATTACHED = "activityNotAttached"
        private const val NOT_SUPPORTED = "notSupported"
        private const val REQUEST_IN_PROGRESS = "requestInProgress"
        private const val INVALID_ARGUMENT = "invalidArgument"
    }
}
