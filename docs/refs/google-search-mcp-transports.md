# MCP Transport Reference

- Search method: Google via browser MCP
- Search query: `site:modelcontextprotocol.io specification transports Streamable HTTP official`
- Retrieved: 2026-09-24
- Primary source: https://modelcontextprotocol.io/specification/2026-07-28/basic/transports

## Relevant findings

The current MCP transport documentation defines UTF-8 JSON-RPC messages and describes Streamable HTTP as client messages sent with HTTP POST to one MCP endpoint, with replies as JSON or request-scoped SSE. Protocol semantics remain the same across transports.

The 2025-11-25 transport specification also requires the MCP endpoint to support POST and GET. A client GET may open an SSE stream; the client must advertise `text/event-stream`, while a server may return 405 when it does not offer an SSE stream. A server that offers the stream returns `Content-Type: text/event-stream`; it may close the stream after sending events, and clients may reconnect.

The APK therefore provides one direct Streamable HTTP MCP endpoint. The debug page calls that endpoint; it does not use a bridge or a separate control protocol.
