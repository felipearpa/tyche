# Design

## Context

See `proposal.md` for the motivation and `specs/loading-screens/spec.md` for the behavior contract. This design follows the agreed simplification; reproducing the existing iOS material appearance is no longer the target.

The current shared components are `iOS/UI/Sources/UI/Loading/LoadingContainerView.swift` and `Android/ui/src/main/java/com/felipearpa/tyche/ui/loading/LoadingContainerView.kt`. iOS applies a radius-6 content blur, `.thinMaterial`, and 15% black dimming. Android applies Cloudy and a theme-dependent 40% gray/green background. Several Android callers pass empty content and draw the actual screen as a sibling, so the loader cannot blur that screen. The iOS Google overlay similarly passes clear content and currently relies on material for the backdrop effect.

Both `BallSpinner` implementations already use the same vector geometry, 64-point/dp size, accent colors, and two-second linear rotation. The committed `logos/TycheLogoBall.svg` matches both platform derivatives and remains the canonical ball source. No replacement glyph is needed.

Shipcker's `ui/src/commonMain/kotlin/com/quamto/shipcker/ui/loading/LoadingContainerView.kt`, inspected under `/Users/felipe/Documents/Quamto/shipcker-android`, imports `androidx.compose.ui.draw.blur` and calls `Modifier.blur(radius = 2.dp)`. This is part of Compose UI, not a third-party blur library. Its blur API is the reference for Android; its circular progress indicator and numerical blur radius are not design requirements for Fortuna.

## Goals / Non-Goals

**Goals:**
- Give each platform one native, state-driven loading host that owns the background effect, ball placement, input isolation, and loading semantics.
- Keep the production screen subtree stable while loading changes, including form state and request ownership.
- Match the presentation's geometry, color, motion, and perceived blur without requiring pixel-identical native blur kernels.

**Non-Goals:**
- A new load-state framework, data-layer changes, altered Google credential handling, or a new navigation coordinator.
- Replacement spinners, additional decorative layers, software blur backports, or dependency additions.
- Changes to placeholder components, ordinary native navigation materials, or operating-system/provider-owned windows.

## Decisions

### 1. Use content blur through SwiftUI and Compose

Use SwiftUI `View.blur(radius:)` and Compose `Modifier.blur`, imported from `androidx.compose.ui.draw.blur`. Remove `.thinMaterial` and the black overlay from the iOS loader. Remove Cloudy and the loading background fill from Android. Remove the Cloudy catalog/dependency entries and loading-specific theme colors after confirming there are no remaining consumers.

Start visual calibration at 6 points on iOS and 6 dp on Android, stored centrally in each platform's loading presentation. These are initial implementation values, not an assertion that the renderers have identical output. Compare the same underlying content and tune only the central radius values if necessary, recording the final values in verification notes. Keep the effect restrained and avoid clipping halos at the screen edges.

On Android API 28–30, select a zero-effect path explicitly; API 31+ uses Compose blur. The API itself is a no-op below Android 12, but an explicit support decision makes the intended fallback reviewable. Do not add another blur implementation or a tinted substitute. No SDK minimum changes are required.

Alternatives considered: retaining Cloudy adds an unnecessary dependency and an older-platform blur behavior the user explicitly does not want; emulating `.thinMaterial` retains the layered design the user chose to simplify; copying Shipcker's 2 dp radius without visual comparison does not establish parity.

### 2. Keep the Tyche ball and its canonical asset

Retain native SwiftUI image rotation and Compose icon rotation using the existing platform derivatives of `logos/TycheLogoBall.svg`. Keep the 64-point/dp area, aspect ratio, accent colors, and two-second linear full turn at normal motion settings. Use one motion-preference policy per platform: static ball for iOS Reduce Motion or disabled Android animations, and native duration scaling for other Android settings. End animation work when loading is no longer presented.

Stock `ProgressView` and `CircularProgressIndicator` were considered and rejected because they change the chosen ball and have different platform appearances. No new icon assets are introduced.

### 3. Keep one content subtree inside a state-driven host

Evolve the shared loading container to accept explicit active/inactive loading state and render its content in one stable location. Callers pass the actual production screen rather than an empty lambda placed over a sibling. Apply blur only to that content; place the ball above it in a separate layer. Use the normal themed screen canvas when there is no previous content.

```text
Loading host (stable identity, full app-content viewport)
├── Existing screen or normal themed canvas
│   └── Native blur only while active and supported
└── While active: clear input barrier + accessible Tyche ball
```

The barrier has no visual fill. Disable or intercept covered pointer and keyboard interaction and exclude covered content from accessibility traversal; blur alone does none of these things. Preserve current text, selection, step, and scroll state when restoring interaction. Do not create fresh feature view models or replay lifecycle-triggered requests merely to redraw a background. This matters particularly for the iOS pool creator, which currently constructs separate stepper branches with locally initialized form state.

