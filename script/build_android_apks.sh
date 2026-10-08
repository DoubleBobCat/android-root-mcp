#!/usr/bin/env bash
set -euo pipefail

# ARMCP release packages. Flutter's ABI names map as follows:
#   v8      -> arm64-v8a
#   v7      -> armeabi-v7a
#   universe -> one universal APK containing all supported ABIs

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_root="$(cd "$script_dir/.." && pwd)"
flutter="$script_dir/flutter.sh"
build_dir="$project_root/build"
flutter_build_link="$project_root/src/build"

cleanup() {
  rm -f "$flutter_build_link"
  rm -rf "$build_dir"
}
trap cleanup EXIT

rm -f "$flutter_build_link"
rm -rf "$build_dir"
mkdir -p "$build_dir"
ln -s ../build "$flutter_build_link"

"$flutter" build apk --release --split-per-abi

mkdir -p "$project_root/release"
rm -rf "$project_root/release/v7" "$project_root/release/v8" "$project_root/release/universe"
cp "$project_root/build/app/outputs/flutter-apk/app-arm64-v8a-release.apk" \
  "$project_root/release/armcp-v8.apk"
cp "$project_root/build/app/outputs/flutter-apk/app-armeabi-v7a-release.apk" \
  "$project_root/release/armcp-v7.apk"

"$flutter" build apk --release

cp "$project_root/build/app/outputs/flutter-apk/app-release.apk" \
  "$project_root/release/armcp-universe.apk"

printf 'ARMCP APKs written to %s/release\n' "$project_root"
