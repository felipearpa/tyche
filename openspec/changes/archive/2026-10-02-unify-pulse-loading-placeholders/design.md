# Design

## Context

See `proposal.md` for motivation and the `loading-placeholders` delta for the behavioral contract.

- iOS's `UI/ShimmerModifier.swift` currently combines native placeholder redaction with SwiftUI-Shimmer 1.5.1. Its moving gradient uses 1.5-second linear legs, a 0.25-second delay, and autoreversal. The app supports iOS 16 and later.
- Android's `ui/ShimmerModifier.kt` uses Accompanist Placeholder Material 0.36.0 with opaque placeholder colors (`#BDBDBD` light, `#424242` dark). Its Material convenience defaults come from Material 2 even though the app supplies Material 3. Android supports API 28 and later.
- Several production components already accept an optional `placeholderModifier` and derive placeholder behavior from its presence. Other rows forward a shimmer modifier through nested elements. Existing paging infrastructure already chooses initial and append loading presentations.
- Leaderboard and pool rows already demonstrate production-layout reuse and some accessibility/image-request suppression. Those protections are inconsistent across the remaining surfaces. Manage gamblers still has independently maintained skeleton geometry on both platforms, plus per-row opacity attenuation.
- Shipcker uses `com.revenuecat.purchases:placeholder:1.0.2`. Its wrapper currently selects shimmer, but the inspected 1.0.2 library also supplies `Pulse`. This dependency is the user's chosen Android replacement; Shipcker's wrapper itself is not the architecture to copy.

## Goals / Non-Goals

**Goals:**

- Give component callers one explicit presentation flag, with behavior that is independent of the visual effect implementation.
- Share visual values through existing platform design-system/theme ownership while keeping component geometry and side effects in the components that already own them.
- Migrate every current model-backed skeleton surface, including initial and append presentations, without altering real-data behavior.
- Keep a restrained pulse available on the entire supported OS range and verify visual parity against real rendered backgrounds.

**Non-Goals:**

- New public loading helpers, generic placeholder wrappers, a skeleton DSL, inherited global loading state, or a sealed loading-state hierarchy introduced solely for this change.
- Moving UI placeholder flags, fake values, or animation state into domain entities, repositories, persistence, or network payloads.
- Pixel-identical native text geometry, global phase synchronization across every row, animated blur, glass loading materials, or a mandatory all-gray recoloring of structural containers.
- Changing full-screen loading overlays, spinners, avatar fallbacks for real accounts, loaded-row design, backend behavior, or Shipcker itself.

## Decisions

### 1. The production component accepts `isPlaceholder = false`

Use the same name and default in SwiftUI initializers and Compose functions. Replace caller-supplied `placeholderModifier`/`shimmerModifier` selection and internal inference from modifier presence with this flag. Existing real-model callers keep their behavior through the default.

The screen or paging presentation supplies a stable placeholder model and sets the flag only for missing data. Existing model factories may be reused; fake values remain presentation-only. Components and nested production primitives that need this distinction receive the flag explicitly. Do not introduce new convenience wrappers merely to supply these two inputs. Existing wrappers containing only model selection can be removed as callers migrate.

The component owns content masking, applicable actions, accessibility, and image-request suppression. A parent button or navigation link must also be omitted or disabled for the placeholder state; hiding a child alone does not make the surrounding activation target inert. Request suppression targets filler identities, not the real screen request needed to finish loading.

**Alternatives:** Nullable visual modifiers couple behavioral state to styling. `isLoading` conflates unavailable content with background refresh. A new state hierarchy or inherited context adds indirection the current components do not need. The explicit boolean matches the user's chosen API and keeps the change bounded.

### 2. Theme owns policy; production views own rendering

Place pulse timing, curve, appearance endpoints, static midpoint, and default shape values in each platform's existing presentation/theme area. These values are the shared treatment; they do not require a public generic helper function.

Production components use SwiftUI or RevenueCat APIs internally. Screens do not import effect types or configure per-row animation. Internal private layout functions remain implementation details; do not add app-wide extensions such as `loadingPlaceholder()` or a new wrapper view as an alternate caller-facing API. Keep animation state out of view models and domain models.

