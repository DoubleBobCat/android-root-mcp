# Android foreground service research

- Source URL: https://developer.android.com/develop/background-work/services/fgs?hl=zh-cn
- Source URL: https://developer.android.com/develop/background-work/services/fgs/launch?hl=zh-cn
- Retrieval date: 2026-09-25

## Relevant findings

- Android foreground services are intended for user-noticeable work and must display a status-bar notification.
- Starting a foreground service requires starting the service and then promoting it with `startForeground()`.
- Apps targeting Android 12/API 31 or higher have restrictions on starting foreground services while already in the background, so the ARMCP enable action remains an explicit user action.
- Apps targeting Android 14/API 34 or higher must declare and satisfy the applicable foreground-service type and permissions. This slice uses the generic foreground-service declaration and reports service failures rather than silently falling back.
