package dev.abdeldjalilchougui.android_special_permissions

/** Special-access permissions, named as on the Dart side. */
enum class SpecialPermission(val wireName: String) {
    USAGE_ACCESS("usageAccess"),
    OVERLAY("overlay"),
    ACCESSIBILITY_SERVICE("accessibilityService"),
    NOTIFICATION_LISTENER("notificationListener"),
    IGNORE_BATTERY_OPTIMIZATIONS("ignoreBatteryOptimizations"),
    EXACT_ALARM("exactAlarm"),
    INSTALL_UNKNOWN_APPS("installUnknownApps"),
    ALL_FILES_ACCESS("allFilesAccess"),
    WRITE_SETTINGS("writeSettings"),
    DO_NOT_DISTURB("doNotDisturb"),
    FULL_SCREEN_INTENT("fullScreenIntent"),
    PICTURE_IN_PICTURE("pictureInPicture"),
    BACKGROUND_LOCATION("backgroundLocation");

    companion object {
        fun fromWire(name: String?): SpecialPermission? = values().firstOrNull { it.wireName == name }
    }
}

/** Result of a check, named as on the Dart side. */
enum class PermissionStatus(val wireName: String) {
    GRANTED("granted"),
    DENIED("denied"),
    NOT_APPLICABLE("notApplicable");

    companion object {
        fun of(granted: Boolean): PermissionStatus = if (granted) GRANTED else DENIED
    }
}

/** Default-app roles handled by RoleManager (API 29+). */
enum class Role(val wireName: String, val roleName: String) {
    DIALER("dialer", "android.app.role.DIALER"),
    SMS("sms", "android.app.role.SMS"),
    HOME("home", "android.app.role.HOME"),
    BROWSER("browser", "android.app.role.BROWSER"),
    ASSISTANT("assistant", "android.app.role.ASSISTANT");

    companion object {
        fun fromWire(name: String?): Role? = values().firstOrNull { it.wireName == name }
    }
}
