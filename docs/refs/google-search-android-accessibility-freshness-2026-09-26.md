# Android Accessibility hierarchy freshness reference

- Search method: Google via browser MCP
- Search query: `site:developer.android.com AccessibilityService getWindows rootInActiveWindow refresh official`
- Retrieved: 2026-09-26
- Primary source: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService

## Relevant findings

Android documents that an accessibility service may retrieve window content through
`getWindows()` and `getRootInActiveWindow()`. It also explicitly warns that a
service may be unaware of hierarchy changes when it only requests a subset of
event types, and that a node can contain outdated information because window
content may change at any time.

The implementation therefore needs to request window-change notifications,
select the currently active accessibility window, refresh nodes before reading
them, and reject/retry a snapshot if a relevant accessibility event arrives
while it is being serialized. The service's result must not be presented as a
current-screen snapshot solely because `rootInActiveWindow` returned a non-null
object.
