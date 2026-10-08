# Flutter Platform Channels Reference

- Search method: Google via browser MCP
- Search query: `site:docs.flutter.dev platform channels Flutter Android official`
- Retrieved: 2026-09-24
- Primary source: https://docs.flutter.dev/platform-integration/platform-channels

## Relevant findings

Flutter documents platform channels as an asynchronous message boundary between Dart UI code and platform code. Android implementations use Kotlin or Java, channel names must be unique, and platform calls need deliberate thread handling.

This supports a small Flutter-to-Kotlin contract for root status, MCP lifecycle, token metadata, and capability status. Privileged Android work stays on the Android side; Flutter owns presentation and operator actions.
