package dev.abdeldjalilchougui.android_special_permissions

import android.app.AppOpsManager

/** API-level rules, kept free of Android runtime calls so they can be unit tested. */
object ApiLevels {
    /** Which AppOpsManager check method is available. */
    enum class AppOpsCheck {
        /** `checkOpNoThrow`, deprecated in API 29. */
        LEGACY,

        /** `unsafeCheckOpNoThrow`, API 29+. */
        UNSAFE,
    }

    /** First API level on which [permission] exists as a user-facing toggle. */
    fun minSdk(permission: SpecialPermission): Int = when (permission) {
        SpecialPermission.USAGE_ACCESS -> 21
        SpecialPermission.OVERLAY -> 23
        SpecialPermission.ACCESSIBILITY_SERVICE -> 1
        SpecialPermission.NOTIFICATION_LISTENER -> 18
        SpecialPermission.IGNORE_BATTERY_OPTIMIZATIONS -> 23
        SpecialPermission.EXACT_ALARM -> 31
        SpecialPermission.INSTALL_UNKNOWN_APPS -> 26
        SpecialPermission.ALL_FILES_ACCESS -> 30
        SpecialPermission.WRITE_SETTINGS -> 23
        SpecialPermission.DO_NOT_DISTURB -> 23
        SpecialPermission.FULL_SCREEN_INTENT -> 34
        SpecialPermission.PICTURE_IN_PICTURE -> 26
        SpecialPermission.BACKGROUND_LOCATION -> 29
    }

    fun isApplicable(permission: SpecialPermission, sdk: Int): Boolean = sdk >= minSdk(permission)

    fun rolesAvailable(sdk: Int): Boolean = sdk >= 29

    fun appOpsCheck(sdk: Int): AppOpsCheck = if (sdk >= 29) AppOpsCheck.UNSAFE else AppOpsCheck.LEGACY

    /** Runs [granted] only when [permission] exists on [sdk]. */
    inline fun statusOf(permission: SpecialPermission, sdk: Int, granted: () -> Boolean): PermissionStatus =
        if (!isApplicable(permission, sdk)) PermissionStatus.NOT_APPLICABLE else PermissionStatus.of(granted())

    /**
     * Interprets an AppOps mode. `MODE_DEFAULT` means the op follows the
     * underlying manifest permission, so [permissionGranted] decides.
     */
    inline fun appOpsGranted(mode: Int, permissionGranted: () -> Boolean): Boolean = when (mode) {
        AppOpsManager.MODE_ALLOWED -> true
        AppOpsManager.MODE_DEFAULT -> permissionGranted()
        else -> false
    }
}
