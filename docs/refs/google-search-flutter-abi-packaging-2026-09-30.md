# Flutter Android ABI packaging research

- Source URL: https://docs.flutter.dev/deployment/android
- Search URL: https://www.google.com/search?q=site%3Adocs.flutter.dev+flutter+build+apk+split-per-abi+universal
- Retrieved: 2026-09-30

## Findings

- Flutter documents Android release APK/App Bundle builds and supports
  architecture-specific APK output through `--split-per-abi`.
- ARMCP uses Flutter's Android target-platform names for the three split
  builds: `android-arm64`, `android-arm`, and `android-x64`.
- A separate release build without `--split-per-abi` produces the universal
  APK. ARMCP labels the resulting four packages `v8a`, `v7a`, `x86_64`, and
  `universe` for distribution.
