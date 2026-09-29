# Design

## Context

See [proposal.md](proposal.md) for motivation and scope. The iOS app and local UI package target iOS 16. The inspected local toolchain is Xcode 26.0.1; CI selects its latest stable Xcode and currently tests an iPhone simulator. Toolchain availability does not establish runtime visual correctness.

`LiquidGlassButtonStyle` and `LiquidGlassProminentButtonStyle` already select native iOS 26 styles with bordered fallbacks. `LiquidGlassTextFieldStyle` applies a custom glass capsule to every consumer on iOS 26. No `GlassEffectContainer` use was found. `PlainToolbarItem` suppresses the shared system background for both toolbar avatars and the 48-point filled create-pool glyph. `PoolHomeView` already uses a native `TabView`.

The drawer has a custom push animation, navigation-layout stabilization, and tab-bar gesture exclusion. The navigation-drawer spec explicitly requires an opaque drawer. Profile also requires the username save action to remain in the scrolling form and preserves initial focus and subsequent selection. These constraints make a navigation redesign or relocating form actions inappropriate for this change.

## Goals / Non-Goals

**Goals:**

- Centralize OS availability and material choices in the shared UI package while leaving feature state and action closures in their existing owners.
- Make material placement explicit enough that subsequent screens can reuse the correct style without inheriting glass throughout their content.
- Complete the new `ios-liquid-glass` contract and the bounded `pool-score-list` exception with existing app architecture.

**Non-Goals:**

- A new router, sidebar root, tab model, or observable-state architecture.
- Custom glass rendering, a blur imitation on old OS versions, decorative morphing, or a material on every button.
- Changing photo cropping, provider-owned sign-in controls, custom dialog presentation, data loading, or business logic.
- Retrofitting unrelated icons or redesigning Android controls.

## Decisions

### 1. Keep the deployment target and reuse the existing button fallbacks

Retain iOS 16 in the app and package manifests. Keep the shared primitive button styles for eligible standalone actions: `.glass` and `.glassProminent` on iOS 26+, `.bordered` and `.borderedProminent` below 26. Every new iOS 26 API, including a glass container, must be behind availability checks in `iOS/UI`. Container fallback content uses the same layout, state bindings, and callbacks without a glass container.

This extends the existing compatibility approach. Raising the deployment target would unnecessarily remove supported devices; scattering OS checks through feature screens would make the material policy harder to maintain.

### 2. Classify controls by their role before selecting a style

| Surface and consumers | Treatment |
| --- | --- |
| Native navigation and pool-home tab bar | Keep system materials and scroll-edge effects; do not add another glass modifier. On iOS 26+ tab content scrolls beneath the floating tab bar; nothing in the tab roots or drawer host may clip or inset it to stop above the bar. Each pool-home tab's large title collapses with that tab's own list, and rows scroll beneath the navigation bar as well; the user chose to restructure pool home within this change to achieve this (the shared navigation bar previously followed only the Scores list), keeping tabs, titles, toolbar items, destinations, and the drawer contract. |
| Toolbar avatar in `PoolScoreListRouter` and `PoolHomeView` | Keep `PlainToolbarItem` and the existing account-avatar component. |
| Create-pool toolbar action | An accent-filled circle of at least 44 points in a `PlainToolbarItem` (app-drawn accent-tinted interactive glass on iOS 26+, filled below 26) with a 28-point unframed add glyph, a full-circle hit area, and a localized create label. Retain its route and toolbar placement. |
| Welcome-screen email sign-in choices | Keep shared glass button styles, grouped locally. Preserve the provider-owned social sign-in presentation. |
| `EmailLinkSignInView`, `PoolJoinerView`, `PoolJoinRequiresSignInView` standalone continuation, confirmation, and recovery actions | Retain appropriate glass emphasis; group adjacent glass siblings where present. |
| Email/password sign-in forms, pool-creation form, username editor | Use standard inputs and standard bordered primary/secondary submission controls. Keep their existing content flow and state behavior. |
| `BetTextField` and `PendingBetItemView` | Standard inputs and inline actions for edit, save, cancel, and retry; preserve current focus and mutation handling. |
| `PoolScoreItem` invitation control | Standard bordered non-glass control with the existing shape, icon, reserved geometry, and accessible action. |
| Paging-error Retry in `LazyPagingVStackConcatenateError` (My pools, leaderboard, manage gamblers) | Standard `.borderedProminent` inline control; keep its label, placement, and single retry dispatch. |
| Leaderboard rows, rank tiles, previews, summaries, drawer body | Preserve existing surfaces and highlights. |
| Existing system share sheets | Keep existing detents and system backgrounds. |

Replace the misleading glass text-field abstraction with a shared standard input style, and migrate its current consumers. Keep the current padding and measurement contract where required by `ScoreWidthModifier`; move that dependency to the standard style or a shared input metric rather than leaving a bet-specific reference to an obsolete glass style. Preserve the existing username editor's text-input implementation and focus/selection ownership.

