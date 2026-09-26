/// Result of a special permission or role check.
enum SpecialPermissionStatus {
  /// The user has granted the access (or the app holds the role).
  granted,

  /// The access exists on this device but is not granted.
  denied,

  /// The device's API level (or hardware) has no such access to grant,
  /// so there is nothing for the user to toggle.
  notApplicable;

  /// Parses the value sent by the native side.
  ///
  /// Throws a [FormatException] for anything that is not one of the
  /// enum names.
  static SpecialPermissionStatus parse(Object? value) {
    for (final status in values) {
      if (status.name == value) return status;
    }
    throw FormatException('Unknown permission status', value);
  }

  /// Whether the access is granted.
  bool get isGranted => this == granted;

  /// Whether the access exists on this device (granted or denied).
  bool get isApplicable => this != notApplicable;
}
