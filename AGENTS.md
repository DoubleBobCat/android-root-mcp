# Agent Instructions

## Repository Status

- Documentation is now the source of truth for the planned product; Slice 0 Flutter/Android scaffolding exists.
- The repository root is intentionally limited to `src`, `build`, `docs`, `test`, `script`, `.github`, `.gitea`, `AGENTS.md`, `README.md`, and `release`.
- The Flutter package root is `src`; its Dart entrypoint is `src/lib/main.dart` and Android entrypoint is `src/android/app/src/main/kotlin/com/doublecat/android_root_mcp/MainActivity.kt`.
- The generated project uses Flutter 3.44.0 / Dart 3.12.0, Android Gradle Plugin 9.0.1, Kotlin 2.3.20, and Gradle 9.1.0. Confirm these values from checked-in configuration before changing them.
- Do not invent project commands. Run the standard Flutter commands through `bash script/flutter.sh` from the repository root; use `flutter doctor -v` to inspect the local toolchain.
- Read the applicable document under `docs/` before changing a boundary, protocol, token policy, or tool contract. Keep changes atomic: one capability, one contract, one focused verification.
- The intended product is a Flutter Android MCP client/server with a global enable switch, root-status detection and escalation, LAN access, and expiring MCP tokens. Treat these as requirements, not implemented behavior.

## Information Retrieval

- External information gathering is mandatory through MCP browser tools.
- Use Google as the search engine; direct official-site search URLs are also allowed for sources such as GitHub.
- Save every research result needed for design or implementation as Markdown under `docs/refs/`, including the source URL, retrieval date, and the relevant findings.
- Prefer official Android, Flutter, and Model Context Protocol documentation. Do not present search-result snippets or unverified assumptions as repository facts.
- `/home/doublecat/Project/zen-mcp/debug-frontend/` is only a reference for metadata-driven forms, connection status, results, and bounded call logs. Do not copy its Go/WebSocket bridge architecture: this APK provides MCP directly.

## Platform Boundaries

- Keep Flutter UI and state in Dart, and put privileged Android operations behind a native Android platform boundary with explicit method contracts.
- The current platform channel is `com.doublecat.android_root_mcp/capabilities`; implemented methods include health/root status, MCP lifecycle, locale, token management, runtime mode, accessibility status, TalkBack adaptation, and accessibility-settings guidance. Extend the contract atomically and update its focused test when adding a method.
- Validate root availability at runtime and expose a user-triggered acquisition attempt when unavailable; never assume that an installed APK has root.
- The desired UI automation surface includes app launch, tap, swipe, text input, view-tree/content reads, gestures, key events, screenshots, device state, and shell commands. Check Android API level and privilege requirements for each operation.
- Android's `UiAutomation` is a privileged/test-oriented API that uses accessibility APIs by default and can inject input, inspect windows/nodes, take screenshots, and execute shell commands. Do not claim it is a general production-app bypass or that it is invisible to other software without device-specific verification.
- Treat the requirement that MCP control be unnoticed by non-root software as an unresolved security/design constraint. Do not implement stealth, evasion, or concealment behavior without an explicit, reviewed design decision and platform evidence.
- Do not describe root shell or UI automation as invisible. Every backend must report capability, privilege, API level, and failure reason.

## MCP and LAN Access

- LAN MCP access must require authentication. Model token lifetimes explicitly for one-time, reusable, fixed-duration, and unlimited modes, with physical deletion and audit-safe handling defined before implementation.
- For HTTP MCP transports, consult and follow the applicable MCP authorization specification and use secure transport/token validation appropriate to the target Android version and LAN threat model.
- Bind network listeners deliberately, minimize exposure, and document discovery, port, cleartext/TLS, local-network permission, and lifecycle behavior once implemented.
- The Android process itself serves MCP and the debug page; do not add a bridge, proxy, extension, or second MCP server.

## Verification

- Run `flutter analyze`, `flutter test`, and `flutter build apk` for the current scaffold. The current APK build succeeds, but device/release setup still depends on the Android SDK state.
- `flutter doctor -v` currently reports missing Android cmdline-tools and unknown Android license status; record any resulting build limitation instead of guessing.
- Once MCP and Android capability slices exist, verify the focused Android integration first, then run the exact checks defined by the checked-in manifests and CI configuration.
