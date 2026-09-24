## Context

See `proposal.md` for motivation and `specs/navigation-drawer/spec.md` for the behavior contract. X and ChatGPT are references for the user's described interaction; this proposal does not assert their current implementation or exact motion values.

Android's shared `PushDrawer` and iOS's `DrawerContainer` already keep a drawer behind a translated screen. Both use an 85% width, a 300 ms curve, 25-unit leading corners, and an overlay that switches between black in light mode and white in dark mode. Neither transforms drawer content. Both recognize a drag by a fixed threshold after release, so the surface does not track a finger. Android independently animates several properties; iOS attaches one implicit animation to the container.

The two consumers on each platform are the pool list and pool home. They already share account headers and menu rows and observe centralized account/avatar state. Pool home additionally renders a full-width green summary, owner-dependent actions, loading/error states, and a deletion confirmation. Its loading view already uses a placeholder model in the production summary component. These data and action paths remain the integration boundary.

The repository has Compose BOM `2026.05.00` and Activity Compose `1.13.0`; the iOS UI package declares iOS 16. Use APIs compatible with the resolved dependencies and supported deployment targets, with availability checks for newer SwiftUI refinements.

## Goals / Non-Goals

**Goals:**
- Give every visual property a common notion of reveal progress so direct manipulation, programmatic requests, and interruption remain coherent.
- Keep animation in the shared platform containers, with consumer views responsible for content and existing actions.
- Build the drawer's layout, drawing, animation, gestures, and accessibility entirely in Compose and SwiftUI respectively.
- Use native typography, control feedback, motion settings, and dismissal conventions while sharing the user-visible navigation contract.

**Non-Goals:**
- Copying another app's destinations, branding, typography, exact measurements, or undocumented animation implementation.
- Adding a permanent tablet sidebar, changing routes or authentication, or redesigning pool screens outside their drawer content.
- Adding Android View/XML drawer widgets, UIKit view/controller or gesture wrappers, WebViews, third-party drawer packages, custom frame loops, or a cross-platform animation engine.
- Scaling or blurring the pushed screen, staggering individual rows, or changing loading semantics elsewhere in the app.

## Decisions

### 1. Keep the push composition and refine its hierarchy

Retain a fixed drawer surface below a full-size foreground screen. Only the drawer's content group scales and fades; the background fills the reveal from its first visible frame. Keep the foreground at its natural scale. This follows the requested spatial relationship and preserves the underlying route's layout.

Use a quieter account-and-pool menu: account identity at the top, Profile immediately below it, then an inset pool summary and pool actions when applicable. Sign out sits in a separated footer. At sufficient height the footer rests at the bottom; at constrained height it participates in the same vertical scroll region so no action is unreachable.

```text
Pool home drawer                    Pushed screen
┌───────────────────────────────┐   ┌─────────
│ avatar  username              │   │ dimmed
│         email                 │   │ current
│ icon    Profile               │   │ screen
│                               │   │
│ ┌───────────────────────────┐ │   │
│ │ Playing now              │ │   │
│ │ Pool name                │ │   │
│ │ Position · Points        │ │   │
│ └───────────────────────────┘ │   │
│ Pool                          │   │
│ icon    Invite                │   │
│ icon    Gamblers       count  │   │
│ icon    Delete pool           │   │
│                               │   │
│ Sign out                      │   │
└───────────────────────────────┘   └─────────
```

The pool list uses the account, Profile, and footer portions without adding empty pool sections. Preserve existing localized labels. The distinctive element is the coordinated reveal; the surrounding styling stays restrained.

Use existing semantic theme resources, with these repository colors as visual anchors rather than hardcoded replacements: surface `#FFFFFF` / `#121212`, subdued group `#F5F5F5` / `#1E1E1E`, foreground `#212121` / `#E0E0E0`, Fortuna accent `#4CAF50`, destructive `#D32F2F` / `#CF6679`, and scrim `#000000`. The pool group uses the subdued surface, primary text, and a restrained accent detail in place of the full-width green fill. iOS uses SwiftUI color assets and semantic foreground styles, including in `DrawerStyle`; remove direct UIKit color bridging from the affected drawer styling. Android uses Material color roles.

