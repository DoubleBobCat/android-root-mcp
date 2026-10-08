import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import 'l10n/app_localizations.dart';
import 'platform/android_capability_client.dart';
import 'ui/brand_mark.dart';
import 'ui/token_panel.dart';

void main() {
  runApp(const AndroidRootMcpApp());
}

class AndroidRootMcpApp extends StatefulWidget {
  const AndroidRootMcpApp({super.key});

  @override
  State<AndroidRootMcpApp> createState() => _AndroidRootMcpAppState();
}

class _AndroidRootMcpAppState extends State<AndroidRootMcpApp> {
  String _languagePreference = 'system';
  Locale? _locale;

  @override
  void initState() {
    super.initState();
    _loadLocale();
  }

  Future<void> _loadLocale() async {
    final preference = await const AndroidCapabilityClient().getLocale();
    if (!mounted) return;
    setState(() {
      _languagePreference = _normaliseLanguagePreference(preference);
      _locale = _explicitLocale(_languagePreference);
    });
  }

  Future<void> _setLocalePreference(String preference) async {
    final normalised = _normaliseLanguagePreference(preference);
    setState(() {
      _languagePreference = normalised;
      _locale = _explicitLocale(normalised);
    });
    await const AndroidCapabilityClient().setLocale(normalised);
  }

  Locale? _explicitLocale(String preference) {
    return preference == 'zh' ? const Locale('zh') :
        preference == 'en' ? const Locale('en') : null;
  }

  String _normaliseLanguagePreference(String value) {
    return value == 'en' || value == 'zh' ? value : 'system';
  }

  Locale _resolveLocale(Locale? deviceLocale) {
    if (deviceLocale?.languageCode == 'zh') return const Locale('zh');
    return const Locale('en');
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      locale: _locale,
      localeResolutionCallback: (deviceLocale, supportedLocales) =>
          _resolveLocale(deviceLocale),
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: AppLocalizations.supportedLocales,
      title: 'ARMCP',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal),
        useMaterial3: true,
      ),
      home: CapabilityHealthPage(
        languagePreference: _languagePreference,
        onLocalePreferenceChanged: _setLocalePreference,
      ),
    );
  }
}

class CapabilityHealthPage extends StatefulWidget {
  const CapabilityHealthPage({
    super.key,
    this.client = const AndroidCapabilityClient(),
    this.languagePreference = 'system',
    this.onLocalePreferenceChanged,
  });

  final AndroidCapabilityClient client;
  final String languagePreference;
  final ValueChanged<String>? onLocalePreferenceChanged;

  @override
  State<CapabilityHealthPage> createState() => _CapabilityHealthPageState();
}

