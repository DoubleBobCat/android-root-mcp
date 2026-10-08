# MCP and Tool Specification

[English](MCP-Spec.md) · [简体中文](MCP-Spec.zh-CN.md)

## Direct transport

- Endpoint: `POST /mcp` and authenticated `GET /mcp` served by the Android APK process.
- Binding: MCP Streamable HTTP and UTF-8 JSON-RPC messages.
- Authentication: `Authorization: Bearer <opaque-token>` on every request.
- Unauthenticated response: HTTP 401 with `WWW-Authenticate` and the protected-resource metadata location required by the applicable MCP authorization specification.
- Debug page: same-origin `GET /debug/`; it uses the same endpoint and token policy.
- Debug tool buttons MUST send `method: "tools/call"` with `params.name` set to the selected registry tool and `params.arguments` set to the form values. The registry tool name is not itself a JSON-RPC method.
- Debug locale control: same-origin `POST /debug/locale` changes only the shared UI language (`en` or `zh`); it does not grant MCP access or alter token policy.
- No WebSocket bridge, proxy, extension, or second MCP server is part of the MVP.

The prototype opaque bearer token is issued and verified by an embedded component in the APK. Its protected-resource metadata is present, but the complete OAuth authorization-server flow is not implemented yet. Before LAN release, validate the exact current MCP authorization metadata and OAuth requirements against the saved official reference; do not treat the prototype bearer flow as final compliance.

The current transport rejects non-JSON MCP POST bodies with `415`, malformed JSON with JSON-RPC `-32700`, invalid JSON-RPC envelopes with `-32600`, unknown methods with `-32601`, and unknown tools/invalid tool parameters with structured tool errors. Notifications are accepted with `202` and no response body. For client compatibility, the server supports both the legacy `ping` health check and modern `server/discover` in addition to the legacy `initialize` handshake and `tools/*` methods.

`GET /mcp` authenticates the Bearer token and returns a primed `text/event-stream` response for clients that open the Streamable HTTP SSE channel. JSON-RPC requests remain POST requests. A one-time token is intentionally unsuitable for a sequence of SSE and POST calls; the Debug page recommends a reusable token.

## Authorization and permission model

The MCP operation process has a read-only baseline. Each bearer token has an independent tool allow-list. The allow-list is stored with the token, is never shared with another token, and is enforced after bearer authentication and before tool execution.

- Read-only tools are enabled by default for a new token.
- `launch_app`, `tap`, `random_tap_by_xpath`, `swipe`, `input_text`, `key_event`, and `shell_exec` are disabled by default.
- `shell_exec` is disabled by default even for reusable, fixed-duration, and unlimited tokens.
- `tools/list` returns only tools enabled for the authenticated token.
- An unauthorized tool call returns an `isError: true` tool result with `TOKEN_TOOL_DISABLED`; the backend is never invoked.
- The permission allow-list is an additional boundary. Root availability, Android API support, runtime permissions, and tool capability checks still apply.

The HTTP authorization profile requires a Bearer token on every protected request. The server validates it before JSON-RPC dispatch. Token deletion physically removes the verifier and metadata; the protocol has no revoked-token field or revoked-token listing state.

## Error model

Tool failures return MCP `isError: true` with a JSON object containing:

```json
{
  "code": "ROOT_UNAVAILABLE",
  "message": "Root capability is unavailable",
  "retryable": true,
  "details": { "state": "unavailable" }
}
```

Codes include `SERVER_DISABLED`, `AUTH_REQUIRED`, `TOKEN_EXPIRED`, `TOKEN_SPENT`, `TOKEN_TOOL_DISABLED`, `ROOT_UNAVAILABLE`, `CAPABILITY_UNSUPPORTED`, `API_LEVEL_UNSUPPORTED`, `INVALID_ARGUMENT`, `EXECUTION_FAILED`, and `TIMEOUT`.

## Tool contract

Every tool definition includes `name`, `description`, `inputSchema`, `requiredCapabilities`, `timeoutMs`, and `sensitiveOutput`. Tool names are stable snake_case names. A tool performs one atomic operation; batching belongs in a later design.

## Initial tool inventory