The standard input fills with `tertiarySystemFill`, which separates it from both plain and grouped backgrounds, and keeps a 1-point boundary of at least 3:1. A tap anywhere inside the field's capsule, including its padding, focuses the field; screens keep ownership of initial focus and selection.

Secondary inline bordered actions (the row invite and the prediction edit Cancel) use the primary color as their tint, so the prominent action is the only accent-colored control in its group, as on the original iOS 26 glass controls. Error-state pairs keep the error tint. Below iOS 26, the shared secondary glass style's bordered fallback also uses the primary tint, so standalone secondary actions such as "Email and password" and "Go to my pools" keep legible labels.

Use semantic colors and native enabled-state presentation. Do not carry the current iOS-26-only fixed opacity reduction into the new standard input style without checking disabled-text legibility. Bordered and prominent bordered styles express ordinary form hierarchy without introducing more refractive surfaces.

A blanket replacement of every button with glass would violate the content-layer boundary. Keeping glass on every text field and every row invite would also multiply effects where the user is reading or editing dense content.

### 3. Group custom glass locally and preserve view identity

Introduce a small shared availability-aware container and wrap the actual standalone glass action groups, not each button individually or the entire screen. On iOS 26+, it uses `GlassEffectContainer`; its spacing is smaller than the visual gap between independent controls so they do not merge at rest. If a group has only one glass control, no grouping is required. The shared glass button styles use the large control size on both branches so standalone glass actions provide at least a 44-point target at every Dynamic Type size.

Do not wrap the native toolbar or tab bar in a custom glass container. Avoid changing state-dependent view identity simply to add material grouping, and keep provider-owned controls outside the custom group. Do not add `glassEffectID`, custom namespaces, or morph animations: the migration has no behavior that needs them.

Containers follow Apple's rendering guidance; they are not a substitute for limiting the number of glass effects. A screen-wide container could create unwanted relationships between unrelated controls.

### 4. Use one canonical source for the changed create glyph

Use the unframed Material Symbols add glyph, commit its canonical SVG under `assets/icons/add.svg`, and derive the iOS image asset and Android vector drawable from it. Include source attribution and any required license notice beside the canonical source. The Android create action keeps its placement and replaces its placeholder content description with the localized "Create pool" label used on iOS. The glyph geometry comes from the shared source; the green surround comes from the control, not the icon.

