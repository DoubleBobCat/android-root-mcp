# MCP transport and debug-console research

- Retrieval date: 2026-09-25
- Sources:
  - https://modelcontextprotocol.io/specification/2025-11-25/basic/transports
  - https://modelcontextprotocol.io/specification/2026-07-28/basic/transports
  - https://modelcontextprotocol.io/specification/2025-06-18/basic/authorization
  - https://docs.flutter.dev/platform-integration/platform-channels

## Findings

- Streamable HTTP uses one MCP endpoint for POST and optional GET/SSE. Requests use UTF-8 JSON-RPC and clients advertise both `application/json` and `text/event-stream`.
- HTTP MCP servers should authenticate all connections, validate Origin when applicable, and use deliberate binding.
- MCP protected-resource authorization metadata is required for an HTTP protected resource. The embedded token issuer remains a documented prototype.
- Flutter platform channels pass JSON-like values asynchronously; privileged Android operations stay behind the native boundary.
