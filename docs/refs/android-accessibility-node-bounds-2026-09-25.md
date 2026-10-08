# Android Accessibility Node Bounds Reference

- Search method: Google via browser MCP
- Retrieved: 2026-09-25
- Primary source: https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo#getBoundsInScreen(android.graphics.Rect)

## Relevant findings

The official `AccessibilityNodeInfo` API exposes `getBoundsInScreen(Rect)` for reading an accessibility node's bounds in screen coordinates. ARMCP uses the resulting rectangle as a top-left-origin screen-pixel rectangle and derives the integer center with `left + (right - left) / 2` and `top + (bottom - top) / 2`. Those values are exposed by the formatted UI inspection documents for direct use with the bounded `tap` tool.
