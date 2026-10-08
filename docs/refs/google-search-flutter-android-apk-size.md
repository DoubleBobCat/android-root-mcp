# Flutter Android APK Size Reference

- Search method: Google via browser MCP
- Search query: `site:docs.flutter.dev deployment android split-per-abi APK size official`
- Retrieved: 2026-09-24
- Primary source: https://docs.flutter.dev/deployment/android

## Relevant Findings

Flutter's Android release documentation distinguishes app bundles from APKs and identifies APK architecture packaging as a release concern. A universal APK contains native binaries for multiple target architectures, while architecture-specific APKs avoid shipping unused native binaries to a device.

The project should use a release build with R8 code shrinking and resource shrinking enabled, and use `flutter build apk --split-per-abi` when distributing installable APKs. The split command is the appropriate way to produce architecture-specific Flutter APKs without excluding supported device architectures from the application configuration.
