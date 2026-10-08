import 'package:flutter/services.dart';

class AndroidCapabilityClient {
  const AndroidCapabilityClient();

  static const _channel = MethodChannel(
    'com.doublecat.android_root_mcp/capabilities',
  );

  Future<PlatformHealth> getHealth() async {
    try {
      final raw = await _channel.invokeMethod<Map<Object?, Object?>>(
        'getHealth',
      );
      return PlatformHealth.fromMap(raw);
    } on MissingPluginException {
      return const PlatformHealth(
        available: false,
        platform: 'unavailable',
        apiLevel: null,
        applicationId: null,
        operationPermission: 'read_only',
        debugUrl: null,
        mcpUrl: null,
        mcpRunning: false,
        reason: 'native_android_boundary_unavailable',
      );
    } on PlatformException catch (error) {
      return PlatformHealth(
        available: false,
        platform: 'error',
        apiLevel: null,
        applicationId: null,
        operationPermission: 'read_only',
        debugUrl: null,
        mcpUrl: null,
        mcpRunning: false,
        reason: error.code,
      );
    }
  }

  Future<RootStatus> getRootStatus() => _invokeRootMethod('getRootStatus');

  Future<RootStatus> attemptRoot() => _invokeRootMethod('attemptRoot');

  Future<List<PermissionStatus>> getPermissionStatus() async {
    try {
      final raw = await _channel.invokeMethod<List<Object?>>(
        'getPermissionStatus',
      );
      return (raw ?? const <Object?>[])
          .whereType<Map<Object?, Object?>>()
          .map(PermissionStatus.fromMap)
          .toList();
    } on PlatformException {
      return const <PermissionStatus>[];
    } on MissingPluginException {
      return const <PermissionStatus>[];
    }
  }

  Future<PermissionGrantResult> grantAllPermissions() async {
    try {
      final raw = await _channel.invokeMethod<Map<Object?, Object?>>(
        'grantAllPermissions',
      );
      return PermissionGrantResult.fromMap(raw);
    } on MissingPluginException {
      return const PermissionGrantResult(
        success: false,
        state: 'unavailable',
        reason: 'native_android_boundary_unavailable',
        permissions: <PermissionOutcome>[],
      );
    } on PlatformException catch (error) {
      return PermissionGrantResult(
        success: false,
        state: 'error',
        reason: error.code,
        permissions: const <PermissionOutcome>[],
      );
    }
  }

  Future<bool> openAccessibilitySettings() async {
    try {
      return await _channel.invokeMethod<bool>('openAccessibilitySettings') ??
          false;
    } on PlatformException {
      return false;
    } on MissingPluginException {
      return false;
    }
  }

  Future<AccessibilityStatus> getAccessibilityStatus() async {
    try {
      final raw = await _channel.invokeMethod<Map<Object?, Object?>>(
        'getAccessibilityStatus',
      );
      return AccessibilityStatus.fromMap(raw);
    } on PlatformException {
      return const AccessibilityStatus(
        serviceEnabled: false,
        talkBackEnabled: false,
        talkBackInstalled: false,
        talkBackAdaptation: false,
        requiresSettings: true,
        reason: 'native_error',
      );
    } on MissingPluginException {
      return const AccessibilityStatus(
        serviceEnabled: false,
        talkBackEnabled: false,
        talkBackInstalled: false,
        talkBackAdaptation: false,
        requiresSettings: true,
        reason: 'native_android_boundary_unavailable',
      );
    }
  }

  Future<AccessibilityStatus> setTalkBackAdaptation(bool enabled) async {
    try {
      final raw = await _channel.invokeMethod<Map<Object?, Object?>>(
        'setTalkBackAdaptation',
        <String, Object>{'enabled': enabled},
      );
      return AccessibilityStatus.fromMap(raw);
    } on PlatformException {
      return (await getAccessibilityStatus()).withReason('native_error');
    } on MissingPluginException {
      return (await getAccessibilityStatus()).withReason(
        'native_android_boundary_unavailable',
      );
    }
  }

  Future<String> getRuntimeMode() async {
    try {
      return await _channel.invokeMethod<String>('getRuntimeMode') ?? 'root';
    } on PlatformException {
      return 'root';
    } on MissingPluginException {
      return 'root';
    }
  }

  Future<String> setRuntimeMode(String mode) async {
    try {
      return await _channel.invokeMethod<String>('setRuntimeMode', <String, Object>{
            'mode': mode,
          }) ??
          'root';
    } on PlatformException {
      return 'root';
    } on MissingPluginException {
      return 'root';
    }
  }

  Future<bool> getMcpEnabled() async {
    try {
      return await _channel.invokeMethod<bool>('getMcpEnabled') ?? false;
    } on PlatformException {
      return false;
    } on MissingPluginException {
      return false;
    }
  }

  Future<bool> getAutoGrantPermissions() async {
    try {
      return await _channel.invokeMethod<bool>('getAutoGrantPermissions') ??
          false;
    } on PlatformException {
      return false;
    } on MissingPluginException {
      return false;
    }
  }

