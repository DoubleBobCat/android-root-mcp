import 'package:flutter/services.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:android_root_mcp/main.dart';
import 'package:android_root_mcp/platform/android_capability_client.dart';

void main() {
  const channel = MethodChannel('com.doublecat.android_root_mcp/capabilities');

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  testWidgets('renders native Android health response', (tester) async {
    var persistedMcpEnabled = false;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          if (call.method == 'getHealth') {
            return <String, Object>{
              'platform': 'android',
              'apiLevel': 36,
              'applicationId': 'com.doublecat.android_root_mcp',
              'operationPermission': 'read_only',
              'debugUrl': 'http://127.0.0.1:8787/debug/',
              'mcpUrl': 'http://127.0.0.1:8787/mcp',
              'mcpRunning': false,
            };
          }
          if (call.method == 'getRootStatus') {
            return <String, Object>{
              'state': 'unavailable',
              'available': false,
              'reason': 'su_command_unavailable',
              'attempt': 'recheck_only',
            };
          }
           if (call.method == 'attemptRoot') {
            return <String, Object>{
              'state': 'unavailable',
              'available': false,
              'reason': 'su_command_unavailable',
              'attempt': 'authorization_request',
            };
          }
          if (call.method == 'getPermissionStatus') {
            return <Object>[
              <String, Object>{
                'name': 'android.permission.POST_NOTIFICATIONS',
                'granted': false,
              },
            ];
          }
          if (call.method == 'getAccessibilityStatus') {
            return <String, Object>{
              'serviceEnabled': false,
              'talkBackEnabled': false,
              'talkBackInstalled': true,
              'talkBackAdaptation': false,
              'requiresSettings': true,
              'reason': 'service_disabled',
            };
          }
          if (call.method == 'getRuntimeMode') return 'root';
          if (call.method == 'grantAllPermissions') {
            return <String, Object>{
              'success': false,
              'state': 'root_unavailable',
              'reason': 'root_unavailable',
              'permissions': <Object>[
                <String, Object>{
                  'name': 'android.permission.CAMERA',
                  'status': 'granted',
                },
                <String, Object>{
                  'name': 'android.permission.POST_NOTIFICATIONS',
                  'status': 'failed',
                },
              ],
            };
          }
          if (call.method == 'openAccessibilitySettings') return true;
          if (call.method == 'getMcpEnabled') return persistedMcpEnabled;
          if (call.method == 'getAutoGrantPermissions') return false;
          if (call.method == 'setAutoGrantPermissions') {
            return call.arguments['enabled'] as bool;
          }
          if (call.method == 'getLocale') return 'en';
          if (call.method == 'setLocale') return null;
          if (call.method == 'setMcpEnabled') {
            persistedMcpEnabled = call.arguments['enabled'] as bool;
            return persistedMcpEnabled;
          }
          if (call.method == 'listTokenTools') {
            return <Object>[
              <String, Object>{'name': 'device_status', 'readOnly': true},
              <String, Object>{'name': 'shell_exec', 'readOnly': false},
            ];
          }
          return null;
        });

    await tester.pumpWidget(const AndroidRootMcpApp());
    await tester.pumpAndSettle();

    expect(find.text('Device access available'), findsOneWidget);
    expect(find.text('ARMCP'), findsOneWidget);
    expect(find.text('android'), findsOneWidget);
    expect(find.text('36'), findsOneWidget);
    expect(find.text('read-only'), findsOneWidget);
    expect(find.text('Home'), findsOneWidget);
    expect(find.text('Runtime support'), findsOneWidget);
    expect(find.text('Tokens'), findsOneWidget);
    expect(find.text('Settings'), findsOneWidget);
    expect(
      find.textContaining('Open this address on the device'),
      findsNothing,
    );
    expect(
      find.textContaining('Use an authenticated POST request'),
      findsNothing,
    );
    expect(find.text('Root unavailable'), findsOneWidget);
    expect(find.text('Request Root authorization'), findsOneWidget);
    await tester.tap(find.text('Runtime support'));
    await tester.pumpAndSettle();
    expect(
       find.text('Automatically grant app permissions (Root)'),
      findsOneWidget,
    );
    final autoGrantTile = find.widgetWithText(
      SwitchListTile,
       'Automatically grant app permissions (Root)',
    );
    await tester.ensureVisible(autoGrantTile);
    await tester.drag(find.byType(ListView).first, const Offset(0, 220));
    await tester.pumpAndSettle();
    await tester.tap(autoGrantTile, warnIfMissed: false);
    await tester.pumpAndSettle();
    expect(find.text('Confirm high-risk action'), findsOneWidget);
    expect(
      find.textContaining('Root will try to grant every permission'),
      findsOneWidget,
    );
    await tester.tap(find.text('Continue'));
    await tester.pumpAndSettle();
    expect(
       find.text('Automatically grant app permissions (Root)'),
      findsOneWidget,
    );
    await tester.tap(find.text('Home'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.text('MCP server'),
      300,
      scrollable: find.byType(Scrollable).first,
    );
    expect(find.text('MCP server'), findsOneWidget);
    expect(find.text('MCP server is off.'), findsOneWidget);
    final globalSwitch = find.widgetWithText(SwitchListTile, 'MCP server');
    final switchTile = tester.widget<SwitchListTile>(globalSwitch);
    switchTile.onChanged!(true);
    await tester.pumpAndSettle();
    await tester.pumpAndSettle();
    expect(persistedMcpEnabled, isTrue);
    expect(find.text('MCP server is on.'), findsOneWidget);
  });

  testWidgets('switches between English and Chinese', (tester) async {
    var persistedLanguage = 'en';
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          if (call.method == 'getHealth') {
            return <String, Object>{
              'platform': 'android',
              'apiLevel': 36,
              'applicationId': 'com.doublecat.android_root_mcp',
              'operationPermission': 'read_only',
              'debugUrl': 'http://127.0.0.1:8787/debug/',
              'mcpUrl': 'http://127.0.0.1:8787/mcp',
              'mcpRunning': false,
            };
          }
          if (call.method == 'getRootStatus' || call.method == 'attemptRoot') {
            return <String, Object>{
              'state': 'unavailable',
              'available': false,
              'reason': 'su_command_unavailable',
              'attempt': 'recheck_only',
            };
          }
          if (call.method == 'getPermissionStatus') return <Object>[];
          if (call.method == 'getAccessibilityStatus') {
            return <String, Object>{
              'serviceEnabled': false,
              'talkBackEnabled': false,
              'talkBackInstalled': true,
              'talkBackAdaptation': false,
              'requiresSettings': true,
              'reason': 'service_disabled',
            };
          }
          if (call.method == 'getRuntimeMode') return 'root';
          if (call.method == 'getMcpEnabled') return false;
          if (call.method == 'getAutoGrantPermissions') return false;
          if (call.method == 'setAutoGrantPermissions') {
            return call.arguments['enabled'] as bool;
          }
          if (call.method == 'getLocale') return persistedLanguage;
          if (call.method == 'setLocale') {
            persistedLanguage = call.arguments['languageCode'] as String;
            return null;
          }
          return null;
        });

    await tester.pumpWidget(const AndroidRootMcpApp());
    await tester.pumpAndSettle();
    await tester.tap(find.text('Settings'));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey<String>('language-selector')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('中文').last);
    await tester.pumpAndSettle();

    expect(find.text('设置'), findsAtLeastNWidgets(1));
    expect(find.text('语言'), findsOneWidget);
    await tester.tap(find.text('主页'));
    await tester.pumpAndSettle();
    expect(find.text('环境检查'), findsOneWidget);
    expect(persistedLanguage, 'zh');
  });

  testWidgets('hides Root authorization after Root becomes available', (tester) async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          if (call.method == 'getHealth') {
            return <String, Object>{
              'platform': 'android',
              'apiLevel': 36,
              'applicationId': 'com.doublecat.android_root_mcp',
              'operationPermission': 'read_only',
              'mcpRunning': false,
            };
          }
          if (call.method == 'getRootStatus' || call.method == 'attemptRoot') {
            return <String, Object>{
              'state': 'available',
              'available': true,
              'reason': 'su_uid_0',
              'attempt': 'authorization_request',
            };
          }
          if (call.method == 'getPermissionStatus') return <Object>[];
          if (call.method == 'getAccessibilityStatus') {
            return <String, Object>{
              'serviceEnabled': true,
              'talkBackEnabled': false,
              'talkBackInstalled': true,
              'talkBackAdaptation': false,
              'requiresSettings': false,
              'reason': 'service_enabled',
            };
          }
          if (call.method == 'getRuntimeMode') return 'root';
          if (call.method == 'getMcpEnabled') return false;
          if (call.method == 'getAutoGrantPermissions') return false;
          if (call.method == 'getLocale') return 'en';
          if (call.method == 'listTokenTools') return <Object>[];
          if (call.method == 'listTokens') return <Object>[];
          return null;
        });

    await tester.pumpWidget(const AndroidRootMcpApp());
    await tester.pumpAndSettle();

    expect(find.text('Root available'), findsOneWidget);
    expect(find.text('Request Root authorization'), findsNothing);
  });

  test(
    'token tool permissions remain independent and delete is physical',
    () async {
      final calls = <MethodCall>[];
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async {
            calls.add(call);
            if (call.method == 'createToken') {
              return <String, Object?>{
                'id': 'token-a',
                'mode': 'reusable',
                'createdAt': 1,
                'enabledTools': <String>['device_status'],
                'useCount': 0,
                'secret': 'armcp_test',
              };
            }
            if (call.method == 'listTokens') {
              return <Object?>[
                <String, Object?>{
                  'id': 'token-a',
                  'mode': 'reusable',
                  'createdAt': 1,
                  'enabledTools': <String>['device_status'],
                  'useCount': 0,
                },
                <String, Object?>{
                  'id': 'token-b',
                  'mode': 'reusable',
                  'createdAt': 2,
                  'enabledTools': <String>['device_status', 'shell_exec'],
                  'useCount': 0,
                },
              ];
            }
            if (call.method == 'deleteToken') return true;
            return null;
          });

      const client = AndroidCapabilityClient();
      final created = await client.createToken(
        mode: 'reusable',
        enabledTools: const {'device_status'},
      );
      final listed = await client.listTokens();
      final deleted = await client.deleteToken('token-a');

      expect(created.enabledTools, {'device_status'});
      expect(listed[0].enabledTools, {'device_status'});
      expect(listed[1].enabledTools, {'device_status', 'shell_exec'});
      expect(deleted, isTrue);
      expect(calls[0].arguments['enabledTools'], ['device_status']);
      expect(calls.last.method, 'deleteToken');
    },
  );
}
