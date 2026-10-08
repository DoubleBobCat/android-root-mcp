# Android ADB Reference

- Search method: Google via browser MCP
- Search query: `site:developer.android.com adb shell input screencap official`
- Retrieved: 2026-09-24
- Primary source: https://developer.android.com/tools/adb

## Relevant Findings

Android's ADB documentation describes ADB as a client-server tool that communicates with a device and provides access to a device Unix shell. It documents device selection, `adb shell` command execution, wireless debugging prerequisites, and port forwarding.

ADB is a development/debugging channel with device-side authorization and version-dependent behavior. It should not be treated as equivalent to an in-app root channel or as evidence that a production Flutter APK can silently obtain the same capabilities.