Typography uses the platform's existing system family: headline/title for account and pool names, body for action labels, and caption/body-small for email and statistics. Use existing spacing tokens, starting with 16-unit outer gutters, a 24-unit section gap, aligned 24-unit menu icons, and native minimum targets of 44 pt on iOS and 48 dp on Android. Let rows grow with text. Keep the existing avatar size appropriate to each platform.

Alternative considered: a standard overlay modal drawer would simplify some infrastructure, but would cover the current screen instead of pushing it as requested. A new card-heavy menu or row-by-row entrance would add competing visual effects without improving navigation.

### 2. One reveal progress drives the content and foreground

Each platform owns a normalized visible progress `p` in `[0, 1]`, distinct from the requested open/closed endpoint and whether a drag or settling animation is active. Keep the caller-facing Boolean binding/callback where possible and encapsulate continuous progress internally.

| Property | Closed (`p = 0`) | Open (`p = 1`) | Application |
| --- | --- | --- | --- |
| Drawer background | Opaque, unscaled | Opaque, unscaled | Fixed behind the foreground |
| Drawer content opacity | 0 | 1 | One group including footer |
| Drawer content scale | Approximately 0.96 Android / 0.95 iOS | 1 | Logical leading-center anchor |
| Foreground offset | 0 | Drawer width toward trailing | Translation only |
| Foreground scrim opacity | 0 | Initially 0.18 light / 0.24 dark | Confined to foreground bounds |
| Foreground leading corners | 0 | Approximately 24 dp/pt | Clip foreground and overlay together |

These values are starting tokens for simulator/device tuning, not a cross-platform equality requirement. Map scale, alpha, offset, scrim, and edge treatment from the same progress rather than launching unrelated delayed transitions. Clamp rendered progress and alpha; prefer a critically damped or low-bounce spring that settles without a visible overshoot. Opening and closing traverse the same property endpoints in opposite directions, with each platform free to settle differently.

Apply scale as a rendering transform after final layout. Do not animate font sizes, padding, row heights, or drawer width to produce the grow effect. Keep content identity stable while closing; hiding it as soon as the Boolean flips would skip the exit. At the fully closed endpoint it is invisible, not hit-testable, and absent from accessibility traversal.

A theme-aware black scrim progressively dims the foreground. This replaces the present white wash in dark mode. The actual screen colors remain untouched, and the drawer never receives the scrim. Retain at most a subtle platform separator/shadow at the exposed foreground edge; avoid a prominent outlined card.

Alternative considered: independent `animate*AsState` or per-view transitions are convenient for taps but make gesture takeover and synchronized interruption harder. A shared progress state gives those cases one transition to control.

### 3. Implement the motion using each platform's native facilities

