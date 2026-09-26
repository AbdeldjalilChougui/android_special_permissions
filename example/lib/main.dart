import 'package:android_special_permissions/android_special_permissions.dart';
import 'package:flutter/material.dart';

/// Class names of the do-nothing services declared in the example manifest.
const _accessibilityService = '.ExampleAccessibilityService';
const _notificationListener = '.ExampleNotificationListener';

void main() => runApp(const ExampleApp());

/// Lists every special permission with its status and buttons to open it.
class ExampleApp extends StatelessWidget {
  /// Creates the example app.
  const ExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Special permissions',
      theme: ThemeData(colorSchemeSeed: Colors.teal, useMaterial3: true),
      home: const PermissionsPage(),
    );
  }
}

/// The single page of the example.
class PermissionsPage extends StatefulWidget {
  /// Creates the page.
  const PermissionsPage({super.key});

  @override
  State<PermissionsPage> createState() => _PermissionsPageState();
}

class _PermissionsPageState extends State<PermissionsPage>
    with WidgetsBindingObserver {
  final _permissions = AndroidSpecialPermissions();
  final _statuses = <SpecialPermission, SpecialPermissionStatus>{};
  final _roles = <AndroidRole, SpecialPermissionStatus>{};

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _refresh();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // After "Open", the user may change a toggle and come back.
    if (state == AppLifecycleState.resumed) _refresh();
  }

  String? _serviceFor(SpecialPermission permission) => switch (permission) {
        SpecialPermission.accessibilityService => _accessibilityService,
        SpecialPermission.notificationListener => _notificationListener,
        _ => null,
      };

  Future<void> _refresh() async {
    for (final permission in SpecialPermission.values) {
      _statuses[permission] = await _permissions.status(permission,
          serviceClassName: _serviceFor(permission));
    }
    for (final role in AndroidRole.values) {
      _roles[role] = await _permissions.roleStatus(role);
    }
    if (mounted) setState(() {});
  }

  void _say(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
        .showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _run(Future<void> Function() action) async {
    try {
      await action();
    } on SpecialPermissionException catch (e) {
      _say(e.message ?? e.code.name);
    }
    await _refresh();
  }

  Future<void> _open(SpecialPermission permission) => _run(() async {
        final opened = await _permissions.openSettings(permission,
            serviceClassName: _serviceFor(permission));
        if (!opened) _say('No Settings screen found');
      });

  Future<void> _request(SpecialPermission permission) => _run(() async {
        final status = await _permissions.request(permission,
            serviceClassName: _serviceFor(permission));
        _say('${permission.name}: ${status.name}');
      });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Special permissions'),
        actions: [
          IconButton(
            tooltip: 'App info',
            icon: const Icon(Icons.info_outline),
            onPressed: () => _run(_permissions.openAppSettings),
          ),
          IconButton(
            tooltip: 'Refresh',
            icon: const Icon(Icons.refresh),
            onPressed: _refresh,
          ),
        ],
      ),
      body: ListView(
        children: [
          for (final permission in SpecialPermission.values)
            _PermissionTile(
              title: permission.name,
              status: _statuses[permission],
              // Background location is status-only: request it with
              // permission_handler.
              onOpen: permission == SpecialPermission.backgroundLocation
                  ? null
                  : () => _open(permission),
              onRequest: permission == SpecialPermission.backgroundLocation
                  ? null
                  : () => _request(permission),
            ),
          const Divider(),
          const ListTile(title: Text('Default-app roles')),
          for (final role in AndroidRole.values)
            _PermissionTile(
              title: role.name,
              status: _roles[role],
              onRequest: () => _run(() => _permissions.requestRole(role)),
            ),
        ],
      ),
    );
  }
}

class _PermissionTile extends StatelessWidget {
  const _PermissionTile({
    required this.title,
    required this.status,
    this.onOpen,
    this.onRequest,
  });

  final String title;
  final SpecialPermissionStatus? status;
  final VoidCallback? onOpen;
  final VoidCallback? onRequest;

  @override
  Widget build(BuildContext context) {
    final applicable = status?.isApplicable ?? false;
    final (icon, color) = switch (status) {
      SpecialPermissionStatus.granted => (Icons.check_circle, Colors.green),
      SpecialPermissionStatus.denied => (Icons.cancel, Colors.red),
      SpecialPermissionStatus.notApplicable => (Icons.block, Colors.grey),
      null => (Icons.hourglass_empty, Colors.grey),
    };
    return ListTile(
      leading: Icon(icon, color: color),
      title: Text(title),
      subtitle: Text(status?.name ?? '...'),
      trailing: Wrap(
        spacing: 4,
        children: [
          if (onOpen != null)
            TextButton(
              onPressed: applicable ? onOpen : null,
              child: const Text('Open'),
            ),
          if (onRequest != null)
            FilledButton.tonal(
              onPressed: applicable ? onRequest : null,
              child: const Text('Request'),
            ),
        ],
      ),
    );
  }
}
