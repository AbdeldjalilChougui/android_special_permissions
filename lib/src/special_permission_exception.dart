/// Why a call to the native side failed.
enum SpecialPermissionErrorCode {
  /// The call needs a foreground Activity and the plugin has none, or the
  /// Activity went away while waiting for the user.
  activityNotAttached,

  /// The operation is not supported, for example requesting background
  /// location, which belongs to `permission_handler`.
  notSupported,

  /// Another `request` or `requestRole` is still waiting for the user.
  requestInProgress,

  /// The native side rejected the arguments.
  invalidArgument,

  /// Any other native error; see [SpecialPermissionException.platformCode].
  unknown,
}

/// Error thrown by `AndroidSpecialPermissions` when the native side fails.
class SpecialPermissionException implements Exception {
  /// Creates an exception with a [code] and an optional [message].
  const SpecialPermissionException(this.code,
      [this.message, String? platformCode])
      : platformCode = platformCode ?? '';

  /// Maps a platform error code to a [SpecialPermissionException].
  factory SpecialPermissionException.fromPlatform(
      String code, String? message) {
    for (final value in SpecialPermissionErrorCode.values) {
      if (value != SpecialPermissionErrorCode.unknown && value.name == code) {
        return SpecialPermissionException(value, message, code);
      }
    }
    return SpecialPermissionException(
        SpecialPermissionErrorCode.unknown, message, code);
  }

  /// The error category.
  final SpecialPermissionErrorCode code;

  /// Human-readable detail from the native side, if any.
  final String? message;

  /// The raw error code sent by the platform, or an empty string.
  final String platformCode;

  @override
  String toString() => 'SpecialPermissionException(${code.name}'
      '${message == null ? '' : ': $message'})';
}
