# Android Adaptive Icon Reference

- Search method: Google via browser MCP
- Retrieved: 2026-09-25
- Primary source: https://developer.android.com/develop/ui/views/launch/icon_design_adaptive

## Relevant findings

Android adaptive launcher icons are defined with an `<adaptive-icon>` resource composed of foreground and background layers. ARMCP uses the selected Node Bridge mark as the legacy launcher PNG source and as an Android 26+ adaptive icon with a dark background and vector foreground. The foreground keeps the bridge nodes and center symbol inside the icon's safe visual area.
