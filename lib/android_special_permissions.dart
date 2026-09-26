/// One API for Android special-access permissions (usage access, overlay,
/// accessibility, notification listener, battery optimisations, exact
/// alarms, all-files access, default roles and more): status checks and
/// deep links into the right Settings screen.
///
/// Android only; on other platforms every status is
/// [SpecialPermissionStatus.notApplicable].
library;

export 'src/android_role.dart';
export 'src/android_special_permissions.dart';
export 'src/special_permission.dart';
export 'src/special_permission_exception.dart';
export 'src/special_permission_status.dart';
