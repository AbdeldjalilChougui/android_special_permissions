package dev.abdeldjalilchougui.android_special_permissions

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** Reads the current state of each special permission. */
internal class PermissionChecker(
    private val context: Context,
    private val sdk: Int = Build.VERSION.SDK_INT,
) {
    private val packageName: String get() = context.packageName

    fun status(permission: SpecialPermission, serviceClassName: String?): PermissionStatus {
        if (permission == SpecialPermission.PICTURE_IN_PICTURE &&
            !context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            return PermissionStatus.NOT_APPLICABLE
        }
        return ApiLevels.statusOf(permission, sdk) { isGranted(permission, serviceClassName) }
    }

    // Each branch is only reached when ApiLevels.isApplicable() holds for `sdk`.
    @SuppressLint("NewApi", "InlinedApi")
    private fun isGranted(permission: SpecialPermission, serviceClassName: String?): Boolean = when (permission) {
        SpecialPermission.USAGE_ACCESS -> ApiLevels.appOpsGranted(appOpsMode(AppOpsManager.OPSTR_GET_USAGE_STATS)) {
            context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) ==
                PackageManager.PERMISSION_GRANTED
        }
        SpecialPermission.OVERLAY -> Settings.canDrawOverlays(context)
        SpecialPermission.ACCESSIBILITY_SERVICE -> AccessibilityServices.isEnabled(
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            packageName,
            requireNotNull(serviceClassName) { "serviceClassName is required" },
        )
        SpecialPermission.NOTIFICATION_LISTENER ->
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(packageName)
        SpecialPermission.IGNORE_BATTERY_OPTIMIZATIONS ->
            service<PowerManager>(Context.POWER_SERVICE).isIgnoringBatteryOptimizations(packageName)
        SpecialPermission.EXACT_ALARM -> service<AlarmManager>(Context.ALARM_SERVICE).canScheduleExactAlarms()
        SpecialPermission.INSTALL_UNKNOWN_APPS -> context.packageManager.canRequestPackageInstalls()
        SpecialPermission.ALL_FILES_ACCESS -> Environment.isExternalStorageManager()
        SpecialPermission.WRITE_SETTINGS -> Settings.System.canWrite(context)
        SpecialPermission.DO_NOT_DISTURB ->
            service<NotificationManager>(Context.NOTIFICATION_SERVICE).isNotificationPolicyAccessGranted
        SpecialPermission.FULL_SCREEN_INTENT ->
            service<NotificationManager>(Context.NOTIFICATION_SERVICE).canUseFullScreenIntent()
        // PiP has no manifest permission behind it; MODE_DEFAULT means allowed.
        SpecialPermission.PICTURE_IN_PICTURE -> ApiLevels.appOpsGranted(appOpsMode(AppOpsManager.OPSTR_PICTURE_IN_PICTURE)) { true }
        SpecialPermission.BACKGROUND_LOCATION ->
            context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("NewApi")
    fun roleStatus(role: Role): PermissionStatus {
        if (!ApiLevels.rolesAvailable(sdk)) return PermissionStatus.NOT_APPLICABLE
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return PermissionStatus.NOT_APPLICABLE
        if (!roleManager.isRoleAvailable(role.roleName)) return PermissionStatus.NOT_APPLICABLE
        return PermissionStatus.of(roleManager.isRoleHeld(role.roleName))
    }

    /** Whether the app's manifest declares [permission]. */
    fun declares(permission: String): Boolean = try {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        info.requestedPermissions?.contains(permission) == true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    // unsafeCheckOpNoThrow is the API 29+ replacement for checkOpNoThrow; the
    // API 36 SDK marks it deprecated too, but it remains the call that works
    // on every API level from 29 up.
    @SuppressLint("NewApi")
    @Suppress("DEPRECATION")
    private fun appOpsMode(op: String): Int {
        val appOps = service<AppOpsManager>(Context.APP_OPS_SERVICE)
        return when (ApiLevels.appOpsCheck(sdk)) {
            ApiLevels.AppOpsCheck.UNSAFE ->
                appOps.unsafeCheckOpNoThrow(op, Process.myUid(), packageName)
            ApiLevels.AppOpsCheck.LEGACY -> legacyCheckOp(appOps, op)
        }
    }

    @Suppress("DEPRECATION")
    private fun legacyCheckOp(appOps: AppOpsManager, op: String): Int =
        appOps.checkOpNoThrow(op, Process.myUid(), packageName)

    @Suppress("UNCHECKED_CAST")
    private fun <T> service(name: String): T = context.getSystemService(name) as T
}