After reviewing the first implementation, the user asked for the create action to be a green-filled control that is larger than an ordinary icon button on both platforms, following the accent contrast rules of section 7. On both platforms it is a circular control filled with the accent (#2E7D32 light, #4CAF50 dark) and an add glyph in the on-accent colour (white in light, 5.13:1; black in dark, 7.56:1). On iOS it is a circle of at least 44 points in the toolbar with a 28-point glyph and a hit area covering the whole circle. On iOS 26 and later the native toolbar clamps prominent glass buttons to the same 44-point circle as ordinary glass buttons and only accepts taps on a 32 × 36 point content area, so the control hides the toolbar's shared background (as `PlainToolbarItem` does for the avatar) and draws its own accent-tinted interactive glass circle; this is the one deliberate app-drawn glass exception in a bar, chosen by the user for a full 44 × 44 point hit area. Below 26 it is an accent-filled circle. On Android it is a Material filled icon button with a 48 dp container and a 28 dp glyph (ordinary icon buttons use a 40 dp container). This supersedes the earlier accent-tinted glyph on plain glass and the `toolbarActionTint()` modifier, which is removed if nothing else uses it. The avatar's background exception is unchanged.

The previous filled circular glyph baked its surround into the icon. The unframed glyph lets the control supply that surround. Existing icons unrelated to this action stay outside scope. Entirely OS-rendered back, sheet, and sharing controls need no canonical app asset.

### 5. Preserve native sheet presentation and the existing drawer

The invitation sheets in `PoolScoreListView` and `PoolHomeRouter` already request medium and large detents without overriding their backgrounds. Verify those surfaces rather than adding glass manually. Do not migrate `MinimalDialog` or the avatar-crop overlay as a side effect.

Keep the avatar background exception: Apple's toolbar guidance explicitly demonstrates hiding a shared background for a profile image. Do not remove `PlainToolbarItem` globally. The create-pool call site changes independently.

Keep the drawer opaque, including during its reveal. Native tab-bar minimization, `NavigationSplitView`, and background-extension effects are deferred because they would change layout/gesture behavior or add a treatment for which the current screens have no identified need. No leaderboard-spec change is needed: its list canvas and surrounding pool-home structure remain as they are.

### 6. Let native materials adapt; verify app-owned behavior

Rely on native glass adaptation for Reduce Transparency and Increase Contrast. Do not map Reduce Transparency to `.identity` by default: removing an effect is not equivalent to providing a legible opaque control. Keep semantic foregrounds and verify contrast over real screen content.

The migration introduces no decorative animation. If affected app-owned state transitions currently animate spatial movement, respect Reduce Motion while preserving the final state. Keep semantic buttons, labels, full visible hit areas, and at least 44-point standalone/toolbar targets. Decorative containers must not change VoiceOver traversal.

Affected loading rows continue to populate production components with placeholder models and the existing shared shimmer. Material changes must not cause remote requests, navigation, or placeholder values to become accessible.

### 7. Keep accent-filled labels legible on both platforms

White labels on the previous accent (#4CAF50) measure 2.78:1, and on iOS the system lightens the accent further with Increase Contrast. Use #2E7D32 (Material Green 800) as the light-appearance accent on iOS and Android, giving 5.13:1 with white labels. Keep #4CAF50 in dark appearance and give accent-filled controls a dark label there (7.56:1 with black). On iOS, `AccentColor` defines explicit high-contrast variants with the same values; the native glass and bordered styles still darken or lighten the rendered fill under Increase Contrast, which is accepted because labels measure at least 7:1 there. Prominent glass and the shared `.standardProminent` style set the dark-appearance label color from the UI package's `OnPrimaryColor`; error-tinted prominent actions keep their error styling. The primary-container color moves from #3D8F44 (4.03:1 with white) to #1B5E20 (7.87:1) with white content in both appearances on iOS and Android. The iOS welcome gradient keeps starting from the primary container; the welcome screen uses the white Fortuna logo and wordmark and light status-bar content over it in both appearances (7.87:1), choosing the most legible option over the previous black logo. Android's welcome screen follows the same treatment: white logo and wordmark and light status-bar icons over the #1B5E20 gradient start. On Android, update `primary` and `onPrimary` (and `primaryContainer`/`onPrimaryContainer`) in both color schemes.

The app icon (iOS and Android launcher), the iOS launch-screen logo, and the Android splash logo move to #2E7D32. Pool illustrations and store screenshots keep their existing colors. The darker light accent drops below 4.5:1 only where it would be used as text on dark content, which dark appearance avoids by keeping #4CAF50.

### 8. Validate behavior and rendering at the correct level

Use existing package and app tests for action routing, pool-row accessibility, prediction draft/focus behavior, username editing, and drawer interaction. Add focused regression coverage only where changed behavior is not covered; assertions that merely check a SwiftUI modifier exists do not establish material correctness.

Capture and inspect representative screens on iOS 26+ and the oldest supported iOS runtime available for execution, explicitly including iOS 16 before compatibility is declared verified. Check iPhone and iPad layouts, large Dynamic Type, light/dark appearance, Reduce Transparency, Increase Contrast, Reduce Motion, and VoiceOver. If a required runtime or device is unavailable, record that gap rather than claiming a pass. Build with the iOS 16 deployment target regardless.

Exercise welcome/sign-in, pool creation, join confirmation and error recovery, My pools invitation, prediction edit/save/failure, username editing, and both drawer hosts. Include fast list scrolling, keyboard presentation, rotation, iPad resizing, tab interactions, and interactive Back from a pushed destination. Inspect repeated rows for excess material layers and scroll stutter; no fixed performance improvement is assumed from container use alone.

## Risks / Trade-offs

- Material appearance varies across OS releases and accessibility preferences → validate behavior and legibility, not a single pixel-identical glass screenshot.
- Native button sizing can disturb row geometry → preserve the pool invite's reserved footprint and verify large-text reflow; permit native sizing changes only within the toolbar control.
- Renaming the text-field style can alter score measurement or recreate an input → migrate `ScoreWidthModifier` and preserve the existing input identity, focus, and bindings together.
- The drawer stabilizes UIKit navigation layout and excludes tab gestures → retain those mechanisms and run the drawer interaction checks after toolbar changes.
- The canonical create glyph and accent colour touch Android in an otherwise iOS-focused migration → confine Android work to that glyph, the accent and on-accent colours, and the launcher icon and splash logo, with no material or navigation changes.
- Darkening the brand accent changes every accent-tinted surface on both platforms → verify primary actions, tints, and the icon in light and dark rather than a single screen.
- Existing pool-list requirements forbid visual changes → the delta renames and replaces the complete preservation requirement with explicit, bounded exceptions while retaining plural and navigation scenarios.

## Migration Plan

1. Establish the shared standard input style, glass-container fallback, and canonical create glyph.
2. Migrate the iOS call sites by the surface table, update the shared accent and app icons, and adapt the Android create-glyph asset/consumer, theme colors, launcher icon, and splash logo.
3. Run relevant existing tests, build at the retained deployment target, and complete the visual/interaction matrix. Record unavailable validation environments.
4. Ship through the ordinary app release process; no backend or data migration is needed. A rollback can revert the UI/resource changes without affecting stored accounts, pools, or predictions.

## References

- [Apple: Materials](https://developer.apple.com/design/human-interface-guidelines/materials) — content/control separation and restrained glass use.
- [Apple: Applying Liquid Glass to custom views](https://developer.apple.com/documentation/swiftui/applying-liquid-glass-to-custom-views) — local containers and their spacing behavior.
- [Apple: Build a SwiftUI app with the new design](https://developer.apple.com/videos/play/wwdc2025/323/) — automatic adoption, profile-image toolbar exception, native sheets, and scroll-edge effects.

These were consulted during exploration. Recheck the current Apple API documentation at implementation time, following the `liquid-glass` skill; repository icon requirements take precedence over its symbol examples.