  Future<bool> setAutoGrantPermissions(bool enabled) async {
    try {
      return await _channel.invokeMethod<bool>(
            'setAutoGrantPermissions',
            <String, Object>{'enabled': enabled},
          ) ??
          false;
    } on PlatformException {
      return false;
    } on MissingPluginException {
      return false;
    }
  }

  Future<String> getLocale() async {
    try {
      return await _channel.invokeMethod<String>('getLocale') ?? 'system';
    } on PlatformException {
      return 'system';
    } on MissingPluginException {
      return 'system';
    }
  }

  Future<void> setLocale(String languageCode) async {
    try {
      await _channel.invokeMethod<void>('setLocale', <String, Object>{
        'languageCode': languageCode,
      });
    } on PlatformException {
      // The in-memory Flutter locale still applies when native persistence fails.
    } on MissingPluginException {
      // The in-memory Flutter locale still applies in non-Android tests.
    }
  }

  Future<bool> setMcpEnabled(bool enabled) async {
    try {
      return await _channel.invokeMethod<bool>(
            'setMcpEnabled',
            <String, Object>{'enabled': enabled},
          ) ??
          false;
    } on PlatformException {
      return false;
    } on MissingPluginException {
      return false;
    }
  }

  Future<TokenRecord> createToken({
    required String mode,
    int? durationSeconds,
    required Set<String> enabledTools,
  }) async {
    final raw = await _channel
        .invokeMethod<Map<Object?, Object?>>('createToken', <String, Object?>{
          'mode': mode,
          'durationSeconds': ?durationSeconds,
          'enabledTools': enabledTools.toList(),
        });
    return TokenRecord.fromMap(raw, includeSecret: true);
  }

  Future<List<TokenRecord>> listTokens() async {
    final raw = await _channel.invokeMethod<List<Object?>>('listTokens');
    return (raw ?? const <Object?>[])
        .whereType<Map<Object?, Object?>>()
        .map(TokenRecord.fromMap)
        .toList();
  }

  Future<List<TokenTool>> listTokenTools() async {
    final raw = await _channel.invokeMethod<List<Object?>>('listTokenTools');
    return (raw ?? const <Object?>[])
        .whereType<Map<Object?, Object?>>()
        .map(TokenTool.fromMap)
        .toList();
  }

  Future<bool> setTokenTools(String id, Set<String> enabledTools) async {
    return await _channel.invokeMethod<bool>('setTokenTools', <String, Object?>{
          'id': id,
          'enabledTools': enabledTools.toList(),
        }) ??
        false;
  }

  Future<bool> deleteToken(String id) async {
    return await _channel.invokeMethod<bool>('deleteToken', <String, Object?>{
          'id': id,
        }) ??
        false;
  }

  Future<RootStatus> _invokeRootMethod(String method) async {
    try {
      final raw = await _channel.invokeMethod<Map<Object?, Object?>>(method);
      return RootStatus.fromMap(raw);
    } on MissingPluginException {
      return const RootStatus(
        state: RootState.error,
        available: false,
        reason: 'native_android_boundary_unavailable',
        attempt: 'not_available',
      );
    } on PlatformException catch (error) {
      return RootStatus(
        state: RootState.error,
        available: false,
        reason: error.code,
        attempt: 'failed',
      );
    }
  }
}

class PermissionGrantResult {
  const PermissionGrantResult({
    required this.success,
    required this.state,
    required this.reason,
    required this.permissions,
  });

  factory PermissionGrantResult.fromMap(Map<Object?, Object?>? raw) {
    final rawPermissions = raw?['permissions'] as List<Object?>? ?? const [];
    return PermissionGrantResult(
      success: raw?['success'] as bool? ?? false,
      state: raw?['state'] as String? ?? 'error',
      reason: raw?['reason'] as String? ?? 'empty_native_response',
      permissions: rawPermissions
          .whereType<Map<Object?, Object?>>()
          .map(PermissionOutcome.fromMap)
          .toList(),
    );
  }

  final bool success;
  final String state;
  final String reason;
  final List<PermissionOutcome> permissions;
}

class AccessibilityStatus {
  const AccessibilityStatus({
    required this.serviceEnabled,
    required this.talkBackEnabled,
    required this.talkBackInstalled,
    required this.talkBackAdaptation,
    required this.requiresSettings,
    required this.reason,
  });

  factory AccessibilityStatus.fromMap(Map<Object?, Object?>? raw) {
    return AccessibilityStatus(
      serviceEnabled: raw?['serviceEnabled'] as bool? ?? false,
      talkBackEnabled: raw?['talkBackEnabled'] as bool? ?? false,
      talkBackInstalled: raw?['talkBackInstalled'] as bool? ?? false,
      talkBackAdaptation: raw?['talkBackAdaptation'] as bool? ?? false,
      requiresSettings: raw?['requiresSettings'] as bool? ?? false,
      reason: raw?['reason'] as String? ?? 'unknown',
    );
  }