**Android:** Keep `PushDrawer` as a composable built from Compose layout and Material surfaces. Use an interruptible Compose animation state for progress, such as `Animatable`, and Compose drag input with velocity tracking. `Modifier.graphicsLayer` applies group scale/alpha and foreground translation/clipping; read rapidly changing values in drawing/layer scopes where practical. Derive all visual values from the same progress and use native spring/easing specifications. Compose supplies interruptible value animations and layer transforms without replacing layout. See [value-based animations](https://developer.android.com/develop/ui/compose/animation/value-based) and [graphics modifiers](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers).

**iOS:** Retain the `.drawer(isShowing:content:)` and drawer-style composition APIs. Build with `GeometryReader`, `ZStack`, `ScrollView`, native `Button` styles, and SwiftUI state/gesture facilities. Animate the group with `.scaleEffect(anchor:)` and `.opacity`, and the foreground with `.offset`, `.overlay`, and an animatable clip shape. Use a SwiftUI spring or smooth animation with supported-OS fallbacks and scope the animation transaction to drawer progress so account refreshes and loading changes do not inherit it. Use `DragGesture` and gesture state for direct manipulation, with an animatable progress modifier or equivalent SwiftUI-only state arrangement when needed to hand off from the currently presented value. Do not add `UIViewRepresentable`, UIKit gesture recognizers, or display-link interpolation. See [SwiftUI DragGesture](https://developer.apple.com/documentation/swiftui/draggesture).

Platform timings, spring response, widths within the bounds below, pressed feedback, and typography remain independent. Reuse existing committed icon assets where possible. For any introduced or replaced icon, prefer an accurate Material Symbol, otherwise allow a custom icon, and derive both platform assets from one committed canonical vector. This asset constraint does not restrict native control behavior or OS-rendered controls.

Alternative considered: recreating Android's cubic timing curve on iOS would reproduce today's coupling and ignore the user's explicit preference for platform-specific behavior. Separate native implementations share endpoints and behavior, not an animation engine.

### 4. Treat gestures, dismissal, and interaction as part of the transition

Keep gesture phases explicit: closed, opening/settling, open, dragging, and closing/settling. A new target cancels or retargets the current transition from its presented value. A drag takes over that same value; release chooses an endpoint using native velocity and displacement conventions. Gesture cancellation returns to the last settled endpoint. A resize recomputes width from the current window while preserving normalized progress or deliberately settles to a valid endpoint.

The menu opener remains an opening control, and the drawer hosts (pool list and pool home) also open by swipe. While a host is the visible screen and its drawer is closed, a drag toward the logical trailing side that starts anywhere on it opens the drawer and tracks the finger; at the closed endpoint a drag toward the leading side is not claimed. While the drawer is open or transitioning, a horizontal drag that starts anywhere on the drawer, including on an action row, or on the exposed foreground strip takes over the reveal. Recognize drawer drags by directional intent: movement beyond a small density-independent slop that is predominantly horizontal. Otherwise vertical scrolling proceeds normally, and descendant horizontal controls and system-rendered bars, such as pool home's tab bar, keep their own drags. Attach these drags at the container level, alongside the dismissal surface, not inside the foreground subtree, which stops hit-testing once the reveal becomes modal mid-drag. Once a drawer drag is recognized, cancel the pending activation of the control under the finger, whether a drawer row or a host control such as a gambler row, so controls activate only on a tap. Prefer a container-level arrangement. iOS 16 SwiftUI has no failure requirement between an ancestor drag and a descendant `Button`, and neither a normal-priority nor a simultaneous drag alone achieves this; if a high-priority container drag would take vertical scrolls, a drawer-provided environment value honoured by the affected row and button styles is acceptable, and a UIKit bridge is not. In Compose, a drag detector on the common ancestor in the default Main pass that consumes after slop cancels child clicks while descendant scrollers win first; add an explicit horizontal-dominance check where no vertical scroller is under the finger.

Disable drawer drags as soon as a destination is opened, including while the drawer is still closing after that choice; the drawer finishes closing on its own. On iOS the drawer wraps the host's `NavigationStack`, whose path is private to each router, so the routers pass `path.isEmpty` through an additive `.drawer` parameter. The drawer then detaches its gestures, for example with `.gesture(_:including:)`, rather than ignoring their values, so they cannot compete with the destination's native back button and back-swipe, including the content-area back-swipe of iOS 26 and later. Add no UIKit gesture bridge or pop-gesture override. On Android, the drawer is composed only inside its two host destinations, so other routes never contain it. The drawer does not request system-gesture exclusion: under gesture navigation both screen edges remain system Back, which dismisses an open drawer before navigating and tracks the finger only through predictive Back integration; under button navigation an edge swipe is an ordinary drawer drag. Use logical leading/trailing signs and density-independent distances, replacing the existing raw 50-pixel threshold.

While progress is nonzero or a transition is active, block underlying content and expose a dedicated dismissal surface above it. Keep that surface outside any disabled content subtree: iOS currently applies `.disabled` after constructing its overlay, which must not disable the new dismissal control. Foreground interaction resumes only at the closed endpoint. Drawer action controls become enabled at the open settled endpoint; an accessible dismiss action remains available while modal. Menu reversal, Back, and programmatic state changes remain possible during transitions.

Use Android Back handling at the composable/host boundary so dismissal precedes route navigation. If the resolved Activity Compose APIs support predictive Back integration, use its progress and cancellation through the same state rather than adding a competing transition; ordinary Back dismissal is required regardless. On iOS, preserve route pop behavior when closed and provide SwiftUI accessibility escape dismissal when open. Do not delay route actions with guessed animation-duration timers. Existing destination callbacks request closure and navigate once; sign-out and deletion retain their existing flows.

Alternative considered: keeping release-only threshold gestures would leave the drawer disconnected from touch and would not exercise the continuous reversal contract. Restricting opening drags to header or edge regions would avoid gesture arbitration, but was rejected in favor of swipe-from-anywhere on the drawer hosts. Enabling the drawer swipe on destinations was also rejected: it would require disabling the platform back-swipe, by hiding the native back button or through a UIKit exception.

### 5. Keep layout, focus, and reduced motion correct at every endpoint

On compact windows, start from the existing 85% width. Bound it near 360 dp on Android and 340 pt on iOS, and always reserve a trailing dismissal strip at least as wide as the platform minimum touch target when the window permits. Use window/container bounds, not physical screen dimensions. Apply safe-area/system-inset padding once, with the opaque surface extending behind it. Use a vertical scroll container with content minimum height so the footer sits low when space permits and scrolls with the menu when it does not.

Hide closed drawer semantics, hide the foreground's descendants while the drawer is modal, and move accessibility focus after opening settles. Restore it to the opener after closing only if that route is still present; allow the destination's normal focus handling after navigation. Provide native button semantics and dismissal actions, not an invisible unlabeled tap target. Keyboard focus follows the same interaction boundaries where supported.

Read SwiftUI's [`accessibilityReduceMotion`](https://developer.apple.com/documentation/swiftui/environmentvalues/accessibilityreducemotion): remove animated scale and translation, place surfaces at their requested endpoints, and use at most a brief opacity change. Android uses Compose animation facilities that respect system duration scaling; zero duration settles immediately, including after drag release. Apply interaction and focus endpoint updates even when no animation frames occur. Keep user-driven manipulation under direct control and remove decorative scaling when motion is reduced.

Preserve the current account/avatar sources and view-model lifetimes. Restyle the pool-summary production component once and pass its existing placeholder model during loading, using shared shimmer and suppressing placeholder accessibility/navigation as needed. Do not add a separate loading layout or cause drawer animation to restart remote requests.

Alternative considered: fixed-height content and alpha-only hiding would leave sign-out unreachable at large text and hidden controls discoverable to screen readers.

## Risks / Trade-offs

- **Swipe-from-anywhere competes with scrolling, taps on drawer and host controls, the tab bar, and route Back** → Recognize drags by slop and directional intent, leave system bars and descendant horizontal controls their drags, cancel the touched control's activation only once a drawer drag is recognized, disable drawer drags as soon as a destination opens, and verify both drawer hosts with gesture and button navigation.
- **SwiftUI animation takeover jumps to a target value** → Distinguish the displayed progress from the target; verify grabbing and reversing midway, using only SwiftUI animatable/gesture state.
- **Native implementations drift visually** → Compare closed, half-open, and open captures on both platforms against the behavior contract; permit native curves and metrics to differ.
- **Group opacity/clip layers cost extra compositing** → Transform a small stable hierarchy, keep avatars cached, avoid blur and large shadows, and inspect frame pacing on representative devices.
- **Large text crowds the summary or footer** → Let rows wrap and grow, bound menu width, and scroll the complete content when needed.
- **Over-broad implicit animation animates unrelated account updates** → Scope transactions to drawer progress and retain consumer identity.
- **Package deployment targets differ from the app target** → Build the shared UI packages and app with their declared targets; use native availability fallbacks instead of raising the minimum OS for this change.

## Migration Plan

No data migration or coordinated backend release is required. Implement the shared container and its consumers per platform, then verify both drawer entry points before shipping each app independently. Keep the existing Boolean APIs and route callbacks unless a small additive state handle is necessary.

Use focused behavioral tests for interruption, cancellation, background hit testing, accessibility visibility, and Back/destination dispatch. Use simulator/device recordings for actual scale/fade synchronization, text stability, and frame pacing. Capture light/dark, compact/wide, right-to-left, large-text, loading/error, and reduced-motion states. Preserve a short verification record with the change. Rollback reverts the affected container and presentation changes without touching account data, cached avatars, or routes.
