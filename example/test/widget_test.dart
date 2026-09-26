import 'package:android_special_permissions/android_special_permissions.dart';
import 'package:android_special_permissions_example/main.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('lists every permission and role with its status',
      (tester) async {
    tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
      const MethodChannel(AndroidSpecialPermissions.channelName),
      (call) async => call.method == 'roleStatus' ? 'notApplicable' : 'denied',
    );
    tester.view.physicalSize = const Size(800, 4000);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);

    await tester.pumpWidget(const ExampleApp());
    await tester.pumpAndSettle();

    for (final permission in SpecialPermission.values) {
      expect(find.text(permission.name), findsOneWidget);
    }
    for (final role in AndroidRole.values) {
      expect(find.text(role.name), findsOneWidget);
    }
    expect(find.text('denied'), findsNWidgets(SpecialPermission.values.length));
    expect(
        find.text('Open'), findsNWidgets(SpecialPermission.values.length - 1));
  });
}