Use the same content structure for real and placeholder models. On Android, apply masks to intended text, avatar, score, and image regions; a modifier on the whole row creates a single block. On iOS, retain native redaction while ensuring image content and custom glyphs are concealed. Structural backgrounds, separators, and gaps stay outside the pulsing content. Avoid applying pulse both to a parent and to an already-pulsing child.

**Alternatives:** Repeating literal values in each component invites drift. Building new skeleton-only primitives or layouts duplicates geometry. A generic loading framework is unnecessary for the agreed component contract.

### 3. RevenueCat handles Android placeholder drawing

Use `com.revenuecat.purchases:placeholder:1.0.2` as the initial version baseline, matching the inspected Shipcker dependency. Verify compatibility with Tyche's existing Kotlin/Compose toolchain; no unrelated toolchain upgrade is part of this change. Remove Accompanist Placeholder declarations and imports after all consumers migrate.

Use the public `Pulse(highlightColor, animationSpec)` with explicit theme values. The inspected stock pulse uses gray at 60% alpha and 1,500 ms plus 300 ms delay per reversing leg, so its defaults do not meet the intended treatment. Set 900 ms legs with reverse repeat and no delay for the normal 1.8-second cycle. The API uses `enabled`, not Accompanist's `visible`.

Specify component-appropriate shapes: the RevenueCat default is rectangular, whereas the current Accompanist Material path rounds corners. Preserve circular avatars, existing indicator silhouettes, and suitable text rounding. Use RevenueCat's renderer directly instead of copying Shipcker's `Modifier.composed` wrapper. The dependency remains confined to Android presentation modules that render placeholders.

The library suppresses underlying content independently from the translucent fill. Nevertheless, modifier order matters: a previously drawn rank-tile background can remain visible through the mask. Review those compositions explicitly.

**Alternatives:** Keeping Accompanist conflicts with the user's replacement decision. A custom blur engine adds compatibility and rendering work that pulse does not require. Newer RevenueCat-only convenience APIs are unnecessary; the inspected version already supplies the required primitives.

### 4. SwiftUI keeps redaction and owns its pulse state

Use native redaction with component-owned opacity/intensity animation, driven by the shared presentation values. Initialize and reset the phase deliberately when entering or leaving placeholder presentation so reuse does not reveal filler content or leave real content dimmed. Share one phase among the relevant content regions within a component where practical, while leaving structural surfaces unchanged.

Use `isPlaceholder` for behavioral decisions; redaction alone does not block actions or remote work. Check placeholders containing custom images, glyphs, or colored backgrounds instead of assuming all descendants are fully sanitized by native redaction. Replace the old shimmer modifier plumbing and remove the SwiftUI-Shimmer package only after confirming it has no remaining consumers.

**Alternatives:** Retaining the moving shimmer plus adding opacity would animate twice. Replacing SwiftUI with Compose to share an effect would exceed the application's architecture and the user's request.

### 5. Match rendered contrast rather than copying raw alpha values

Use this initial design preset:

| Setting | Initial value |
| --- | --- |
| Full cycle at normal speed | 1.8 seconds |
| Each fade direction | 0.9 seconds, symmetric ease-in-out |
| Endpoint pause / blur | None / zero |
| Light appearance fill target | Black at 12% → 20% → 12% over the local background |
| Dark appearance fill target | White at 16% → 24% → 16% over the local background |
| Static midpoint target | 16% black in light; 20% white in dark |

Timing is the specified default. Opacity values are initial visual tuning targets from exploration, not an empirically established optimum or literal parameters to copy into both renderers. RevenueCat composites a highlight over a base; SwiftUI redaction also contributes its own fill. Configure each platform to achieve comparable observed endpoints, and record calibrated theme values and captured results during implementation. Do not set a base and highlight to these two alpha values independently and assume the result matches the table.

Keep existing row canvases and structural rank/container fills, including the leaderboard's specified `surfaceVariant`. Neutral masks conceal filler content; this change does not approve a separate all-monochrome redesign of existing colored tiles. Resolve accidental tinting and any visible filler glyphs locally without changing the production layout or loaded appearance.

**Alternatives:** Blindly multiplying whole-row opacity dims backgrounds and controls as well as placeholders. Exact numerical equality across different renderers does not guarantee visual parity. Blur has no role in this preset.

### 6. Motion and loading lifecycle stay explicit

