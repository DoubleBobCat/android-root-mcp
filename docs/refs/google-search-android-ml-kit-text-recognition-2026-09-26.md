# Android screenshot OCR reference

- Search method: Google via browser MCP
- Retrieved: 2026-09-26
- Official source: https://developers.google.com/ml-kit/vision/text-recognition/v2/android?hl=zh-cn

## Relevant findings

Google's ML Kit Text Recognition v2 documentation supports processing a
`Bitmap`, byte buffer, byte array, or file image and returns recognized text
hierarchically as blocks, lines, elements, and symbols with bounding
coordinates. The Chinese text-recognition model is available as a bundled or
Google Play services dependency.

This provides a viable fallback for Taobao-like screens whose pixels contain
text but whose accessibility tree contains no semantic descendants. The
result should be reported as OCR output with confidence and screen-coordinate
metadata, not as a native accessibility hierarchy. OCR can misrecognize text,
especially during scrolling, animation, low contrast, or image-backed UI, so
it needs a fresh screenshot, bounded processing time, and an explicit backend
label.

The current project does not yet implement OCR. It remains a candidate
capability for a separate design and focused verification slice.
