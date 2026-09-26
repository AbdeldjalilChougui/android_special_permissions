import 'package:android_special_permissions/android_special_permissions.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel('test/android_special_permissions');
  final messenger =
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  late List<MethodCall> calls;
  late Object? Function(MethodCall call) reply;
  late AndroidSpecialPermissions plugin;

  setUp(() {
    calls = <MethodCall>[];
    reply = (_) => null;
    messenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      return reply(call);
    });
    plugin = AndroidSpecialPermissions(channel: channel);
  });

  tearDown(() {
    messenger.setMockMethodCallHandler(channel, null);
    debugDefaultTargetPlatformOverride = null;
  });

  group('wire names', () {
    test('every permission has a unique wire name', () {
      final names = SpecialPermission.values.map((p) => p.wireName).toSet();
      expect(names, hasLength(SpecialPermission.values.length));
    });

    test('permission wire names match the native side', () {
      expect(SpecialPermission.usageAccess.wireName, 'usageAccess');
      expect(SpecialPermission.overlay.wireName, 'overlay');
      expect(SpecialPermission.accessibilityService.wireName,
          'accessibilityService');
      expect(SpecialPermission.notificationListener.wireName,
          'notificationListener');
      expect(SpecialPermission.ignoreBatteryOptimizations.wireName,
          'ignoreBatteryOptimizations');
      expect(SpecialPermission.exactAlarm.wireName, 'exactAlarm');
      expect(
          SpecialPermission.installUnknownApps.wireName, 'installUnknownApps');
      expect(SpecialPermission.allFilesAccess.wireName, 'allFilesAccess');
      expect(SpecialPermission.writeSettings.wireName, 'writeSettings');
      expect(SpecialPermission.doNotDisturb.wireName, 'doNotDisturb');
      expect(SpecialPermission.fullScreenIntent.wireName, 'fullScreenIntent');
      expect(SpecialPermission.pictureInPicture.wireName, 'pictureInPicture');
      expect(
          SpecialPermission.backgroundLocation.wireName, 'backgroundLocation');
    });

    test('role wire names match the native side', () {
      expect(AndroidRole.dialer.wireName, 'dialer');
      expect(AndroidRole.sms.wireName, 'sms');
      expect(AndroidRole.home.wireName, 'home');
      expect(AndroidRole.browser.wireName, 'browser');
      expect(AndroidRole.assistant.wireName, 'assistant');
    });

    test('permissions that need a component are flagged', () {
      expect(
          SpecialPermission.accessibilityService.needsServiceClassName, isTrue);
      expect(SpecialPermission.notificationListener.needsServiceClassName,
          isFalse);
      expect(SpecialPermission.overlay.needsServiceClassName, isFalse);
    });
  });

  group('status parsing', () {
    test('parses each status string', () {
      expect(SpecialPermissionStatus.parse('granted'),
          SpecialPermissionStatus.granted);
      expect(SpecialPermissionStatus.parse('denied'),
          SpecialPermissionStatus.denied);
      expect(SpecialPermissionStatus.parse('notApplicable'),
          SpecialPermissionStatus.notApplicable);
    });

    test('rejects unknown or null values', () {
      expect(() => SpecialPermissionStatus.parse('maybe'),
          throwsA(isA<FormatException>()));
      expect(() => SpecialPermissionStatus.parse(null),
          throwsA(isA<FormatException>()));
    });

    test('convenience getters', () {
      expect(SpecialPermissionStatus.granted.isGranted, isTrue);
      expect(SpecialPermissionStatus.denied.isGranted, isFalse);
      expect(SpecialPermissionStatus.notApplicable.isGranted, isFalse);
      expect(SpecialPermissionStatus.notApplicable.isApplicable, isFalse);
      expect(SpecialPermissionStatus.denied.isApplicable, isTrue);
    });
  });

  group('status', () {
    test('encodes the permission and parses the reply', () async {
      reply = (_) => 'granted';
      final status = await plugin.status(SpecialPermission.overlay);
      expect(status, SpecialPermissionStatus.granted);
      expect(calls.single.method, 'status');
      expect(calls.single.arguments, <String, Object?>{
        'permission': 'overlay',
      });
    });

    test('sends the service class name for accessibility', () async {
      reply = (_) => 'denied';
      final status = await plugin.status(
        SpecialPermission.accessibilityService,
        serviceClassName: 'com.example.app.MyService',
      );
      expect(status, SpecialPermissionStatus.denied);
      expect(calls.single.arguments, <String, Object?>{
        'permission': 'accessibilityService',
        'serviceClassName': 'com.example.app.MyService',
      });
    });

    test('sends the optional listener class for notification listener',
        () async {
      reply = (_) => 'granted';
      await plugin.status(
        SpecialPermission.notificationListener,
        serviceClassName: '.Listener',
      );
      expect(calls.single.arguments, <String, Object?>{
        'permission': 'notificationListener',
        'serviceClassName': '.Listener',
      });
    });

    test('accessibility without a service class throws ArgumentError',
        () async {
      expect(
        () => plugin.status(SpecialPermission.accessibilityService),
        throwsArgumentError,
      );
      expect(calls, isEmpty);
    });

    test('an empty service class name is rejected', () async {
      expect(
        () => plugin.status(SpecialPermission.accessibilityService,
            serviceClassName: ''),
        throwsArgumentError,
      );
    });

    test('an unexpected reply surfaces as a FormatException', () async {
      reply = (_) => 42;
      expect(() => plugin.status(SpecialPermission.overlay),
          throwsA(isA<FormatException>()));
    });

    test('statuses queries every requested permission', () async {
      reply = (call) {
        final args = call.arguments as Map<Object?, Object?>;
        return args['permission'] == 'overlay' ? 'granted' : 'notApplicable';
      };
      final result = await plugin.statuses(const [
        SpecialPermission.overlay,
        SpecialPermission.fullScreenIntent,
      ]);
      expect(result, {
        SpecialPermission.overlay: SpecialPermissionStatus.granted,
        SpecialPermission.fullScreenIntent:
            SpecialPermissionStatus.notApplicable,
      });
      expect(calls, hasLength(2));
    });
  });

  group('openSettings', () {
    test('returns whether a screen was opened', () async {
      reply = (_) => true;
      expect(await plugin.openSettings(SpecialPermission.usageAccess), isTrue);
      expect(calls.single.method, 'openSettings');
      expect(calls.single.arguments, <String, Object?>{
        'permission': 'usageAccess',
      });
    });

    test('accessibility may omit the service class', () async {
      reply = (_) => false;
      expect(
        await plugin.openSettings(SpecialPermission.accessibilityService),
        isFalse,
      );
      expect(calls.single.arguments, <String, Object?>{
        'permission': 'accessibilityService',
      });
    });

    test('forwards the service class when given', () async {
      reply = (_) => true;
      await plugin.openSettings(SpecialPermission.accessibilityService,
          serviceClassName: 'a.B');
      expect(calls.single.arguments, <String, Object?>{
        'permission': 'accessibilityService',
        'serviceClassName': 'a.B',
      });
    });

    test('a null reply counts as not opened', () async {
      reply = (_) => null;
      expect(await plugin.openSettings(SpecialPermission.overlay), isFalse);
    });

    test('openAppSettings calls its own method', () async {
      reply = (_) => true;
      expect(await plugin.openAppSettings(), isTrue);
      expect(calls.single.method, 'openAppSettings');
    });
  });

  group('request', () {
    test('encodes the permission and returns the re-checked status', () async {
      reply = (_) => 'granted';
      final status = await plugin.request(SpecialPermission.exactAlarm);
      expect(status, SpecialPermissionStatus.granted);
      expect(calls.single.method, 'request');
      expect(calls.single.arguments, <String, Object?>{
        'permission': 'exactAlarm',
      });
    });

    test('accessibility request needs a service class', () async {
      expect(
        () => plugin.request(SpecialPermission.accessibilityService),
        throwsArgumentError,
      );
      expect(calls, isEmpty);
    });
  });

  group('roles', () {
    test('roleStatus encodes the role', () async {
      reply = (_) => 'denied';
      expect(await plugin.roleStatus(AndroidRole.sms),
          SpecialPermissionStatus.denied);
      expect(calls.single.method, 'roleStatus');
      expect(calls.single.arguments, <String, Object?>{'role': 'sms'});
    });

    test('requestRole encodes the role', () async {
      reply = (_) => 'granted';
      expect(await plugin.requestRole(AndroidRole.browser),
          SpecialPermissionStatus.granted);
      expect(calls.single.method, 'requestRole');
      expect(calls.single.arguments, <String, Object?>{'role': 'browser'});
    });
  });

  group('errors', () {
    void replyError(String code) {
      reply = (_) => throw PlatformException(code: code, message: 'boom');
    }

    test('activityNotAttached is mapped', () async {
      replyError('activityNotAttached');
      await expectLater(
        plugin.request(SpecialPermission.overlay),
        throwsA(isA<SpecialPermissionException>()
            .having((e) => e.code, 'code',
                SpecialPermissionErrorCode.activityNotAttached)
            .having((e) => e.message, 'message', 'boom')),
      );
    });

    test('notSupported is mapped', () async {
      replyError('notSupported');
      await expectLater(
        plugin.request(SpecialPermission.backgroundLocation),
        throwsA(isA<SpecialPermissionException>().having(
            (e) => e.code, 'code', SpecialPermissionErrorCode.notSupported)),
      );
    });

    test('requestInProgress is mapped', () async {
      replyError('requestInProgress');
      await expectLater(
        plugin.requestRole(AndroidRole.dialer),
        throwsA(isA<SpecialPermissionException>().having((e) => e.code, 'code',
            SpecialPermissionErrorCode.requestInProgress)),
      );
    });

    test('invalidArgument is mapped', () async {
      replyError('invalidArgument');
      await expectLater(
        plugin.openSettings(SpecialPermission.overlay),
        throwsA(isA<SpecialPermissionException>().having(
            (e) => e.code, 'code', SpecialPermissionErrorCode.invalidArgument)),
      );
    });

    test('unknown codes are mapped to unknown and keep the raw code', () async {
      replyError('somethingElse');
      await expectLater(
        plugin.status(SpecialPermission.overlay),
        throwsA(isA<SpecialPermissionException>()
            .having((e) => e.code, 'code', SpecialPermissionErrorCode.unknown)
            .having((e) => e.platformCode, 'platformCode', 'somethingElse')),
      );
    });

    test('toString mentions the code', () {
      const e = SpecialPermissionException(
          SpecialPermissionErrorCode.notSupported, 'nope');
      expect(e.toString(), contains('notSupported'));
      expect(e.toString(), contains('nope'));
    });
  });

  group('non-Android platforms', () {
    test('status is notApplicable without touching the channel', () async {
      debugDefaultTargetPlatformOverride = TargetPlatform.iOS;
      expect(await plugin.status(SpecialPermission.overlay),
          SpecialPermissionStatus.notApplicable);
      expect(await plugin.request(SpecialPermission.overlay),
          SpecialPermissionStatus.notApplicable);
      expect(await plugin.roleStatus(AndroidRole.sms),
          SpecialPermissionStatus.notApplicable);
      expect(await plugin.requestRole(AndroidRole.sms),
          SpecialPermissionStatus.notApplicable);
      expect(await plugin.openSettings(SpecialPermission.overlay), isFalse);
      expect(await plugin.openAppSettings(), isFalse);
      expect(calls, isEmpty);
    });
  });

  test('default constructor uses the plugin channel name', () {
    expect(AndroidSpecialPermissions.channelName,
        'dev.abdeldjalilchougui/android_special_permissions');
  });
}
