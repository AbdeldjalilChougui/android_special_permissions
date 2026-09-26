package dev.abdeldjalilchougui.android_special_permissions

import android.provider.Settings

/** A Settings intent described as plain data, so the fallback order can be unit tested. */
data class IntentSpec(
    val action: String,
    val data: String? = null,
    val extras: Map<String, String> = emptyMap(),
)

/** Chooses the Settings screen for each permission, most specific first. */
object SettingsIntents {
    /**
     * Per-service accessibility page. Settings only starts it for callers holding
     * [PERMISSION_OPEN_ACCESSIBILITY_DETAILS_SETTINGS], so it is offered only then.
     */
    const val ACTION_ACCESSIBILITY_DETAILS_SETTINGS = "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"

    /** Signature|installer permission required to start [ACTION_ACCESSIBILITY_DETAILS_SETTINGS]. */
    const val PERMISSION_OPEN_ACCESSIBILITY_DETAILS_SETTINGS =
        "android.permission.OPEN_ACCESSIBILITY_DETAILS_SETTINGS"

    /** Same value as `Intent.EXTRA_COMPONENT_NAME`; the details page reads it as a flattened string. */
    const val EXTRA_COMPONENT_NAME = "android.intent.extra.COMPONENT_NAME"

    /** Picture-in-picture special access list; not in the public SDK, so always resolve-checked. */
    const val ACTION_PICTURE_IN_PICTURE_SETTINGS = "android.settings.PICTURE_IN_PICTURE_SETTINGS"

    /** API level from which the accessibility details page is used. */
    const val ACCESSIBILITY_DETAILS_MIN_SDK = 33

    enum class Purpose {
        /** Open the screen and return at once. */
        OPEN_SETTINGS,

        /** Open the screen and wait for the user; may use a direct dialog. */
        REQUEST,
    }

    /** Last-resort screens: this app's info page, then Settings home. */
    fun appDetails(packageName: String): List<IntentSpec> = listOf(
        IntentSpec(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(packageName)),
        IntentSpec(Settings.ACTION_SETTINGS),
    )

    /**
     * Candidate intents for [permission] on [sdk], most specific first.
     * Empty for background location, which this plugin does not request.
     *
     * [batteryDialogDeclared] tells whether the app declares
     * `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, without which the direct
     * dialog does nothing.
     *
     * [accessibilityDetailsAllowed] tells whether the app holds
     * [PERMISSION_OPEN_ACCESSIBILITY_DETAILS_SETTINGS]; ordinary apps do not.
     */
    fun candidates(
        permission: SpecialPermission,
        sdk: Int,
        packageName: String,
        serviceClassName: String?,
        purpose: Purpose,
        batteryDialogDeclared: Boolean = false,
        accessibilityDetailsAllowed: Boolean = false,
    ): List<IntentSpec> {
        val pkg = packageUri(packageName)
        val specific: List<IntentSpec> = when (permission) {
            SpecialPermission.BACKGROUND_LOCATION -> return emptyList()
            SpecialPermission.USAGE_ACCESS -> buildList {
                if (sdk >= 29) add(IntentSpec(Settings.ACTION_USAGE_ACCESS_SETTINGS, pkg))
                add(IntentSpec(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            SpecialPermission.OVERLAY -> packageThenPlain(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkg)
            SpecialPermission.ACCESSIBILITY_SERVICE -> buildList {
                if (sdk >= ACCESSIBILITY_DETAILS_MIN_SDK && serviceClassName != null && accessibilityDetailsAllowed) {
                    add(
                        IntentSpec(
                            ACTION_ACCESSIBILITY_DETAILS_SETTINGS,
                            extras = mapOf(
                                EXTRA_COMPONENT_NAME to AccessibilityServices.flatten(packageName, serviceClassName),
                            ),
                        ),
                    )
                }
                add(IntentSpec(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            SpecialPermission.NOTIFICATION_LISTENER -> buildList {
                if (sdk >= 30 && serviceClassName != null) {
                    add(
                        IntentSpec(
                            Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS,
                            extras = mapOf(
                                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME to
                                    AccessibilityServices.flatten(packageName, serviceClassName),
                            ),
                        ),
                    )
                }
                add(IntentSpec(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            SpecialPermission.IGNORE_BATTERY_OPTIMIZATIONS -> buildList {
                if (purpose == Purpose.REQUEST && batteryDialogDeclared) {
                    add(IntentSpec(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg))
                }
                add(IntentSpec(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
            SpecialPermission.EXACT_ALARM -> packageThenPlain(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg)
            SpecialPermission.INSTALL_UNKNOWN_APPS -> packageThenPlain(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, pkg)
            SpecialPermission.ALL_FILES_ACCESS -> listOf(
                IntentSpec(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, pkg),
                IntentSpec(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
            )
            SpecialPermission.WRITE_SETTINGS -> packageThenPlain(Settings.ACTION_MANAGE_WRITE_SETTINGS, pkg)
            SpecialPermission.DO_NOT_DISTURB -> listOf(IntentSpec(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            SpecialPermission.FULL_SCREEN_INTENT -> listOf(
                IntentSpec(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg),
                IntentSpec(
                    Settings.ACTION_APP_NOTIFICATION_SETTINGS,
                    extras = mapOf(Settings.EXTRA_APP_PACKAGE to packageName),
                ),
            )
            SpecialPermission.PICTURE_IN_PICTURE -> packageThenPlain(ACTION_PICTURE_IN_PICTURE_SETTINGS, pkg)
        }
        return specific + appDetails(packageName)
    }

    /**
     * Returns the first candidate accepted by [accept], or null. [accept]
     * is typically "resolves and starts"; probing stops at the first match.
     */
    inline fun choose(candidates: List<IntentSpec>, accept: (IntentSpec) -> Boolean): IntentSpec? =
        candidates.firstOrNull(accept)

    private fun packageUri(packageName: String) = "package:$packageName"

    private fun packageThenPlain(action: String, pkg: String) = listOf(IntentSpec(action, pkg), IntentSpec(action))
}
