# Operation

[English](Operation.md) · [简体中文](Operation.zh-CN.md)

## Start and stop

The operator enables MCP in Flutter. The app checks root capability, local-network permission, port validity, and token policy before starting the ARMCP foreground service. The service shows a persistent notification while the listener is enabled, including when the Flutter activity is in the background. Disabling the switch closes the listener, stops the service, and invalidates active request execution; it does not silently restart the server. Android force-stop, device shutdown, policy restrictions, or a process/service kill can still stop it; the next explicit enable or eligible service restart reports the resulting state.

The loopback server uses a bounded client executor. It accepts at most four simultaneous request handlers and queues at most 32 additional connections; excess connections are closed. Request bodies are limited to 1 MiB. Stopping MCP closes the listener and interrupts the client executor so repeated switch changes cannot accumulate one thread per connection.

## Tokens

- Create tokens only from the Flutter operator UI or an authenticated local control path.
- Display the secret once and instruct the operator to copy it immediately.
- Delete suspected or unused tokens. Deletion removes the verifier and metadata immediately.
- Unlimited mode is explicit and deletable; it is not a default.
- New tokens start with read-only tools enabled. Launch, input, and shell operations are individually disabled until selected; `shell_exec` is always presented as an explicit high-risk switch.
- On process restart, reusable/fixed/unlimited records remain with their individual tool switches; one-time records remain usable only if they were not atomically consumed. This behavior requires persistence tests.

## Debug page

When enabled, the server listens on port `8787` on local network interfaces. Open the displayed `/debug/` URL, enter a reusable current token, and click `Refresh tools` or a tool action. The debug page is served by the APK's MCP server and sends authenticated POST requests to the same-origin `/mcp` endpoint. No `adb reverse` is required when the device and computer can reach each other over the LAN.

For an HTTP MCP client such as CC Switch, the header value must include the authentication scheme. The endpoint supports legacy MCP clients that probe with `ping` as well as clients that use `server/discover`, `initialize`, `tools/list`, and `tools/call`:

```json
{
  "type": "http",
  "url": "http://<device-lan-ip>:8787/mcp",
  "headers": {
    "Authorization": "Bearer <reusable-token>"
  }
}
```

The token itself is not a complete `Authorization` header value. A raw value such as `armcp_...` returns `401`; `Bearer armcp_...` is required. Use a reusable or fixed-duration token for MCP clients because initialization, tool discovery, and tool calls are separate authenticated requests. A one-time token is consumed by the first successful request.

The Flutter control surface displays both the current debug URL and MCP URL, together with the listener state. The debug page requires only a token for MCP actions: enter it once in the page and use the action buttons. The debug page itself is public on the selected local interface; MCP requests remain protected by Bearer-token authentication.

## Language

The control surface supports System, English, and Simplified Chinese. System is
the default: Flutter follows the device locale when it is supported and uses
English for unsupported locales. English or Simplified Chinese can be selected
explicitly in Settings and applies immediately to the Flutter UI; the native
MCP protocol and debug endpoint remain unchanged.

The debug page reads the same persisted language setting. Its language selector writes the shared setting through `POST /debug/locale` and reloads the page, so the next APK screen load uses the same language.

## Main navigation

The app has four bottom destinations:

- **Home:** native boundary health, Root state and authorization action,
  non-accessibility permission state, accessibility-service state, and MCP
  switch.
- **Runtime support and permissions:** Root permission controls when Root mode
  is selected; ARMCP running support and accessibility guidance when non-Root
  mode is selected.
- **MCP access tokens:** token creation, tool allow-lists, and deletion.
- **Settings:** Root/non-Root operation mode and language selection.

The setting is stored in `android_root_mcp_settings.xml` and is included in Android backup/device-transfer rules. A normal APK upgrade must keep the same application ID and signing identity to retain local app data. Uninstall/reinstall, clearing app data, changing the application ID, or installing with a different signing identity removes or separates the local preference; backup restore is subject to Android backup availability and user/device settings. ARMCP is only an operator-facing alias and does not replace the package ID.

## Diagnostics

Show root state, Android API, target SDK, bind mode, permission state, enabled state, and last error code. Never show raw token material, unrestricted shell output, or full screenshots in persistent logs.

## All permissions

The ARMCP control surface has an **Automatically grant all permissions** switch, disabled by default. Turning it on first shows a risk warning because Root is used to attempt every permission declared in the APK; this can expand the impact of a compromised ARMCP process and may bypass the normal user-consent workflow. After confirmation, native Android persists the opt-in and runs the existing bounded grant operation when MCP is enabled and when the app opens with MCP already enabled. The result shows the overall outcome and lists only permissions that failed; granted and already-granted permissions are summarized rather than listed individually. Signature, privileged, special-access, notification, device-policy, and OS-restricted permissions are not promised and remain visible as failures. Turning the switch off stops future automatic attempts; it does not revoke permissions already granted.

UI inspection declares an ARMCP `AccessibilityService` with `canRetrieveWindowContent=true`. The service is the primary backend because it reads active-window `AccessibilityNodeInfo` trees without registering another UiAutomation session. Dynamic applications may invalidate nodes while they are being read, so refresh and child retrieval are best effort with bounded retries and a latest-complete-snapshot fallback. Once the service is connected, ARMCP does not switch to `uiautomator dump` just because that window is temporarily unreadable; it reports a service-backed inspection error instead. On a rooted device ARMCP attempts to enable this special access; when Root is unavailable, the control surface opens Android Accessibility settings for explicit user confirmation. The Root `uiautomator dump` fallback is reserved for a service that is unavailable and may return `UI_AUTOMATION_CONFLICT` when another automation session is active.

## Incident response

1. Disable the global MCP switch.
2. Delete all active tokens.
3. Collect redacted audit metadata and device/API information.
4. Recheck root and network state before re-enabling.
5. Preserve the failing tool request as a minimal reproducible fixture without secrets.
# TalkBack operation policy

- **Non-root mode:** the TalkBack control is an onboarding shortcut. Enabling
  it opens Android's accessibility settings and the user enables TalkBack
  there. ARMCP does not modify secure settings.
- **Root mode:** the control means “allow on-demand TalkBack recovery”. It does
  not start TalkBack when the tab is opened or when the checkbox is selected.
  There is no separate persistent TalkBack-start action.
- During Root `view_tree`/`view_content`, if the accessibility backend fails and
  a bounded screenshot contains sampled non-background pixels, ARMCP temporarily enables
  TalkBack, retries the read once, and restores the exact previous accessibility
  service list and global accessibility flag afterward.
- If the preference is disabled, the fallback is not attempted. If Root is
  unavailable or TalkBack is not installed, the original structured UI error is
  returned.
