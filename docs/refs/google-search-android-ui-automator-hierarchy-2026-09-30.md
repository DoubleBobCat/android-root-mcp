# Android UI hierarchy backend research

- Source URL: https://developer.android.com/training/testing/other-components/ui-automator
- Search URL: https://www.google.com/search?q=site%3Adeveloper.android.com+UiAutomation+AccessibilityNodeInfo+shell+uiautomator+dump
- Retrieved: 2026-09-30

## Findings

- UI Automator interacts with accessibility window nodes from outside an
  application process and is documented primarily as a UI test framework.
- An accessibility service with `canRetrieveWindowContent` is the supported
  long-lived application boundary for reading active-window node content.
- Root does not expose a documented universal API that directly reads another
  app's private View hierarchy without accessibility/UiAutomation or app
  cooperation. `dumpsys window` describes windows, not a general view tree;
  screenshots are pixels, not structure.
- ARMCP keeps its connected accessibility service as the primary backend and
  retains Root `uiautomator dump` as a disclosed fallback only when the ARMCP
  service is unavailable.
