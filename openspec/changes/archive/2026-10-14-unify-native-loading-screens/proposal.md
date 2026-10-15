# Proposal

## Why

Fortuna's full-screen loading presentation differs between iOS and Android: iOS combines content blur, a material layer, and dimming, while Android uses Cloudy and a colored overlay that sometimes covers content outside the blur container. A shared, simpler treatment will retain the recognizable Tyche ball while making loading consistent and preserving support for older Android versions.

## What Changes

- Keep the Tyche ball, its existing theme colors, 64-point/dp size, and two-second linear rotation at normal animation speed on both platforms.
- Use native content blur as the only loading background effect: SwiftUI `View.blur` on iOS and Compose `Modifier.blur` on Android, using the same Compose API as `shipcker-android`.
- Remove the loader's iOS material and black dimming layers and Android's gray/green overlay and Cloudy dependency. Do not add a replacement scrim, material, or spinner card.
- On Android below 12/API 31, omit blur while retaining the ball, normal screen colors, and loading interaction behavior. Keep all existing minimum operating-system versions.
- Keep actual screen content mounted behind the loader, preserve drafts and scroll position, block interaction with covered controls, and expose an accessible loading state. Honor motion preferences without replacing the ball.
- Apply the same treatment to existing full-screen loading states in email and password sign-in, email-link verification, Google sign-in, pool creation, pool loading/joining, and pool deletion. Use the normal themed background when no prior screen content exists.
- Leave model-backed placeholders, shimmer/pulse effects, inline progress, refresh, pagination, launch/splash presentation, and provider-owned screens outside this change.

## Capabilities

### New Capabilities

- `loading-screens`: Shared Tyche-ball loading presentation, native blur and older-Android fallback, content preservation, interaction and accessibility behavior, placement, and loading lifecycle.

### Modified Capabilities

None. Existing placeholder, native-navigation, edge-to-edge, and Google sign-in contracts remain in force.

## Impact

- iOS shared `LoadingContainerView` and `BallSpinner`, plus affected Account, Pool, Home, and PoolHome presentation call sites.
- Android shared loading components, affected account/pool/app presentation call sites, obsolete loading-background theme values, UI module dependencies, and the version catalog.
- Existing committed Tyche ball vector and platform derivatives remain the asset source; no new icon or native stock spinner replaces it.
- Verification covers iOS, Android API 31+ blur, Android API 28–30 without blur, light/dark appearance, interaction isolation, retained state, motion preferences, and loading completion/error transitions.
- No backend, network API, persistence, authentication orchestration, navigation-flow redesign, or operating-system minimum changes.
