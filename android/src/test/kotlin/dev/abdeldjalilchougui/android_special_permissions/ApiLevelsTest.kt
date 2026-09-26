package dev.abdeldjalilchougui.android_special_permissions

import android.app.AppOpsManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ApiLevelsTest {
    @Test
    fun wireNames_roundTrip() {
        for (p in SpecialPermission.values()) {
            assertEquals(p, SpecialPermission.fromWire(p.wireName))
        }
        assertNull(SpecialPermission.fromWire("nope"))
        assertNull(SpecialPermission.fromWire(null))
        for (r in Role.values()) {
            assertEquals(r, Role.fromWire(r.wireName))
        }
        assertNull(Role.fromWire("camera"))
    }

    @Test
    fun roleNames_matchRoleManagerConstants() {
        assertEquals("android.app.role.DIALER", Role.DIALER.roleName)
        assertEquals("android.app.role.SMS", Role.SMS.roleName)
        assertEquals("android.app.role.HOME", Role.HOME.roleName)
        assertEquals("android.app.role.BROWSER", Role.BROWSER.roleName)
        assertEquals("android.app.role.ASSISTANT", Role.ASSISTANT.roleName)
    }

    @Test
    fun minimumApiLevels() {
        assertEquals(21, ApiLevels.minSdk(SpecialPermission.USAGE_ACCESS))
        assertEquals(23, ApiLevels.minSdk(SpecialPermission.OVERLAY))
        assertEquals(1, ApiLevels.minSdk(SpecialPermission.ACCESSIBILITY_SERVICE))
        assertEquals(18, ApiLevels.minSdk(SpecialPermission.NOTIFICATION_LISTENER))
        assertEquals(23, ApiLevels.minSdk(SpecialPermission.IGNORE_BATTERY_OPTIMIZATIONS))
        assertEquals(31, ApiLevels.minSdk(SpecialPermission.EXACT_ALARM))
        assertEquals(26, ApiLevels.minSdk(SpecialPermission.INSTALL_UNKNOWN_APPS))
        assertEquals(30, ApiLevels.minSdk(SpecialPermission.ALL_FILES_ACCESS))
        assertEquals(23, ApiLevels.minSdk(SpecialPermission.WRITE_SETTINGS))
        assertEquals(23, ApiLevels.minSdk(SpecialPermission.DO_NOT_DISTURB))
        assertEquals(34, ApiLevels.minSdk(SpecialPermission.FULL_SCREEN_INTENT))
        assertEquals(26, ApiLevels.minSdk(SpecialPermission.PICTURE_IN_PICTURE))
        assertEquals(29, ApiLevels.minSdk(SpecialPermission.BACKGROUND_LOCATION))
    }

    @Test
    fun isApplicable_isInclusiveAtTheBoundary() {
        assertFalse(ApiLevels.isApplicable(SpecialPermission.EXACT_ALARM, 30))
        assertTrue(ApiLevels.isApplicable(SpecialPermission.EXACT_ALARM, 31))
        assertFalse(ApiLevels.isApplicable(SpecialPermission.FULL_SCREEN_INTENT, 33))
        assertTrue(ApiLevels.isApplicable(SpecialPermission.FULL_SCREEN_INTENT, 34))
        assertTrue(ApiLevels.isApplicable(SpecialPermission.ACCESSIBILITY_SERVICE, 21))
    }

    @Test
    fun roles_needApi29() {
        assertFalse(ApiLevels.rolesAvailable(28))
        assertTrue(ApiLevels.rolesAvailable(29))
    }

    @Test
    fun appOpsCheck_usesUnsafeVariantFromApi29() {
        assertEquals(ApiLevels.AppOpsCheck.LEGACY, ApiLevels.appOpsCheck(28))
        assertEquals(ApiLevels.AppOpsCheck.UNSAFE, ApiLevels.appOpsCheck(29))
    }

    @Test
    fun statusOf_mapsApplicabilityAndGrant() {
        var probed = false
        assertEquals(
            PermissionStatus.NOT_APPLICABLE,
            ApiLevels.statusOf(SpecialPermission.EXACT_ALARM, 30) { probed = true; true },
        )
        assertFalse(probed, "the check must not run below the minimum API level")
        assertEquals(PermissionStatus.GRANTED, ApiLevels.statusOf(SpecialPermission.EXACT_ALARM, 31) { true })
        assertEquals(PermissionStatus.DENIED, ApiLevels.statusOf(SpecialPermission.EXACT_ALARM, 31) { false })
    }

    @Test
    fun statusWireNames() {
        assertEquals("granted", PermissionStatus.GRANTED.wireName)
        assertEquals("denied", PermissionStatus.DENIED.wireName)
        assertEquals("notApplicable", PermissionStatus.NOT_APPLICABLE.wireName)
    }

    @Test
    fun appOpsMode_allowedIsGranted() {
        assertTrue(ApiLevels.appOpsGranted(AppOpsManager.MODE_ALLOWED) { false })
    }

    @Test
    fun appOpsMode_ignoredAndErroredAreDenied() {
        assertFalse(ApiLevels.appOpsGranted(AppOpsManager.MODE_IGNORED) { true })
        assertFalse(ApiLevels.appOpsGranted(AppOpsManager.MODE_ERRORED) { true })
    }

    @Test
    fun appOpsMode_defaultFallsBackToPermissionCheck() {
        assertTrue(ApiLevels.appOpsGranted(AppOpsManager.MODE_DEFAULT) { true })
        assertFalse(ApiLevels.appOpsGranted(AppOpsManager.MODE_DEFAULT) { false })
    }
}