class _CapabilityHealthPageState extends State<CapabilityHealthPage>
    with WidgetsBindingObserver {
  PlatformHealth? _health;
  RootStatus? _rootStatus;
  List<PermissionStatus> _permissionStatuses = const <PermissionStatus>[];
  AccessibilityStatus? _accessibilityStatus;
  bool _mcpEnabled = false;
  bool _loading = true;
  bool _rootLoading = true;
  bool _permissionLoading = true;
  bool _accessibilityLoading = true;
  bool _mcpLoading = true;
  bool _permissionsLoading = false;
  bool _autoGrantPermissions = false;
  bool _autoGrantLoading = true;
  PermissionGrantResult? _permissionResult;
  String _runtimeMode = 'root';
  int _selectedIndex = 0;

  bool get _isRootMode => _runtimeMode == 'root';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _loadState();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _refreshEnvironment();
    }
  }

  Future<void> _loadState() async {
    final values = await Future.wait<Object>([
      widget.client.getHealth(),
      widget.client.getRootStatus(),
      widget.client.getPermissionStatus(),
      widget.client.getAccessibilityStatus(),
      widget.client.getRuntimeMode(),
      widget.client.getMcpEnabled(),
      widget.client.getAutoGrantPermissions(),
    ]);
    if (!mounted) return;
    setState(() {
      _health = values[0] as PlatformHealth;
      _rootStatus = values[1] as RootStatus;
      _permissionStatuses = values[2] as List<PermissionStatus>;
      _accessibilityStatus = values[3] as AccessibilityStatus;
      _runtimeMode = values[4] as String == 'non_root' ? 'non_root' : 'root';
      _mcpEnabled = values[5] as bool;
      _autoGrantPermissions = values[6] as bool;
      _loading = false;
      _rootLoading = false;
      _permissionLoading = false;
      _accessibilityLoading = false;
      _mcpLoading = false;
      _autoGrantLoading = false;
    });
    if (_mcpEnabled && _autoGrantPermissions) {
      await _runAutomaticPermissionGrant();
    }
  }

  Future<void> _refreshEnvironment() async {
    final values = await Future.wait<Object>([
      widget.client.getHealth(),
      widget.client.getRootStatus(),
      widget.client.getPermissionStatus(),
      widget.client.getAccessibilityStatus(),
    ]);
    if (!mounted) return;
    setState(() {
      _health = values[0] as PlatformHealth;
      _rootStatus = values[1] as RootStatus;
      _permissionStatuses = values[2] as List<PermissionStatus>;
      _accessibilityStatus = values[3] as AccessibilityStatus;
      _rootLoading = false;
      _permissionLoading = false;
      _accessibilityLoading = false;
    });
  }

  Future<void> _requestRoot() async {
    setState(() => _rootLoading = true);
    final status = await widget.client.attemptRoot();
    if (!mounted) return;
    setState(() {
      _rootStatus = status;
      _rootLoading = false;
    });
    await _refreshEnvironment();
  }

  Future<void> _setMcpEnabled(bool enabled) async {
    setState(() => _mcpLoading = true);
    final saved = await widget.client.setMcpEnabled(enabled);
    if (!mounted) return;
    setState(() {
      _mcpEnabled = saved;
      _mcpLoading = false;
    });
    await _refreshEnvironment();
    if (saved && _autoGrantPermissions) await _runAutomaticPermissionGrant();
  }

  Future<void> _runAutomaticPermissionGrant() async {
    if (!mounted) return;
    setState(() => _permissionsLoading = true);
    final result = await widget.client.grantAllPermissions();
    if (!mounted) return;
    setState(() {
      _permissionsLoading = false;
      _permissionResult = result;
    });
    await _refreshEnvironment();
  }

  Future<void> _setAutoGrantPermissions(bool enabled) async {
    if (enabled) {
      final l10n = AppLocalizations.of(context);
      final confirmed = await showDialog<bool>(
        context: context,
        builder: (context) => AlertDialog(
          title: Text(l10n.permissionRiskTitle),
          content: Text(l10n.permissionRiskMessage),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              child: Text(l10n.cancel),
            ),
            FilledButton(
              onPressed: () => Navigator.of(context).pop(true),
              child: Text(l10n.continueText),
            ),
          ],
        ),
      );
      if (confirmed != true || !mounted) return;
    }
    setState(() => _autoGrantLoading = true);
    final saved = await widget.client.setAutoGrantPermissions(enabled);
    if (!mounted) return;
    setState(() {
      _autoGrantPermissions = saved;
      _autoGrantLoading = false;
    });
    if (saved && _mcpEnabled) await _runAutomaticPermissionGrant();
  }

  Future<void> _setRuntimeMode(String mode) async {
    final saved = await widget.client.setRuntimeMode(mode);
    if (!mounted) return;
    setState(() => _runtimeMode = saved == 'non_root' ? 'non_root' : 'root');
    await _refreshEnvironment();
  }

  Future<void> _setTalkBackAdaptation(bool enabled) async {
    final status = await widget.client.setTalkBackAdaptation(enabled);
    if (!mounted) return;
    setState(() => _accessibilityStatus = status);
    if (enabled && !_isRootMode) await widget.client.openAccessibilitySettings();
  }

  void _selectTab(int index) => setState(() => _selectedIndex = index);

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Scaffold(
      appBar: AppBar(
        leading: const Padding(
          padding: EdgeInsets.all(8),
          child: ArmcpBrandMark(size: 40),
        ),
        title: Text(l10n.appTitle),
      ),
      body: IndexedStack(
        index: _selectedIndex,
        children: [
          _buildHome(l10n),
          _buildRuntimeSupport(l10n),
          TokenPanel(client: widget.client),
          _buildSettings(l10n),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: _selectTab,
        destinations: [
          NavigationDestination(
            icon: const Icon(Icons.home_outlined),
            selectedIcon: const Icon(Icons.home),
            label: l10n.homeTab,
          ),
          NavigationDestination(
            icon: const Icon(Icons.security_outlined),
            selectedIcon: const Icon(Icons.security),
            label: l10n.runtimeSupportTab,
          ),
          NavigationDestination(
            icon: const Icon(Icons.key_outlined),
            selectedIcon: const Icon(Icons.key),
            label: l10n.tokensTab,
          ),
          NavigationDestination(
            icon: const Icon(Icons.settings_outlined),
            selectedIcon: const Icon(Icons.settings),
            label: l10n.settingsTab,
          ),
        ],
      ),
    );
  }

  Widget _buildHome(AppLocalizations l10n) {
    final health = _health;
    final isReady = health?.available == true;
    return ListView(
      key: const PageStorageKey<String>('home'),
      padding: const EdgeInsets.all(20),
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(
                      isReady ? Icons.check_circle : Icons.warning_amber,
                      color: isReady ? Colors.teal : Colors.orange,
                    ),
                    const SizedBox(width: 12),
                    Text(
                      _loading
                          ? l10n.nativeBoundaryChecking
                          : isReady
                          ? l10n.nativeBoundaryAvailable
                          : l10n.nativeBoundaryUnavailable,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                  ],
                ),
                if (!_loading && health != null) ...[
                  const SizedBox(height: 16),
                  _DetailRow(label: l10n.platform, value: health.platform),
                  _DetailRow(
                    label: l10n.androidApi,
                    value: health.apiLevel?.toString() ?? l10n.unavailable,
                  ),
                  _DetailRow(
                    label: l10n.applicationId,
                    value: health.applicationId ?? l10n.unavailable,
                  ),
                  _DetailRow(
                    label: l10n.operationPermission,
                    value: l10n.operationPermissionValue(
                      health.operationPermission,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        _EnvironmentCard(
          rootStatus: _rootStatus,
          rootLoading: _rootLoading,
          permissionStatuses: _permissionStatuses,
          permissionLoading: _permissionLoading,
          accessibilityStatus: _accessibilityStatus,
          accessibilityLoading: _accessibilityLoading,
          onRootTap: _requestRoot,
        ),
        const SizedBox(height: 12),
        _McpSwitchCard(
          enabled: _mcpEnabled,
          loading: _mcpLoading,
          onChanged: _setMcpEnabled,
        ),
        const SizedBox(height: 12),
        if (health?.mcpUrl != null)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _DetailRow(
                    label: l10n.debugUrl,
                    value: health!.debugUrl ?? l10n.unavailable,
                    selectable: health.debugUrl != null,
                  ),
                  _DetailRow(
                    label: l10n.mcpUrl,
                    value: health.mcpUrl ?? l10n.unavailable,
                    selectable: health.mcpUrl != null,
                  ),
                  Text(
                    health.mcpRunning ? l10n.mcpRunning : l10n.mcpNotRunning,
                    style: TextStyle(
                      color: health.mcpRunning
                          ? Colors.teal
                          : Theme.of(context).colorScheme.error,
                    ),
                  ),
                ],
              ),
            ),
          ),
        const SizedBox(height: 12),
        FilledButton.icon(
          onPressed: _loading ? null : _refreshEnvironment,
          icon: const Icon(Icons.refresh),
          label: Text(l10n.refreshCapabilityStatus),
        ),
      ],
    );
  }

  Widget _buildRuntimeSupport(AppLocalizations l10n) {
    return ListView(
      key: const PageStorageKey<String>('runtime-support'),
      padding: const EdgeInsets.all(20),
      children: [
        if (_isRootMode) ...[
          _RootStatusCard(
            status: _rootStatus,
            loading: _rootLoading,
            onRetry: _requestRoot,
          ),
          const SizedBox(height: 16),
          _PermissionsCard(
            loading: _permissionsLoading,
            result: _permissionResult,
            autoGrantEnabled: _autoGrantPermissions,
            autoGrantLoading: _autoGrantLoading,
            onAutoGrantChanged: _setAutoGrantPermissions,
            onOpenAccessibilitySettings:
                widget.client.openAccessibilitySettings,
          ),
          const SizedBox(height: 16),
        ],
        _RuntimeModeCard(
          isRootMode: _isRootMode,
          loading: _accessibilityLoading,
          status: _accessibilityStatus,
          onOpenAccessibilitySettings: widget.client.openAccessibilitySettings,
          onTalkBackChanged: _setTalkBackAdaptation,
        ),
      ],
    );
  }

  Widget _buildSettings(AppLocalizations l10n) {
    return ListView(
      key: const PageStorageKey<String>('settings'),
      padding: const EdgeInsets.all(20),
      children: [
        Text(l10n.settingsTitle, style: Theme.of(context).textTheme.headlineSmall),
        const SizedBox(height: 16),
        Card(
          child: Column(
            children: [
              ListTile(
                leading: const Icon(Icons.security),
                title: Text(l10n.runtimeMode),
                subtitle: Text(l10n.runtimeModeHintShort),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                child: DropdownButtonFormField<String>(
                  initialValue: _runtimeMode,
                  decoration: InputDecoration(labelText: l10n.runtimeMode),
                  items: [
                    DropdownMenuItem(
                      value: 'root',
                      child: Text(l10n.rootMode),
                    ),
                    DropdownMenuItem(
                      value: 'non_root',
                      child: Text(l10n.nonRootMode),
                    ),
                  ],
                  onChanged: (value) {
                    if (value != null) _setRuntimeMode(value);
                  },
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        Card(
          child: ListTile(
            leading: const Icon(Icons.language),
            title: Text(l10n.language),
            subtitle: Text(l10n.languageHint),
            trailing: DropdownButton<String>(
              key: const ValueKey<String>('language-selector'),
              value: widget.languagePreference,
              underline: const SizedBox.shrink(),
              items: [
                DropdownMenuItem(
                  value: 'system',
                  child: Text(l10n.systemLanguage),
                ),
                DropdownMenuItem(value: 'en', child: Text(l10n.english)),
                DropdownMenuItem(value: 'zh', child: Text(l10n.chinese)),
              ],
              onChanged: (value) {
                if (value != null) widget.onLocalePreferenceChanged?.call(value);
              },
            ),
          ),
        ),
      ],
    );
  }
}

class _EnvironmentCard extends StatelessWidget {
  const _EnvironmentCard({
    required this.rootStatus,
    required this.rootLoading,
    required this.permissionStatuses,
    required this.permissionLoading,
    required this.accessibilityStatus,
    required this.accessibilityLoading,
    required this.onRootTap,
  });

  final RootStatus? rootStatus;
  final bool rootLoading;
  final List<PermissionStatus> permissionStatuses;
  final bool permissionLoading;
  final AccessibilityStatus? accessibilityStatus;
  final bool accessibilityLoading;
  final VoidCallback onRootTap;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final rootAvailable = rootStatus?.available == true;
    final permissionsGranted = permissionStatuses.isNotEmpty &&
        permissionStatuses.every((permission) => permission.granted);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(l10n.environmentChecks, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: Icon(
                rootAvailable ? Icons.verified_user : Icons.lock_outline,
                color: rootAvailable ? Colors.teal : Colors.orange,
              ),
              title: Text(rootLoading
                  ? l10n.checkingRootStatus
                  : rootAvailable
                  ? l10n.rootAvailable
                  : l10n.rootUnavailable),
              trailing: rootAvailable
                  ? null
                  : OutlinedButton(
                      onPressed: onRootTap,
                      child: Text(l10n.requestRootAuthorization),
                    ),
            ),
            _StatusTile(
              icon: Icons.apps,
              label: l10n.appPermissionStatus,
              value: permissionLoading
                  ? l10n.checkingRootStatus
                  : permissionStatuses.isEmpty
                  ? l10n.noDeclaredPermissions
                  : permissionsGranted
                  ? l10n.granted
                  : l10n.notGranted,
            ),
            _StatusTile(
              icon: Icons.accessibility_new,
              label: l10n.accessibilityStatus,
              value: accessibilityLoading
                  ? l10n.checkingRootStatus
                  : accessibilityStatus?.serviceEnabled == true
                  ? l10n.granted
                  : l10n.notGranted,
            ),
          ],
        ),
      ),
    );
  }
}

