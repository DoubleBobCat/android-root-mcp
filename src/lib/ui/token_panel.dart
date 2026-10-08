import 'package:flutter/material.dart';

import '../l10n/app_localizations.dart';
import '../platform/android_capability_client.dart';
import 'tool_selection_page.dart';

class TokenPanel extends StatefulWidget {
  const TokenPanel({super.key, required this.client});

  final AndroidCapabilityClient client;

  @override
  State<TokenPanel> createState() => _TokenPanelState();
}

class _TokenPanelState extends State<TokenPanel> {
  static const _modes = <String>[
    'one_time',
    'reusable',
    'fixed_duration',
    'unlimited',
  ];

  String _mode = 'reusable';
  final _durationController = TextEditingController(text: '3600');
  List<TokenRecord> _tokens = const <TokenRecord>[];
  List<TokenTool> _tools = const <TokenTool>[];
  Set<String> _newTokenTools = <String>{};
  String? _newSecret;
  String? _error;
  bool _loading = true;
  bool _creating = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _durationController.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final results = await Future.wait([
        widget.client.listTokens(),
        widget.client.listTokenTools(),
      ]);
      if (!mounted) return;
      final tokens = results[0] as List<TokenRecord>;
      final tools = results[1] as List<TokenTool>;
      setState(() {
        _tokens = tokens;
        _tools = tools;
        _newTokenTools = tools
            .where((tool) => tool.readOnly)
            .map((tool) => tool.name)
            .toSet();
        _loading = false;
        _error = null;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = error.toString();
      });
    }
  }

  Future<void> _createToken() async {
    final l10n = AppLocalizations.of(context);
    final duration = int.tryParse(_durationController.text.trim());
    if (_mode == 'fixed_duration' && (duration == null || duration < 1)) {
      setState(() => _error = l10n.fixedDurationError);
      return;
    }

    setState(() {
      _creating = true;
      _newSecret = null;
      _error = null;
    });
    try {
      final token = await widget.client.createToken(
        mode: _mode,
        durationSeconds: _mode == 'fixed_duration' ? duration : null,
        enabledTools: _newTokenTools,
      );
      if (!mounted) return;
      setState(() {
        _tokens = <TokenRecord>[token, ..._tokens];
        _newSecret = token.secret;
        _creating = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _creating = false;
        _error = error.toString();
      });
    }
  }

  Future<void> _delete(TokenRecord token) async {
    final deleted = await widget.client.deleteToken(token.id);
    if (!mounted || !deleted) return;
    await _load();
  }

  Future<void> _setTools(TokenRecord token, Set<String> tools) async {
    final updated = await widget.client.setTokenTools(token.id, tools);
    if (!mounted || !updated) return;
    setState(() {
      _tokens = _tokens
          .map(
            (item) => item.id == token.id
                ? TokenRecord(
                    id: item.id,
                    mode: item.mode,
                    createdAt: item.createdAt,
                    expiresAt: item.expiresAt,
                    enabledTools: tools,
                    useCount: item.useCount,
                    secret: item.secret,
                  )
                : item,
          )
          .toList();
    });
  }

  Future<void> _selectTools({TokenRecord? token}) async {
    final selected = await Navigator.of(context).push<Set<String>>(
      MaterialPageRoute(
        builder: (_) => ToolSelectionPage(
          tools: _tools,
          selected: token?.enabledTools ?? _newTokenTools,
        ),
      ),
    );
    if (!mounted || selected == null) return;
    if (token == null) {
      setState(() => _newTokenTools = selected);
    } else {
      await _setTools(token, selected);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              l10n.mcpAccessTokens,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            Text(l10n.tokenSecretOnce),
            const SizedBox(height: 4),
            Text(l10n.readOnlyDefault),
            const SizedBox(height: 16),
            DropdownButtonFormField<String>(
              initialValue: _mode,
              decoration: InputDecoration(labelText: l10n.tokenLifetime),
              items: _modes
                  .map(
                    (mode) => DropdownMenuItem(
                      value: mode,
                      child: Text(l10n.tokenMode(mode)),
                    ),
                  )
                  .toList(),
              onChanged: _creating || _loading
                  ? null
                  : (value) => setState(() => _mode = value!),
            ),
            if (_mode == 'fixed_duration') ...[
              const SizedBox(height: 12),
              TextField(
                controller: _durationController,
                keyboardType: TextInputType.number,
                decoration: InputDecoration(labelText: l10n.durationSeconds),
              ),
            ],
            if (_tools.isNotEmpty) ...[
              const SizedBox(height: 12),
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.tune),
                title: Text(l10n.tokenPermissions),
                subtitle: Text(l10n.selectedTools(_newTokenTools.length)),
                trailing: const Icon(Icons.chevron_right),
                onTap: _creating || _loading ? null : () => _selectTools(),
              ),
            ],
            const SizedBox(height: 12),
            FilledButton.icon(
              onPressed: _creating || _loading ? null : _createToken,
              icon: const Icon(Icons.key),
              label: Text(l10n.createToken),
            ),
            if (_newSecret != null) ...[
              const SizedBox(height: 16),
              SelectableText('${l10n.copyTokenNow}\n$_newSecret'),
            ],
            if (_error != null) ...[
              const SizedBox(height: 12),
              Text(
                _error!,
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
            ],
            const SizedBox(height: 20),
            Text(
              l10n.issuedTokens,
              style: Theme.of(context).textTheme.labelLarge,
            ),
            if (_loading)
              const Padding(
                padding: EdgeInsets.only(top: 12),
                child: LinearProgressIndicator(),
              )
            else if (_tokens.isEmpty)
              Padding(
                padding: EdgeInsets.only(top: 12),
                child: Text(l10n.noTokensIssued),
              )
            else
              ..._tokens.map(
                (token) => ExpansionTile(
                  tilePadding: EdgeInsets.zero,
                  title: Text('${l10n.tokenMode(token.mode)} · ${token.id}'),
                  subtitle: Text('${l10n.uses}: ${token.useCount}'),
                  trailing: IconButton(
                    tooltip: l10n.deleteToken,
                    onPressed: () => _delete(token),
                    icon: const Icon(Icons.delete_outline),
                  ),
                   children: [
                     ListTile(
                       leading: const Icon(Icons.tune),
                       title: Text(l10n.tokenPermissions),
                       subtitle: Text(l10n.selectedTools(token.enabledTools.length)),
                       trailing: const Icon(Icons.chevron_right),
                       onTap: () => _selectTools(token: token),
                     ),
                   ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}
