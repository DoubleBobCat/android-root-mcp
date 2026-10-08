# MCP HTTP Authorization Reference

- Search method: Google via browser MCP
- Search query: `site:modelcontextprotocol.io specification authorization authentication official`
- Retrieved: 2026-09-27
- Primary source: https://modelcontextprotocol.io/specification/2025-06-18/basic/authorization

## Relevant Findings

The MCP authorization specification says authorization is optional for MCP implementations, but HTTP-based implementations should conform when authorization is supported. It models a protected MCP server as an OAuth resource server and requires protected-resource metadata and authorization-server discovery for the specified flow.

The specification requires `Authorization: Bearer <access-token>` on every protected HTTP request, requires the server to validate the token before processing the request, and uses HTTP 403 for insufficient permissions. It also describes token audience binding, token theft, communication security, and privilege restriction as security considerations.

The ARMCP contract therefore treats each issued token as its own authorization context. A token stores an allow-list of registered tool names, `tools/list` is filtered to that allow-list, and `tools/call` checks it again before execution. The default allow-list contains read-only tools only; `shell_exec` and other input or launch operations are opt-in. Deleting a token removes its record and verifier, so there is no revoked-token state to retain or expose.
