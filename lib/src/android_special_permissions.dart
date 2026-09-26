import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'android_role.dart';
import 'special_permission.dart';
import 'special_permission_exception.dart';
import 'special_permission_status.dart';

/// Checks Android special-access permissions and opens the Settings screen
/// where the user grants them.
///
/// On platforms other than Android every status is
/// [SpecialPermissionStatus.notApplicable] and nothing is opened.
class AndroidSpecialPermissions {
  /// Creates the API. A custom [channel] is only useful in tests.
  AndroidSpecialPermissions({MethodChannel? channel})
      : _channel = channel ?? const MethodChannel(channelName);

  /// Name of the platform channel used by the plugin.
  static const String channelName =
      'dev.abdeldjalilchougui/android_special_permissions';

  final MethodChannel _channel;

  static bool get _isAndroid =>
      !kIsWeb && defaultTargetPlatform == TargetPlatform.android;

  /// Returns the current status of [permission].
  ///
  /// [SpecialPermission.accessibilityService] needs [serviceClassName]: the
  /// fully qualified class name of your `AccessibilityService`, or a name
  /// starting with `.` relative to the app package. For
  /// [SpecialPermission.notificationListener] it is optional and ignored
  /// by the check (the check is per package).
  Future<SpecialPermissionStatus> status(
    SpecialPermission permission, {
    String? serviceClassName,
  }) async {
    final args = _args(permission, serviceClassName, required: true);
    if (!_isAndroid) return SpecialPermissionStatus.notApplicable;
    return SpecialPermissionStatus.parse(await _invoke('status', args));
  }

  /// Returns the status of each permission in [permissions].
  ///
  /// [SpecialPermission.accessibilityService] cannot be part of the list,
  /// because it needs a service class name; use [status] for it.
  Future<Map<SpecialPermission, SpecialPermissionStatus>> statuses(
    Iterable<SpecialPermission> permissions,
  ) async {
    final result = <SpecialPermission, SpecialPermissionStatus>{};
    for (final permission in permissions) {
      result[permission] = await status(permission);
    }
    return result;
  }

  /// Opens the most specific Settings screen for [permission] and returns
  /// immediately.
  ///
  /// Returns `true` when a screen was opened, `false` when no Settings
  /// activity on the device handles any of the candidate intents.
  ///
  /// For [SpecialPermission.ignoreBatteryOptimizations] this opens the
  /// list of all apps' battery optimisation settings; [request] opens the
  /// direct "allow" dialog instead.
  Future<bool> openSettings(
    SpecialPermission permission, {
    String? serviceClassName,
  }) async {
    final args = _args(permission, serviceClassName, required: false);
    if (!_isAndroid) return false;
    return await _invoke('openSettings', args) == true;
  }

  /// Opens the Settings screen for [permission], waits until the app is
  /// resumed, then checks again and returns the new status.
  ///
  /// When the status is already granted or not applicable it is returned
  /// without opening anything. Throws a [SpecialPermissionException] with
  /// [SpecialPermissionErrorCode.notSupported] for
  /// [SpecialPermission.backgroundLocation].
  Future<SpecialPermissionStatus> request(
    SpecialPermission permission, {
    String? serviceClassName,
  }) async {
    final args = _args(permission, serviceClassName, required: true);
    if (!_isAndroid) return SpecialPermissionStatus.notApplicable;
    return SpecialPermissionStatus.parse(await _invoke('request', args));
  }

  /// Opens this app's "App info" page in Settings.
  Future<bool> openAppSettings() async {
    if (!_isAndroid) return false;
    return await _invoke('openAppSettings', null) == true;
  }

  /// Whether this app holds [role]. Not applicable below API 29 or when
  /// the device does not offer the role.
  Future<SpecialPermissionStatus> roleStatus(AndroidRole role) async {
    if (!_isAndroid) return SpecialPermissionStatus.notApplicable;
    return SpecialPermissionStatus.parse(
        await _invoke('roleStatus', <String, Object?>{'role': role.wireName}));
  }

  /// Shows the system dialog asking the user to make this app the holder
  /// of [role], then returns the resulting status.
  ///
  /// The app must meet the role's requirements (for example declare the
  /// matching intent filters), otherwise Android denies it silently.
  Future<SpecialPermissionStatus> requestRole(AndroidRole role) async {
    if (!_isAndroid) return SpecialPermissionStatus.notApplicable;
    return SpecialPermissionStatus.parse(
        await _invoke('requestRole', <String, Object?>{'role': role.wireName}));
  }

  Map<String, Object?> _args(
    SpecialPermission permission,
    String? serviceClassName, {
    required bool required,
  }) {
    if (serviceClassName != null && serviceClassName.isEmpty) {
      throw ArgumentError.value(
          serviceClassName, 'serviceClassName', 'must not be empty');
    }
    if (required &&
        permission.needsServiceClassName &&
        serviceClassName == null) {
      throw ArgumentError.notNull('serviceClassName');
    }
    return <String, Object?>{
      'permission': permission.wireName,
      if (serviceClassName != null) 'serviceClassName': serviceClassName,
    };
  }

  Future<Object?> _invoke(String method, Object? args) async {
    try {
      return await _channel.invokeMethod<Object?>(method, args);
    } on PlatformException catch (e) {
      throw SpecialPermissionException.fromPlatform(e.code, e.message);
    }
  }
}
