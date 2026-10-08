# MCP Method Compatibility Reference

- Search method: Google via browser MCP, followed by the official MCP specification pages
- Retrieved: 2026-09-25
- Primary sources:
  - https://modelcontextprotocol.io/specification/2025-11-25/basic/lifecycle
  - https://modelcontextprotocol.io/specification/2026-07-28/server/discover
  - https://modelcontextprotocol.io/specification/2026-07-28/basic/versioning

## Relevant findings

- Legacy MCP clients using the 2025-11-25 lifecycle can send `ping` during connection health checks. A successful ping returns a JSON-RPC result containing an empty object.
- The 2026-07-28 protocol removes `ping` and requires `server/discover` for modern servers. `server/discover` advertises supported protocol versions, capabilities, and server identity.
- A dual-era HTTP server can support both the legacy `initialize` flow and modern discovery so clients do not interpret a compatibility probe as an unavailable MCP endpoint.
