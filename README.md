# ARMCP (Android Root MCP)

[English](README.md) · [简体中文](README.zh-CN.md)

Flutter Android application whose APK directly serves an authenticated MCP server and its same-origin debug page. ARMCP is the operator-facing alias for Android Root MCP; the Android package ID remains `com.doublecat.android_root_mcp`. The app provides Flutter-to-Kotlin health reporting, runtime Root status, a user-triggered Root authorization attempt, a risk-confirmed permission action, a persisted global MCP setting, a visible foreground-service listener, an authenticated local-network MCP server, token lifecycle controls, and `/debug/`.

## Entry points

- Flutter UI: `src/lib/main.dart`
- Flutter-to-native client: `src/lib/platform/android_capability_client.dart`
- Android host: `src/android/app/src/main/kotlin/com/doublecat/android_root_mcp/MainActivity.kt`
- Platform channel: `com.doublecat.android_root_mcp/capabilities`
- Implemented native methods: `getHealth`, `getPermissionStatus`, `getAccessibilityStatus`, `getLocale`, `setLocale`, `grantAllPermissions`, and accessibility-settings guidance
- Root methods: `getRootStatus`, `attemptRoot` (the latter requests a Root-manager authorization attempt and reports the resulting status; it does not root the device)
- Runtime methods: `getRuntimeMode`, `setRuntimeMode`, `getMcpEnabled`, `setMcpEnabled`, `getAutoGrantPermissions`, and `setAutoGrantPermissions`
- Direct MCP server: `POST /mcp`, `GET /debug/`, registry-backed device, app, input, shell, screenshot, Root UI inspection, and hardware-random XPath input tools

## Commands

The Flutter package root is `src/`. Use the checked-in wrapper so commands can be
run from the repository root:

```bash
bash script/flutter.sh pub get
bash script/flutter.sh analyze
bash script/flutter.sh test
bash script/flutter.sh build apk
bash script/flutter.sh run
```

The current `flutter build apk` succeeds locally. Device and release setup may still require a complete Android SDK command-line toolchain and accepted licenses; check the local state with `flutter doctor -v`.

Build support files and intermediate output are rooted at `build/`. Release APKs
are copied directly to `release/` by `script/build_android_apks.sh`:
`armcp-v8.apk`, `armcp-v7.apk`, and `armcp-universe.apk`.

## Repository layout

```text
src/       Flutter package and Android host (`src/android`, `src/lib`)
build/     Flutter/Gradle build support and intermediate output
docs/      Product, protocol, operations, and research documentation
test/      Flutter widget and contract tests
script/    Repository build and Flutter command wrappers
release/   Release APKs, one file per supported package
```

## Architecture boundary

```text
Flutter UI/state
        -> typed platform channel
Kotlin Android capability layer
        -> direct MCP Streamable HTTP server
        -> same-origin schema-driven /debug/ page
```

The APK provides MCP directly. When the global switch is enabled, the listener
binds local network interfaces on port `8787`; all MCP POST requests require a
Bearer token. See [`docs/README.md`](docs/README.md) ([中文](docs/README.zh-CN.md))
for the document map and [`docs/Roadmap.md`](docs/Roadmap.md)
([中文](docs/Roadmap.zh-CN.md)) for implementation slices.
