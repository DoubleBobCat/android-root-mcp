# Android accessibility dynamic-window reference

- Search method: Google via browser MCP
- Retrieved: 2026-09-26
- Primary source: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService

## Relevant findings

Android documents that an accessibility service may retrieve window content through
`getWindows()` and `getRootInActiveWindow()`. It also warns that accessibility
content may change at any time and that a service can receive an outdated node.

The ARMCP implementation therefore treats `AccessibilityNodeInfo.refresh()` and
child retrieval as best-effort operations. A node that cannot be refreshed during
a dynamic transition can still expose readable current fields. The service retries
with a fresh active-window root and returns the latest complete snapshot it has
when the application continues changing during the bounded retry window.

Most importantly, a connected ARMCP accessibility service remains the selected
backend even when a particular active window cannot currently be serialized. It
must not silently fall back to `uiautomator dump`, because that path creates or
uses Android's singleton UiAutomation machinery and may conflict with an existing
automation session.
