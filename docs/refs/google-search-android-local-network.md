# Android Local Network Permission Reference

- Search method: Google via browser MCP
- Search query: `site:developer.android.com Android local network permission official`
- Retrieved: 2026-09-24
- Primary source: https://developer.android.com/privacy-and-security/local-network-permission

## Relevant findings

Android documentation describes local-network protections for direct TCP, UDP, mDNS, and related traffic. It states that Android 17 targeting requires explicit handling of `ACCESS_LOCAL_NETWORK`, while lower target SDK behavior differs during the transition. The app must handle permission denial and revocation.

LAN MCP startup must therefore check the target SDK and effective local-network permission before opening the listener, and must show the resulting state to the operator.