  AccessibilityStatus withReason(String value) => AccessibilityStatus(
        serviceEnabled: serviceEnabled,
        talkBackEnabled: talkBackEnabled,
        talkBackInstalled: talkBackInstalled,
        talkBackAdaptation: talkBackAdaptation,
        requiresSettings: requiresSettings,
        reason: value,
      );

  final bool serviceEnabled;
  final bool talkBackEnabled;
  final bool talkBackInstalled;
  final bool talkBackAdaptation;
  final bool requiresSettings;
  final String reason;
}

class PermissionOutcome {
  const PermissionOutcome({
    required this.name,
    required this.status,
    this.reason,
  });

  factory PermissionOutcome.fromMap(Map<Object?, Object?> raw) {
    return PermissionOutcome(
      name: raw['name'] as String? ?? 'unknown',
      status: raw['status'] as String? ?? 'failed',
      reason: raw['reason'] as String?,
    );
  }

  final String name;
  final String status;
  final String? reason;
}

class PermissionStatus {
  const PermissionStatus({required this.name, required this.granted});

  factory PermissionStatus.fromMap(Map<Object?, Object?> raw) {
    return PermissionStatus(
      name: raw['name'] as String? ?? 'unknown',
      granted: raw['granted'] as bool? ?? false,
    );
  }

  final String name;
  final bool granted;
}

enum RootState { unknown, available, unavailable, error }

class RootStatus {
  const RootStatus({
    required this.state,
    required this.available,
    required this.reason,
    required this.attempt,
  });

  factory RootStatus.fromMap(Map<Object?, Object?>? raw) {
    final stateName = raw?['state'] as String?;
    final state = RootState.values.firstWhere(
      (item) => item.name == stateName,
      orElse: () => RootState.error,
    );
    return RootStatus(
      state: state,
      available: raw?['available'] as bool? ?? false,
      reason: raw?['reason'] as String? ?? 'empty_native_response',
      attempt: raw?['attempt'] as String? ?? 'unknown',
    );
  }

  final RootState state;
  final bool available;
  final String reason;
  final String attempt;
}

class PlatformHealth {
  const PlatformHealth({
    required this.available,
    required this.platform,
    required this.apiLevel,
    required this.applicationId,
    required this.operationPermission,
    required this.debugUrl,
    required this.mcpUrl,
    required this.mcpRunning,
    required this.reason,
  });

  factory PlatformHealth.fromMap(Map<Object?, Object?>? raw) {
    if (raw == null) {
      return const PlatformHealth(
        available: false,
        platform: 'error',
        apiLevel: null,
        applicationId: null,
        operationPermission: 'read_only',
        debugUrl: null,
        mcpUrl: null,
        mcpRunning: false,
        reason: 'empty_native_response',
      );
    }

    return PlatformHealth(
      available: raw['platform'] == 'android',
      platform: raw['platform'] as String? ?? 'unknown',
      apiLevel: raw['apiLevel'] as int?,
      applicationId: raw['applicationId'] as String?,
      operationPermission: raw['operationPermission'] as String? ?? 'read_only',
      debugUrl: raw['debugUrl'] as String?,
      mcpUrl: raw['mcpUrl'] as String?,
      mcpRunning: raw['mcpRunning'] as bool? ?? false,
      reason: null,
    );
  }

  final bool available;
  final String platform;
  final int? apiLevel;
  final String? applicationId;
  final String operationPermission;
  final String? debugUrl;
  final String? mcpUrl;
  final bool mcpRunning;
  final String? reason;
}

class TokenRecord {
  const TokenRecord({
    required this.id,
    required this.mode,
    required this.createdAt,
    required this.expiresAt,
    required this.enabledTools,
    required this.useCount,
    required this.secret,
  });

  factory TokenRecord.fromMap(
    Map<Object?, Object?>? raw, {
    bool includeSecret = false,
  }) {
    return TokenRecord(
      id: raw?['id'] as String? ?? 'unknown',
      mode: raw?['mode'] as String? ?? 'unknown',
      createdAt: raw?['createdAt'] as int? ?? 0,
      expiresAt: raw?['expiresAt'] as int?,
      enabledTools: (raw?['enabledTools'] as List<Object?>? ?? const [])
          .whereType<String>()
          .toSet(),
      useCount: raw?['useCount'] as int? ?? 0,
      secret: includeSecret ? (raw?['secret'] as String?) : null,
    );
  }

  final String id;
  final String mode;
  final int createdAt;
  final int? expiresAt;
  final Set<String> enabledTools;
  final int useCount;
  final String? secret;
}

class TokenTool {
  const TokenTool({required this.name, required this.readOnly});

  factory TokenTool.fromMap(Map<Object?, Object?> raw) {
    return TokenTool(
      name: raw['name'] as String? ?? 'unknown',
      readOnly: raw['readOnly'] as bool? ?? false,
    );
  }

  final String name;
  final bool readOnly;
}
