# Android XML parser compatibility research

- Retrieval date: 2026-09-26
- Search engine: Google, used through the Zen MCP browser tools
- Official source: https://developer.android.com/reference/javax/xml/parsers/DocumentBuilderFactory
- Search source: https://www.google.com/search?q=Android+DocumentBuilderFactory+setFeature+ParserConfigurationException+official+documentation
- Security source search: https://www.google.com/search?q=site%3Adeveloper.android.com%2Fprivacy-and-security%2Frisks+xml+external+entities+Android

## Relevant findings

- Android's `DocumentBuilderFactory.setFeature(String, boolean)` documentation states that `ParserConfigurationException` is thrown when the factory or its document builders cannot support a requested feature.
- The Android API reference lists `setFeature`, `setExpandEntityReferences`, and `setXIncludeAware` as separate factory controls; implementations may support different feature sets.
- Google results for Android XML parsing show that Xerces-specific feature URIs are not uniformly supported by Android's built-in parser implementations. A configuration that applies all four feature calls unconditionally can therefore fail before XPath evaluation.
- The implementation keeps the supported baseline controls (`isExpandEntityReferences = false`, `isXIncludeAware = false` where available), treats optional feature URIs as best-effort for runtime compatibility, and installs an entity resolver that rejects external entities. This preserves the XML safety boundary while allowing Android's parser to initialize when a particular Xerces feature is unavailable.
