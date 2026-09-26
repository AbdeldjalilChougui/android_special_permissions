# android_special_permissions

One API for the Android "special access" permissions that the runtime
permission dialog does not cover: check the status, open the right Settings
screen, or open it and wait for the user to come back with the new status.

[![pub package](https://img.shields.io/pub/v/android_special_permissions.svg)](https://pub.dev/packages/android_special_permissions)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

**Android only; iOS not supported.** On other platforms every status is
`notApplicable` and nothing is opened.

## Features

- `status()` for 13 special permissions, returning `granted`, `denied` or
  `notApplicable` (the device's API level has no such toggle).
- `openSettings()` opens the most specific screen available: package-scoped
  where Android supports it, then the general list, then App info, then
  Settings home. Every candidate intent is checked with `resolveActivity`
  before it is started.
- `request()` opens the screen, waits until your app resumes, checks again
  and returns the new status. It uses an `ActivityAware` binding and a
  lifecycle observer, survives configuration changes and fails cleanly with
  `activityNotAttached` if the activity goes away.
- Default-app roles (dialer, SMS, home, browser, assistant) through
  `RoleManager`: `roleStatus()` and `requestRole()`, which waits for the
  activity result.
- Accessibility-service detection that reads the short (`pkg/.Svc`) and long
  (`pkg/pkg.Svc`) component forms in `ENABLED_ACCESSIBILITY_SERVICES`.
- The plugin declares no permissions. Your app declares only what it uses.

## Install

```yaml
dependencies:
  android_special_permissions: ^0.1.0
```

The plugin's `minSdk` is 21. Declare in your app's `AndroidManifest.xml` the
permissions behind the accesses you use (see the table below). Without the
declaration, most Settings screens will not list your app.

## Usage

```dart
import 'package:android_special_permissions/android_special_permissions.dart';

final permissions = AndroidSpecialPermissions();

Future<void> ensureUsageAccess() async {
  final status = await permissions.status(SpecialPermission.usageAccess);
  if (status == SpecialPermissionStatus.denied) {
    // Opens "Usage access", waits for the user to return, re-checks.
    final after = await permissions.request(SpecialPermission.usageAccess);
    print('usage access: ${after.name}');
  }
}

Future<void> accessibility() async {
  // Your AccessibilityService class: fully qualified, or relative with '.'.
  const service = '.MyAccessibilityService';
  final status = await permissions.status(
    SpecialPermission.accessibilityService,
    serviceClassName: service,
  );
  if (!status.isGranted) {
    await permissions.request(
      SpecialPermission.accessibilityService,
      serviceClassName: service,
    );
  }
}

Future<void> batteryAndRoles() async {
  // Direct "allow" dialog if REQUEST_IGNORE_BATTERY_OPTIMIZATIONS is declared,
  // otherwise the list screen.
  await permissions.request(SpecialPermission.ignoreBatteryOptimizations);
  // The list of all apps, without the direct dialog.
  await permissions.openSettings(SpecialPermission.ignoreBatteryOptimizations);

  final sms = await permissions.roleStatus(AndroidRole.sms);
  if (sms == SpecialPermissionStatus.denied) {
    await permissions.requestRole(AndroidRole.sms);
  }
}

Future<void> handleErrors() async {
  try {
    await permissions.request(SpecialPermission.exactAlarm);
  } on SpecialPermissionException catch (e) {
    if (e.code == SpecialPermissionErrorCode.activityNotAttached) {
      // No foreground activity, or it was destroyed while waiting.
    }
    print('failed: $e');
  }
}
```

`request()` returns at once, without opening anything, when the status is
already `granted` or `notApplicable`. Only one `request()` or `requestRole()`
can wait at a time; a second one fails with `requestInProgress`.

## Permissions

The API levels are those where the plugin reports something other than
`notApplicable`. The Google Play notes summarise the policies as the author
understands them at the time of writing. The
[Play policy centre](https://support.google.com/googleplay/android-developer/topic/9858052)
is authoritative, so check it before you ship.

| Permission (`SpecialPermission`) | Manifest | Android | What the user sees | Google Play notes |
| --- | --- | --- | --- | --- |
| `usageAccess` | `PACKAGE_USAGE_STATS` | 5.0+ (API 21) | "Usage access" list; on API 29+ the app's own toggle when the device honours the package URI | The author knows of no dedicated Console form, but usage data falls under the User Data policy: show a prominent disclosure and get consent before sending the user there. |
| `overlay` | `SYSTEM_ALERT_WINDOW` | 6.0+ (API 23) | "Display over other apps". Android 11+ ignores the package URI and shows the list | No declaration form. Overlays must not obscure system UI or be used deceptively. |
| `accessibilityService` | an `AccessibilityService` guarded by `BIND_ACCESSIBILITY_SERVICE` | all | "Accessibility" settings. The service's own page (API 33+) is used only by apps holding the system `OPEN_ACCESSIBILITY_DETAILS_SETTINGS` permission. Enabling shows a "full control" warning. On Android 13+, sideloaded apps first need "Allow restricted settings" in App info. | **Declaration required** (Accessibility API form in Play Console) plus prominent disclosure, unless the app is an accessibility tool (`isAccessibilityTool`). |
| `notificationListener` | a `NotificationListenerService` | all | "Notification access" list; on API 30+ the listener's own page when you pass `serviceClassName`. The restricted-settings gate also applies to sideloaded apps on Android 13+. | The author knows of no dedicated form; notification content is user data (User Data policy). |
| `ignoreBatteryOptimizations` | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (for the direct dialog) | 6.0+ (API 23) | `request()`: a system dialog asking to let the app always run in the background. `openSettings()`: the "Battery optimization" list, where the user must switch the filter to all apps. | **Restricted.** Play allows the direct dialog only when Doze breaks the app's core function (see "acceptable use cases for exemptions" in Android's Doze documentation). Otherwise use `openSettings()`, which needs no permission. |
| `exactAlarm` | `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM` | 12+ (API 31) | "Alarms & reminders" toggle for the app. Denied by default on Android 14+ for new installs. | `SCHEDULE_EXACT_ALARM` is user-granted. `USE_EXACT_ALARM` is auto-granted but **needs a declaration** and is limited to alarm-clock and calendar apps. |
| `installUnknownApps` | `REQUEST_INSTALL_PACKAGES` | 8.0+ (API 26) | "Install unknown apps" toggle for the app | **Declaration required**; allowed only when installing packages is a core function. |
| `allFilesAccess` | `MANAGE_EXTERNAL_STORAGE` | 11+ (API 30) | "All files access" toggle for the app | **Declaration required** (All files access form); only for file managers, backup, antivirus and similar core uses. |
| `writeSettings` | `WRITE_SETTINGS` | 6.0+ (API 23) | "Modify system settings" toggle | No declaration form. |
| `doNotDisturb` | `ACCESS_NOTIFICATION_POLICY` | 6.0+ (API 23) | "Do Not Disturb access" list | No declaration form. |
| `fullScreenIntent` | `USE_FULL_SCREEN_INTENT` | 14+ (API 34) | "Full screen notifications" toggle for the app, otherwise the app's notification settings | **Declaration required** on Play. Auto-granted only to calling and alarm apps; other apps must send the user to Settings. |
| `pictureInPicture` | `android:supportsPictureInPicture` on an activity | 8.0+ (API 26), and the device must support PiP | "Picture-in-picture" special access. This is a non-public Settings action, so it often falls back to App info | No declaration form. |
| `backgroundLocation` | `ACCESS_BACKGROUND_LOCATION` | 10+ (API 29) | Status only, see below | **Declaration required** (location permissions form, with a video) plus prominent disclosure. |

| Role (`AndroidRole`) | Android | What the user sees | Google Play notes |
| --- | --- | --- | --- |
| `dialer`, `sms` | 10+ (API 29) | A system dialog asking to make the app the default phone or SMS app | Needed before using the restricted SMS and Call Log permissions, which have their own **declaration form**. The app must implement the handler's required components. |
| `home`, `browser`, `assistant` | 10+ (API 29) | A system dialog, or nothing if the app does not qualify for the role | No declaration form. The app must declare the matching intent filters, otherwise Android refuses the role. |

### Background location: the two-step flow

`ACCESS_BACKGROUND_LOCATION` is a runtime permission, so this plugin only
reports its status (`SpecialPermission.backgroundLocation`). `request()` and
`openSettings()` throw `SpecialPermissionErrorCode.notSupported`. Request it
with [`permission_handler`](https://pub.dev/packages/permission_handler):

1. Ask for foreground location (`Permission.locationWhenInUse`) first.
2. Once that is granted, explain why the app needs location all the time,
   then request `Permission.locationAlways`. On Android 11+ this shows no
   dialog: it sends the user to the app's location page, where they pick
   "Allow all the time".

On API 21-28 there is no separate background permission, so the status is
`notApplicable`; background access follows the foreground grant.

## Limitations

- Below each permission's API level, `notApplicable` means "no toggle
  exists". On API 21-22 the permissions behind `overlay`, `writeSettings`
  and similar are granted at install time when declared; this plugin does
  not report that.
- A permission that is not declared in the manifest usually reads as
  `denied`, and its Settings screen may not list the app.
- `request()` completes on the first resume after the app was paused. If the
  user comes back by another route, you still get the status at that moment.
- The accessibility details page (`android.settings.ACCESSIBILITY_DETAILS_SETTINGS`)
  requires the signature/installer permission `OPEN_ACCESSIBILITY_DETAILS_SETTINGS`,
  so ordinary apps get the general Accessibility screen; the plugin only tries the
  details page when the app holds that permission. The picture-in-picture screen
  is not a public SDK action; it is tried only when it resolves, and if Settings
  refuses to start it the plugin falls back to the next screen.
- Android kills the app process when some accesses are revoked (seen for
  "Install unknown apps" on the emulator; Android documents the same for
  "Alarms & reminders"). A pending `request()` then never completes; check the
  status again on the next launch.
- OEM Settings apps vary. The fallback chain always ends at App info and then
  Settings home, but the most specific screen may be missing on some devices.
- Roles need API 29+. The older dialer and SMS default-app APIs are not
  wrapped.
- The picture-in-picture status is the AppOps state. It does not check that
  one of your activities declares `supportsPictureInPicture`.
- Tests cover the Dart API (mocked `MethodChannel`) and the pure Kotlin logic
  (component parsing, API-level rules, intent fallback order, resume
  tracking, argument errors) with JUnit. The example app also has an
  emulator integration test (`example/integration_test/`, run with
  `dart run tool/run_on_device.dart <applicationId>` from `example/`) that
  flips each access with adb (`appops`, `cmd notification`, `deviceidle`,
  `settings`, `pm grant`) and checks every status, every Settings screen and
  the `request()` round trip.

**Tested on:** Android Emulator, Pixel 10 system image (`sdk_gphone16k_x86_64`),
API 37. Not yet tested on a physical phone or OEM Settings apps.

## License

MIT. See [LICENSE](LICENSE).

Made by [Abdeldjalil Chougui](https://abdeldjalilchougui.biz/).
