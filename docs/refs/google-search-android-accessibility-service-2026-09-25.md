# Android accessibility service research

- Source URL: https://developer.android.com/guide/topics/ui/accessibility/service?hl=zh-cn
- Source URL: https://developer.android.com/reference/android/app/UiAutomation
- Retrieval date: 2026-09-25

## Relevant findings

- Android documents `AccessibilityService` as a background service that can inspect screen content and interact with applications for accessibility purposes.
- A service that reads the UI hierarchy must declare `android:canRetrieveWindowContent="true"` and use `android.permission.BIND_ACCESSIBILITY_SERVICE`.
- The service can read the hierarchy through `rootInActiveWindow`; Android documents that this may be null when the window is unavailable.
- `UiAutomation` is a privileged/test-oriented accessibility API. ARMCP's current Root UI backend invokes `uiautomator dump` directly and does not declare an ARMCP `AccessibilityService`.
- Android/MIUI may still reject or terminate the platform dump; Root availability does not replace the platform UiAutomation implementation or guarantee success.
