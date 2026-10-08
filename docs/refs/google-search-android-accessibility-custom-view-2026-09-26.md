# Android accessibility custom-view reference

- Search method: Google via browser MCP
- Retrieved: 2026-09-26
- Primary source: https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo
- Supporting source: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService

## Relevant findings

Android documents that an accessibility window is exposed as an
`AccessibilityNodeInfo` tree, and that the tree does not necessarily map one to
one to the normal View hierarchy. A custom view is free to report itself as a
tree of accessibility nodes, which also means it may expose only the custom
view itself when it does not provide virtual descendants or semantic content.

The device evidence matches this boundary: Taobao's active
`com.taobao.themis.container.app.TMSActivity` exposes a full-screen
`android.view.View` with no text, content description, or descendants for the
order body. The visible text in the screenshot therefore cannot be recovered
by changing XML parsing, XPath, filtering, or snapshot retry logic.

The same Android reference says that window content is available to an
accessibility service only through the content exposed by the target window.
ARMCP's `canRetrieveWindowContent=true`, requested window/content events,
interactive-window flag, node refresh, and bounded retries improve freshness
but cannot synthesize missing semantics.
