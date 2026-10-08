# Android accessibility service and TalkBack research

- Source URL: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- Guide URL: https://developer.android.com/guide/topics/ui/accessibility/service
- Search URL: https://www.google.com/search?q=site%3Adeveloper.android.com+ACTION_ACCESSIBILITY_SETTINGS+TalkBack+accessibility+service
- Retrieved: 2026-09-30

## Findings

- Android manages an `AccessibilityService` lifecycle; starting one is
  triggered by the user explicitly turning it on in device settings.
- A service can request window-content retrieval. Active windows and
  `AccessibilityNodeInfo` trees are available, but nodes can be stale or
  change during traversal.
- `canPerformGestures` is required to dispatch gestures. Node actions and
  global actions are supported interaction paths.
- TalkBack is itself an accessibility service. ARMCP can detect enabled
  accessibility components and, only with Root, request a secure-settings
  update. Without Root it must open `ACTION_ACCESSIBILITY_SETTINGS` and let
  the user enable TalkBack.
- Enabling TalkBack does not make every application readable or guarantee that
  ARMCP can control every surface; backend and failure reasons remain exposed.
- ARMCP treats TalkBack as an on-demand action. In Root mode the adaptation
  checkbox only permits temporary automatic use during a failed UI read; in
  non-root mode it opens Android accessibility settings for user guidance.
- The implementation's Root recovery path preserves the exact secure-setting
  values it found, performs one bounded retry, and restores those values after
  the retry rather than treating TalkBack as a permanent startup dependency.
- Android documents secure settings as system preferences that normal
  applications cannot write; the Root-only settings mutation is deliberately
  isolated behind the native Root capability boundary. The settings action
  `ACTION_ACCESSIBILITY_SETTINGS` is the supported non-root user-guidance path.