Keep request state and result callbacks owned by the existing feature/view model. Preserve loaded-but-awaiting-navigation intervals where a flow already treats them as busy. A visual state transition must not duplicate success callbacks or reopen Google UI. A screenshot snapshot or a second background-only copy of the form is unnecessary and risks stale state.

### 4. Migrate all existing full-screen loading call sites

| Flow | iOS presentation | Android presentation | Treatment |
| --- | --- | --- | --- |
| Send email link | `Account/ByEmail/EmailSignInView` | `account/byemail/EmailSignInView` | Retain submitted form beneath active host |
| Email/password sign-in | `Account/ByEmailAndPassword/EmailAndPasswordSignInView` | `account/byemailandpassword/EmailAndPasswordSignInView` | Retain form through authentication handoff |
| Email-link verification | `Account/ByEmail/EmailLinkSignInView` | `account/byemail/EmailLinkSignInView` | Normal themed canvas until existing result |
| Google sign-in | `Tyche/UI/Home/HomeRouter` | `app/home/HomeNavViewProvider` | Wrap actual welcome content; preserve provider flow |
| Pool creation | `Pool/Creator/PoolFromLayoutCreatorView` | `pool/creator/PoolFromLayoutCreatorView` | Retain current step and draft beneath active host |
| Pool loading/joining | `Pool/Joiner/PoolJoinerView` | `pool/joiner/PoolJoinerView` | Themed canvas for initial load, retained pool content for join |
| Pool deletion | `Tyche/UI/PoolHome/PoolHomeRouter` | `app/poolhome/PoolHomeView` | Retain current pool screen until existing result |

Confirm this inventory against all production `LoadingContainerView` usages during implementation. Do not apply a full-screen loader to inline avatar upload, username editing, history-summary progress, pagination, or model placeholders.

### 5. Give placement and accessibility to the host

Place the host outside form padding so its available bounds are the full app-content viewport. Center the ball in the viewport's safe visible area, accounting once for system bars, cutouts, caption controls, and any visible keyboard. Existing native navigation styles and system-bar protections remain intact; do not reproduce or blur external windows or the software keyboard. Covered app actions remain gated by loading, while existing allowed Back/cancel behavior remains flow-owned.

Expose one localized indeterminate loading status through native accessibility semantics, not an actionable logo or a percentage. Hide the covered subtree from traversal while active and restore the resulting screen's semantics afterward. Reuse the project's localized status conventions. No additional visible copy is required by this change.

## Risks / Trade-offs

- Native blur radii can produce different perceived strength → Compare matching content in both themes, calibrate central values, and document renderer differences without claiming pixel equality.
- No-blur Android retains visually busy content and blur-only screens have no contrast veil → Inspect the ball against actual affected screens in both themes, especially API 28–30. Record any legibility issue; do not silently introduce a card, scrim, or alternate spinner.
- Switching content branches can reset drafts, scroll position, or requests → Keep the loading host and production subtree stable and verify failures, retries, and navigation handoffs with real entered values.
- A transparent overlay can leak pointer, keyboard, or accessibility activation → Verify input isolation independently of blur on both API sides, including the already-focused field and parent navigation actions.
- Safe-area and keyboard handling can shift the ball or double padding → Validate portrait, landscape/resized windows, both Android navigation modes, and keyboard-visible loading.

## Migration Plan

1. Record the affected flows and their current pending/result behavior. Keep baseline captures and observations with the change's implementation verification evidence.
2. Update the iOS shared host and ball behavior, migrate its callers, and verify native blur, retained content, loading semantics, and motion settings.
3. Update the Android host with Compose blur and the explicit API fallback, migrate callers, then remove unused Cloudy and loading-tint resources.
4. Build affected targets and validate real flows. Before authoring or changing executable tests, explore the corresponding Android paths with the `android` CLI and verify the iOS paths on a simulator/device. Follow repository device selection rules; author only necessary behavioral tests with observed locators and explicit waits. Use controlled preview/test states to hold loading for captures without delaying production requests. From group 4 on (decided by the user on 2026-10-14), Android caller migrations only pass existing screen content into the host, and final verification is limited to builds, tests, and the cross-platform visual comparison in light and dark; per-flow walkthroughs are not repeated because behavior lives in the shared host already verified in groups 1–3. The `android` CLI replaced ARTEMIS for Android exploration on 2026-10-14 because ARTEMIS autonomous runs were unavailable (Gemini quota exhausted).
5. Compare both platforms in light and dark appearance, verify older-Android behavior, record final blur values and evidence, and validate the OpenSpec change. Do not mark unavailable device/version checks complete.

No backend migration or feature flag is needed. Rollback is a coordinated revert of the presentation and dependency changes, retaining existing request state machines and persisted data.

## References

- [Compose blur support and behavior](https://developer.android.com/develop/ui/compose/graphics/images/customize#blur-image)
- [SwiftUI native blur](https://developer.apple.com/documentation/swiftui/view/blur%28radius%3Aopaque%3A%29)

Planning is based on source inspection and platform documentation. Runtime and visual verification are implementation tasks, not completed evidence in this proposal.
