// Emulator integration test: calls the real plugin on a device and uses adb
// (through tool/run_on_device.dart) to change each access before asserting.
//
// Run from example/:
//   dart run tool/run_on_device.dart \
//     dev.abdeldjalilchougui.android_special_permissions_example emulator-5554

import 'package:android_special_permissions/android_special_permissions.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

import 'adb_bridge.dart';

const _pkg = 'dev.abdeldjalilchougui.android_special_permissions_example';
const _a11y = '.ExampleAccessibilityService';
const _listener = '.ExampleNotificationListener';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();
  final adb = AdbBridge(_pkg);
  final plugin = AndroidSpecialPermissions();

  Future<SpecialPermissionStatus> status(SpecialPermission p) =>
      plugin.status(p,
          serviceClassName: switch (p) {
            SpecialPermission.accessibilityService => _a11y,
            SpecialPermission.notificationListener => _listener,
            _ => null,
          });

  Future<SpecialPermissionStatus> becomes(
          SpecialPermission p, SpecialPermissionStatus expected) =>
      eventually(() => status(p), (s) => s == expected);

  Future<String> topActivity() => adb.topResumedActivity();

  /// Presses back until this app is on top again.
  Future<void> backToApp() async {
    if (!await adb.backTo(_pkg)) {
      expect(await topActivity(), contains(_pkg));
    }
  }

  Future<void> appops(String op, String mode) =>
      adb.shell('appops set $_pkg $op $mode');

  testWidgets('app starts', (tester) async {
    await tester.pumpWidget(const SizedBox());
    expect(await adb.shell('getprop ro.build.version.sdk'), contains('37'));
  });

  testWidgets('every status returns a value on a fresh install',
      (tester) async {
    await adb.shell('appops reset $_pkg');
    final all = <SpecialPermission, SpecialPermissionStatus>{};
    for (final p in SpecialPermission.values) {
      all[p] = await status(p);
    }
    debugPrint('fresh statuses: $all');
    // Everything exists on API 37, so nothing is notApplicable except
    // picture-in-picture when the device lacks the feature.
    for (final p in SpecialPermission.values) {
      if (p == SpecialPermission.pictureInPicture) continue;
      expect(all[p], isNot(SpecialPermissionStatus.notApplicable),
          reason: '$p');
    }
    for (final p in [
      SpecialPermission.usageAccess,
      SpecialPermission.overlay,
      SpecialPermission.accessibilityService,
      SpecialPermission.notificationListener,
      SpecialPermission.installUnknownApps,
      SpecialPermission.allFilesAccess,
      SpecialPermission.writeSettings,
      SpecialPermission.doNotDisturb,
      SpecialPermission.backgroundLocation,
    ]) {
      expect(all[p], SpecialPermissionStatus.denied, reason: '$p');
    }
  });

  testWidgets('appops-backed accesses follow adb appops', (tester) async {
    const cases = {
      SpecialPermission.overlay: 'SYSTEM_ALERT_WINDOW',
      SpecialPermission.usageAccess: 'GET_USAGE_STATS',
      SpecialPermission.writeSettings: 'WRITE_SETTINGS',
      SpecialPermission.fullScreenIntent: 'USE_FULL_SCREEN_INTENT',
    };
    for (final entry in cases.entries) {
      await appops(entry.value, 'allow');
      expect(await becomes(entry.key, SpecialPermissionStatus.granted),
          SpecialPermissionStatus.granted,
          reason: '${entry.key} after allow');
      await appops(entry.value, 'deny');
      expect(await becomes(entry.key, SpecialPermissionStatus.denied),
          SpecialPermissionStatus.denied,
          reason: '${entry.key} after deny');
    }
  });

  // Android kills the app when these three are revoked ("REQUEST_INSTALL_PACKAGES
  // changed" in logcat), so only the grant direction can be observed in-process.
  testWidgets('grant-only appops become granted', (tester) async {
    const cases = {
      SpecialPermission.installUnknownApps: 'REQUEST_INSTALL_PACKAGES',
      SpecialPermission.allFilesAccess: 'MANAGE_EXTERNAL_STORAGE',
      SpecialPermission.exactAlarm: 'SCHEDULE_EXACT_ALARM',
    };
    for (final entry in cases.entries) {
      expect(await status(entry.key), SpecialPermissionStatus.denied,
          reason: '${entry.key} before allow');
      await appops(entry.value, 'allow');
      expect(await becomes(entry.key, SpecialPermissionStatus.granted),
          SpecialPermissionStatus.granted,
          reason: '${entry.key} after allow');
    }
  });

  testWidgets('picture-in-picture follows its appop', (tester) async {
    if (await status(SpecialPermission.pictureInPicture) ==
        SpecialPermissionStatus.notApplicable) {
      debugPrint('device has no picture-in-picture feature');
      return;
    }
    await appops('PICTURE_IN_PICTURE', 'deny');
    expect(
        await becomes(
            SpecialPermission.pictureInPicture, SpecialPermissionStatus.denied),
        SpecialPermissionStatus.denied);
    await appops('PICTURE_IN_PICTURE', 'default');
    expect(
        await becomes(SpecialPermission.pictureInPicture,
            SpecialPermissionStatus.granted),
        SpecialPermissionStatus.granted);
  });

  testWidgets('battery optimisation follows the deviceidle allowlist',
      (tester) async {
    await adb.shell('dumpsys deviceidle whitelist +$_pkg');
    expect(
        await becomes(SpecialPermission.ignoreBatteryOptimizations,
            SpecialPermissionStatus.granted),
        SpecialPermissionStatus.granted);
    await adb.shell('dumpsys deviceidle whitelist -$_pkg');
    expect(
        await becomes(SpecialPermission.ignoreBatteryOptimizations,
            SpecialPermissionStatus.denied),
        SpecialPermissionStatus.denied);
  });

  testWidgets('do not disturb and notification listener follow cmd',
      (tester) async {
    await adb.shell('cmd notification allow_dnd $_pkg');
    expect(
        await becomes(
            SpecialPermission.doNotDisturb, SpecialPermissionStatus.granted),
        SpecialPermissionStatus.granted);
    await adb.shell('cmd notification disallow_dnd $_pkg');
    expect(
        await becomes(
            SpecialPermission.doNotDisturb, SpecialPermissionStatus.denied),
        SpecialPermissionStatus.denied);

    await adb.shell('cmd notification allow_listener $_pkg/$_pkg$_listener');
    expect(
        await becomes(SpecialPermission.notificationListener,
            SpecialPermissionStatus.granted),
        SpecialPermissionStatus.granted);
    await adb.shell('cmd notification disallow_listener $_pkg/$_pkg$_listener');
    expect(
        await becomes(SpecialPermission.notificationListener,
            SpecialPermissionStatus.denied),
        SpecialPermissionStatus.denied);
  });

  testWidgets('accessibility service follows the secure setting',
      (tester) async {
    final before =
        (await adb.shell('settings get secure enabled_accessibility_services'))
            .trim();
    try {
      await adb.shell(
          'settings put secure enabled_accessibility_services $_pkg/$_a11y');
      expect(
          await becomes(SpecialPermission.accessibilityService,
              SpecialPermissionStatus.granted),
          SpecialPermissionStatus.granted);
      // The fully qualified form must match too.
      expect(
          await plugin.status(SpecialPermission.accessibilityService,
              serviceClassName: '$_pkg$_a11y'),
          SpecialPermissionStatus.granted);
    } finally {
      if (before == 'null' || before.isEmpty) {
        await adb
            .shell('settings delete secure enabled_accessibility_services');
      } else {
        await adb.shell(
            "settings put secure enabled_accessibility_services '$before'");
      }
    }
    expect(
        await becomes(SpecialPermission.accessibilityService,
            SpecialPermissionStatus.denied),
        SpecialPermissionStatus.denied);
  });

  testWidgets('background location follows pm grant', (tester) async {
    await adb.shell('pm grant $_pkg android.permission.ACCESS_FINE_LOCATION');
    await adb
        .shell('pm grant $_pkg android.permission.ACCESS_BACKGROUND_LOCATION');
    expect(
        await becomes(SpecialPermission.backgroundLocation,
            SpecialPermissionStatus.granted),
        SpecialPermissionStatus.granted);
    await expectLater(plugin.request(SpecialPermission.backgroundLocation),
        throwsA(isA<SpecialPermissionException>()));
  });

  testWidgets('openSettings opens a Settings activity for each access',
      (tester) async {
    final since = '${(await adb.shell('date +%s')).trim()}.000';
    for (final p in SpecialPermission.values) {
      if (p == SpecialPermission.backgroundLocation) continue;
      final opened = await plugin.openSettings(p,
          serviceClassName: switch (p) {
            SpecialPermission.accessibilityService => _a11y,
            SpecialPermission.notificationListener => _listener,
            _ => null,
          });
      expect(opened, isTrue, reason: '$p');
      final top = await eventually(
          topActivity, (t) => t.contains('com.android.settings'));
      debugPrint('openSettings($p) -> $top');
      expect(top, contains('settings'), reason: '$p');
      await backToApp();
    }
    // No candidate should be refused by the system: the accessibility
    // details page is only tried by apps allowed to open it.
    final log = await adb.shell("logcat -d -T $since -s ActivityTaskManager:W");
    expect(
        log
            .split('\n')
            .where((l) => l.contains('Permission Denial') && l.contains(_pkg)),
        isEmpty);
    expect(await plugin.openAppSettings(), isTrue);
    final top = await eventually(
        topActivity, (t) => t.contains('com.android.settings'));
    expect(top, contains('settings'));
    await backToApp();
  });

  testWidgets('request waits for the user and returns the new status',
      (tester) async {
    await appops('SYSTEM_ALERT_WINDOW', 'deny');
    final pending = plugin.request(SpecialPermission.overlay);
    final top = await eventually(
        topActivity, (t) => t.contains('com.android.settings'));
    expect(top, contains('settings'));
    // Play the user: flip the toggle, then come back.
    await appops('SYSTEM_ALERT_WINDOW', 'allow');
    await backToApp();
    expect(await pending.timeout(const Duration(seconds: 15)),
        SpecialPermissionStatus.granted);

    // Already granted: returns at once without opening anything.
    expect(await plugin.request(SpecialPermission.overlay),
        SpecialPermissionStatus.granted);
    expect(await topActivity(), contains(_pkg));
    await appops('SYSTEM_ALERT_WINDOW', 'deny');
  });

  testWidgets('role status is reported for every role', (tester) async {
    for (final role in AndroidRole.values) {
      final s = await plugin.roleStatus(role);
      debugPrint('roleStatus($role) = $s');
      // The example qualifies for no role, so it never holds one.
      expect(s, isNot(SpecialPermissionStatus.granted), reason: '$role');
    }
    // Browser and home are available on the stock emulator image.
    expect(await plugin.roleStatus(AndroidRole.browser),
        SpecialPermissionStatus.denied);
    expect(await plugin.roleStatus(AndroidRole.home),
        SpecialPermissionStatus.denied);
  });

  testWidgets('requestRole for an unqualified app returns without hanging',
      (tester) async {
    final pending = plugin.requestRole(AndroidRole.browser);
    await Future<void>.delayed(const Duration(seconds: 2));
    // If the system showed a dialog, dismiss it like a user would.
    if (!(await topActivity()).contains(_pkg)) await backToApp();
    expect(await pending.timeout(const Duration(seconds: 15)),
        SpecialPermissionStatus.denied);
  });
}
