# Android WebView Security Reference

- Search method: Google via browser MCP
- Search query: `site:developer.android.com WebView JavaScript interface security official`
- Retrieved: 2026-09-24
- Primary source: https://developer.android.com/privacy-and-security/risks/insecure-webview-native-bridges

## Relevant findings

Android security guidance warns about native bridges exposed to WebView content and recommends removing JavaScript interfaces before untrusted content is loaded. It also treats JavaScript and URI loading as security-sensitive WebView behavior.

The debug page should be served as authenticated same-origin static content by the APK. Avoid a privileged WebView JavaScript bridge; use ordinary authenticated HTTP requests to `/mcp` instead.
