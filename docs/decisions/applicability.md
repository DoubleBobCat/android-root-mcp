# Applicability Decisions

- Stage 5 Plugin Specification: N/A. Approved by project owner on 2026-09-24. This APK does not load or distribute MCP plugins in the MVP.
- Stage 5 Workflow Specification: N/A. Approved by project owner on 2026-09-24. The MVP exposes atomic MCP tools and has no multi-step workflow engine.
- Stage 5 Event Specification: N/A. Approved by project owner on 2026-09-24. The MVP does not define server-initiated domain events; request/response MCP behavior is covered by `docs/specs/MCP-Spec.md`.
- Stage 5 Specification: applicable. `docs/specs/MCP-Spec.md` defines the direct MCP transport, authentication, errors, and tool contract.
- Stage 6 Data Design: applicable. `docs/Database-Design.md` defines settings, token, and audit data.
- Stage 7 API Design: applicable. `docs/API/openapi.yaml` defines the operator control API and direct MCP endpoint surface.
- Stage 10 Deployment/Operation: applicable. `docs/Deployment.md` and `docs/Operation.md` define runtime and release behavior.
