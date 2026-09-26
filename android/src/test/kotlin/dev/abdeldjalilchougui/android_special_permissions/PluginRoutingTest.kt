package dev.abdeldjalilchougui.android_special_permissions

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito
import kotlin.test.Test

internal class PluginRoutingTest {
    private fun call(method: String, args: Map<String, Any?>? = null): MethodChannel.Result {
        val plugin = AndroidSpecialPermissionsPlugin()
        val result = Mockito.mock(MethodChannel.Result::class.java)
        plugin.onMethodCall(MethodCall(method, args), result)
        return result
    }

    @Test
    fun unknownMethod_isNotImplemented() {
        Mockito.verify(call("nope")).notImplemented()
    }

    @Test
    fun unknownPermission_isInvalidArgument() {
        val result = call("status", mapOf("permission" to "camera"))
        Mockito.verify(result).error(Mockito.eq("invalidArgument"), anyString(), isNull())
    }

    @Test
    fun missingPermission_isInvalidArgument() {
        val result = call("request", emptyMap())
        Mockito.verify(result).error(Mockito.eq("invalidArgument"), anyString(), isNull())
    }

    @Test
    fun unknownRole_isInvalidArgument() {
        val result = call("roleStatus", mapOf("role" to "camera"))
        Mockito.verify(result).error(Mockito.eq("invalidArgument"), anyString(), isNull())
    }

    @Test
    fun accessibilityStatusWithoutService_isInvalidArgument() {
        val result = call("status", mapOf("permission" to "accessibilityService"))
        Mockito.verify(result).error(Mockito.eq("invalidArgument"), anyString(), isNull())
    }

    @Test
    fun requestingBackgroundLocation_isNotSupported() {
        val result = call("request", mapOf("permission" to "backgroundLocation"))
        Mockito.verify(result).error(Mockito.eq("notSupported"), anyString(), isNull())
    }

    @Test
    fun openingBackgroundLocation_isNotSupported() {
        val result = call("openSettings", mapOf("permission" to "backgroundLocation"))
        Mockito.verify(result).error(Mockito.eq("notSupported"), anyString(), isNull())
    }

    @Test
    fun requestWithoutActivity_isActivityNotAttached() {
        val result = call("request", mapOf("permission" to "overlay"))
        Mockito.verify(result).error(Mockito.eq("activityNotAttached"), anyString(), isNull())
    }

    @Test
    fun openSettingsWithoutActivity_isActivityNotAttached() {
        val result = call("openSettings", mapOf("permission" to "overlay"))
        Mockito.verify(result).error(Mockito.eq("activityNotAttached"), anyString(), isNull())
    }

    @Test
    fun requestRoleWithoutActivity_isActivityNotAttached() {
        val result = call("requestRole", mapOf("role" to "sms"))
        Mockito.verify(result).error(Mockito.eq("activityNotAttached"), anyString(), isNull())
    }

    @Test
    fun statusWithoutEngine_isActivityNotAttached() {
        val result = call("status", mapOf("permission" to "overlay"))
        Mockito.verify(result).error(Mockito.eq("activityNotAttached"), anyString(), isNull())
    }
}