class _StatusTile extends StatelessWidget {
  const _StatusTile({required this.icon, required this.label, required this.value});

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: Icon(icon),
      title: Text(label),
      trailing: Text(value),
    );
  }
}

class _McpSwitchCard extends StatelessWidget {
  const _McpSwitchCard({required this.enabled, required this.loading, required this.onChanged});

  final bool enabled;
  final bool loading;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Card(
      child: SwitchListTile.adaptive(
        value: enabled,
        onChanged: loading ? null : onChanged,
        title: Text(l10n.globalMcpSwitch),
        subtitle: Text(enabled ? l10n.mcpEnabledDescription : l10n.mcpDisabledDescription),
      ),
    );
  }
}

class _RuntimeModeCard extends StatelessWidget {
  const _RuntimeModeCard({
    required this.isRootMode,
    required this.loading,
    required this.status,
    required this.onOpenAccessibilitySettings,
    required this.onTalkBackChanged,
  });

  final bool isRootMode;
  final bool loading;
  final AccessibilityStatus? status;
  final VoidCallback onOpenAccessibilitySettings;
  final ValueChanged<bool> onTalkBackChanged;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final accessibility = status;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(l10n.armcpRunningSupport, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            Text(l10n.armcpRunningSupportHint),
            const SizedBox(height: 12),
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: Icon(
                accessibility?.serviceEnabled == true
                    ? Icons.check_circle
                    : Icons.warning_amber,
                color: accessibility?.serviceEnabled == true
                    ? Colors.teal
                    : Colors.orange,
              ),
              title: Text(l10n.armcpAccessibilityService),
              subtitle: Text(
                accessibility?.serviceEnabled == true
                    ? l10n.accessibilityEnabled
                    : l10n.accessibilityNeedsSettings,
              ),
              trailing: OutlinedButton(
                onPressed: onOpenAccessibilitySettings,
                child: Text(l10n.openAccessibilitySettings),
              ),
            ),
            SwitchListTile.adaptive(
              contentPadding: EdgeInsets.zero,
              value: accessibility?.talkBackAdaptation == true,
              onChanged: loading ? null : onTalkBackChanged,
              title: Text(isRootMode ? l10n.talkBackAutoUse : l10n.talkBackGuideEnable),
              subtitle: Text(
                accessibility?.talkBackEnabled == true
                    ? l10n.talkBackEnabled
                    : isRootMode
                    ? l10n.talkBackAutoUseHint
                    : l10n.talkBackGuideEnableHint,
              ),
              secondary: const Icon(Icons.record_voice_over),
            ),
          ],
        ),
      ),
    );
  }
}

