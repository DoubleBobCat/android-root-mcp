# Deployment

[English](Deployment.md) · [简体中文](Deployment.zh-CN.md)

## Build target

The deliverable is an Android APK produced by the checked-in Flutter/Gradle project. Release builds enable R8 code shrinking and resource shrinking. For installable APK distribution, build the three ARMCP packages with:

```bash
bash script/build_android_apks.sh
```

The Flutter and Gradle intermediate files remain below `build/`. The three
distribution APKs are copied directly below `release/`. Use an Android App Bundle for Play distribution when
possible.

## Runtime modes

- **Off:** no MCP listener.
- **Loopback:** authenticated listener for on-device/local debugging; default bind mode.
- **LAN:** authenticated listener on local network interfaces after the explicit global MCP switch is enabled and required Android network permission is available.

The implementation listens on port `8787` and binds all local interfaces while MCP is enabled. It advertises the current non-loopback IPv4 address. If the device has no non-loopback address, the advertised URL falls back to loopback and is only locally reachable.

The Flutter UI shows the current debug URL `/debug/` and MCP endpoint `/mcp`. They are reachable only while the global MCP switch is enabled and the listener reports running. The debug URL uses `GET`; the MCP URL requires authenticated `POST` requests. The displayed address is the device's current LAN address when available.

## Network policy

Prefer TLS for LAN deployment. If an initial debug build uses cleartext, it must be explicitly labeled, scoped to a controlled network, and documented as temporary development mode. Android local-network permission behavior must follow the target SDK; Android 17-targeting builds need explicit handling of `ACCESS_LOCAL_NETWORK` or an approved system-mediated alternative.

## Release checklist

- Confirm root and UI backend capability claims on target API levels.
- Confirm server disabled by default.
- Confirm no secrets, debug pages, or unrestricted shell paths are exposed without authentication.
- Confirm release signing and version values from checked-in configuration.
- Build, install, launch, run focused integration tests, then run the complete configured checks.

## CI/CD

The `main` branch has automatic CI in both `.github/workflows/ci.yml` and
`.gitea/workflows/ci.yml`. CI runs dependency resolution, Dart analysis,
Flutter tests, and a debug APK build on pushes and pull requests targeting
`main`.

Release CD is manual only. Dispatch the matching `Release` workflow from the
`main` branch, provide an existing `v`-prefixed tag from `main` and its title,
and set `confirm_release` to exactly `true`. The workflow verifies that the tag
belongs to the selected `main` history, then builds the three APKs and
publishes a release plus the APK assets through the configured host API. CI
also runs for `v*` tag pushes.

Configure these repository variables on each host:

- `RELEASE_API_URL`: release creation API base, such as `https://api.github.com`
  or `https://gitea.example.com/api/v1`.
- `RELEASE_UPLOAD_URL`: asset upload API base, such as
  `https://uploads.github.com` or the Gitea API base.
- `RELEASE_REPOSITORY`: `owner/repository`.

Configure `RELEASE_TOKEN` as an Actions secret with permission to create
releases and upload assets. GitHub environments may additionally be configured
with required reviewers, but the explicit dispatch input remains the portable
approval gate because Gitea ignores `jobs.<job_id>.environment`.
## ARMCP release APK packaging

The release package set has three APKs:

| Product name | Android ABI | Flutter output |
| --- | --- | --- |
| `release/armcp-v8.apk` | `arm64-v8a` | `app-arm64-v8a-release.apk` |
| `release/armcp-v7.apk` | `armeabi-v7a` | `app-armeabi-v7a-release.apk` |
| `release/armcp-universe.apk` | arm64-v8a + armeabi-v7a + x86_64 | `app-release.apk` |

Build all three with:

```bash
bash script/build_android_apks.sh
```

Flutter's `--split-per-abi` creates the architecture-specific APKs; the
final build creates the universal APK. The script copies all three release
packages directly into `release/` with the product names above. The x86_64 split
is retained as an intermediate build output but is not part of the release set.
