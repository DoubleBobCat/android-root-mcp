# Flutter Internationalization Reference

- Search method: Google via browser MCP
- Search query: `site:docs.flutter.dev internationalization localization Flutter official`
- Retrieved: 2026-09-24
- Primary source: https://docs.flutter.dev/ui/internationalization

## Relevant Findings

Flutter internationalization is configured through `MaterialApp` with a supported locale list and localization delegates. The official guide recommends `flutter_localizations` for localized Material and Cupertino widgets, and describes overriding the app locale at runtime.

This project uses the same `MaterialApp.locale`, `supportedLocales`, and delegate boundary for an explicit Chinese/English selector. App-specific strings are kept in a small typed localization delegate because this slice has a fixed two-language surface and does not need generated plural or date messages yet.
