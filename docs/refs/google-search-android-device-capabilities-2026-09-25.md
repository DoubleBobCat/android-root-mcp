# Android device capability research

- Retrieval date: 2026-09-25
- Sources:
  - https://developer.android.com/reference/android/app/UiAutomation
  - https://developer.android.com/tools/adb?hl=zh-cn
  - https://developer.android.com/privacy-and-security/local-network-permission?hl=zh-cn

## Findings

- `UiAutomation` is a privileged/test-oriented API whose default interaction model uses accessibility APIs. UI inspection must report its backend and privilege rather than claim invisibility or universal availability.
- Android's documented debugging surface includes shell operations for device queries, package-manager operations, screenshots, and input. This app keeps bounded root-shell operations behind the runtime root check.
- Android 17 target applications need deliberate local-network permission handling for broad LAN access. The direct listener remains explicitly enabled and authenticated.
- Package and location capabilities are independent. A package being installed does not prove that a runtime permission is granted; location reads return stable permission errors instead of silently degrading.