Observe iOS Reduce Motion and Android system motion duration scale. Use a visible static midpoint when reduced/disabled motion is requested; on Android a null highlight plus the midpoint fill is sufficient. Respect nonzero Android scaling and update the rendering when preferences change. Observe applicable transparency/contrast preferences without exposing filler data.

The existing loading/paging owners retain responsibility for choosing initial, append, content, empty, and error states. Keep usable data during refresh and existing rows during append. Do not delay data presentation to finish the pulse or impose a minimum skeleton display time. Animation state must stop with the loading presentation and reset cleanly for future loading.

Do not introduce a global coordinator merely to synchronize every row. Consistent timing, one effect per visual element, and component-local coordination are sufficient; the selected 1.0.2 dependency does not need newer coordination APIs.

### 7. Scope and specification reconciliation

Inventory all current `shimmer`, `ShimmerModifier`, and skeleton-only call sites on each platform. Cover My pools, leaderboard, pending/live/finished bets and history, match header and gambler bets, pool templates, drawer pool summaries, and Manage gamblers wherever they currently show model-backed loading content. Do not create new skeleton flows in unrelated screens.

Migrate Manage gamblers to its existing production item with a placeholder model, replacing the fixed skeleton geometry and independent per-row opacity attenuation. Audit small reused indicators and avatars so the explicit flag reaches the leaves that own behavior.

The five existing capability deltas replace their shimmer requirements with the new shared contract, preserving their full scenario sets. The My pools preservation requirement receives only the necessary placeholder-appearance exception. During implementation, change the future-authoring references in `AGENTS.md` and `openspec/config.yaml` from shared shimmer to the shared loading-placeholder treatment governed by `loading-placeholders`, retaining the production-model/layout rules. Do not rewrite archived proposals or historical verification artifacts. The later explicit pulse request supersedes the older shimmer wording for this change.

## Risks / Trade-offs

- **Rendered alpha differs across platforms** → Compare the same representative content at dim, middle, and bright phases in both appearances; calibrate central values and record results.
- **Changing masks exposes background or fake glyphs** → Inspect tile/avatar compositions and modifier ordering, retain redaction/content suppression, and cover parent activation targets.
- **Boolean propagation touches nested components** → Add focused defaults only where required; retain one production layout and avoid unrelated component rewrites.
- **Separate animation clocks can drift** → Keep common timing and avoid nested effects; global phase synchronization is intentionally outside this change.
- **Dependency migration changes rounding or transitions** → Specify shapes, inspect the exact pinned API, and compile the real Android modules before visual verification.
- **Existing refresh behavior clears otherwise usable data** → Fix presentation retention in the affected flow without changing repository, caching, or request semantics.
- **Skeleton-only layouts hide missing placeholder models** → Create presentation-only placeholder data for the real component and verify it cannot request remote data or become actionable.

## Migration Plan

1. Establish the platform theme values and explicit component API, then migrate one representative production row on each platform.
2. Complete the surface inventory and remove old modifier parameters, separate skeleton layouts, and unused effect dependencies. Preserve loaded rendering throughout.
3. Verify initial loading, append, refresh, success, empty, failure, and navigation away, including motion and accessibility preferences. Capture both platforms in light/dark appearance and large text, with an older supported OS represented.
4. Before authoring or changing executable tests, explore and verify the relevant Android interactions with ARTEMIS as required by the repository instructions; diagnose devices and ask for a serial when selection is ambiguous. Verify corresponding iOS behavior on a simulator/device. Tests must use observed interactions and explicit waits. This planning change does not create test code.
5. Update only relevant behavioral tests after exploration; check placeholder action/request suppression and loaded-state restoration. Use visual captures or recordings for pulse, geometry, and contrast rather than tests that merely duplicate rendering formulas.
6. Update future-authoring guidance and complete strict OpenSpec validation. Main spec synchronization and archival remain separate workflow steps after implementation.

No server deployment or data migration is required. Rollback can restore the prior presentation implementation and dependency declarations per platform without data conversion; retain the committed pre-change state rather than maintaining two live rendering systems.

## Open Questions

- Exact final opacity endpoints may be calibrated during visual verification without changing the component API, animation contract, library choice, production layout, or task breakdown. Record any tuning and its light/dark evidence in the change's verification notes.
