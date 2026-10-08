import 'package:flutter/material.dart';

import '../l10n/app_localizations.dart';
import '../platform/android_capability_client.dart';

class ToolSelectionPage extends StatefulWidget {
  const ToolSelectionPage({
    super.key,
    required this.tools,
    required this.selected,
  });

  final List<TokenTool> tools;
  final Set<String> selected;

  @override
  State<ToolSelectionPage> createState() => _ToolSelectionPageState();
}

class _ToolSelectionPageState extends State<ToolSelectionPage> {
  final Set<String> _selected = <String>{};

  @override
  void initState() {
    super.initState();
    _selected.addAll(widget.selected);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.selectTokenTools),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(_selected),
            child: Text(l10n.done),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Text(l10n.selectTokenToolsHint),
          const SizedBox(height: 12),
          ...widget.tools.map(
            (tool) => CheckboxListTile(
              value: _selected.contains(tool.name),
              title: Text(tool.name),
              subtitle: tool.readOnly ? Text(l10n.readOnlyTool) : null,
              onChanged: (enabled) => setState(() {
                if (enabled == true) {
                  _selected.add(tool.name);
                } else {
                  _selected.remove(tool.name);
                }
              }),
            ),
          ),
        ],
      ),
    );
  }
}
