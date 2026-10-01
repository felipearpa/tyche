# Proposal

## Why

Fortuna already uses Liquid Glass for some iOS controls, but adoption is inconsistent: the create-pool toolbar action suppresses the system surface, nearby custom glass controls are not grouped, and glass is repeated inside forms and list rows. Complete adoption around a clear separation between navigation controls and content while retaining iOS 16 support and the current navigation structure.

## What Changes

- Use native iOS 26 navigation, tab, toolbar, and sheet materials; retain the background-free toolbar avatar as an intentional exception.
- Present the create-pool action on both platforms as a larger accent-filled circular control with an unframed add icon in the on-accent colour and a localized accessible name.
- Retain glass for the standalone sign-in and join action groups, and coordinate nearby glass effects through shared, availability-guarded containers.
- Replace glass in content-layer text fields, inline form submission actions, prediction-row actions, and pool-row invitation controls with standard, theme-aware controls. Preserve their geometry where feasible, action hierarchy, state, and accessibility semantics.
- Keep the opaque push drawer, existing navigation destinations, tab arrangement, sheet detents, and the username editor's scrolling save action.
- Preserve iOS 16 compatibility through shared fallbacks and verify accessibility, keyboard interaction, scrolling, and drawer gestures on iPhone and iPad.
- Derive changed app-rendered icons on both platforms from the same committed canonical vector.
- Replace glass on the shared paging-error Retry inside lists with a standard prominent control.
- Darken the shared light-appearance accent to #2E7D32 on iOS and Android, keep #4CAF50 in dark appearance with dark labels on accent fills, darken the primary-container colour to #1B5E20, and apply the light accent to the app icons and splash/launch logos, so labels on accent-filled controls stay legible. Android work is limited to the create-pool icon and its filled control, the accent colour and its on-accent label colours, and the launcher icon and splash logo.

## Capabilities

### New Capabilities

- `ios-liquid-glass`: Material placement, native navigation adoption, coordinated glass controls, compatibility, and accessibility for Fortuna's existing iOS and iPadOS interface.

### Modified Capabilities

- `pool-score-list`: Permit the bounded iOS control-material and create-toolbar changes while retaining the list's layout, content, interaction, and screen-reader contract.

## Impact

- Shared SwiftUI presentation in `iOS/UI`, including button and text-field styles, availability wrappers, and the toolbar-avatar exception.
- UI consumers in `iOS/Account`, `iOS/Bet`, `iOS/Pool`, and `iOS/Tyche/Tyche/UI`; no domain, repository, or backend API changes.
- Canonical icon source and derived iOS/Android resources for the create-pool glyph; no Android glass treatment or navigation redesign.
- The iOS `AccentColor` and primary-container colour assets, app icon, and launch-screen logo, and the Android theme colours, launcher icon, and splash logo. Pool illustrations and store screenshots keep their existing colours.
- Existing drawer, leaderboard, profile, and loading contracts remain in force. Model-backed placeholders continue to use production components and the shared shimmer.
- No deployment-target increase, new third-party dependency, tab-bar minimization, sidebar migration, or new navigation animation is required.
