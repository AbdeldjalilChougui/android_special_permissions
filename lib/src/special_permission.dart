/// Android special-access permissions that are granted from a Settings
/// screen rather than the runtime permission dialog.
enum SpecialPermission {
  /// Usage access (`PACKAGE_USAGE_STATS`), checked through `AppOpsManager`.
  usageAccess('usageAccess'),

  /// Display over other apps (`SYSTEM_ALERT_WINDOW`), API 23+.
  overlay('overlay'),

  /// An accessibility service of this app is enabled.
  ///
  /// Status checks and requests need the service class name.
  accessibilityService('accessibilityService', needsServiceClassName: true),

  /// Notification listener access for this app.
  ///
  /// A listener class name is optional; it enables the per-listener
  /// Settings page on API 30+.
  notificationListener('notificationListener'),

  /// The app is exempt from battery optimisations (Doze), API 23+.
  ignoreBatteryOptimizations('ignoreBatteryOptimizations'),

  /// Schedule exact alarms (`SCHEDULE_EXACT_ALARM`), API 31+.
  exactAlarm('exactAlarm'),

  /// Install unknown apps (`REQUEST_INSTALL_PACKAGES`), API 26+.
  installUnknownApps('installUnknownApps'),

  /// All-files access (`MANAGE_EXTERNAL_STORAGE`), API 30+.
  allFilesAccess('allFilesAccess'),

  /// Modify system settings (`WRITE_SETTINGS`), API 23+.
  writeSettings('writeSettings'),

  /// Do Not Disturb access (`ACCESS_NOTIFICATION_POLICY`), API 23+.
  doNotDisturb('doNotDisturb'),

  /// Full-screen intent notifications (`USE_FULL_SCREEN_INTENT`), API 34+.
  fullScreenIntent('fullScreenIntent'),

  /// Picture-in-picture, checked through `AppOpsManager`, API 26+.
  pictureInPicture('pictureInPicture'),

  /// Background location (`ACCESS_BACKGROUND_LOCATION`), API 29+.
  ///
  /// Status only: it is a runtime permission, so request it with
  /// `permission_handler` after the foreground location permission.
  backgroundLocation('backgroundLocation');

  const SpecialPermission(this.wireName, {this.needsServiceClassName = false});

  /// Name used on the platform channel.
  final String wireName;

  /// Whether `status` and `request` require a service class name.
  final bool needsServiceClassName;
}
