# Floating Pill Bottom Nav — Design Note

**Date:** 2026-07-16 · **Scope:** `ui/main/MainScreen.kt` (bottom nav only)

Replace the generic Material `NavigationBar` with a modern, music-player-style **floating pill** nav:

- **Shape/placement:** a detached, fully-rounded capsule (`RoundedCornerShape(32.dp)`, ~64dp tall) centered above the bottom edge with side margins (~40dp) and a soft shadow (`shadowElevation ~12dp`), color `surfaceContainer`. Sits above the system nav bar (`navigationBarsPadding`) and reserves its own bottomBar height so list content still insets correctly. MiniPlayer keeps floating just above it.
- **Tabs:** icon-only (no labels), 4 evenly-spaced icons (Home/Library/Search/Settings), filled active icon variant.
- **Active state:** a soft accent **glow pill** (`primary` at low alpha) that **slides** to the active tab with a spring; active icon tinted `primary` with a subtle scale-up; inactive icons `onSurfaceVariant`.
- **Theme:** all colors from `MaterialTheme.colorScheme` → adapts to every theme.
- **Unchanged:** tab list, routes, 4 destinations, navigation behavior. Purely the bar's look + motion.

Verification: on-device screenshots across a couple of themes; confirm tab switching animates and content isn't clipped.
