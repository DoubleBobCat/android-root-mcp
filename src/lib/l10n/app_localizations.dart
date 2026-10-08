import 'package:flutter/widgets.dart';

class AppLocalizations {
  const AppLocalizations(this.locale);

  final Locale locale;

  static const supportedLocales = <Locale>[Locale('en'), Locale('zh')];

  static const delegate = _AppLocalizationsDelegate();

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations) ??
        const AppLocalizations(Locale('en'));
  }

  bool get isChinese => locale.languageCode == 'zh';

  String get appTitle => 'ARMCP';
  String get nativeBoundaryChecking =>
      isChinese ? '正在检查设备访问' : 'Checking device access';
  String get nativeBoundaryAvailable =>
      isChinese ? '设备访问可用' : 'Device access available';
  String get nativeBoundaryUnavailable =>
      isChinese ? '设备访问不可用' : 'Device access unavailable';
  String get platform => isChinese ? '平台' : 'Platform';
  String get androidApi => isChinese ? 'Android API' : 'Android API';
  String get applicationId => isChinese ? '应用 ID' : 'Application ID';
  String get operationPermission => isChinese ? '访问级别' : 'Access level';
  String operationPermissionValue(String value) =>
      value == 'read_only' ? (isChinese ? '只读' : 'read-only') : value;
  String get reason => isChinese ? '原因' : 'Reason';
  String get debugUrl => isChinese ? '调试页面地址' : 'Debug page address';
  String get mcpUrl => 'MCP URL';
  String get mcpRunning => isChinese ? 'MCP 服务正在运行' : 'MCP server is running';
  String get mcpNotRunning => isChinese
      ? 'MCP 服务未运行，请打开开关'
      : 'MCP server is off; turn on the switch to start it';
  String get unavailable => isChinese ? '不可用' : 'unavailable';
  String get globalMcpSwitch => isChinese ? 'MCP 服务' : 'MCP server';
  String get mcpEnabledDescription =>
      isChinese ? 'MCP 服务已开启。' : 'MCP server is on.';
  String get mcpDisabledDescription =>
      isChinese ? 'MCP 服务已关闭。' : 'MCP server is off.';
  String get refreshCapabilityStatus => isChinese ? '刷新状态' : 'Refresh status';
  String get checkingRootStatus =>
      isChinese ? '正在检查 Root 状态' : 'Checking root status';
  String get rootAvailable => isChinese ? 'Root 可用' : 'Root available';
  String get rootUnavailable => isChinese ? 'Root 不可用' : 'Root unavailable';
  String get rootNeedsAttention =>
      isChinese ? 'Root 状态未知' : 'Root status unknown';
  String get state => isChinese ? '状态' : 'State';
  String get latestAction => isChinese ? '最近操作' : 'Latest action';
  String get visibleRootRecheck => isChinese
      ? '设备未授予 Root 权限。可以再次检查。'
      : 'Root access is not available. You can check again.';
  String get tryRootRecheck => isChinese ? '再次检查 Root' : 'Check Root again';
  String get requestRootAuthorization =>
      isChinese ? '尝试获取 Root 权限' : 'Request Root authorization';
  String get rootAuthorizationHint => isChinese
      ? '将请求设备上的 Root 管理器授予 ARMCP 权限。'
      : 'Ask the device Root manager to authorize ARMCP.';
  String get grantAllPermissions =>
      isChinese ? 'Root 权限操作' : 'Root permission action';
  String get autoGrantPermissions => isChinese
      ? '自动授予应用权限（Root）'
      : 'Automatically grant app permissions (Root)';
  String get autoGrantPermissionsHint => isChinese
      ? '启用后，在 MCP 开启或应用启动时自动尝试。'
      : 'When enabled, ARMCP retries when MCP starts or the app opens.';
  String get grantAllPermissionsHint => isChinese
      ? '仅尝试授予应用声明的权限；Android 可能拒绝受限权限。'
      : 'Only permissions declared by the app are attempted; Android may refuse restricted permissions.';
  String get permissionRiskTitle =>
      isChinese ? '高风险操作确认' : 'Confirm high-risk action';
  String get permissionRiskMessage => isChinese
      ? '此操作将使用 Root 尝试授予此应用声明的全部权限。应用或 MCP 服务被滥用时，影响范围可能扩大，也可能绕过正常的用户授权流程。是否继续？'
      : 'Root will try to grant every permission declared by this app. If the app or MCP service is misused, the impact may be greater and normal user consent may be bypassed. Continue?';
  String get cancel => isChinese ? '取消' : 'Cancel';
  String get continueText => isChinese ? '继续' : 'Continue';
  String get permissionsGranted => isChinese ? '权限处理结果' : 'Permission result';
  String get openAccessibilitySettings =>
      isChinese ? '打开系统无障碍设置' : 'Open accessibility settings';
  String get accessibilityServiceHint => isChinese
      ? '要读取其他应用界面，请在系统设置中启用 ARMCP 界面读取。Root 可尝试自动启用；没有 Root 时请手动启用。'
      : 'Enable ARMCP screen reading in system settings to read other apps. Root can try to enable it; without Root, enable it manually.';
  String permissionResult(String reason) =>
      isChinese ? '结果：$reason' : 'Result: $reason';
  String permissionStatus(String status) =>
      <String, String>{
        'granted': isChinese ? '已获取' : 'granted',
        'already_granted': isChinese ? '已拥有' : 'already granted',
        'failed': isChinese ? '失败' : 'failed',
      }[status] ??
      status;
  String get language => isChinese ? '语言' : 'Language';
  String get systemLanguage => isChinese ? '跟随系统' : 'System';
  String get english => 'English';
  String get chinese => '中文';

  String get homeTab => isChinese ? '主页' : 'Home';
  String get runtimeSupportTab => isChinese ? '运行方式与权限' : 'Runtime support';
  String get tokensTab => isChinese ? '令牌管理' : 'Tokens';
  String get settingsTab => isChinese ? '设置' : 'Settings';
  String get environmentChecks => isChinese ? '环境检查' : 'Environment checks';
  String get appPermissionStatus => isChinese ? '应用权限' : 'App permissions';
  String get accessibilityStatus => isChinese ? '无障碍权限' : 'Accessibility permission';
  String get granted => isChinese ? '已获取' : 'Granted';
  String get notGranted => isChinese ? '未获取' : 'Not granted';
  String get noDeclaredPermissions =>
      isChinese ? '没有需要检查的运行时权限。' : 'No runtime permissions to check.';
  String get settingsTitle => isChinese ? '设置' : 'Settings';
  String get languageHint => isChinese ? '选择界面语言' : 'Choose the interface language';
  String get runtimeModeHintShort =>
      isChinese ? '选择 ARMCP 使用 Root 还是非 Root 运行。' : 'Choose whether ARMCP runs with Root or non-Root access.';
  String get armcpRunningSupport => isChinese ? 'ARMCP 运行支持' : 'ARMCP running support';
  String get armcpRunningSupportHint => isChinese
      ? '非 Root 模式使用 ARMCP 无障碍服务完成支持的界面读取和输入操作。'
      : 'Non-root mode uses the ARMCP accessibility service for supported screen reading and input.';
  String get rootPermissionSupport => isChinese ? 'Root 权限支持' : 'Root permission support';

  String get mcpAccessTokens => isChinese ? 'MCP 访问令牌' : 'MCP access tokens';
  String get tokenSecretOnce => isChinese
      ? '令牌只显示一次，请在离开前保存。应用不会再次显示完整令牌。'
      : 'The token is shown once. Save it before leaving; the full token cannot be shown again.';
  String get tokenLifetime => isChinese ? '令牌有效期' : 'Token lifetime';
  String get durationSeconds => isChinese ? '有效时长（秒）' : 'Duration in seconds';
  String get createToken => isChinese ? '创建令牌' : 'Create token';
  String get copyTokenNow => isChinese ? '请保存此令牌：' : 'Save this token:';
  String get issuedTokens => isChinese ? '现有令牌' : 'Existing tokens';
  String get noTokensIssued => isChinese ? '暂无令牌。' : 'No tokens yet.';
  String get uses => isChinese ? '使用次数' : 'Uses';
  String get tokenPermissions => isChinese ? '允许的工具' : 'Allowed tools';
  String get readOnlyDefault =>
      isChinese ? '只读工具默认开启' : 'Read-only tools start enabled';
  String get deleteToken => isChinese ? '删除令牌' : 'Delete token';
  String get readOnlyTool => isChinese ? '只读' : 'read-only';
  String get fixedDurationError => isChinese
      ? '固定时长令牌需要大于零的秒数。'
      : 'Fixed-duration tokens require seconds greater than zero.';
  String get runtimeMode => isChinese ? '运行模式' : 'Runtime mode';
  String get runtimeModeHint => isChinese
      ? 'Root 模式支持 shell 操作；非 Root 模式使用 ARMCP 无障碍服务。'
      : 'Root mode supports shell operations; non-root mode uses the ARMCP accessibility service.';
  String get rootMode => isChinese ? 'Root' : 'Root';
  String get nonRootMode => isChinese ? '非 Root' : 'Non-root';
  String get rootModeTab => isChinese ? 'Root 模式' : 'Root mode';
  String get nonRootModeTab => isChinese ? '非 Root 模式' : 'Non-root mode';
  String get armcpAccessibilityService =>
      isChinese ? 'ARMCP 界面读取' : 'ARMCP screen reading';
  String get accessibilityEnabled =>
      isChinese ? '已启用，可读取支持的界面' : 'Enabled; supported screens can be read';
  String get accessibilityNeedsSettings =>
      isChinese ? '请在 Android 设置中启用' : 'Enable it in Android settings';
  String get talkBackAdaptation =>
      isChinese ? 'TalkBack 适配' : 'TalkBack adaptation';
  String get talkBackAutoUse =>
      isChinese ? '允许按需自动使用 TalkBack' : 'Allow on-demand TalkBack use';
  String get talkBackGuideEnable =>
      isChinese ? '引导开启 TalkBack' : 'Guide me to enable TalkBack';
  String get talkBackEnabled => isChinese ? 'TalkBack 已启用' : 'TalkBack is on';
  String get talkBackAutoUseHint => isChinese
      ? '读取界面失败且屏幕有内容时临时开启，读取成功后自动关闭'
      : 'Temporarily turn it on when a screen read fails and content is present, then turn it off after recovery';
  String get talkBackGuideEnableHint => isChinese
      ? '勾选后打开系统无障碍设置，由用户手动开启 TalkBack'
      : 'Open Android accessibility settings so you can enable TalkBack';
  String get selectTokenTools => isChinese ? '选择允许的工具' : 'Choose allowed tools';
  String get selectTokenToolsHint => isChinese
      ? '选择此令牌可以调用的工具。只读工具默认开启。'
      : 'Choose which tools this token can call. Read-only tools start enabled.';
  String get done => isChinese ? '完成' : 'Done';
  String selectedTools(int count) =>
      isChinese ? '已选择 $count 项' : '$count tools selected';

  String reasonText(String value) {
    final messages = <String, String>{
      'su_command_unavailable': isChinese
          ? '设备未提供 Root 命令'
          : 'Root command is unavailable',
      'su_uid_0_not_confirmed': isChinese
          ? '未确认 Root 权限'
          : 'Root access was not confirmed',
      'root_probe_timeout': isChinese ? 'Root 检查超时' : 'Root check timed out',
      'root_unavailable': isChinese ? 'Root 不可用' : 'Root is unavailable',
      'root_mode_required': isChinese
          ? '请先切换到 Root 模式'
          : 'Switch to Root mode first',
      'switch_to_root_mode': isChinese
          ? '请先切换到 Root 模式'
          : 'Switch to Root mode first',
      'all_declared_permissions_granted': isChinese
          ? '已处理所有应用声明的权限'
          : 'All declared app permissions were processed',
      'some_permissions_refused': isChinese
          ? '部分权限未获授予'
          : 'Some permissions were refused',
      'android_refused_grant': isChinese
          ? 'Android 拒绝授予此权限'
          : 'Android refused this permission',
      'grant_timeout': isChinese ? '授予权限超时' : 'Permission request timed out',
      'grant_command_failed': isChinese
          ? '权限请求失败'
          : 'Permission request failed',
      'android_refused_enable': isChinese
          ? 'Android 拒绝启用此服务'
          : 'Android refused to enable this service',
      'enable_command_failed': isChinese
          ? '启用服务失败'
          : 'Could not enable the service',
      'native_android_boundary_unavailable': isChinese
          ? '设备访问不可用'
          : 'Device access is unavailable',
      'native_error': isChinese ? 'Android 操作失败' : 'Android operation failed',
      'empty_native_response': isChinese
          ? '未收到设备状态'
          : 'No device status was returned',
    };
    return messages[value] ?? (isChinese ? '操作失败' : 'Operation failed');
  }

  String rootState(String value) =>
      <String, String>{
        'unknown': isChinese ? '未知' : 'unknown',
        'available': isChinese ? '可用' : 'available',
        'unavailable': isChinese ? '不可用' : 'unavailable',
        'error': isChinese ? '检查失败' : 'check failed',
      }[value] ??
      (isChinese ? '未知' : 'unknown');

  String rootAttempt(String value) => value == 'recheck_only'
      ? (isChinese ? '再次检查' : 'checked again')
      : value == 'authorization_request'
      ? (isChinese ? '已请求授权' : 'authorization requested')
      : value == 'status_check'
      ? (isChinese ? '已检查状态' : 'status checked')
      : (isChinese ? '尚未完成' : 'not completed');

  String tokenMode(String mode) {
    if (!isChinese) return mode;
    return <String, String>{
          'one_time': '一次性',
          'reusable': '可重复使用',
          'fixed_duration': '固定时长',
          'unlimited': '不限时',
        }[mode] ??
        mode;
  }
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) => AppLocalizations.supportedLocales.any(
    (item) => item.languageCode == locale.languageCode,
  );

  @override
  Future<AppLocalizations> load(Locale locale) async => AppLocalizations(
    locale.languageCode == 'zh' ? const Locale('zh') : const Locale('en'),
  );

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}