class _PermissionsCard extends StatelessWidget {
  const _PermissionsCard({
    required this.loading,
    required this.result,
    required this.autoGrantEnabled,
    required this.autoGrantLoading,
    required this.onAutoGrantChanged,
    required this.onOpenAccessibilitySettings,
  });

  final bool loading;
  final PermissionGrantResult? result;
  final bool autoGrantEnabled;
  final bool autoGrantLoading;
  final ValueChanged<bool> onAutoGrantChanged;
  final VoidCallback onOpenAccessibilitySettings;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(l10n.rootPermissionSupport, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            Text(l10n.grantAllPermissionsHint),
            const SizedBox(height: 12),
            SwitchListTile.adaptive(
              contentPadding: EdgeInsets.zero,
              value: autoGrantEnabled,
              onChanged: autoGrantLoading ? null : onAutoGrantChanged,
              title: Text(l10n.autoGrantPermissions),
              subtitle: Text(l10n.autoGrantPermissionsHint),
              secondary: Icon(Icons.admin_panel_settings, color: autoGrantEnabled ? Colors.teal : null),
            ),
            if (loading) ...[
              const SizedBox(height: 8),
              Text(l10n.checkingRootStatus),
            ],
            if (result != null) ...[
              const SizedBox(height: 12),
              Text(l10n.permissionsGranted),
              Text(l10n.permissionResult(l10n.reasonText(result!.reason))),
              ...result!.permissions
                  .where((permission) => permission.status == 'failed')
                  .map((permission) => _DetailRow(
                        label: l10n.permissionStatus(permission.status),
                        value: permission.name,
                      )),
            ],
            if (result?.state == 'root_unavailable' ||
                (result?.permissions.any((permission) =>
                        permission.name == 'ACCESSIBILITY_SERVICE' && permission.status == 'failed') ?? false)) ...[
              const SizedBox(height: 8),
              Text(l10n.accessibilityServiceHint),
              const SizedBox(height: 8),
              OutlinedButton.icon(
                onPressed: onOpenAccessibilitySettings,
                icon: const Icon(Icons.accessibility_new),
                label: Text(l10n.openAccessibilitySettings),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _RootStatusCard extends StatelessWidget {
  const _RootStatusCard({required this.status, required this.loading, required this.onRetry});

  final RootStatus? status;
  final bool loading;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final isAvailable = status?.state == RootState.available;
    final isUnavailable = status?.state == RootState.unavailable;
    final title = loading
        ? l10n.checkingRootStatus
        : isAvailable
        ? l10n.rootAvailable
        : isUnavailable
        ? l10n.rootUnavailable
        : l10n.rootNeedsAttention;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(isAvailable ? Icons.verified_user : Icons.lock_outline,
                    color: isAvailable ? Colors.teal : Colors.orange),
                const SizedBox(width: 12),
                Text(title, style: Theme.of(context).textTheme.titleMedium),
              ],
            ),
            if (!loading && status != null) ...[
              const SizedBox(height: 16),
              _DetailRow(label: l10n.state, value: l10n.rootState(status!.state.name)),
              _DetailRow(label: l10n.reason, value: l10n.reasonText(status!.reason)),
              _DetailRow(
                label: l10n.latestAction,
                value: l10n.rootAttempt(status!.attempt),
              ),
            ],
            if (!loading && isUnavailable) ...[
              const SizedBox(height: 8),
              Text(l10n.rootAuthorizationHint),
              const SizedBox(height: 12),
              OutlinedButton.icon(
                onPressed: onRetry,
                icon: const Icon(Icons.admin_panel_settings),
                label: Text(l10n.requestRootAuthorization),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _DetailRow extends StatelessWidget {
  const _DetailRow({required this.label, required this.value, this.selectable = false});

  final String label;
  final String value;
  final bool selectable;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(width: 120, child: Text(label)),
          Expanded(child: selectable ? SelectableText(value) : Text(value)),
        ],
      ),
    );
  }
}
