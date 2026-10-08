# Flutter project layout reference

- Source URL: https://docs.flutter.dev/tools/pubspec
- Source URL: https://docs.flutter.dev/reference/flutter-cli
- Retrieved: 2026-10-08

## Relevant findings

- Flutter's package configuration is declared in `pubspec.yaml`; the Flutter
  command-line tool operates on the package selected by the current project
  directory.
- Flutter commands support an explicit target entrypoint, while the standard
  Android build command uses the package's configured Android project and build
  outputs.
- This repository therefore treats `src/` as the Flutter package root and
  provides `script/flutter.sh` as the repository-root command entrypoint. The
  repository-level `build/` directory is linked from the package build path so
  intermediate output remains in the requested location.
