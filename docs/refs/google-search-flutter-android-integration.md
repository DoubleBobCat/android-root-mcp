# Flutter Android Integration Reference

- Search method: Google via browser MCP
- Search query: `site:docs.flutter.dev Android Flutter platform integration official`
- Retrieved: 2026-09-24
- Primary source: https://docs.flutter.dev/platform-integration/android

## Relevant Findings

Flutter's Android platform integration documentation identifies native Android integration as the place to call Android APIs, host native Android views, launch native activities, and request local-network permissions before opening Dart sockets.

For this project, the Flutter layer should own user-facing controls and state presentation, while Android-specific root, device-control, screenshot, shell, and network lifecycle work should be exposed through a deliberate native integration boundary.
