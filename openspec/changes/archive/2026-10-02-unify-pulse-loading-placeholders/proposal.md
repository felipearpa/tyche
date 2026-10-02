# Proposal

## Why

Fortuna's iOS and Android loading placeholders have different contrast and moving shimmer effects, and Android depends on deprecated Accompanist Placeholder. A coordinated pulse treatment and an explicit component contract will make loading consistent while keeping production layouts and component responsibilities clear.

## What Changes

- Replace moving shimmer with a gentle, theme-aware pulse on both platforms without requiring blur or raising minimum operating-system versions.
- Replace Android's Accompanist Placeholder dependency with RevenueCat Placeholder, the library used by Shipcker. Keep iOS rendering native to SwiftUI.
- Standardize affected production components on an explicit `isPlaceholder` parameter, defaulting to `false`. The same flag governs placeholder appearance, interaction, accessibility, and suppression of requests made only for filler identities.
- Render existing production components with placeholder models for initial and append loading, including converting existing separate skeleton layouts in Manage gamblers.
- Keep visual settings in each platform's design-system theme. Components own rendering through native or library APIs; screens do not pass effect modifiers or configure animation, and this change does not introduce a public generic helper API or a new loading-state framework.
- Honor motion preferences, retain existing content during refresh and pagination, and reveal ready content without waiting for an animation cycle.
- Reconcile affected specifications and, during implementation, update future authoring guidance to refer to the shared loading-placeholder treatment instead of mandating shimmer.

## Capabilities

### New Capabilities

- `loading-placeholders`: Cross-platform pulse presentation, production-component reuse, explicit `isPlaceholder` behavior, architecture boundaries, supported platforms, and loading lifecycle.

### Modified Capabilities

- `pool-leaderboard`: Use the shared pulse contract for initial and append placeholders while retaining production rows, navigation, and paging behavior.
- `pool-score-list`: Preserve placeholder accessibility and geometry while permitting the shared pulse treatment within the existing presentation boundary.
- `navigation-drawer`: Apply the pulse contract to production pool-summary placeholders without changing drawer motion or data ownership.
- `ios-liquid-glass`: Apply the shared pulse contract to affected production placeholders without introducing glass skeletons.
- `android-edge-to-edge`: Apply the pulse contract while retaining identical safe bounds for loading and loaded content.

## Impact

- iOS UI/theme and feature presentation code for pools, leaderboard, bets/history, match details, pool templates, drawer summaries, and Manage gamblers; component initializers and internal placeholder rendering.
- Android UI/theme and corresponding feature components, version catalog, module dependency declarations, and placeholder rendering imports.
- Existing shimmer modifier parameters and wrappers are migration points; generic paging and load-state infrastructure retain their current responsibilities.
- Targeted changes to `AGENTS.md` and `openspec/config.yaml` authoring guidance are implementation tasks. Main specifications will be updated through the normal delta-spec workflow; archived change artifacts remain historical.
- No backend, network contract, persistence, loaded-content redesign, navigation redesign, full-screen loading-overlay redesign, or operating-system minimum change.
