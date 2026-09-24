## Why

Fortuna's drawers already push the current screen aside, but their content appears at full size without an entrance or exit transition, and gestures only toggle the drawer after release. Redesigning this interaction around the drawer pattern described by the user in X and ChatGPT will make navigation feel fluid, with a clear visual relationship between the revealed menu and the current screen.

## What Changes

- Refresh both the pool list and pool home drawers with a quiet surface, a clear account header, consistently aligned action rows, a restrained pool summary, and a separated sign-out action.
- Animate the drawer's content as one group: gently grow and fade in while opening, then shrink and fade out while closing. Keep its background stable and its layout at the final size throughout.
- Coordinate the foreground screen's horizontal push, subtle edge treatment, and animated dimming overlay with the content reveal. Closing restores the screen's original appearance.
- Support continuous, reversible, finger-tracking transitions for menu activation and dismissal, including cancellation and interrupted animations. A horizontal swipe from anywhere on the pool list or pool home opens the drawer, and a horizontal drag anywhere on the open drawer or the current screen beside it closes it. System navigation gestures keep priority, and screens opened from the pool list or pool home keep their platform back gesture instead of opening the drawer.
- Implement independently with Jetpack Compose on Android and SwiftUI on iOS, using each platform's components, modifiers, animation, gesture, and accessibility facilities. Timing, easing, typography, feedback, and platform navigation behavior can differ.
- Stabilize the iOS pushed navigation title and toolbar avatar with a narrow UIKit layout bridge driven by the SwiftUI reveal, preserving their relative positions in portrait and landscape.
- Keep navigation reachable with screen readers, large text, reduced motion, system Back or accessibility dismissal, and constrained window sizes.

## Capabilities

### New Capabilities

- `navigation-drawer`: Presentation, content hierarchy, coordinated reveal and dismissal, gesture handling, platform-native implementation, accessibility, and integration of Fortuna's mobile push drawers.

### Modified Capabilities

None. Existing account, avatar, profile, pool list, and leaderboard contracts remain in force.

## Impact

- Android shared container: `Android/ui/src/main/java/com/felipearpa/tyche/ui/PushDrawer.kt` and its UI verification coverage.
- Android consumers: pool list and pool home drawer views, `AccounHeaderDrawer.kt`, `DrawerButtonRow.kt`, and their host navigation callbacks.
- iOS shared container: `iOS/UI/Sources/UI/DrawerView.swift`, `DrawerStyle.swift`, and UI verification coverage.
- iOS consumers: pool list and pool home drawer views, `AccountHeaderDrawer.swift`, `DrawerButtonRow.swift`, and their routers, which also tell the drawer when a destination is shown. Host-screen controls change only if needed so that a drawer drag starting on them does not also activate them.
- Reuse existing account/avatar stores, localization, theme resources, icon assets, and production pool-summary components. Any affected icon asset changes follow the canonical-vector policy; restyled loading summaries continue to use placeholder models and shared shimmer.
- No backend, persistence, API, or authentication changes. Use the installed native UI dependencies; a third-party drawer or animation library is unnecessary.