| Tool | Required input | Required capability | Status |
| --- | --- | --- | --- |
| `device_status` | none | `device.read` | Prototype implemented; device verification pending |
| `root_status` | none | `root.probe` | Prototype implemented |
| `list_apps` | optional system-app filter | `app.read` | Prototype implemented; package-manager result |
| `app_permissions` | package name | `permission.read` | Prototype implemented; requested/granted state |
| `location_status` | none | `location.read` | Prototype implemented; provider and permission state |
| `get_location` | none | `location.read` | Prototype implemented; permission-gated last known location |
| `battery_status` | none | `device.read` | Prototype implemented |
| `network_status` | none | `device.read` | Prototype implemented |
| `display_info` | none | `device.read` | Prototype implemented |
| `launch_app` | package name | `app.launch` | Prototype implemented; device verification pending |
| `tap` | x, y | `input.inject`, `root.exec` | Prototype implemented; rooted-device verification pending |
| `random_tap_by_xpath` | XPath expression | `ui.inspect`, `input.inject`, `root.exec`, `random.secure` | Reads the selected element bounds, samples a uniform integer point inside the rectangle using `/dev/hwrng` or Android `SecureRandom`, and injects one tap |
| `swipe` | start/end coordinates, duration | `input.inject`, `root.exec` | Prototype implemented; rooted-device verification pending |
| `input_text` | text | `input.inject`, `root.exec` | Prototype implemented; rooted-device verification pending |
| `key_event` | key code | `input.inject`, `root.exec` | Prototype implemented; rooted-device verification pending |
| `gesture` | bounded point sequence | `input.inject` | Planned |
| `screenshot` | none | `screen.capture`, `root.exec` | Prototype implemented; rooted-device verification pending |
| `view_tree` | optional window/filter | `ui.inspect` | Returns directly readable, indented XML from the refreshed ARMCP AccessibilityService; disclosed Root `uiautomator` fallback |
| `view_content` | optional text/package filter | `ui.inspect` | Returns directly readable, indented JSON from the same refreshed snapshot; disclosed Root fallback |
| `shell_exec` | command, timeout | `root.exec` | Prototype implemented; rooted-device verification pending |

Every row is backed by a registry entry, schema, stable capability/error contract, and bounded executor path. UI inspection serializes requests. The primary service selects the active window, best-effort refreshes the root and nodes, retries dynamic-window reads, and retains the latest complete snapshot available during bounded continuous updates; it does not register UiAutomation. If the service is unavailable, the Root fallback may use `uiautomator dump`; Android registration conflicts use `UI_AUTOMATION_CONFLICT` with backend diagnostics. If the service is connected but the active window exposes no readable content, the result uses `UI_INSPECTION_UNAVAILABLE` with `backend: accessibility_service` and `serviceConnected: true` rather than invoking UiAutomation. The hierarchy and formatted document have no ARMCP byte-size limit, subject to device memory, command timeout, and transport/client limits outside this tool contract.

### UI document and coordinate contract

The first MCP text content item returned by `view_tree` is the formatted XML document. The first MCP text content item returned by `view_content` is the formatted JSON document. The text is the document itself rather than a JSON-escaped string nested inside another result field. The content item's `_meta` reports `mimeType`, `backend`, `privilege`, and whether a filter was applied.

Every element with a valid Android `bounds` value exposes:

- `bounds`: screen-pixel rectangle `{left, top, right, bottom}` in JSON, or the corresponding XML attributes;
- `size`: width and height;
- `tap`: `{x, y, point: "center"}` in JSON, or `tap-x`/`tap-y` attributes in XML.

The coordinate origin is the screen's top-left corner, with x increasing rightward and y increasing downward. Center coordinates are calculated as `left + (right - left) / 2` and `top + (bottom - top) / 2`, using integer screen pixels, so the returned `tap` values can be passed directly to the `tap` tool. Filtering preserves matching ancestors and descendants as a valid document instead of filtering individual XML lines.

`random_tap_by_xpath` evaluates the supplied XPath against the same current UI XML snapshot used by `view_tree`/`view_content`. The selected element MUST have a valid `bounds` rectangle. The click rectangle is the element's screen rectangle `[left, right) × [top, bottom)`, equivalent to the center plus or minus one half of its width and height. Coordinates are sampled uniformly with rejection sampling from raw bytes read from Linux `/dev/hwrng` when available. If `/dev/hwrng` is absent or cannot provide the bounded read, Android `SecureRandom` is used as the secure fallback; the result reports `randomSource: "linux_hwrng"` or `randomSource: "android_secure_random"` respectively. No predictable software PRNG or fixed center fallback is used.

### Debug UI inspection contract

When the Debug page displays a successful `view_tree` or `view_content` call, it provides a local inspector below the result. The inspector has an expandable-style element tree, a selectable screen-coordinate box model, and a details panel. The element tree grows with the returned hierarchy instead of imposing an independent fixed-height scrollbar. The box model scales its complete screen-coordinate canvas down to the available panel width and does not require horizontal browsing; the original screen coordinates remain unchanged in the details and tap action. Selecting either a tree row or a box selects the same element. The details panel displays the JSON path, element attributes, bounds, size, and center tap coordinates. For `view_tree`, it also displays the generated XPath and provides a copy action. The generated paths are derived locally from the returned snapshot; selecting an element never causes an additional device inspection request. If the element has bounds, the inspector can call the existing `tap` tool with the calculated center coordinates.

## Token modes

Creation is an operator action in Flutter. The raw secret is returned once. The server stores only a verifier/hash, token id, mode, creation time, expiry, enabled tool names, and use count. One-time consumption is atomic with authorization to prevent concurrent reuse. Deletion removes the record instead of marking it revoked.
