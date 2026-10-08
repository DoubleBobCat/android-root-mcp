# Android LAN Server Socket Reference

- Search method: Google via browser MCP
- Search query: `site:developer.android.com ServerSocket local network Android INTERNET permission official`
- Retrieved: 2026-09-25
- Primary source: https://developer.android.com/privacy-and-security/local-network-permission

## Relevant Findings

Android's local-network guidance says that devices on a LAN can be accessed by apps holding the `INTERNET` permission, subject to the target-SDK permission behavior. A TCP service must bind an appropriate local interface and listen on a port; Root is not a substitute for selecting a non-loopback bind address or for network routing.

This project binds the authenticated MCP server to all local interfaces only when the explicit global MCP switch is enabled. The server advertises the device's current non-loopback IPv4 address, while retaining Bearer-token authentication for every MCP request. If no non-loopback address exists, the UI reports the loopback fallback and the endpoint is only locally reachable.
