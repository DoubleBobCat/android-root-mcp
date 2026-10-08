# Browser DOM Inspection Reference

- Search method: Google via browser MCP
- Retrieved: 2026-09-25
- Primary sources:
  - https://developer.mozilla.org/en-US/docs/Web/API/Document/evaluate
  - https://developer.mozilla.org/en-US/docs/Web/API/Element/getBoundingClientRect

## Relevant findings

The browser `Document.evaluate()` API evaluates XPath expressions against a document. The `Element.getBoundingClientRect()` API exposes an element's viewport-relative rectangle, including left, top, right, bottom, width, and height. The ARMCP debug page uses the same inspection principles for its returned Android UI document: it builds stable tree paths and XPath expressions from the snapshot, uses the returned screen bounds for the selectable box model, and does not execute arbitrary response JavaScript.
