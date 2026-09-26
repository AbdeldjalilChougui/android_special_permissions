/// Default-app roles managed by Android's `RoleManager` (API 29+).
enum AndroidRole {
  /// Default phone app (`android.app.role.DIALER`).
  dialer('dialer'),

  /// Default SMS app (`android.app.role.SMS`).
  sms('sms'),

  /// Default home / launcher app (`android.app.role.HOME`).
  home('home'),

  /// Default browser (`android.app.role.BROWSER`).
  browser('browser'),

  /// Default digital assistant (`android.app.role.ASSISTANT`).
  assistant('assistant');

  const AndroidRole(this.wireName);

  /// Name used on the platform channel.
  final String wireName;
}
