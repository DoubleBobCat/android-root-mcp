# Flutter locale resolution reference

- Source URL: https://docs.flutter.dev/ui/accessibility-and-internationalization/internationalization
- Retrieved: 2026-10-08

## Relevant findings

- Flutter supports an app-level locale override through `MaterialApp.locale`.
- When the override is null, the app can use the device locale and the
  supported locale list to resolve the active localization.
- A locale resolution callback can provide an explicit fallback when the device
  locale does not match a supported locale.

ARMCP uses this behavior for its persisted `system` language choice and returns
English from the resolution callback when the device language is not English or
Simplified Chinese.
