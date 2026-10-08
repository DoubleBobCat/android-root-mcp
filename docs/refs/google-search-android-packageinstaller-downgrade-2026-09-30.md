# Android APK installation and downgrade research

- Source URL: https://developer.android.com/reference/android/content/pm/PackageInstaller
- Search URL: https://www.google.com/search?q=site%3Adeveloper.android.com%2Freference%2Fandroid%2Fcontent%2Fpm%2FPackageInstaller+INSTALL_ALLOW_DOWNGRADE
- Retrieved: 2026-09-30

## Findings

- `PackageInstaller` installs, upgrades, and removes APKs through a staged
  `PackageInstaller.Session`.
- A normal application can create a session, but committing may require user
  intervention. Device owner and affiliated profile owner are examples of
  roles that can complete an installation automatically.
- The public API exposes status results such as `STATUS_PENDING_USER_ACTION`,
  `STATUS_FAILURE_CONFLICT`, and `STATUS_FAILURE_INVALID`; it does not grant
  an ordinary application a blanket right to bypass downgrade policy.
- This research is retained as historical feasibility evidence. APK install,
  repair, and downgrade are not part of the ARMCP control surface.
