# Android Auto Backup Reference

- Search method: Google via browser MCP
- Search query: `site:developer.android.com Auto Backup SharedPreferences app update uninstall restore official`
- Retrieved: 2026-09-25
- Primary source: https://developer.android.com/identity/data/autobackup

## Relevant findings

Android Auto Backup includes shared preferences by default for apps targeting Android 6.0/API 23 or later. The Android documentation describes restoring app data after installation through Google Play, device setup, or `adb install`; it also documents explicit XML rules for `sharedpref` data and separate `cloud-backup` and `device-transfer` sections for Android 12/API 31 or later.

The app therefore explicitly includes only `android_root_mcp_settings.xml` in backup and device transfer rules. Token records remain device-local and are not included in backup. A normal APK update with the same application ID and signing identity should retain the existing preferences; uninstall/reinstall or an identity change is a separate restore/install scenario.
