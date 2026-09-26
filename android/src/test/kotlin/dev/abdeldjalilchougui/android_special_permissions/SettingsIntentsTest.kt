package dev.abdeldjalilchougui.android_special_permissions

import android.provider.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class SettingsIntentsTest {
    private val pkg = "com.example.app"
    private val pkgUri = "package:com.example.app"
    private val appDetails = IntentSpec(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)
    private val settingsHome = IntentSpec(Settings.ACTION_SETTINGS)

    private fun open(p: SpecialPermission, sdk: Int, cls: String? = null) =
        SettingsIntents.candidates(p, sdk, pkg, cls, SettingsIntents.Purpose.OPEN_SETTINGS)

    private fun request(p: SpecialPermission, sdk: Int, cls: String? = null, batteryDeclared: Boolean = false) =
        SettingsIntents.candidates(p, sdk, pkg, cls, SettingsIntents.Purpose.REQUEST, batteryDeclared)

    @Test
    fun everyScreenEndsWithAppDetailsThenSettings() {
        for (p in SpecialPermission.values()) {
            if (p == SpecialPermission.BACKGROUND_LOCATION) continue
            val list = open(p, 34)
            assertEquals(listOf(appDetails, settingsHome), list.takeLast(2), "fallbacks for $p")
        }
    }

    @Test
    fun backgroundLocation_hasNoScreen() {
        assertTrue(open(SpecialPermission.BACKGROUND_LOCATION, 34).isEmpty())
    }

    @Test
    fun usageAccess_api29_addsPackageData() {
        val list = open(SpecialPermission.USAGE_ACCESS, 29)
        assertEquals(IntentSpec(Settings.ACTION_USAGE_ACCESS_SETTINGS, pkgUri), list[0])
        assertEquals(IntentSpec(Settings.ACTION_USAGE_ACCESS_SETTINGS), list[1])
    }

    @Test
    fun usageAccess_api28_hasNoPackageData() {
        val list = open(SpecialPermission.USAGE_ACCESS, 28)
        assertEquals(IntentSpec(Settings.ACTION_USAGE_ACCESS_SETTINGS), list[0])
        assertEquals(appDetails, list[1])
    }

    @Test
    fun overlay_packageScopedFirst() {
        val list = open(SpecialPermission.OVERLAY, 30)
        assertEquals(IntentSpec(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri), list[0])
        assertEquals(IntentSpec(Settings.ACTION_MANAGE_OVERLAY_PERMISSION), list[1])
    }

    @Test
    fun accessibility_api33WithService_opensDetailsPage() {
        val list = open(SpecialPermission.ACCESSIBILITY_SERVICE, 33, ".MyService")
        assertEquals(
            IntentSpec(
                SettingsIntents.ACTION_ACCESSIBILITY_DETAILS_SETTINGS,
                extras = mapOf(SettingsIntents.EXTRA_COMPONENT_NAME to "com.example.app/com.example.app.MyService"),
            ),
            list[0],
        )
        assertEquals(IntentSpec(Settings.ACTION_ACCESSIBILITY_SETTINGS), list[1])
    }

    @Test
    fun accessibility_api32_opensGeneralScreen() {
        val list = open(SpecialPermission.ACCESSIBILITY_SERVICE, 32, ".MyService")
        assertEquals(IntentSpec(Settings.ACTION_ACCESSIBILITY_SETTINGS), list[0])
    }

    @Test
    fun accessibility_withoutService_opensGeneralScreen() {
        assertEquals(IntentSpec(Settings.ACTION_ACCESSIBILITY_SETTINGS), open(SpecialPermission.ACCESSIBILITY_SERVICE, 34)[0])
    }

    @Test
    fun notificationListener_api30WithClass_opensDetail() {
        val list = open(SpecialPermission.NOTIFICATION_LISTENER, 30, ".Listener")
        assertEquals(
            IntentSpec(
                Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS,
                extras = mapOf(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME to "com.example.app/com.example.app.Listener",
                ),
            ),
            list[0],
        )
        assertEquals(IntentSpec(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS), list[1])
    }

    @Test
    fun notificationListener_api29_opensList() {
        assertEquals(
            IntentSpec(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
            open(SpecialPermission.NOTIFICATION_LISTENER, 29, ".Listener")[0],
        )
    }

    @Test
    fun notificationListener_api30WithoutClass_opensList() {
        assertEquals(
            IntentSpec(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
            open(SpecialPermission.NOTIFICATION_LISTENER, 30)[0],
        )
    }

    @Test
    fun battery_openSettings_usesListScreen() {
        val list = open(SpecialPermission.IGNORE_BATTERY_OPTIMIZATIONS, 30)
        assertEquals(IntentSpec(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), list[0])
        assertEquals(appDetails, list[1])
    }

    @Test
    fun battery_requestWhenDeclared_usesDirectDialogFirst() {
        val list = request(SpecialPermission.IGNORE_BATTERY_OPTIMIZATIONS, 30, batteryDeclared = true)
        assertEquals(IntentSpec(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkgUri), list[0])
        assertEquals(IntentSpec(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), list[1])
    }

    @Test
    fun battery_requestWhenNotDeclared_skipsDirectDialog() {
        val list = request(SpecialPermission.IGNORE_BATTERY_OPTIMIZATIONS, 30, batteryDeclared = false)
        assertEquals(IntentSpec(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), list[0])
    }

    @Test
    fun exactAlarm_packageScoped() {
        val list = open(SpecialPermission.EXACT_ALARM, 31)
        assertEquals(IntentSpec(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkgUri), list[0])
        assertEquals(IntentSpec(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM), list[1])
    }

    @Test
    fun installUnknownApps_packageScoped() {
        val list = open(SpecialPermission.INSTALL_UNKNOWN_APPS, 26)
        assertEquals(IntentSpec(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, pkgUri), list[0])
        assertEquals(IntentSpec(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES), list[1])
    }

    @Test
    fun allFiles_appScreenThenGeneralScreen() {
        val list = open(SpecialPermission.ALL_FILES_ACCESS, 30)
        assertEquals(IntentSpec(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, pkgUri), list[0])
        assertEquals(IntentSpec(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), list[1])
    }

    @Test
    fun writeSettings_packageScoped() {
        val list = open(SpecialPermission.WRITE_SETTINGS, 23)
        assertEquals(IntentSpec(Settings.ACTION_MANAGE_WRITE_SETTINGS, pkgUri), list[0])
        assertEquals(IntentSpec(Settings.ACTION_MANAGE_WRITE_SETTINGS), list[1])
    }

    @Test
    fun doNotDisturb_listScreen() {
        assertEquals(IntentSpec(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS), open(SpecialPermission.DO_NOT_DISTURB, 23)[0])
    }

    @Test
    fun fullScreenIntent_thenAppNotificationSettings() {
        val list = open(SpecialPermission.FULL_SCREEN_INTENT, 34)
        assertEquals(IntentSpec(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkgUri), list[0])
        assertEquals(
            IntentSpec(Settings.ACTION_APP_NOTIFICATION_SETTINGS, extras = mapOf(Settings.EXTRA_APP_PACKAGE to pkg)),
            list[1],
        )
    }

    @Test
    fun pictureInPicture_packageScopedThenList() {
        val list = open(SpecialPermission.PICTURE_IN_PICTURE, 26)
        assertEquals(IntentSpec(SettingsIntents.ACTION_PICTURE_IN_PICTURE_SETTINGS, pkgUri), list[0])
        assertEquals(IntentSpec(SettingsIntents.ACTION_PICTURE_IN_PICTURE_SETTINGS), list[1])
    }

    @Test
    fun appDetails_candidates() {
        assertEquals(listOf(appDetails, settingsHome), SettingsIntents.appDetails(pkg))
    }

    @Test
    fun choose_picksFirstResolvable() {
        val list = open(SpecialPermission.USAGE_ACCESS, 30)
        val chosen = SettingsIntents.choose(list) { it.data == null }
        assertEquals(IntentSpec(Settings.ACTION_USAGE_ACCESS_SETTINGS), chosen)
    }

    @Test
    fun choose_fallsBackToAppDetails() {
        val list = open(SpecialPermission.OVERLAY, 30)
        assertEquals(appDetails, SettingsIntents.choose(list) { it.action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS })
    }

    @Test
    fun choose_returnsNullWhenNothingResolves() {
        assertNull(SettingsIntents.choose(open(SpecialPermission.OVERLAY, 30)) { false })
    }

    @Test
    fun choose_stopsProbingAfterFirstMatch() {
        val probed = mutableListOf<IntentSpec>()
        SettingsIntents.choose(open(SpecialPermission.OVERLAY, 30)) { probed += it; true }
        assertEquals(1, probed.size)
    }
}
