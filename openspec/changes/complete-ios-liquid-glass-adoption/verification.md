# Verification

## iOS — tasks 1.1–1.4 (2026-09-27)

Toolchain: Xcode 26.0.1 (17A400) on macOS 27.0 (26A428). Simulators: iPhone 17 on iOS 26.5 (`tmp-ci-repro-iPhone17-26.5`) and iPhone 16 Pro on iOS 18.1. No iOS 16 runtime was available (see "Not verified").

### 1.1 Apple guidance, deployment target, availability guards

- Rechecked Apple's current documentation for `GlassEffectContainer` and "Applying Liquid Glass to custom views". `GlassEffectContainer(spacing:content:)` is iOS 26.0+. Shapes blend when the container spacing exceeds the gap between them, and stay separate at rest when it is less than or equal to that gap. Apple recommends grouping nearby glass effects in a container and limiting the number of effects on screen. `.glass` and `.glassProminent` button styles are interactive automatically.
- Deployment targets are unchanged: every local package manifest declares `.iOS(.v16)`, and every `IPHONEOS_DEPLOYMENT_TARGET` in `Tyche.xcodeproj` is `16.0`.
- The UI package's iOS 26 APIs (`glassEffect`, `GlassEffectContainer`, `.glass`, `.glassProminent`, `sharedBackgroundVisibility`) are all inside `#available(iOS 26.0, *)` branches. The compiler enforces this at a 16.0 target.
- `xcodebuild -workspace Tyche.xcworkspace -scheme UI -destination 'generic/platform=iOS Simulator' clean build` succeeded. The `UI` module compiled with `-target arm64-apple-ios16.0-simulator` and `x86_64-apple-ios16.0-simulator`.
- `xcodebuild -workspace Tyche.xcworkspace -scheme Tyche -destination 'generic/platform=iOS Simulator' build` succeeded.

### 1.2 Local glass container

`LiquidGlassContainer` was verified with a throwaway harness app. The harness depends on the local UI package at an iOS 16.0 deployment target and is driven by XCUITest; it is not committed. The harness shows a `.liquidGlassProminent` / `.liquidGlass` pair in a `VStack(spacing: 8)` inside the container.

| Check | iOS 26.5 light | iOS 26.5 dark, AX Large text | iOS 18.1 light | iOS 18.1 dark |
| --- | --- | --- | --- | --- |
| Separate at rest (frames don't intersect; 8 pt gap visible) | Pass | Pass | Pass | Pass |
| Primary above secondary, same emphasis as without the container | Pass | Pass | Pass (bordered fallback) | Pass (bordered fallback) |
| Taps near all four edges of each control reach only that control, one action per tap | Pass (8/8) | Pass (8/8) | Pass (8/8) | Pass (8/8) |
| No extra accessibility element (exactly two buttons exposed) | Pass | Pass | Pass | Pass |

Captures: `verification/1.x-harness-ios26.5-light.png`, `verification/1.x-harness-ios26.5-dark-ax-large.png`, `verification/1.x-harness-ios18.1-light.png`, `verification/1.x-harness-ios18.1-dark.png`.

Observation for group 3: at the default control size, the shared glass buttons and their bordered fallbacks are 34.3 pt tall at Large text (53.7 pt at AX Large). This matches today's buttons and is not changed by the container. The 44-point minimum for changed standalone controls still has to be addressed when task 3.1 applies the container.

### 1.3 Standard input style

- `StandardTextFieldStyle` (`.standard`) replaces `LiquidGlassTextFieldStyle`. It keeps the 16 pt padding and capsule geometry through `InputMetrics.contentPadding`. The enabled field has a `secondarySystemBackground` fill and a 1 pt `systemGray` boundary (the increased-contrast variant in light appearance). The disabled field has full-contrast text, no fill, and a `separator` boundary.
- Migrated consumers: `EmailSignInView`, `EmailAndPasswordSignInView` (email and password), `PoolFromLayoutCreatorStepTwoView` (pool name), `UsernameEditor` (username), and `BetTextField` (prediction scores). `ScoreWidthModifier` now reads `InputMetrics.contentPadding`, with the same value as before.
- Fortuna, signed out, on iOS 26.5 (light at Large text, and dark at AX Large text), driven by XCUITest without submitting anything:
  - Email and password screen: the email field took keyboard focus and bound typed text. A double-tap selection was replaced by typed text. Focus moved to the password field, which bound 9 typed characters. Field heights were 54 pt (22 pt text + 2 × 16 pt padding) at Large text.
  - Email-link screen: the email field took focus and bound typed text, and Sign in became enabled.
  - Captures: `verification/1.3-email-password-ios26.5-light.png`, `verification/1.3-email-password-ios26.5-dark-ax-large.png`, `verification/1.3-email-link-ios26.5-light.png`. iOS hides secure-field contents in screenshots, so the password field appears empty in the captures; the test read its 9-character value.
- Harness on iOS 26.5 and 18.1, light and dark. The harness used the same style, a numeric score field sized like `scoreWidth()`, and a disabled field.
  - Focus moved between fields, bindings updated, selection replacement worked, and numeric score input worked. On iOS 18.1 the caret landed before the existing digit, which gave `123` instead of `312`. That is caret placement, not a style issue.
  - The disabled field stayed non-interactive.
- Contrast is measured by `StandardInputColorsTests` in light and dark, at base and elevated levels. The enabled boundary is at least 3:1 against `systemBackground` and `systemGroupedBackground`. Entered text is at least 4.5:1 on the fill and on both screen backgrounds, which covers a disabled field without its fill. Plain `systemGray` measured 2.92:1 on light grouped backgrounds, so light appearance uses its increased-contrast variant.

#### 1.3 follow-up: tap anywhere in the field to focus it (2026-09-27)

- The user asked that a tap anywhere inside the field capsule, including the 16 pt padding, focus the field. `StandardTextFieldStyle` now attaches its own additional `@FocusState` binding to the field, plus a tap handler on the capsule background, which sits behind the field. This covers every standard-input consumer (email-link, email and password, pool name, username, prediction scores) without changing any consumer's focus state, initial-focus code, bindings, or selection handling. Taps on the text line still reach the system field directly.
- Harness on iOS 26.5 (light, Large text) and iOS 18.1 (light, Large text):
  - Tapping each field from an unfocused state, on the top, bottom, leading, and trailing padding, focused that field and raised the keyboard. The harness's own `@FocusState` reported the focused field every time. This covered two text fields and a score field sized like `scoreWidth()`.
  - Setting focus through the harness's binding still focused the score field and accepted typing, so programmatic and initial focus remain owned by the consumer.
  - A padding tap on a disabled field did not focus it.
  - Capture: `verification/1.3-padding-tap-harness-ios26.5-light.png`.
- Control run on iOS 26.5 with the tap handler temporarily removed: the first padding tap on the email field did not focus it. So the handler is what makes padding taps work. The handler was restored and the full run above was repeated.
- Fortuna, signed out, on iOS 26.5, light at Large text:
  - Email and password screen: focus moved between the two fields using only padding taps (top, bottom, and leading on the password field; all four sides on the email field), and each field reported keyboard focus exclusively. The trailing padding of the password field holds the existing show/hide toggle, which keeps its own tap, so trailing taps were not used there.
  - Email-link screen: padding taps on all four sides left the email field focused.
  - Typing, selection replacement, and secure entry still worked after these changes.
- Unit tests on iOS 18.1 (Tyche scheme, skipping `TycheUITests`) passed again after the change, 61 Swift Testing tests plus the XCTest bundles. `Package.resolved` files were unchanged.

#### 1.3 follow-up: signed-in inputs on Prod, read-only (2026-09-27)

Fortuna debug build on the iOS 26.5 simulator (light, Large text), signed in by the user. Driven by XCUITest. Nothing was saved: no Done on the pool-name step, no Save username, no keyboard return in the username field (it submits), no Save on a prediction, and no Sign out. Each screen was backed out or cancelled, and the app was left signed in.

- **Pool name** (My pools, then Create, then the "Copa Mundial de la FIFA 2026" template):
  - The field opens unfocused and pre-filled with the template name. The text line is 22 pt tall inside the 16 pt padding, the same geometry as the email fields.
  - The screen was reopened fresh for each padding side. A single tap on the top, bottom, leading, or trailing padding focused the field and raised the keyboard.
  - Typing appended to the bound value. A double-tap selection was replaced by typed text.
  - Backed out with Back; no pool was created.
- **Username** (drawer, then Profile, then Username):
  - The field received its existing initial focus on open, with the stored username.
  - Typing updated the draft, the grapheme counter (`11/100`), and the live pool preview. A double-tap selection was replaced by typed text, and Save username became enabled but was not tapped.
  - After Back, the Profile row still shows the stored username.
  - Padding taps on all four sides kept the field focused. Limitation: this screen has only one field and scrolling doesn't dismiss the keyboard, so these taps started from an already-focused field. Focusing an unfocused field by a padding tap is covered by the pool-name, prediction, email, and harness checks, which use the same shared style.
- **Prediction** ("Copa Mundial de la FIFA prur" pool, Bets tab, pending Colombia vs Portugal, Edit):
  - Edit gave the first score field its existing automatic focus.
  - Each score capsule is `scoreWidth()` wide: a 30.7 pt three-digit text line plus 2 × 16 pt padding, and 54 pt tall.
  - Padding taps on each side alternated keyboard focus between the two score fields, with exactly one field focused each time.
  - Typing with the number pad bound the values `2` and `1`, and Save became enabled but was not tapped.
  - Cancel returned the row to its Edit state without saving.
- Captures (they show account data):
  - `verification/1.3-signed-in-poolname-typed-ios26.5-light.png`
  - `verification/1.3-signed-in-username-initial-ios26.5-light.png`
  - `verification/1.3-signed-in-username-typed-ios26.5-light.png`
  - `verification/1.3-signed-in-prediction-editing-ios26.5-light.png`
  - `verification/1.3-signed-in-prediction-cancelled-ios26.5-light.png`
- Observation: the username screen uses a grouped background (`#F2F2F7` in light appearance), the same color as the field's `secondarySystemBackground` fill. There the field is identified by its boundary, which measures at least 3:1, rather than by its fill. The field's own contrast is correct; whether to change the fill color there is a design choice to take up in task 3.2 or 3.5.

#### 1.3 follow-up: background-adaptive fill, dark and large text, iOS 18.1 (2026-09-27)

- **Fill change.** The field fill is now `tertiarySystemFill`, Apple's semantic fill for input fields. It replaces the opaque `secondarySystemBackground` fill, which was identical to the grouped background (`#F2F2F7`, ratio 1.0) on the username screen.
  - The new fill is translucent, so the shared style separates from any background without per-screen parameters or environment flags.
  - Composited over plain and grouped backgrounds, at base and elevated levels, it measures 1.14–1.15:1 against the background in light appearance and 1.24–1.33:1 in dark.
  - The disabled presentation (no fill, `separator` boundary, full-contrast text) and the 3:1 `systemGray` boundary are unchanged.
  - `StandardTextFieldStyle.swift` now imports UIKit explicitly.
- **Tests.** `StandardInputColorsTests` now composites the fill over each background.
  - New rule: the fill differs from `systemBackground` and `systemGroupedBackground` by at least 1.1:1. This is a visual-separation threshold, not a WCAG criterion; the boundary still carries the 3:1 identification requirement. The previous fill fails it on grouped backgrounds.
  - Entered text is at least 4.5:1 on the composited fill over every background.
  - The Tyche scheme unit run on iOS 18.1 passed: 62 Swift Testing tests plus the XCTest bundles. `Package.resolved` files were unchanged.
- **Signed-in, read-only checks after the fill change.** Same procedure as above: no Done, Save username, keyboard return in the username field, prediction Save, or Sign out; each screen backed out or cancelled.

| Run | Pool name | Username | Prediction |
| --- | --- | --- | --- |
| iOS 26.5, light, Large | Pass | Pass | Pass |
| iOS 26.5, dark, AX Large | Pass | Pass | Pass |
| iOS 18.1 (iPhone 16 Pro), light, Large | Pass | Pass, including padding taps from an unfocused field | Pass |
| iOS 18.1 (iPhone 16 Pro), dark, AX Large | Pass | Pass, including padding taps from an unfocused field | Pass |

- **Per-field results:**
  - Pool name: opens unfocused. A single padding tap on each side focused it, typing bound the value, and a selection was replaced by typing.
  - Username: open-time focus kept. Typing updated the counter and the live preview, and a selection was replaced by typing. The stored username was unchanged after Back.
  - Prediction: Edit focuses the first score. Padding taps alternated focus between the two scores, the number pad bound `2` and `1`, and Cancel restored the row.
- **Sizing at AX Large.**
  - The field text lines grow from 22 pt to 41.3 pt, inside the unchanged 16 pt padding.
  - The score text line grows from 30.7 pt to 61.3 pt wide, because `scoreWidth()` measures three digits in the current body font. It stays aligned with the rows' other score cells, which use the same modifier.
- **One flaky run.** On iOS 26.5, light, Large, the first pool-name run typed only the space of " QA" (a UI-test typing flake). The immediate rerun, and the dark, AX Large run, passed.
- **The iOS 18.1 simulator was already signed in** when the app was installed and launched, so no sign-in was needed there.
- **Unfocusing the username field without saving.**
  - iOS 18.1: dragging the content down toward the keyboard dismisses it. Every padding side was then tapped from an unfocused state and focused the field.
  - iOS 26.5: none of these read-only attempts unfocused the field:
    - tapping the subtitle or the "Pool preview" label;
    - dragging the content toward the keyboard;
    - swiping the content;
    - backgrounding and reactivating the app (focus and the keyboard are restored);
    - a cancelled interactive back swipe.
  - The screen has no other focusable control. The only remaining exits are the keyboard's return key and Save username, which both save, and Back, which removes the field.
- **Simulator settings.** iOS 26.5 was restored to light / Large, and iOS 18.1 is at light / Large. The iOS 18.1 simulator was shut down; the iOS 26.5 simulator remains booted and signed in.
- **Captures (they show account data):**
  - iOS 26.5, light, Large: `verification/1.3-signed-in-{poolname-typed,username-typed,prediction-editing}-ios26.5-light-large.png`
  - iOS 26.5, dark, AX Large: `verification/1.3-signed-in-{poolname-typed,username-typed,prediction-editing}-ios26.5-dark-ax-large.png`
  - iOS 18.1, light, Large: `verification/1.3-signed-in-{poolname-typed,username-typed,prediction-editing}-ios18.1-light-large.png`
  - iOS 18.1, dark, AX Large: `verification/1.3-signed-in-{poolname-typed,username-typed,prediction-editing}-ios18.1-dark-ax-large.png`

### 1.4 Documentation and tests

- `iOS/UI/README.md` documents the surface-selection table, glass button fallbacks, `LiquidGlassContainer` usage and limits, `StandardTextFieldStyle`, and `InputMetrics`. The names and behavior match the shared APIs above.
- `xcodebuild test -workspace Tyche.xcworkspace -scheme Tyche -destination 'platform=iOS Simulator,id=<iPhone 16 Pro iOS 18.1>,arch=arm64' -skip-testing:TycheUITests CODE_SIGNING_ALLOWED=NO`: every package bundle passed, including the new `StandardInputColorsTests`. `TycheTests` could not start with signing disabled: the host app trapped in `StorageInUserDefaults.symmetricKey()` because the unsigned app had no keychain access. That failure is unrelated to this change.
- `xcodebuild test ... -only-testing:TycheTests` with simulator signing on iOS 18.1: passed.
- `Package.resolved` files were unchanged after the runs.

### Not verified

- iOS 16 runtime: none installed. `xcodebuild -downloadPlatform iOS -buildVersion 16.4` stayed at "Finding content..." without downloading (two attempts, more than 35 minutes in total). The iOS 16 fallback was checked on iOS 18.1 instead, which takes the same pre-26 branches. The builds use the 16.0 deployment target.
- Pool-name, username, and prediction inputs in the running app: these screens need a signed-in account, and saving would write to Prod. They were covered by the shared-style harness (the same style, and a score field sized like `scoreWidth()`) and by the unchanged `UsernameEditorViewModelTests` and `UsernameFieldInitializationTests`. The on-device checks are left to tasks 3.2 and 3.3. (Superseded: see "1.3 follow-up: signed-in inputs".)
- iPad, Reduce Transparency, Increase Contrast, and VoiceOver passes for these primitives are scheduled with the screen migrations (tasks 3.5 and 5.2).
- Follow-up (2026-09-27): the user accepted the iOS 16 runtime as not verified for the whole change, with iOS 18.1 standing in for versions below 26. No further download attempts were made.
- Follow-up (2026-09-27): the signed-in checks of the pool-name, username, and prediction fields are done on iOS 26.5 and 18.1, in light / Large and dark / AX Large (see "1.3 follow-up: background-adaptive fill"). A padding tap focusing the username field from an unfocused state is verified on iOS 18.1. On iOS 26.5 it is not verified: no read-only way to unfocus that field was found there. It uses the same shared mechanism as the pool-name, prediction, and email fields, which were verified from unfocused on iOS 26.5.

Accepted without the iOS 16 runtime check (iOS 18.1 stands in for pre-26) for tasks 1.1 and 1.2 by the user on 2026-09-27.

Accepted without the iOS 16 runtime check (iOS 18.1 stands in for pre-26) and without the iOS 26.5 unfocused-username padding-tap check for task 1.3 by the user on 2026-09-27.

## iOS — tasks 2.1–2.4 (2026-09-27)

Toolchain: Xcode 26.0.1 (17A400) on macOS 27.0. Simulators, all signed in to Prod:

- iPhone 17 on iOS 26.5 (`tmp-ci-repro-iPhone17-26.5`)
- iPhone 16 Pro on iOS 18.1
- iPad Air 11-inch (M4) on iOS 27.0 (the user signed in for this pass)

Prod was used read-only throughout:

- The pool-creation flow was opened and backed out with Back.
- Invitation sheets were dismissed unsent.
- The delete-pool alert in the existing drawer pass was cancelled.
- Nobody was signed out.

The runtime checks used a throwaway XCUITest, which was not committed. The existing `DrawerPassUITests` ran unchanged.

### 2.1 Canonical add glyph

- `assets/icons/add.svg` is Google's Material Symbols "add" (Outlined, fill 0, weight 400, grade 0, optical size 24), downloaded unmodified from the `google/material-design-icons` repository at commit `bd8cb85bd4bad964fe6918f79665bb40c3a8efef`. The fonts.gstatic.com copy of the same symbol is byte-identical.
  - `assets/icons/README.md` records the source URL, style, SHA-256, and the platform mapping.
  - `assets/icons/MATERIAL_SYMBOLS_LICENSE.txt` is the Apache 2.0 text from the same commit.
- The iOS asset `iOS/UI/Sources/UI/Assets/Assets.xcassets/Icons/add.imageset/add.svg` is a byte-identical copy, exposed as `SharedImageResource.add`. Its image set declares template rendering and preserved vector data.
- `assetutil --info` on the compiled `UI_UI.bundle/Assets.car` shows an `add` vector rendition and a template-mode monochrome raster with `Preserved Vector Representation: true`.
- The previous `filled_add_circle` image set had no other consumer. It was removed with its `SharedImageResource` entry.
- Rendered geometry: the glyph draws at its 24 pt intrinsic size. On iOS 26.5 and 18.1, in light and dark, the plus measures 14 × 14 pt with 2 pt bars. That matches the SVG's 560 × 560 of 960 units with 80-unit bars.
- The template takes the tint set in 2.2 in both appearances. Capture: `verification/2.1-add-glyph-template-ios26.5-ios18.1-light-dark.png` (left to right: 26.5 light, 26.5 dark, 18.1 light, 18.1 dark).

### 2.2 Create-pool toolbar action

- `PoolScoreListRouter` places the create action in an ordinary `ToolbarItem(placement: .topBarTrailing)`.
  - The button's label is a `Label`: its title is the new `create_pool_action` string and its icon is the add glyph. Toolbars show only the icon.
  - The fixed 48 pt frame and the `.plain` button style are gone. The avatar keeps `PlainToolbarItem`.
- Glyph tint (the user's decision): the button applies the new shared modifier `toolbarActionTint()` (`iOS/UI/Sources/UI/ToolbarActionTint.swift`). It sets `.tint(Color.accentColor)` on iOS 26 and later, inside the native glass control, and `.tint(.primary)` below iOS 26. The OS check stays in the UI package, and `iOS/UI/README.md` documents the modifier in its surface table.
- New string `create_pool_action` in the Tyche app catalog (`extractionState: manual`): "Create pool" (en), "Crear polla" (es), "Crear quiniela" (es-ES).
  - The generated `createPoolAction` symbol exists.
  - The built app's `en`, `es`, and `es-ES` `Localizable.strings` contain the three values.

Results after the tint change:

| Check | iOS 26.5 light, en | iOS 26.5 dark, es | iOS 18.1 light, en | iOS 18.1 dark, es |
| --- | --- | --- | --- | --- |
| Exactly one toolbar button, button role, localized name ("Create pool" / "Crear polla") | Pass | Pass | Pass | Pass |
| Surface | One native glass circle, 44 pt wide (measured), no second surround | Same | Plain bar button (pre-26 fallback) | Same |
| Glyph tint (measured pixel) | Accent `#4CAF50` | Accent, rendered `#58BC5D` | Primary, black | Primary, white |
| Glyph contrast against the surface behind it | 2.78:1 on the white glass (`#FFFFFF` over the white list) | 7.34:1 on the dark glass (`#191919`) | 21:1 on the white bar | 21:1 on the black bar |
| Accessibility frame | 36 × 36 pt | 36 × 36 pt | 43 × 44 pt | 43 × 44 pt |
| Taps at the centre and at 20, 21.5, and 19 × 19 pt diagonal offsets from it (11 points covering a 43 × 43 pt square): each opened the creator, and one Back returned to My pools | 11/11 | 11/11 | 11/11 | 11/11 |
| Avatar still opens the drawer (photo, no glass surround); drawer closes with Close menu | Pass | Pass | Pass | Pass |

- One route per tap: after each tap a single Back returned to My pools with the creator gone, so the creator was pushed once.
- On iOS 26 in light appearance, the accent glyph measures 2.78:1 against the glass over the white list, below the 3:1 non-text target. The user chose the accent there, so it is kept.
- On iPad (iOS 27.0, full screen, light), the glyph renders `#4BAE4F` on a `#FDFDFD` glass surface, 2.76:1.
- Hit target: the user accepted native toolbar sizing, and no frame is forced.
  - iOS 26.5: the glass control is 44 pt wide, its accessibility frame is 36 × 36 pt, and taps up to 21.5 pt from its centre on each axis activate it.
  - iOS 18.1: the frame is 43 × 44 pt.
- Older-iOS fallback: iOS 18.1 shows the same action, name, and route as a primary-tinted plain bar button.
- Captures:
  - `verification/2.2-toolbar-ios26.5-light.png`
  - `verification/2.2-toolbar-ios26.5-dark-es.png`
  - `verification/2.2-toolbar-ios18.1-light.png`
  - `verification/2.2-toolbar-ios18.1-dark-es.png`
  - `verification/2.2-creator-ios26.5-light.png`
  - `verification/2.2-creator-ios18.1-light.png`

### 2.3 Navigation, tab, and sheet audit

- Source audit of `iOS/` (excluding tests):
  - No `toolbarBackground`, `toolbarColorScheme`, `presentationBackground`, `UINavigationBarAppearance`, `UITabBarAppearance`, `appearance()` proxy, `tabBarMinimizeBehavior`, `tabViewStyle`, `backgroundExtensionEffect`, or bar or tab-bar `glassEffect`.
  - `PoolHomeView` uses a native `TabView` with three tabs (Scores, Bets, History), with `drawerTabBarBoundary` and `excludesTabBarFromDrawerDrags`.
  - Both invitation sheets (`PoolScoreListView` and `PoolHomeRouter`) present `UIActivityViewController` with `.presentationDetents([.medium, .large])` and no background override.
  - The only `sharedBackgroundVisibility` is inside `PlainToolbarItem`, which now serves only the two avatars.
  - `ProfileView` hides the navigation bar only while the avatar crop overlay is shown, which is out of scope.
  - No conflicting customization was found, so nothing was changed.

| Check | iOS 26.5 light | iOS 18.1 light |
| --- | --- | --- |
| My pools row invite opens the share sheet at the medium detent | Pass | Pass |
| Dragging the grabber up reaches the large detent | Pass | Pass |
| Sheet background | System glass | System standard sheet |
| Close dismisses it and returns to My pools without opening the pool | Pass | Pass |
| Pool-home drawer Invite opens the share sheet at medium and dismisses back to pool home (`DrawerPassUITests.testPoolHomeDrawer`, 3 pools) | Pass | Pass |
| Pool home shows three tabs in order: Scores, Bets, History | Pass | Pass |
| Scrolling the leaderboard 250 pt keeps the tab bar at the same frame (no minimization) | Pass (402 × 83 pt before and after) | Pass (402 × 83 pt) |

- No new tab or sheet transitions were added. The source is unchanged apart from the create button.
- Captures:
  - `verification/2.3-invite-sheet-{medium,large}-{ios26.5,ios18.1}-light.png`
  - `verification/2.3-pool-home-drawer-invite-sheet-{ios26.5,ios18.1}-light.png`
  - `verification/2.3-pool-home-{tabs,scrolled}-{ios26.5,ios18.1}-light.png`

### 2.4 Drawer and navigation

- `xcodebuild test -workspace Tyche.xcworkspace -scheme Tyche -destination 'platform=iOS Simulator,id=<sim>' -parallel-testing-enabled NO -only-testing:TycheUITests/DrawerPassUITests`, with `TEST_RUNNER_DRAWER_UI_PASS=1`:

| Test | iOS 26.5 | iOS 18.1 |
| --- | --- | --- |
| `testDrawerNavigationLayout` (both hosts, portrait / landscape left / landscape right, two open-close cycles each) | Pass | Fail (pre-existing, see below) |
| `testPoolListDrawer` (open, tap-to-close without reaching the row, Profile opens once, one Back returns with the drawer closed) | Pass | Pass |
| `testPoolHomeDrawer` (three pools: drawer content, owner delete alert cancelled, Invite sheet, back to My pools) | Pass | Pass |
| `testPoolListDrawerGestures` (swipe open and closed, reversed leading swipes and flicks declined, back-swipes from Profile, vertical scroll at AX XXXL) | Pass | Pass |
| `testPoolHomeDrawerGestures` (swipe open and closed, tab tap and tab-bar slide, leaderboard scroll, back-swipes from a gambler's bets) | Pass | Pass |

- iOS 18.1 `testDrawerNavigationLayout` fails at its first assertion on the list host in portrait: "avatar shifted within the pushed screen" (opener minX − bar minX = −324 instead of 16).
  - The pre-change router and asset (`HEAD` versions, temporarily restored and rebuilt) fail identically, so this change did not cause the failure.
  - Visually, the avatar moves with the pushed screen on iOS 18.1 (`verification/2.4-drawer-open-ios18.1-light.png`). The failure looks like XCUITest reporting the toolbar avatar's untransformed frame on that runtime. It is left for a separate follow-up.
- The avatar source is unchanged: `AutoEmailAvatar` in `PlainToolbarItem`, named "Open menu". The captures show the account photo without a glass surround, and the drawer stays opaque.
- Keyboard and accessibility focus restoration is covered by the passing UI-package drawer tests below. VoiceOver focus itself was not observed (see "Not verified").
- Unit tests passed on iOS 18.1, with simulator signing, before the tint change: `** TEST SUCCEEDED **`.
  - Command: `xcodebuild test -workspace Tyche.xcworkspace -scheme Tyche -destination 'platform=iOS Simulator,id=<iPhone 16 Pro iOS 18.1>' -parallel-testing-enabled NO -skip-testing:TycheUITests`.
  - The run included `DrawerHostNavigationTests`, `DrawerInteractionTests`, `DrawerRevealTests`, and `TycheTests`.
  - The tint change adds only a view modifier. The app and every test bundle rebuilt with it for each later UI run.
  - `Package.resolved` files were unchanged.
- Captures: `verification/2.4-drawer-open-ios26.5-light.png`, `verification/2.4-drawer-open-ios18.1-light.png`.

### iPad (iOS 27.0, iPad Air 11-inch (M4), light, Large text)

The app runs as a resizable window (the system's windowed-apps mode). Window sizes were set by dragging the window's corner grabber with synthesized touches.

**Window sizes exercised**

- Full screen: 820 × 1180 pt.
- 375-point compact width at heights 949, 756, and 716 pt. Drags aimed at about 560 pt wide snapped to 375 pt, so no intermediate width between compact and full screen was reached.
- The 375-point window rotated to landscape.
- The window was restored to full screen afterwards.

**Toolbar, sheets, and tabs** (throwaway XCUITest, at full screen and at the 375-point window)

| Check | Full screen, 820 pt (regular width) | 375 pt window (compact width) |
| --- | --- | --- |
| Create button: one control, reachable, accent glyph on glass; opens the creator once, and Back returns | Pass (frame 36 × 36 pt) | Pass (frame 36 × 36 pt) |
| Avatar opens the list drawer, which is opaque; Close menu closes it | Pass | Pass |
| Row invite: the share sheet opens, Close dismisses it back to My pools without opening the pool | Pass (centred form sheet; the system presents a regular-width sheet without detents) | Pass (medium detent with grabber) |
| Pool home tabs: Scores, Bets, History in order | Pass (top tab bar, the system's regular-width presentation) | Pass (bottom tab bar, 375 × 72 pt) |
| Pool-home drawer opens with Invite and closes | Pass | Pass |
| List drawer in landscape, 375 pt window | n/a | Pass |

**Pool home through rotation** (full screen, throwaway XCUITest)

- In portrait, landscape left, and landscape right, the drawer opened with the pushed screen displaced 340 pt, and its avatar was not hittable.
- Close menu returned the avatar to the same x position.

**Gestures, full screen** (throwaway XCUITest using window-relative points)

- A tap on the pushed strip closed the drawer without opening the pool.
- A swipe from a row opened the drawer, and a leftward drag closed it.
- Content-area and edge back-swipes from Profile each returned to My pools with the drawer closed.
- The leaderboard scrolled vertically (row 210 → 79 pt) without opening the drawer.
- Tab taps switched tabs without opening the drawer.

**Gestures, 375 pt window**

- The pushed-strip tap and the content-area back-swipe passed.
- An edge back-swipe starting 3 pt or 10 pt inside the floating window's leading edge did not go back. This was not compared against the pre-change build. This change touched only the trailing create item, and the drawer detaches its drags while a destination is shown.

**`DrawerPassUITests` on iPad** (unchanged; they assume a full-screen iPhone)

- Full screen:
  - `testPoolListDrawer` and `testPoolListDrawerGestures` pass.
  - `testDrawerNavigationLayout` passes all three orientations for the list host.
  - That test, `testPoolHomeDrawer`, and `testPoolHomeDrawerGestures` then stop at `app.tabBars` (line 42, 139, and 283). Regular-width iPad presents the tabs at the top, and XCUITest does not report them as a tab bar.
- 375-point window: four tests fail, using screen-normalized points that fall outside the floating window, and `testPoolHomeDrawer` passes. The failures are "avatar shifted" (16 vs 84), "tapping the pushed screen did not close", "the content-area back-swipe on Profile did not return", and "the leaderboard did not scroll".
- The pre-change router and asset (`HEAD`, temporarily restored and rebuilt) produced the same five results with the same messages in that window. None of these results come from this change. The window-relative checks above cover the same behaviors.

**Captures**

- Window sizes: `verification/2.x-ipad27-{full,mid,narrow}-{list,list-drawer,invite-sheet,home,home-drawer}.png`
  - `mid` is the 375 × 756 pt window and `narrow` the 375 × 716 pt window. There is no `mid-list-drawer` capture.
- Rotation: `verification/2.x-ipad27-narrow-landscape-list-drawer.png`, `verification/2.4-ipad27-full-home-drawer-portrait.png`, `verification/2.4-ipad27-full-home-drawer-landscape.png`
- The captures show account data. The landscape frames are rotated because XCUITest captures the portrait framebuffer.

### Simulator settings

- iOS 26.5 and iOS 18.1 were switched to dark for the dark runs and restored to light. Text size stayed Large.
- Language changes used launch arguments only.
- The iPad window was resized during the checks and restored to full screen. Its orientation was returned to portrait and its appearance was left at light.
- The iOS 18.1 simulator was shut down. iOS 26.5 and the iPad remain booted and signed in.

### Not verified

- VoiceOver speech: accepted by the user as not verified. The simulator has no VoiceOver. The accessible name and button role were read through the accessibility tree (XCUITest) in English and Spanish. On-device VoiceOver announcement and focus restoration are left to task 3.5.
- iOS 16 runtime: not installed. Accepted for the whole change, with iOS 18.1 standing in for versions below 26.
- iPad at an intermediate window width: resizing snapped between 375 pt and full screen, so only those two widths were exercised.
- iPad edge back-swipe in a floating 375 pt window: it did not go back and was not compared against the pre-change build. It works at full screen.
- iPad `DrawerPassUITests` as written: they do not fit iPad presentations (regular-width top tabs, floating window coordinates). They fail identically on the pre-change build, and the window-relative checks above stand in for them.
- iPhone landscape visuals: the drawer pass asserts layout in landscape on iOS 26.5, and the iPad rotation captures cover the visuals.

Accepted without VoiceOver speech verification and with native toolbar sizing (43 × 44 pt on iOS 18.1; 44 pt glass circle with a 36 pt accessibility frame on iOS 26.5) for task 2.2 by the user on 2026-09-27.

The user kept the green accent glyph on iOS 26+ (2.78:1 against light glass) on 2026-09-27.

Accepted without the intermediate iPad window width check for task 2.3, and without the floating-window edge back-swipe, VoiceOver focus, and a full unit-test rerun after the tint change for task 2.4, by the user on 2026-09-27.

## iOS — tasks 3.1–3.5 (2026-09-27)

Toolchain: Xcode 26.0.1 (17A400) on macOS 27.0. Simulators:

- iPhone 17 on iOS 26.5 (`tmp-ci-repro-iPhone17-26.5`), signed in
- iPhone 16 Pro on iOS 18.1, signed in (stands in for pre-26)
- iPhone 17 on iOS 27.0 (`4D209B45…`), signed out, for the welcome, sign-in, email-link, and sign-in-required join screens
- A throwaway iPhone 16 Pro on iOS 18.1, created signed out for the welcome screen and deleted afterwards

Prod was used read-only. Nothing was saved or sent: no pool-name Done, no Save username or username keyboard return, no prediction Save, no Join, no Sign in, no invitation, and no Sign out. Invitation sheets were closed unsent, and join screens were left with Go to my pools. Checks that need a write used unit tests with test doubles, or rendered the production views in each state from a test (see 3.2 and 3.3). A local write-blocking proxy was considered for failure injection. Auto mode denied it, so nothing was sent through one.

The runtime checks used throwaway XCUITests and render tests, which were deleted afterwards. Captures under `verification/` show account data where noted.

### 3.1 Standalone glass actions

Code:

- **Welcome** (`HomeView`): the Email and Email and password buttons are wrapped in one `LiquidGlassContainer` (`VStack`, 8 pt spacing). The Google provider button stays outside the container.
- **Join** (`PoolJoinerView`): the Join / Go to my pools pair and the Retry / Go to my pools pair are each wrapped in one container (4 pt spacing).
- **Single-button screens** get no container: email-link Get started, email-link Retry, the join load failure, and the sign-in-required Got it.
- **Shared glass styles** (the user's 44 pt decision): `LiquidGlassButtonStyle` and `LiquidGlassProminentButtonStyle` now apply `.controlSize(.large)` on both branches (glass on iOS 26+, bordered below). Every standalone glass action is affected, including `LazyPagingVStackConcatenateError`'s Retry (see Open questions).
- **New test** `LiquidGlassButtonSizeTests` (UI package, Swift Testing) lays out both styles at every `DynamicTypeSize` and requires at least 44 × 44 pt.
  - It passes on iOS 26.5 and 18.1.
  - With the large control size temporarily removed, it failed with 14 issues on iOS 26.5.
  - Measured heights: regular was 31 pt at xSmall and 34.3 pt at Large. Large is 47 pt at xSmall, 50.3 pt at Large, 57.7 pt at xxxLarge, and 78 pt at AX3. The values are the same on iOS 26.5 and 18.1.

| Check | iOS 27.0 (signed out) | iOS 26.5 | iOS 18.1 |
| --- | --- | --- | --- |
| Welcome pair: frames 370 × 50.3 pt, 8 pt apart, Google button outside the pair | Pass | n/a (signed in) | Pass (bordered fallback, throwaway signed-out simulator) |
| Welcome: taps near the top, bottom, leading, and trailing edges of each glass button (8 taps) each open only that button's form. One Back returns to the welcome screen | 8/8 | n/a | 8/8 |
| Welcome accessibility tree: exactly `google_logo`, `Email`, `Email and password`, with no container element | Pass | n/a | Pass |
| Join pair (pool "Prueba", opened by its join link): Join and Go to my pools are 386 × 50.3 pt with a 4 pt background gap between them | n/a | Pass | Pass (bordered) |
| Join: a tap 10 % from the top of Go to my pools (the edge next to Join) returns to My pools without joining | n/a | Pass (light; dark with Reduce Transparency, Increase Contrast, and Reduce Motion) | Pass |
| Sign-in-required join (signed out, join link): Got it is 386 × 50.3 pt; tapping it returns to the welcome screen | Pass | n/a | n/a |
| Email-link failure (a link with an invalid code, which fails sign-in): the error and Retry, 386 × 50.3 pt | Pass | n/a | n/a |

- **Separation.** On iOS 26.5 the pixel column between Join and Go to my pools shows 4 pt of background (`#F4F4F4`). The secondary glass edge nearest Join picks up a green refraction, but the shapes stay separate.
- **Disabled and loading.** The join loading state wraps the content in `LoadingContainerView` with empty callbacks, and its material overlay covers the buttons. This is unchanged, and the container does not alter it. It was not observed live, because it needs a join request.
- **Provider-owned presentation.** The Google button is unchanged and outside the container.
- Captures:
  - `verification/3.1-welcome-ios27-ios18.1-light-large-dark-axl.png`
  - `verification/3.1-join-email-link-ios26.5-ios27-ios18.1-light.png` (account data: pool name)

### 3.2 Inline form actions

Code: Sign in (`EmailSignInView`, `EmailAndPasswordSignInView`), Done (`PoolFromLayoutCreatorStepTwoView`), and Save username / Try again (`UsernameEditor`) now use `.borderedProminent` at the regular size. Their geometry is unchanged: 34.3 pt at Large and 53.7 pt at AX Large.

| Check | iOS 27.0 / 26.5 | iOS 18.1 |
| --- | --- | --- |
| Email-link form: Sign in is disabled while empty and enabled for `qa@example.com` (not tapped) | Pass (27.0) | Pass |
| Email and password form: Sign in stays disabled with an email and no password (no password typed, to avoid the system save-password prompt) | Pass (27.0) | Pass |
| Pool name (template "Copa Mundial de la FIFA 2026"): opens pre-filled with Done enabled. When cleared, Done is disabled and the length error shows. "Polla QA" re-enables Done. Back leaves without creating | Pass (26.5 light / Large). At dark / AX Large the test driver's clearing step failed (no Select All menu), so the invalid state was not reached | Pass (light / Large and dark / AX Large) |
| Username: open-time focus with the stored value. Typing updates the draft and enables Save username. A double-tap selection is replaced by typing. Back keeps the stored username | Pass (26.5 light / Large, dark / AX Large, and light with Reduce Transparency, Increase Contrast, and Reduce Motion) | Pass (light / Large, dark / AX Large) |
| Username Save position with the keyboard shown | Large: Save (y 411–445) is above the keyboard (top 583) and hittable. AX Large: Save (y 564–618) is partly under the keyboard. Scrolling the content moved it to y 477–531, fully above the keyboard, still below the guidance, with the draft and keyboard kept | Same: Large hittable. AX Large reachable after scrolling (y 549 → 476) |

- **Saving and failure** can't be staged live without saving.
  - Covered by the existing `UsernameEditorViewModelTests`: saving then saved, failure keeps the attempted value, retry resubmits the same value, reset, blank draft, and in-flight saves ignored.
  - The production `UsernameEditor` was rendered in the app host with a view model whose save hangs (saving) or fails (failure), in light and dark on iOS 26.5 and 18.1.
  - In every state Save username / Try again stays in the scroll content below the guidance.
  - Saving shows the spinner in a disabled prominent button, and the field is disabled with its draft readable.
  - Failure shows "Username not saved. Try again." and a prominent Try again.
- Captures:
  - `verification/3.2-forms-ios27-ios26.5-light-large.png` (account data)
  - `verification/3.2-username-axl-keyboard-ios26.5-ios18.1-dark.png` (account data)
  - `verification/3.2-username-states-rendered-ios26.5-ios18.1.png` (sample data)

### 3.3 Prediction-row actions

Code:

- `EditableDefaultActionBar`: Cancel uses `.bordered` with `.tint(.primary)`, and Save uses `.borderedProminent`.
- `FailureActionBar`: Cancel and Retry use `.bordered` and `.borderedProminent`, keeping their existing error tint.
- The mutation state machine, bindings, focus, and `onReceive` guard are unchanged.
- The primary tint on Cancel is a correction under 3.5: the accent label on the bordered fill fails the text contrast that the glass control's black label met.

Existing bet coverage was an empty XCTest (`BetTests.testExample`). New `PendingBetItemViewModelTests` (Swift Testing) use a repository double that holds each submission until the test completes it. They pass on iOS 26.5 and 18.1:

- Pending submission shows the attempted score, and a failure keeps the original and the attempted value.
- Retry submits the same prediction once, and success moves the row to mutated.
- Cancel after a failure restores the saved prediction without another submission.

Live, pool "Copa Mundial de la FIFA prur", Bets tab, the only editable match:

| Check | iOS 26.5 light / Large | iOS 26.5 dark / AX Large | iOS 26.5 light and dark with Reduce Transparency, Increase Contrast, and Reduce Motion | iOS 18.1 light / Large | iOS 18.1 dark / AX Large |
| --- | --- | --- | --- | --- | --- |
| Edit focuses the first score. Save is disabled while empty and with only the home score (invalid). Typing 7 and 3 enables Save and moves focus to the second field | Pass | Pass | Pass | Pass | Pass |
| Cancel / Save frames | 76.7 / 61 × 34.3 pt, same as the glass buttons in 1.3 | 131.3 / 102 × 53.7 pt | same as without the settings | same as 26.5 | same as 26.5 |
| Scroll down 12 times and back with the draft open: still editing, draft 7/3, keyboard up | Pass | Pass | Pass | Pass | Pass |
| Cancel leaves edit mode (not saved) | Pass | Pass | Pass | Pass | Pass |

- **Paging while editing** was not staged. Each of the three pools has at most one editable prediction and one page (5 distinct texts across the scroll), so no page load happened during the scroll. The paging guard is unchanged code.
- **Rendered row states.** `StatefulPendingBetItemView` was rendered from a test, not committed, in view, editing unchanged / invalid / changed, pending, and failure, light and dark, on iOS 26.5 and 18.1. The package test has no app accent, so the accent renders blue there.
  - Pending shows only the spinner, with no actions.
  - Failure shows the error icon, Cancel, and Retry in the error tint.
  - Invalid disables Save.
- **Measured contrast.**
  - Cancel: black on `#D1D1D1` is 13.75:1 (light); white on `#404040` is 10.37:1 (dark).
  - Failure Cancel (red on its tinted fill): 5.18:1 light and 4.47:1 dark. That is the same bordered presentation iOS 18.1 already had (see Open questions).
- Captures:
  - `verification/3.3-prediction-live-ios26.5-ios18.1.png` (account data)
  - `verification/3.3-row-states-rendered-ios26.5.png` and `verification/3.3-row-states-rendered-ios18.1.png` (sample data)

### 3.4 My pools invitation control

- **Code.** `PoolScoreItem`'s invite uses `.bordered` with `.buttonBorderShape(.capsule)` and `.tint(.primary)`.
  - With the default accent tint, the glyph measured 2.29:1 on the bordered fill in light appearance, below 3:1. The glass glyph measured 20.65:1.
  - With the primary tint it measures 13.75:1 (light) and 10.37:1 (dark).
- **Geometry.**
  - Layout: `.bordered` and `.glass` measure the same with a 20 pt icon, 44 × 34 pt. Row pitch on iOS 26.5 is 88 pt before and after at Large, and 296.7 pt before and after at AX Large, where the row reflows with the invite beside the rank tile.
  - Rows on iOS 18.1 are also 88 pt at Large and 295.3 pt at AX Large. The fallback there was already `.bordered`.

| Check | iOS 26.5 light / Large | iOS 26.5 dark / AX Large | iOS 26.5 with Reduce Transparency, Increase Contrast, and Reduce Motion (light, dark) | iOS 18.1 light / Large | iOS 18.1 dark / AX Large |
| --- | --- | --- | --- | --- | --- |
| Invite taps at 87 %, 90 %, and 93 % of the row width open the share sheet without opening the pool; Close returns to My pools | 3/3 | 6/6 | 6/6 each | 6/6 | 6/6 |
| Row tap on the leading content opens pool home, with no share sheet | Pass | Pass | Pass | Pass | Pass |
| Row label in English, with the "Invite to pool" custom action | Pass | Pass | Pass | Pass | Pass |

- The first iOS 18.1 run missed one of three invite taps, which was sent while the previous share sheet was still dismissing. With a 2.5 s settle, 6/6 passed. The first iOS 18.1 dark / AX Large run tapped the row's vertical centre, where the reflowed row has the pool name, and opened the pool. That was a test-coordinate error; the rerun aimed at the invite and passed 6/6.
- **Placeholders.**
  - Live placeholders could not be captured: the list loads from cache on launch (no placeholder frames in 12 rapid screenshots), and with three pools there is no append page.
  - `PoolScoreItem` was rendered from a test, not committed, with the dummy model and with the placeholder model plus `ShimmerModifier`, at Large and AX3, light and dark, on iOS 26.5 and 18.1.
  - The placeholder uses the production layout, including the reflowed AX layout and the invite capsule, all shimmered, with no readable filler text.
  - The existing `PoolScoreItemAccessibilityTests` pass on iOS 18.1: the placeholder has an empty label and is hidden. The run covered 20 Swift Testing tests in `PoolTests`.
- Captures:
  - `verification/3.4-my-pools-ios26.5-before-after-light-large-axl.png`, left to right: before Large, after Large, before AX Large, after dark AX Large (account data)
  - `verification/3.4-pool-row-loaded-placeholder-rendered-ios18.1-ios26.5.png` (sample data)

### 3.5 Appearance and accessibility settings

Settings were set with `simctl ui` (appearance, content size, and Increase Contrast) and with `defaults write com.apple.Accessibility` (`EnhancedBackgroundContrastEnabled` for Reduce Transparency, and `ReduceMotionEnabled`), followed by an app relaunch. That these settings took effect was confirmed from the rendering. With Reduce Transparency the secondary glass became opaque white (`#FFFFFF`, from `#F5FAF5`). With Increase Contrast the accent fill darkened to `#3C8B3F` in light appearance.

| Screen | Tested on |
| --- | --- |
| Welcome | iOS 27.0: light / Large, dark / AX Large, Reduce Transparency, and Increase Contrast. iOS 18.1: light / Large, dark / AX Large |
| Email-link and email-and-password forms | iOS 27.0 and 18.1, light / Large |
| Email-link failure, sign-in-required join | iOS 27.0, light / Large |
| Join | iOS 26.5: light / Large; light and dark with Reduce Transparency, Increase Contrast, and Reduce Motion; dark with Reduce Transparency and Reduce Motion only. iOS 18.1: light / Large |
| My pools rows, pool name, username, prediction | See the tables in 3.2–3.4 |

- **Corrected here.** The invite glyph and prediction Cancel take the primary tint (see 3.3 and 3.4). Both were regressions introduced by the move from glass to bordered on iOS 26.
- **Reduce Motion.** The migration adds no animation. Under Reduce Motion, prediction edit, typing, scrolling, and Cancel reached the same final states, and the drawer was not in scope for this group. The row's existing `withAnimation(.spring)` state changes were not changed by this migration.
- **VoiceOver (accessibility tree only).**
  - Welcome exposes exactly three buttons, and join exposes exactly two, so the containers add no stop.
  - The row exposes one element with the "Invite to pool" action.
  - Button names are localized, and the disabled state is reported as `isEnabled` false.
- **Measured contrast problems that this migration did not introduce** (see Open questions):
  - White labels on the accent fill: 2.88:1 by default, and 4.24:1 with Increase Contrast in light appearance.
  - Dark appearance with Increase Contrast lightens the accent. The glass Join label measures 1.8:1, and the bordered prominent prediction Save label 1.17:1. Without Increase Contrast, Join is 2.82:1.
- Captures:
  - `verification/3.5-a11y-ios27-welcome-baseline-rt-ic.png`
  - `verification/3.5-a11y-ios26.5-light-rt-ic-rm.png` and `verification/3.5-a11y-ios26.5-dark-rt-ic-rm.png` (account data)

### Tests and builds

- `xcodebuild test -workspace Tyche.xcworkspace -scheme Tyche -destination 'platform=iOS Simulator,id=<iPhone 16 Pro iOS 18.1>' -parallel-testing-enabled NO -skip-testing:TycheUITests`, with simulator signing: `** TEST SUCCEEDED **`.
  - The run covered the AccountTests, BetTests, CoreTests, DataBetTests, PoolTests, SessionTests, TycheTests, and UITests bundles.
  - It included `UsernameEditorViewModel`, `UsernameFieldInitialization`, `UsernameDraftRules`, `ProfileViewModel`, `PoolScoreItemAccessibilityTests`, `PendingBetItemViewModelTests`, `LiquidGlassButtonSizeTests`, `StandardInputColorsTests`, and the drawer suites.
- `-only-testing:BetTests` and `-only-testing:UITests/LiquidGlassButtonSizeTests` also passed on iOS 26.5.
- The app built for the simulator at the 16.0 deployment target for every run. `Package.resolved` files were unchanged, and no throwaway test file remains.

### Simulator settings

- iOS 26.5 and iOS 18.1 are back to light / Large, with Increase Contrast off and Reduce Motion off. The Reduce Transparency key was deleted, which was its state before this pass.
- The iOS 27.0 iPhone 17 is back to dark / Large (its starting appearance), with the same accessibility settings.
- The throwaway signed-out iOS 18.1 simulator was deleted.
- Both signed-in simulators remain signed in. iOS 26.5, iOS 18.1, the iOS 27.0 iPhone, and the iPad are booted.

### Not verified

- VoiceOver speech and focus order: the simulators have no VoiceOver, so only the accessibility tree was read.
- Live username saving and failure, prediction save, pending, failure, and retry, the pool-creation submit, join loading and join failure (Retry / Go to my pools), and the email-link success screen. Each needs a write or a valid sign-in link. They are covered by the unit tests and render captures above. No failure-injection seam exists in the app, and the local proxy was not allowed.
- Paging while editing a prediction: no pool has a second page of pending predictions.
- Live My pools placeholders (initial and append): the list loads from cache, and the data has no second page. They are covered by the render test and the existing accessibility tests.
- The pool-name invalid state at iOS 26.5 dark / AX Large: the test driver's Select All step failed. It is verified at 26.5 light / Large and 18.1 dark / AX Large.
- iPad: not rerun for this group; left to task 5.2.
- iOS 16 runtime: accepted for the whole change, with iOS 18.1 standing in.

### Open questions

- **Accent contrast in prominent buttons (pre-existing).** White labels on the accent fill measure 2.88:1, below 4.5:1, for every prominent glass and bordered action. In dark appearance with Increase Contrast, the system's lightened accent gives 1.8:1 (glass) and 1.17:1 (bordered). This comes from the `AccentColor` asset, which has no high-contrast variants. Adding darker high-contrast variants, or darkening the brand accent, is a palette decision for the user.
- **Failure-bar Cancel in dark (pre-existing on iOS 18.1).** It measures 4.47:1, just under 4.5:1.
- **Pre-26 secondary glass fallback (pre-existing).** Accent text on the grey `.bordered` fill appears on the iOS 18.1 welcome and join screens. Its contrast was not measured.
- **Google provider button (pre-existing).** It is exposed to accessibility as `google_logo`.
- **`LazyPagingVStackConcatenateError` Retry.** This paging-error Retry inside lists still uses `.liquidGlassProminent`. It now gets the large size through the shared style. The surface table would call it a content-layer inline action, but no task names it.

The user chose the primary tint (option C in `verification/3.x-tint-comparison-*.png`) for the pool-row invite and prediction edit Cancel on 2026-09-28.

Accepted without join loading/failure, email-link success, live prediction save/pending/failure/retry, paging while editing, live My pools placeholders, VoiceOver speech, and the 26.5 dark / AX Large pool-name invalid state for tasks 3.1–3.5 by the user on 2026-09-28.

## iOS — tasks 3.6–3.7 (2026-09-28)

Toolchain: Xcode 26.0.1 (17A400) on macOS 27.0. Simulators:

- iPhone 17 on iOS 26.5 (`tmp-ci-repro-iPhone17-26.5`), signed in
- iPhone 16 Pro on iOS 18.1, signed in (stands in for pre-26)
- iPhone 17 on iOS 27.0 (`4D209B45…`), signed out, for the welcome screen only

Prod was used read-only. Nothing was saved or sent: no prediction Save, no Save username or keyboard return, no Join, no invitation, no gambler removal, and no Sign out. Drafts were cancelled or backed out, and join screens were left with Go to my pools.

### Code

- **Accent asset.** `AccentColor` is #2E7D32 (light) and #4CAF50 (dark), each with a high-contrast variant of the same value.
- **On-accent label.** The UI package's unused `OnPrimaryColor` is now white (light) and black (dark), with high-contrast variants of the same values.
- **`StandardProminentButtonStyle` (`.standardProminent`), new.** It is `.borderedProminent` at the inherited control size, with the label drawn in `onPrimary` while enabled. While disabled, the label sets no color, so the system's disabled label and fill are unchanged. Only the label view changes with the enabled state; the button keeps its role, action, and identity.
- **`LiquidGlassProminentButtonStyle`.** Both branches (`.glassProminent` and the `.borderedProminent` fallback) draw the same on-accent label.
- **Migrated to `.standardProminent`:**
  - email-link and email-and-password Sign in
  - pool-creation Done
  - Save username and Try again
  - prediction Save
  - the avatar crop's Use photo
  - the paging-error Retry in `LazyPagingVStackConcatenateError` (task 3.6)
- **The paging-error Retry** is no longer glass. It is back at the regular control size it had before group 3 gave the shared glass style the large size: 64.7 × 34.3 pt at Large text, below the error message as before.
- **Error-tinted actions unchanged.** The prediction failure Cancel and Retry keep `.bordered` / `.borderedProminent` with the error tint and the system label.
- **App icon.** `ASSETCATALOG_COMPILER_APPICON_NAME` is `tyche`, so the icon is the Icon Composer file `tyche.icon`. Only its background fill changed, from `srgb:0.29804,0.68627,0.31373` (#4CAF50) to `srgb:0.18039,0.49020,0.19608` (#2E7D32). The glyph layer, its amber fill, and every other property are unchanged. The legacy `AppIcon.appiconset` has no #4CAF50 background (a green glyph on transparency) and isn't the build's icon, so it wasn't changed.
- **Other #4CAF50 values in iOS:** only `TycheLogoSplash.svg`, the launch-screen logo. It was not changed, because the plan names only the Android splash (see Open questions). No hard-coded #4CAF50 exists in Swift or in the UI package's color assets.
- **README.** `iOS/UI/README.md` documents the accent values, `.standardProminent`, the on-accent label, and the paging-error Retry row.

### Tests

- **New `AccentFilledLabelTests`** (UI package, Swift Testing). Each case renders a button tinted with the accent value for its appearance, in light and dark, with normal and increased contrast. It then measures the label against the rendered fill.
  - `.standardProminent` and the `.liquidGlassProminent` bordered branch reach 5.13:1 in light and 7.56:1 in dark.
  - A disabled `.standardProminent` renders pixel-identical to a disabled `.borderedProminent`. The same holds for the pre-26 branch of `.liquidGlassProminent`.
  - Rerun with the label color removed, the dark cases failed at 2.78:1.
  - An offscreen layer render doesn't draw Liquid Glass. The glass cases are therefore skipped on iOS 26 and later, and the glass branch was measured on screen instead (below).
- **New `AccentColorTests`** (app, Swift Testing): `AccentColor` resolves to #2E7D32 and #4CAF50 with and without high contrast.
- **Unit run on iOS 18.1:** `xcodebuild test -workspace Tyche.xcworkspace -scheme Tyche -destination 'platform=iOS Simulator,id=<iPhone 16 Pro iOS 18.1>' -parallel-testing-enabled NO -skip-testing:TycheUITests`, with simulator signing: `** TEST SUCCEEDED **`. This covers every package bundle and `TycheTests`, including the drawer, input, row-accessibility, and bet suites.
- **Run on iOS 26.5:** `-only-testing:UITests/AccentFilledLabelTests -only-testing:UITests/LiquidGlassButtonSizeTests -only-testing:TycheTests/AccentColorTests` passed, with the two glass cases skipped.
- `Package.resolved` files were unchanged.

### 3.6 Paging-error Retry

- **Staging the failure.** A failed page load was staged with a local-only fault injection that was removed afterwards and never committed. `LazyPagingCursorSource` gave the first page a synthetic next key, answered that key with a `URLError`, and logged each append attempt and each Retry tap. Nothing else was written or sent.
- **Runs.** The screens were driven by a throwaway XCUITest, deleted afterwards:
  - iOS 26.5: light (all three lists), dark (My pools and leaderboard), and light and dark with Increase Contrast (My pools)
  - iOS 18.1: all three lists in light, dark, and both with Increase Contrast

| Check | My pools | Leaderboard ("Copa Mundial de la FIFA 2026", 13 rows) | Manage gamblers (owned pool, 1 row) |
| --- | --- | --- | --- |
| Label and role: a button named "Retry", below the error message, centred | Pass (26.5, 18.1) | Pass | Pass |
| Placement: after the last row. Frames (pt): 26.5 | y 632, below the Prueba row | y 748 after scrolling to the end, above the tab bar | y 383, below the owner row |
| Placement: after the last row. Frames (pt): 18.1 | y 617 | y 749 | y 367 |
| Size | 64.7 × 34.3 pt everywhere, regular control size | same | same |
| List layout: rows, dividers, and error view unchanged, with no overlap | Pass | Pass | Pass |
| One tap gives one Retry action (logged tap count) | 1 per tap | 1 per tap | 1 per tap |
| Label on fill: 26.5 | 5.13:1 light, 7.56:1 dark, 11.75:1 light with Increase Contrast, 17.23:1 dark with Increase Contrast | 5.13:1 light | 5.13:1 light |
| Label on fill: 18.1 | 5.13 / 7.56 / 11.75 / 17.23:1 | 5.13 / 7.56 / 13.90 / 17.96:1 | 5.13 / 7.56 / 11.75 / 17.23:1 |

- **Single dispatch.** Each of 22 taps logged exactly one Retry action and never more than one resulting page load.
- **Reload after Retry (pre-existing, not caused by this change).** Only 3 of those taps, all on the 13-row leaderboard, reloaded the page: 26.5 light and dark, and 18.1 light with Increase Contrast. My pools and manage gamblers never reloaded after Retry. The cause is in `lazy-paging-swift` 0.0.3. `LazyPagingItems.prependIfNeeded` sets `retryAction` to a prepend whenever a row within `prefetchDistance` (5) of the top is accessed. That replaces the append retry, so `retry()` becomes a no-op in lists shorter than about 6 rows, and sometimes on longer ones. The button passes its action through unchanged.
- **Capture:** `verification/3.6-paging-retry-mypools-leaderboard-gamblers-ios26.5-ios18.1.png` (account data). Left to right:
  1. My pools, iOS 26.5 light
  2. Leaderboard, iOS 26.5 light
  3. Manage gamblers, iOS 26.5 light
  4. My pools, iOS 26.5 dark
  5. Leaderboard, iOS 18.1 dark
  6. Manage gamblers, iOS 18.1 light

### 3.7 Accent-filled labels

Labels were measured on screenshots: the fill is the most frequent colour inside the control, and the label is the drawn colour with the highest contrast against it. "IC" means Increase Contrast.

| Control | OS | Light | Dark | Light + IC | Dark + IC |
| --- | --- | --- | --- | --- | --- |
| Welcome Email (glass prominent) | 27.0 | #2D7B31 / white 5.26 | #4FB053 / black 7.67 | #246328 / white 7.27 | #5EDB63 / black 11.79 |
| Join (glass prominent) | 26.5 | #2E7D32 / white 5.13 | #4AAE4E / black 7.46 | #246327 / white 7.27 | #5CDA61 / black 11.66 |
| Join (bordered fallback) | 18.1 | #2E7D32 / white 5.13 | #4CAF50 / black 7.56 | #256428 / white 7.16 | #5FDB64 / black 11.80 |
| Save username | 26.5 and 18.1 | #2E7D32 / white 5.13 | #4CAF50 / black 7.56 | #18401A / white 11.75 | #9CFFA0 / black 17.23 |
| Prediction Save | 26.5 and 18.1 | #2E7D32 / white 5.13 | #4CAF50 / black 7.56 | #133314 / white 13.90 | #B5FFB8 / black 17.96 |
| Prediction Cancel (primary tint, unchanged) | 26.5 and 18.1 | 13.75 | 10.37 | 13.75 | 10.37 |
| Create glyph on the toolbar glass | 26.5 | #2E7D32 on #FFFFFF 5.13 | #58BC5D on #191919 7.34 | #18401A 11.16 | #9CFFA0 15.11 |
| Create glyph (primary tint below 26, unchanged) | 18.1 | 21.00 | 21.00 | 21.00 | 21.00 |

- **Every accent-filled label reaches at least 4.5:1 in all four settings on both runtimes.** Before this change, group 3 measured 2.88:1 (light), 1.8:1 (glass Join, dark + IC), and 1.17:1 (bordered Save, dark + IC). The toolbar create glyph rose from 2.78:1 to 5.13:1 in light appearance.
- **Increase Contrast still changes the fill.** It darkens the fill in light appearance and lightens it in dark appearance, on both runtimes, even though the asset's high-contrast variants resolve to the unchanged values (`AccentColorTests`). The glass and bordered button styles apply that adjustment themselves. With the dark label in dark appearance, the adjustment now raises contrast instead of lowering it. See Open questions.
- **Enabled states and actions.** Save username and prediction Save were enabled by a typed draft and never tapped. Prediction Cancel returned the row to Edit, and Back from Username left the stored name. Join was left with Go to my pools.
- **Error pair (not changed).** The failure-bar Retry keeps the white system label on the error fill: 7.33:1 on #B00020 (light), and 3.6:1 on #CF6679 (dark), which is below 4.5:1 and pre-existing. The failure Cancel measured 5.18:1 and 4.47:1 in group 3. These can't be staged live without a failing save.
- **App icon.** The compiled `tyche60x60@2x.png` in the built app has an #2E7D32 background (rendered #2F7D31 / #2D7D31) behind the unchanged amber glyph. Capture: `verification/3.7-app-icon-compiled-60pt@2x.png`. The iOS 26 Liquid Glass home-screen rendering of the icon was not inspected.
- **Captures** (account data, except the welcome and icon captures). Each shows light, dark, light + IC, and dark + IC from left to right:
  - `verification/3.7-welcome-ios27-light-dark-ic.png`
  - `verification/3.7-join-ios26.5-light-dark-ic.png` and `verification/3.7-join-ios18.1-light-dark-ic.png`
  - `verification/3.7-prediction-ios26.5-light-dark-ic.png` and `verification/3.7-prediction-ios18.1-light-dark-ic.png`
  - `verification/3.7-username-ios26.5-light-dark-ic.png` and `verification/3.7-username-ios18.1-light-dark-ic.png`
  - `verification/3.7-create-glyph-ios26.5-light-dark-ic.png`

### Simulator settings

- **Settings changed with `simctl ui` and restored:** appearance and Increase Contrast. iOS 26.5 and 18.1 are back to light / Large with Increase Contrast off. The iOS 27.0 iPhone is back to dark / Large with Increase Contrast off. Content size was not changed.
- **App builds.** Every simulator that ran the fault-injection build was reinstalled with the clean build. Both signed-in simulators remain signed in, and the iOS 27.0 iPhone remains signed out.
- **Clean-up.** The scratch DerivedData folder was deleted.

### Not verified

- VoiceOver speech: the simulators have no VoiceOver.
- The avatar crop's Use photo on screen: it needs a photo pick. It uses the same `.standardProminent` style that was measured above.
- Email-link and email-and-password Sign in, and pool-creation Done, in dark or with Increase Contrast. They use the same style as Save username and were not reopened for this group.
- The failure-bar pair live, and the iOS 26 home-screen rendering of the app icon.
- A successful reload after Retry on My pools and manage gamblers: blocked by the `lazy-paging-swift` retry issue above.
- iPad: left to task 5.2.
- iOS 16 runtime: accepted for the whole change, with iOS 18.1 standing in.

### Open questions

- **Accent under Increase Contrast.** The spec says the iOS accent keeps #2E7D32 / #4CAF50 with Increase Contrast. The asset does, but the native glass and bordered styles still adjust the drawn fill (for example #18401A in light, and #9CFFA0 in dark). Keeping the exact fill would need custom-drawn buttons. Recommendation: accept the native adjustment, since labels measure 7.16–17.96:1 with it, and reword the requirement to the asset value plus label contrast.
- **Launch-screen logo.** `TycheLogoSplash.svg` (iOS launch screen) still uses #4CAF50. The plan moves only the Android splash logo. Should the iOS launch logo match the icon?
- **Retry no-op (pre-existing).** Paging Retry does nothing in short lists: the `lazy-paging-swift` 0.0.3 `retryAction` is overwritten by `prependIfNeeded`. This is a library fix outside this change.
- **Other pre-existing contrast issues, not accent fills:**
  - White on `PrimaryContainerColor` (#3D8F44) is 4.03:1. Examples: the owner row in manage gamblers, the pool pill on the join screen, and the selected template in pool creation.
  - The failure-bar Retry in dark is 3.6:1.

Accepted without a successful reload after Retry on My pools and manage gamblers (blocked by the pre-existing lazy-paging-swift retry bug), VoiceOver speech, and iPad for task 3.6; and without VoiceOver speech, Use photo on screen, Sign in and pool-creation Done in dark or Increase Contrast, the live failure bar, the iOS 26 home-screen icon, and iPad for task 3.7, by the user on 2026-09-28. The user accepted the system Increase Contrast fill adjustment; the requirement was reworded accordingly.

## iOS — task 3.8 (2026-09-28)

Toolchain: Xcode 26.0.1 (17A400) on macOS 27.0. Simulators:

- iPhone 17 on iOS 26.5, signed in
- iPhone 16 Pro on iOS 18.1, signed in (stands in for pre-26)
- iPhone 17 on iOS 27.0, signed out, for the launch screen and welcome screen

Prod was used read-only. The join screen was opened by its join link and left with Go to my pools. Pool creation was left with Back after a template was selected, and nothing was created. Manage gamblers was only viewed.

### Code

- **Primary container.** `PrimaryContainerColor` (UI package) moved from #3D8F44 to #1B5E20 in both appearances.
- **Content color.** The existing `OnPrimaryContainterColor` was already white in both appearances, and every primary-container surface already uses it, so it was not changed.
- **Launch-screen logo.** `TycheLogoSplash.svg` had one `fill="#4CAF50"` path, now #2E7D32. The #FFC107 path is unchanged.
- **Other launch-screen colors.** The rest of `LaunchScreen.storyboard` uses only `SplashBackground` (white / black), `SplashForeground` (black / white), and the `FelipearpaBrand` image tinted with `SplashForeground`. None of them uses the old green.
- **Every consumer of `primaryContainer`:**
  - manage-gamblers owner row
  - join-screen pool pill
  - selected pool-creation template
  - welcome-screen gradient top (see Open questions)
  - `PostionIndicator` with `shouldUsePrimeryColor: true`, which only its previews use

### Tests

- **New `PrimaryContainerColorTests`** (UI package, Swift Testing), in light and dark, with normal and increased contrast. The container resolves to #1B5E20 and the content color to white. White on the container measures at least 4.5:1, and so does the 70 % white used by the owner row's email line.
- **Unit run on iOS 18.1:** `xcodebuild test -workspace Tyche.xcworkspace -scheme Tyche -destination 'platform=iOS Simulator,id=<iPhone 16 Pro iOS 18.1>' -parallel-testing-enabled NO -skip-testing:TycheUITests`, with simulator signing: `** TEST SUCCEEDED **`.
- `Package.resolved` files were unchanged.

### Surfaces

Measured on screenshots taken with a throwaway XCUITest, deleted afterwards. The selected template was measured from a throwaway render test of the production `PoolFromLayoutCreatorItem`, also deleted afterwards.

| Surface | iOS 26.5 light | iOS 26.5 dark | iOS 18.1 light | iOS 18.1 dark |
| --- | --- | --- | --- | --- |
| Join-screen pool pill ("Prueba"), white text | #1B5E20 / 7.87:1 | 7.87:1 | 7.87:1 | 7.87:1 |
| Owner row, username (white) | 7.87:1 | 7.87:1 | 7.87:1 | 7.87:1 |
| Owner row, email (70 % white, #BBCFBD) | 4.79:1 | 4.79:1 | 4.79:1 | 4.79:1 |
| Selected template, title and date (rendered production item) | 7.87:1 | 7.87:1 | 7.87:1 | 7.87:1 |

- **Before.** White on #3D8F44 was 4.03:1. The owner email line was about 2.9:1.
- **Live check of the selected template.** Selecting a template pushes step two immediately, and Back from step two returns to My pools, so the selected card is only visible during the push. It was captured mid-transition on iOS 26.5 (`verification/3.8-selected-template-live-transition-ios26.5.png`). That frame is faded, so it was not used for measurement. The render test gives the steady-state card.

Captures (account data except the rendered template):

- `verification/3.8-join-pill-ios26.5-ios18.1-light-dark.png`
- `verification/3.8-owner-row-ios26.5-ios18.1-light-dark.png`
- `verification/3.8-selected-template-rendered-ios26.5-ios18.1-light-dark.png`
- `verification/3.8-selected-template-live-transition-ios26.5.png`

### Launch screen

- **The system caches launch-screen snapshots.** After the new build was installed, iOS 26.5 and 18.1 still showed the old #4CAF50 logo, even after the app's `Library/SplashBoard/Snapshots` cache was cleared. Rebooting those two simulators and clearing that cache again showed the new logo. The app stayed signed in, and the cache is regenerated by the system.
- **iOS 27.0.** Screenshots of the launch screen show exactly #2E7D32 with #FFC107, on white (light) and on black (dark).
- **iOS 26.5 and 18.1.** These apps launch too fast for a full-opacity screenshot, so each launch was recorded as a video and the logo frame was taken from it. H.264 shifts colors, so the green reads about #2A7C2F against #FDC005 for #FFC107. That is consistent with #2E7D32 and not with #4CAF50. In a dark iOS 18.1 frame the logo was mid-fade; scaling it back gives #2D7E32.
- **Capture:** `verification/3.8-launch-screen-ios27-ios26.5-ios18.1-light-dark.png`. Left to right: iOS 27 light, iOS 27 dark, iOS 26.5 light, iOS 26.5 dark, iOS 18.1 light, iOS 18.1 dark. The iOS 26.5 and 18.1 frames are video frames that include the system's launch zoom.

### Side effect: welcome-screen gradient

`HomeView` fades from `primaryContainer` at the top to `surface` at the bottom, so its top is now darker. Weakest contrast against the gradient behind each element, measured on iOS 27.0 before and after:

| Element | Light before | Light after | Dark before | Dark after |
| --- | --- | --- | --- | --- |
| Fortuna logo and wordmark (black in light, white in dark) | 6.07:1 | 3.40:1 | 4.66:1 | 8.63:1 |
| Tagline text | 9.25:1 | 6.38:1 | 7.48:1 | 11.39:1 |
| "Continue with:" | 14.56:1 | 12.55:1 | 12.81:1 | 15.30:1 |

- In light appearance, the system status-bar text (black) over the new gradient top (about #1B5E20) is about 2.7:1.
- `HomeView` was not changed.
- Capture: `verification/3.8-welcome-ios27-light-before-after.png`.

### Simulator settings

- **Appearance** was changed with `simctl ui` and restored: iOS 26.5 and 18.1 are back to light / Large, and the iOS 27.0 iPhone is back to dark / Large. Increase Contrast stayed off throughout.
- **Reboots.** The iOS 26.5 and 18.1 simulators were rebooted, and both remain signed in.
- **Clean-up.** The scratch DerivedData folder was deleted, and no throwaway test files remain.

### Not verified

- VoiceOver speech: the simulators have no VoiceOver. No accessibility semantics changed.
- A steady-state live capture of the selected template: the flow pushes step two on selection. It is covered by the render of the production item.
- An exact-color still of the launch screen on iOS 26.5 and 18.1: the launch is too fast. It is covered by video frames and by exact stills on iOS 27.0.
- The welcome screen on iOS 26.5 and 18.1: both simulators are signed in, and the welcome screen needs a signed-out install.
- iPad: left to task 5.2.

### Open questions

- **Welcome screen in light appearance.** The black Fortuna logo and wordmark now measure 3.40:1 at the gradient top, down from 6.07:1, and the black status-bar text is about 2.7:1. Options:
  - (a) accept, because the logo is a brand mark;
  - (b) start the welcome gradient from a lighter tint instead of `primaryContainer`;
  - (c) use the light (white) logo and a light status bar on the welcome screen.

  Recommendation: (b). It keeps the brand look without inverting the logo.

### Follow-up: white logo and light status bar on the welcome screen (2026-09-28)

The user chose option (c): keep the gradient starting from the primary container (#1B5E20), and use the white logo and wordmark and light status-bar content in both appearances. This supersedes the open question above.

- **Code** (`HomeView`):
  - The header (the template `TycheLogo` and `TycheTitle` images) takes `.foregroundStyle(.white)`.
  - The view sets `.toolbarColorScheme(.dark, for: .navigationBar)`, so the status bar uses light content while the welcome screen is the root of its navigation stack.
  - The gradient, the tagline, "Continue with:", and the buttons are unchanged.
- **Discarded attempt.** An intermediate lighter-gradient attempt (option (b), a new `WelcomeGradientStart` colour asset) was reverted before any verification. No trace of it remains.
- **Installs.** Both were signed out: the iOS 27.0 iPhone, and a throwaway iPhone 16 Pro on iOS 18.1 created for this check and deleted afterwards. The signed-in simulators were not touched.

Weakest contrast of each element against the gradient behind it:

| Element | iOS 27 light: pre-3.8 | Light: after 3.8, before this fix | Light: after | Dark: before | Dark: after | iOS 18.1 light / dark: after |
| --- | --- | --- | --- | --- | --- | --- |
| Status bar | black 5.30:1 | white 6.80:1 (iOS 27 adapts the status bar by itself) | white 6.80:1 | white 7.96:1 | white 7.96:1 | white 6.80:1 / white 7.96:1 |
| Logo and wordmark | black 6.05:1 | black 3.35:1 | white 5.19:1 | white 8.62:1 | white 8.62:1 | white 5.19:1 / 8.62:1 |
| Tagline (label colour) | black 9.14:1 | black 6.29:1 | black 6.29:1 | white 11.35:1 | white 11.35:1 | 6.29:1 / 11.35:1 |
| "Continue with:" | black 14.56:1 | 12.55:1 | 12.55:1 | 15.30:1 | 15.30:1 | 12.55:1 / 15.30:1 |
| Email (accent-filled) | — | — | white 5.34:1 | — | black 7.66:1 | white 5.13:1 / black 7.56:1 |
| Email and password (secondary) | — | — | black on light glass 19.58:1 | — | white on dark glass 12.02:1 | accent on grey `.bordered` fill: 3.84:1 / 4.32:1 |

- **Gradient.** Every text over the gradient reaches at least 4.5:1, and the logo reaches at least 3:1, in both appearances and on both runtimes. The gradient's lower part doesn't weaken any black or white text.
- **Status bar on iOS 18.1.** The light content is set by `toolbarColorScheme`. No before-capture exists on 18.1: the earlier welcome checks there used a different throwaway simulator.
- **Dark appearance.** It is unchanged by this fix: the logo was already white, and still measures 8.62:1.
- **Below 4.5:1: the secondary action on iOS 18.1 (pre-existing, not caused by this fix).** Below iOS 26 the `.liquidGlass` fallback is `.bordered`, which draws accent-coloured text on a grey fill. On the welcome screen that measures 3.84:1 in light and 4.32:1 in dark. With the old #4CAF50 accent in light appearance it was lower. The same fallback appears on the join screen's Go to my pools.
  - Suggested fix: give `LiquidGlassButtonStyle`'s pre-26 `.bordered` branch `.tint(.primary)`, as the row invite and prediction Cancel already use. That draws a black or white label, at about 13.75:1 or 10.37:1 on the plain bordered fill.
  - Not changed here.
- **Captures:**
  - `verification/3.8-welcome-white-logo-ios27-before-after-light-dark.png`, left to right:
    1. Light, pre-3.8
    2. Light, after 3.8 and before this fix
    3. Light, after this fix
    4. Dark, pre-3.8
    5. Dark, before this fix
    6. Dark, after this fix
  - `verification/3.8-welcome-white-logo-ios18.1-light-dark.png`
- **Tests.** No unit test covers this view-level colour change. The build with the change succeeded (`xcodebuild build -workspace Tyche.xcworkspace -scheme Tyche -destination 'generic/platform=iOS Simulator'`).
- **Settings.** The iOS 27.0 iPhone is back to dark / Large, and the throwaway iOS 18.1 simulator was deleted. The signed-in iOS 26.5 and 18.1 simulators still have the build from before this fix installed; it differs only on the signed-out welcome screen. The scratch DerivedData folder was deleted.
- **Not verified:**
  - VoiceOver: no accessibility semantics changed.
  - iOS 26.5 signed out: no signed-out 26.5 install was made. iOS 27.0 covers the glass branch.
  - iPad: left to task 5.2.

The user chose option (c) for the welcome screen (white logo, wordmark, and status bar over the primary container) on 2026-09-28.

Accepted without VoiceOver speech, a steady-state live capture of the selected pool-creation template, an exact-colour launch-screen still on iOS 26.5 and 18.1, the welcome screen signed out on iOS 26.5, and iPad for task 3.8 by the user on 2026-09-28.

### Follow-up: primary tint on the pre-26 secondary fallback (2026-09-28)

- **Code.** Below iOS 26, `LiquidGlassButtonStyle` (`.liquidGlass`) falls back to `.bordered`, and that branch now applies `.tint(.primary)`. The iOS 26+ `.glass` branch is unchanged.
- **Callers.** `.liquidGlass` is used by the welcome Email and password action, the join screen's two Go to my pools actions, and the `LiquidGlassContainer` preview. None of them sets a tint of its own, and no ancestor sets one. The tint is applied to the inner button inside the style, so a caller's `.tint(...)` placed after `.buttonStyle(.liquidGlass)` would not reach the fallback. The style's doc comment and `iOS/UI/README.md` say so.
- **Test.** New `AccentFilledLabelTests.liquidGlassSecondaryFallbackLabelIsLegible` covers light and dark, with and without high contrast, on pre-26 only. It renders `.liquidGlass` under the app accent tint and requires the label to reach at least 4.5:1 on its fill.
  - With the new tint removed, it failed in light at 3.98:1.
  - UI package tests on iOS 18.1 (`-only-testing:UITests`): `** TEST SUCCEEDED **`, 70 tests in 12 suites.

Live on iOS 18.1:

| Screen (iOS 18.1) | Light: secondary label | Dark: secondary label | Primary label (light / dark) |
| --- | --- | --- | --- |
| Welcome, signed out (throwaway simulator, deleted afterwards): Email and password | black on #C2C6C2, 12.15:1 (was 3.84:1) | white on #4F534F, 7.83:1 (was 4.32:1) | Email: 5.13:1 / 7.56:1 |
| Join (signed-in simulator, join link, only Go to my pools tapped): Go to my pools | black on #D1D1D1, 13.75:1 | white on #404040, 10.37:1 | Join: 5.13:1 / 7.56:1 |

- **Emphasis.** The primary action keeps its accent fill. The secondary action is a neutral gray capsule with a black or white label, so the prominent action is still the only accent-coloured control in each pair.
- **Go to my pools** returned to My pools in both runs. Nothing was joined.
- **Capture:** `verification/3.8-secondary-fallback-primary-tint-ios18.1-welcome-join-light-dark.png`, left to right: welcome light, welcome dark, join light, join dark. The join frames show account data (the pool name).
- **Settings.** The signed-in iOS 18.1 simulator is back to light / Large and still signed in, with this build installed. The throwaway simulator and the scratch DerivedData folder were deleted, and the throwaway UI test was removed.
- **Not verified:**
  - VoiceOver speech: no semantics changed.
  - iOS 26+: that branch is unchanged, so it wasn't rechecked.
  - The email-link and sign-in-required screens: they use only `.liquidGlassProminent`, so they are unaffected.

## Android — tasks 4.1–4.3 (2026-09-28)

Devices:

- **Phone:** Samsung SM-G955F, Android 9 (API 28), light appearance only (no system dark toggle was used). `com.felipearpa.fortuna` 2.0.0 was a debug, `TEST_ONLY` Prod build (no cleartext flag, the same debug signing certificate as the new build). The new `:app:assembleProdDebug` APK was installed over it with `adb install -r -t`. `firstInstallTime` stayed 2026-05-02, and the app stayed signed in.
- **Emulator:** `emulator-5554`, Pixel_10_Pro_XL, API 36, signed in. Same install method (Prod debug over Prod debug, same certificate). Used for dark appearance through `cmd uimode night yes`, restored to `no`.

Prod was used read-only: the create-pool flow was opened once and left with its Back arrow, the username editor was opened and left without typing, and manage gamblers was only viewed. Nothing was created, saved, sent, or joined, and nobody was signed out.

### Code

- **Glyph.** New `Android/ui/src/main/res/drawable/add.xml`, derived from `assets/icons/add.svg`: the SVG path data is copied unchanged inside a 960 × 960 viewport with a `translateY="960"` group standing in for the `0 -960 960 960` viewBox. `PoolScoreListView`'s create action uses it. The `IconButton`, `primary` tint, `Modifier.size(48.dp)`, top-bar placement, and `onPoolCreate` routing are unchanged. The old `filled_add.xml` had no other consumer and was removed.
- **Theme** (`Color.kt`): light `primary` #2E7D32 (white `onPrimary`), dark `primary` #4CAF50 with `onPrimary` #000000, and `primaryContainer` #1B5E20 with white `onPrimaryContainer` in both schemes. The light and dark color schemes in `Theme.kt` became `internal` so tests can read them.
- **Launcher icon.** The adaptive icon's background `ic_launcher_background.xml` moved from #4CAF50 to #2E7D32. The amber foreground and white monochrome layers are unchanged. The app has no raster mipmaps (minSdk 28, `mipmap-anydpi-v26` only), and there is no other launcher or Play-store icon resource in the Android project.
- **Splash logo.** The green path in `ic_tyche_logo_splash.xml` moved from #4CAF50 to #2E7D32; the #FFC107 path is unchanged. The splash theme's other colours (`splash_background`, `splash_foreground`) contain no green.
- **Unchanged:** `people_playing_body` (#4CAF50) in `pool/src/main/res/values*/colors.xml`, a pool illustration. No other #4CAF50 or #3D8F44 remains in Android Kotlin or XML.
- **`assets/icons/README.md`** has the Android row in the platform mapping.

### Tests and builds

- New `PrimaryColorTest` (ui, JUnit 5 + Kotest): the light and dark schemes hold the values above, and `onPrimary`/`primary` and `onPrimaryContainer`/`primaryContainer` reach at least 4.5:1 in both. With the previous `Color.kt`, both cases failed. The contrast helper moved from `CurrentUserColorTest` into a shared test file.
- `./gradlew :ui:testDebugUnitTest :pool:testDebugUnitTest :app:testProdDebugUnitTest :app:assembleProdDebug`: BUILD SUCCESSFUL. All suites passed (ui: `PrimaryColorTest` 2, `CurrentUserColorTest` 2 and the others; pool 2; app 60).
- The compiled APK's `res/drawable/add.xml` holds the canonical path string, and the launcher background and splash green compile to `#ff2e7d32`.
- No instrumented tests were run.

### 4.1 Canonical glyph geometry

- The path string in `add.xml` equals the `d` attribute of `assets/icons/add.svg`, and the iOS `add.imageset/add.svg` is byte-identical to it (compared by script; SHA-256 `e9532de2…c376`).
- **Rendered size.** The M3 `IconButton` constrains the 48 dp `Icon` to its 40 dp container, both before and after. Measured:

| Device | Old filled circle | New plus | Bar thickness | Expected from the SVG (560 and 80 of 960, in 40 dp) |
| --- | --- | --- | --- | --- |
| SM-G955F, API 28, light (2.625 px/dp) | 87 px diameter | 61 × 61 px | 9 px | 61.25 px, 8.75 px |
| Emulator, API 36, dark (3 px/dp) | — | 70 × 70 px | 10 px | 70 px, 10 px |

- The glyph box, the button bounds ([944,85][1070,211] on the phone), and the centre are unchanged. The tint is `primary`: #2E7D32 in light and #4CAF50 in dark on #121212 (6.74:1), with no fill behind it.

### 4.2 Create action, pool rows, and drawer

- **One tap, one destination.** On the phone, a single tap opened "Polla desde plantilla" (the template list), and one tap on its Back arrow returned to My pools, so only one destination was pushed.
- **Pool rows.** The My pools list below the top bar and the top bar's left half (avatar and title) are pixel-identical before and after the install on the phone (image diff of the full-resolution captures). The only change on the screen is the create glyph.
- **Drawer.** The open drawer on My pools is pixel-identical before and after (diff below the status bar), and so is the list after closing it.
- **Captures:**
  - `verification/4.1-create-glyph-android-sm-g955f-before-after-light-emulator-dark.png`: phone light before, phone light after, emulator dark after.
  - `verification/4.2-mypools-drawer-create-sm-g955f-light-before-after.png` (account data): My pools before and after, drawer before and after, and the template list opened by the create action.

### 4.3 Accent colours, launcher icon, and splash

Colours were read from screenshots as the most frequent colour in the element.

| Surface | Light (SM-G955F, API 28) | Dark (emulator, API 36) |
| --- | --- | --- |
| Profile camera badge (`primary` fill, `onPrimary` icon) | white on #2E7D32, 5.13:1 (was 2.78:1) | black on #4CAF50, 7.56:1 (was white, 2.78:1) |
| "Change photo" text button (`primary` text) | #2E7D32 on white, 5.13:1 (was 2.78:1) | #4CAF50 on #121212, 6.74:1, unchanged |
| Pool-home selected tab label and indicator | #2E7D32 | #4CAF50, unchanged |
| Username field focused border | #2E7D32 | not opened |
| Manage gamblers owner row (`primaryContainer`) | not opened | #1B5E20: white username 7.87:1, 70 % white email (#BBCFBD) 4.79:1 (was 4.03:1 and about 2.9:1) |

- **Filled buttons.** `Button` uses `primary` and `onPrimary`, so it takes the pairs measured by `PrimaryColorTest` and the camera badge. No enabled filled button was reachable read-only: the username Save stays disabled without a draft, and prediction Save needs a pending bet in edit mode.
- **Accent text on dark surfaces.** Dark `primary` is unchanged (#4CAF50), so text buttons, tab labels, and tints on dark surfaces read as before. In light appearance they get darker and gain contrast on white.
- **Launcher icon** (phone app drawer, Samsung One UI on API 28): #2E7D32 background behind the unchanged #FFC107 ball, read exactly from the screenshot.
- **Splash.** Recorded as video, because the splash is shown for about 4 seconds. Light on the phone: the green reads #2A7C2F with #FDC003 for #FFC107, on white. Dark on the emulator: #2A7C2F with #FBBE04, on #101010. The shift matches the codec shift seen on iOS; the compiled drawable holds #2E7D32.
- **Captures:**
  - `verification/4.3-accent-sm-g955f-light-emulator-dark.png` (account data), left to right: pool home light, Profile light, username editor light, pool home dark, Profile dark, manage gamblers dark.
  - `verification/4.3-splash-light-dark-launcher-icon.png`: splash light (phone), splash dark (emulator), launcher icon (phone, enlarged).

### Device settings

- Emulator night mode was switched to dark and restored to light. The phone's settings were not changed.
- Both devices keep the new Prod debug build installed and stay signed in. No temporary files were left on either device.

### Not verified

- **Dark appearance on the phone:** API 28 has no system dark mode, so dark was checked on the API 36 emulator.
- **An enabled filled `Button` on screen** (for example Sign in, Save username, prediction Save, Join) in either appearance. Enabling one needed typing a username draft on Prod, which was not done. The label colours are covered by `PrimaryColorTest` and the camera badge, which uses the same pair.
- **The welcome screen, email sign-in, and join screen:** both devices are signed in.
- **The launcher icon on API 36 and themed (monochrome) icons:** the monochrome layer did not change.
- **TalkBack:** no semantics changed.
- **Pool-row placeholders compared pixel by pixel:** after the install they were seen only in the dark launch recording on the emulator, with the production rows and shimmer. Before the install they were captured on the phone in light. They use no primary colour.

### Open questions

- **Android welcome screen in light appearance.** `HomeView` fades from `primaryContainer` to `surface`, so its top is now #1B5E20. The header logo and wordmark use the default content colour (#212121 in light), which measures about 2.05:1 on #1B5E20 (4.00:1 on the old #3D8F44), and the status bar uses dark icons in light. In dark, the #E0E0E0 logo improves from 3.05:1 to 5.96:1. This is the Android counterpart of the iOS 3.8 welcome issue, where option (c) (white logo and light status bar) was chosen. It was not changed here; it wasn't measured on a device because both are signed in.
- **Create action's accessible name (pre-existing).** The Android create `Icon` has `contentDescription = "Localized description"`, so TalkBack reads that placeholder. iOS got a localized create label in 2.2. It was left unchanged, as the task keeps the control as it is.
- **`surfaceTint`.** `lightColorScheme()` defaults `surfaceTint` to `primary`, so tonal-elevation overlays on elevated surfaces in light appearance (dialogs, menus, sheets) now tint slightly darker green. None were opened.

### Follow-up: welcome header and create-action name (2026-09-28)

The user chose to match iOS on the Android welcome screen (white logo and wordmark, light status-bar icons over the #1B5E20 gradient start), and to give the create action a localized name.

#### Code

- **`HomeView`:** the `ic_tyche_logo` and `tyche_title` icons are tinted white in both appearances. While the welcome screen is shown, a `DisposableEffect` sets light status-bar icons (`isAppearanceLightStatusBars = false`) and restores the previous value when the screen leaves; this follows `AvatarCropView`'s existing pattern. The gradient, tagline, "Continue with:", and buttons are unchanged.
- **Create action:** `contentDescription` is `stringResource(R.string.create_pool_action)` in the pool module, replacing the "Localized description" placeholder. Android ships `values`, `values-es` (polla), and `values-es-rES` (quiniela), so the strings are "Create pool", "Crear polla", and "Crear quiniela", matching the iOS `create_pool_action` (en, es, es-ES).

#### Welcome screen (Pixel_7_API_36_signed_out, API 36, en-US, never signed in)

The Prod debug build was installed fresh (the AVD had no Fortuna install). `emulator-5554` (Pixel_10_Pro_XL) was shut down with `emu kill` first, so only one emulator ran at a time. Each value is the weakest ratio of the element's colour against the gradient row behind it (sampled at the screen's left edge, where the gradient is uniform across the row).

| Element | Light | Dark |
| --- | --- | --- |
| Status-bar icons (window appearance has no `LIGHT_STATUS_BARS`) | white, 7.18:1 | white, 7.98:1 |
| Logo | white, 5.35:1 (was #212121: about 2.05:1 at the gradient top) | white, 8.53:1 (was #E0E0E0: 6.46:1 on the same row) |
| Wordmark | white, 5.60:1 | white, 8.64:1 |
| Tagline (unchanged) | #212121, 5.30:1 | #E0E0E0, 8.90:1 |
| "Continue with:" (unchanged) | #212121, 10.72:1 | #E0E0E0, 12.16:1 |
| Email (filled) | white on #2E7D32, 5.13:1 | black on #4CAF50, 7.56:1 |
| Email and password (outlined, unchanged) | #1B1B1B, 15.06:1 | #EAEAEA, 14.83:1 |

- Every text reaches at least 4.5:1 and the logo at least 3:1 in both appearances. Dark appearance improved (logo 8.53:1, above the previous 5.96:1 floor). Nothing weakened.
- Capture: `verification/4.3-welcome-white-header-pixel7-api36-signed-out-light-dark.png` (light, dark).
- The AVD was returned to light (`cmd uimode night no`) and shut down. It keeps the signed-out Fortuna install; nobody signed in.

#### Create action's accessible name

`uiautomator dump`: the Compose `IconButton` node ([944,85][1070,211] on the phone) holds the icon child carrying the description, which the button merges for TalkBack.

- **es-CO:** SM-G955F, API 28 (system locale es-CO, not changed): "Crear polla". The build was installed over the Prod debug app with `adb install -r -t`; `firstInstallTime` stayed 2026-05-02 and the app stayed signed in.
- **en-US:** Pixel_10_Pro_XL, API 36, signed in: "Create pool".
- **es-ES:** the same emulator with a per-app locale (`cmd locale set-app-locales com.felipearpa.fortuna --locales es-ES`): "Crear quiniela", with the title "Mis quinielas". The app locale was reset to the system default (`get-app-locales` returns `[]`).

#### Tests and builds

`./gradlew :ui:testDebugUnitTest :pool:testDebugUnitTest :app:testProdDebugUnitTest :app:assembleProdDebug`: BUILD SUCCESSFUL; ui 44, pool 2, app 60 tests, no failures.

#### Not verified

- **Status-bar restore after leaving the welcome screen:** verified later; see the next subsection.
- **TalkBack speech:** checked only through the accessibility tree.
- **The welcome screen on API 28:** the phone is signed in and was left alone for this step.

#### Follow-up: status-bar restore (2026-09-28)

Device: Pixel_7_API_36_signed_out (API 36, en-US), never signed in, with the same build as the follow-up above. `emulator-5554` (Pixel_10_Pro_XL) was shut down with `emu kill` first. Nothing was typed or submitted on the sign-in screens.

Each step was read two ways: the window's `mLastAppearance` from `dumpsys window`, and the status-bar clock colour against the bar's background in a screenshot. "Light icons" means no `LIGHT_STATUS_BARS` flag and a white clock; "dark icons" means `LIGHT_STATUS_BARS` and a grey clock (#666666) on white.

| Step | Light appearance | Dark appearance |
| --- | --- | --- |
| Welcome | light icons (`LIGHT_NAVIGATION_BARS` only), white on #1F6124 | light icons (no flags), white on #1B5D20 |
| Email (email-link sign-in) | dark icons (`LIGHT_STATUS_BARS LIGHT_NAVIGATION_BARS`) on #FFFFFF | light icons (no flags) on #121212, the normal dark appearance |
| Email, rotated to landscape and back | dark icons in both orientations | light icons |
| Email, after Home and resume | dark icons | light icons |
| Back to welcome (toolbar Back arrow) | light icons | light icons |
| Email and password | dark icons | light icons |
| Back to welcome (system Back) | light icons | light icons |
| Email again | dark icons | not repeated |

Also checked in light appearance:

- Rotating on the welcome screen (landscape and back) keeps light icons, and Email opened afterwards has dark icons.
- Switching from dark to light while on the welcome screen keeps light icons there. Email opened afterwards has dark icons, and Back to welcome gives light icons again.

The restore is correct in every case, so `HomeView.kt` was not changed again.

- Capture: `verification/4.3-welcome-status-bar-restore-pixel7-api36-light-dark.png`, the top of each screen, left to right: light welcome, light Email, light Email and password, dark welcome, dark Email, dark Email and password.
- **Settings.** Rotation was driven with `accelerometer_rotation 0` / `user_rotation`, then restored to auto-rotate on (`accelerometer_rotation 1`, `user_rotation 0`). Night mode was returned to `no`. The signed-out AVD was shut down in light mode and is still not signed in.
- **Emulators.** Pixel_10_Pro_XL is running again as `emulator-5554`, signed in (My pools shown) and in light mode.
- **Not verified:** the same sequence on API 28 (the phone was left alone).

The user chose the white welcome header on Android (matching iOS) and a localized "Create pool" TalkBack label on 2026-09-28.

Accepted without dark appearance on the phone (checked on the API 36 emulator), a placeholder pixel comparison, an enabled filled button on screen, the signed-out screens and status-bar restore on API 28, the API 36 launcher and themed icons, and TalkBack speech for tasks 4.2 and 4.3 by the user on 2026-09-28.

## iOS — task 2.5 (2026-09-28)

Toolchain: Xcode 26.0.1 (17A400) on macOS 27.0. Simulators, all signed in to Prod:

- iPhone 17 on iOS 26.5 (`tmp-ci-repro-iPhone17-26.5`)
- iPhone 16 Pro on iOS 18.1 (stands in for pre-26)
- iPad Air 11-inch (M4) on iOS 27.0, full screen, portrait

Prod was used read-only: the creator was opened and left with Back each time, and the drawer was opened with the avatar and closed with Close menu. Nothing was created, and nobody was signed out. The runtime checks used a throwaway XCUITest, which was deleted afterwards.

This record reflects the user's revision: a custom glass circle with a full hit area on iOS 26 and later, replacing the first implementation's native prominent toolbar item.

### Code

- **`ToolbarProminentButtonStyle` (`.toolbarProminent`), new in the UI package.** The OS check stays in the package.
  - **iOS 26 and later.** A `.plain` button whose label is framed at 44 × 44 pt with a circular content shape. The style draws `.glassEffect(.regular.tint(Color.accentColor).interactive(), in: Circle())` itself. Glass takes a concrete tint color, so this branch uses the app accent directly.
  - **Below 26.** A 44 pt circle filled with the inherited tint (the app accent). It dims while pressed and has a gray fill while disabled.
  - **Label.** Both branches draw it in `onPrimary`, like `.liquidGlassProminent`.
- **`PoolScoreListRouter`.**
  - The create action moved to a `PlainToolbarItem(placement: .topBarTrailing)`, which hides the shared toolbar background on iOS 26 as it does for the avatar. The route is unchanged.
  - Its label is the canonical `add` image, resizable at `ToolbarProminentButtonStyle.glyphSize` (28 pt).
  - The button's accessibility label is `create_pool_action`.
- **Why the label is the image.** In a toolbar, iOS 26 reduces a `Label` to its icon at the icon's intrinsic 24 pt size, in a system label colour. That dropped both the enlarged glyph and the on-accent colour.
- **Why not the native prominent item.** It was tried first. The toolbar draws its glass at a fixed 44 pt circle but takes taps only on a 32 × 36 pt content area, about ±16 × ±18 pt from the centre. The user chose the custom circle for a full hit area.
- **Removed:** `ToolbarActionTint.swift` (`toolbarActionTint()`). Nothing else used it.
- **README.** `iOS/UI/README.md` has a surface-table row for the accent-filled toolbar action (`PlainToolbarItem` with `.toolbarProminent`, the one app-drawn glass exception in a bar) and a `.toolbarProminent` entry under Accent-filled actions. The mention of `toolbarActionTint()` is gone.
- **New test.** `AccentFilledLabelTests.toolbarProminentFallbackGlyphIsLegibleOnTheAccent` renders the add glyph with `.toolbarProminent` under the accent tint, in light and dark, with normal and high contrast. It measures the glyph against the fill and requires 4.5:1. It runs below iOS 26 only, because an offscreen render doesn't draw Liquid Glass.

### Tests

- `xcodebuild test -workspace Tyche.xcworkspace -scheme Tyche -destination 'platform=iOS Simulator,id=<iPhone 16 Pro iOS 18.1>' -parallel-testing-enabled NO -only-testing:UITests/AccentFilledLabelTests`: 6 tests passed, including the 4 new cases.
- The same command on iOS 26.5 passed, with the new test and the other pre-26 cases skipped.
- The app built for iOS 26.5, 18.1, and the iPad through `Tyche.xcworkspace`, with the package deployment target still iOS 16.
- `Package.resolved` files were unchanged.

### Glyph contrast

The glyph and fill were measured on screenshots: the fill is the most frequent colour inside the circle, and the glyph is the most frequent colour with more than 2:1 against it. "IC" means Increase Contrast.

| Device | Light | Dark | Light + IC | Dark + IC |
| --- | --- | --- | --- | --- |
| iPhone, iOS 26.5 (custom glass) | #2E7D32 / white 5.13 | #4AAE4E / black 7.46 | #173F19 / white 11.92 | #97FF9B / black 17.09 |
| iPhone, iOS 18.1 (filled circle) | #2E7D32 / white 5.13 | #4CAF50 / black 7.56 | #18401A / white 11.75 | #9CFFA0 / black 17.23 |
| iPad, iOS 27.0 (custom glass) | #2D7C31 / white 5.20 | #4BAF4F / black 7.54 | #246328 / white 7.27 | #5DDB62 / black 11.77 |

- **Sizes.** The circle measures 44 pt on every device. The plus measures 50 px at 3× (16.7 pt), up from 14 pt before, which matches the 28 pt glyph (560 of 960 units).
- **Increase Contrast** darkens the fill in light appearance and lightens it in dark appearance, and the glyph contrast rises. The tinted glass adapts on its own; the fallback gets the same adjustment from the system accent.
- **While pressed** (below), the iOS 26 glass lightens to about #7CB87F in light and #8CD78F in dark. The glyph measures about 2.3:1 and 3.2:1 during the press. This is the system's interactive-glass highlight, and the glyph returns to the values above on release.

### Hit area

The scan tapped from the circle's centre along 8 directions in 1 pt steps from 16 to 24 pt. The table shows the last distance that opened the creator; a single Back then returned to My pools with the creator gone.

| Device | Circle frame (pt) | +x | −x | +y | −y (up) | Diagonals |
| --- | --- | --- | --- | --- | --- | --- |
| iOS 26.5 | 338, 62, 44 × 44 | 21 | 22 | 21 | 21 | 24 (the scan's limit) |
| iOS 18.1 | 342, 56.3, 44 × 44 | 21 | 22 | 22 | 21 | 24 |
| iPad 27.0 | 766, 32, 44 × 44 | 21 | 22 | 21 | 22 | 24 |

- **The whole circle takes taps, upward too, on every device.** The diagonal taps at 24 pt (17 pt on each axis) are outside the circle but inside its 44 pt square, so the hit area is the square.
- **Upward limit in the earlier trial.** The single trial run of this approach stopped at 15 pt upward. In three later scans (two on iOS 26.5, one on iPad) the same code reached 21–22 pt. The earlier miss is taken to be a one-off tap while the list was settling, not a hit-area limit.

### Interactive glass

- **iOS 26.5 and iPad.** A 3-second press on the circle was captured mid-press, and the press was then slid off so it didn't activate.
  - The glass grew from 44 pt to about 60 pt and lightened, which is the system's interactive-glass response.
  - It returned to rest on release, and the slide-off didn't open the creator.
  - Capture: `verification/2.5-interactive-glass-press-ios26.5-ipad27-light.png`.
- **iOS 18.1.** The same press capture showed the fallback circle unchanged (44 pt, #2E7D32), so the style's pressed dimming was not observed there. See Not verified.

### Route, name, and layout

| Check | iOS 26.5 | iOS 18.1 | iPad 27.0 |
| --- | --- | --- | --- |
| Exactly one button with the localized name, button type: "Create pool" (en), "Crear polla" (es), "Crear quiniela" (es-ES) | Pass, all three | Pass, all three | Pass, all three |
| One route per tap: taps at the centre and at (±20, 0), (0, ±20), (−14, −14), and (14, 14) pt each opened the creator, and a single Back returned with the creator gone | 7/7 in light (en) and dark (es) | 7/7 in light (en) and dark (es) | 7/7 in light (en) and dark (es) |
| Avatar still opens the drawer, which closes with Close menu | Pass | Pass | Pass |
| Avatar and "My pools" title pixel-identical to the previous build, in light and dark | Pass | Pass | Pass (light, against `2.x-ipad27-full-list.png`) |
| Avatar ("Open menu") frame, unchanged | 22, 68, 32 × 32 | 16, 62.3, 32 × 32 | 16, 38, 32 × 32 |
| Create circle compared with where the native item sat | 4 pt left of the native glass (338–382 pt against 342–386 pt); vertical centre unchanged (y 84) | Before there was no surround, only a plus centred at (374, 78) pt. The new circle spans 342–386 pt, so the glyph centre is 10 pt further left (364) and the vertical centre is unchanged. The circle ends 16 pt from the screen edge, as the iOS 26 glass does | Aligned: 766–810 pt, centre (788, 54), the same as before |

- **The iPhone offset.** On the iOS 26.5 iPhone the toolbar ends an item's content 4 pt before the edge of the glass it draws around a native item. The custom circle fills the content frame, so it sits 4 pt further left.
- **Why the offset stays.** A trial with `.padding(.horizontal, -4)` put the circle exactly on the native position (342–386 pt). But taps are limited to the content frame, so the horizontal hit area shrank to +17 / −18 pt. The full hit area takes priority, so the offset is kept.
- **The iPad** toolbar has no such inset, so the circle is aligned there.
- **iOS 18.1.** The glyph moved because the fallback is a 44 pt circle where the plain bar button only drew the plus.

### Captures

- `verification/2.5-toolbar-before-after-ios26.5-ios18.1-light-dark.png`: top row light, bottom row dark. Left to right: 26.5 before, 26.5 after, 18.1 before, 18.1 after (account photo).
- `verification/2.5-create-circle-ios26.5-ios18.1-ipad27-light-dark-ic.png`: rows are 26.5, 18.1, and iPad. Columns are light (en), dark (es), light + IC, and dark + IC.
- `verification/2.5-interactive-glass-press-ios26.5-ipad27-light.png`: at rest and pressed, on iOS 26.5 and iPad.
- `verification/2.5-toolbar-ipad27-before-after-light-dark.png`: iPad before (from 2.3), then after in light and in dark (es) (account photo).

### Simulator settings

- Appearance and Increase Contrast were changed with `simctl ui` and restored. All three simulators are back to light, Increase Contrast off, and Large text.
- Language changes used launch arguments only.
- **iPad window.** One press-and-slide-off run on the iPad started near the window's top edge and moved the app into a floating 554 pt window. The runs made in that window were discarded and repeated at full screen. The window was restored to full screen (820 × 1180 pt) by dragging it, and the iPad's orientation was not changed.
- The simulators run the current build and remain signed in.
- The scratch DerivedData folder was deleted.

### Not verified

- VoiceOver speech: the simulators have no VoiceOver. The name and button type were read from the accessibility tree.
- **Pressed dimming of the pre-26 circle.** It didn't show in the mid-press capture on iOS 18.1. It is not required by the spec, and taps activate normally.
- The disabled appearance of either branch: nothing disables the create action.
- iPad in a resized window and in landscape.
- iOS 16 runtime: accepted for the whole change, with iOS 18.1 standing in.

### Open questions

- **iPhone alignment on iOS 26.** The create circle sits 4 pt left of where the native item's glass sat, and its vertical centre is unchanged. Matching the native position would cut the hit area to about ±17 pt horizontally. Keep the offset? Recommended: yes, since the full hit area was the reason for this approach.
- **Design section 2.** The create-pool row of the surface table still says "in an ordinary `ToolbarItem` (native prominent glass on iOS 26+ …)". Section 4, the spec, and the task now describe the hidden shared background and custom glass circle. The row needs the same update; the planning files were not edited here.

The user kept the 4 pt left offset of the create circle on iOS 26 iPhones to preserve the full 44 × 44 pt hit area, on 2026-09-28.

Accepted without VoiceOver speech, pre-26 pressed dimming, a disabled look, and iPad landscape or resized-window checks for task 2.5 by the user on 2026-09-28.

## Android — task 4.4 (2026-09-28)

Devices:

- **Phone:** Samsung SM-G955F, Android 9 (API 28), es-CO, signed in. It was in dark appearance for this task: its system night mode reads `ui_night_mode=2`, set outside this session; earlier today it read `1`. I didn't change it.
- **Emulator:** `emulator-5554`, Pixel_10_Pro_XL, API 36, en-US, signed in. Used for light, and for dark through `cmd uimode night yes`, restored to `no`.

The Prod debug build was installed over the existing Prod debug app on both devices with `adb install -r -t`, and `firstInstallTime` was unchanged on both. Prod was used read-only: the create flow was opened and left with Back, and the drawer was opened and closed. "Before" is the 4.1–4.3 build (a plain `primary`-tinted plus in an ordinary `IconButton`), captured on the same devices just before the install.

### Code

`PoolScoreListView`'s create action is a Material 3 `FilledIconButton`:

- colours `IconButtonDefaults.filledIconButtonColors(containerColor = primary, contentColor = onPrimary)`;
- `Modifier.size(48.dp)`;
- the canonical `add` drawable at 28 dp, untinted, so it takes `onPrimary`;
- the same `create_pool_action` content description, top-bar `actions` slot and `onPoolCreate` route.

No new unit test was added. The glyph-on-container pair is `onPrimary` on `primary`, which `PrimaryColorTest` already checks in both schemes.

### Tests and builds

`./gradlew :ui:testDebugUnitTest :pool:testDebugUnitTest :app:testProdDebugUnitTest :app:assembleProdDebug`: BUILD SUCCESSFUL. ui 44, pool 2 and app 60 tests, no failures.

### Contrast and size

The fill is the most frequent green in the button. The glyph was measured inside the circle.

| Device | Fill / glyph | Contrast | Circle | Plus (560 of 960 units of 28 dp = 16.3 dp) | Bar (2.33 dp) |
| --- | --- | --- | --- | --- | --- |
| Emulator, light | #2E7D32 / #FFFFFF | 5.13:1 | 144 px = 48.0 dp | 48 px = 16.0 dp | 6 px = 2.0 dp |
| Emulator, dark | #4CAF50 / #000000 | 7.56:1 | 144 px = 48.0 dp | 50 px = 16.7 dp | 8 px = 2.7 dp |
| Phone, dark | #4CAF50 / #000000 | 7.56:1 | 126 px = 48.0 dp | 44 px = 16.8 dp | 6 px = 2.3 dp |

- The plus is centred in the circle within half a pixel. The bar widths differ from 2.33 dp only by anti-aliasing.
- **Against an ordinary icon button.** The avatar's `IconButton` and the previous create `IconButton` draw a 40 dp container inside a 48 dp touch target. On the phone, the avatar node is [12,85][138,211] and the drawn avatar is 32 dp. The new create button draws its full 48 dp container, which is also its 48 dp touch target: [1188,183][1332,327] on the emulator and [943,84][1069,210] on the phone.
- **Touch target.** A tap 20 dp right of the circle's centre, near its edge, on the emulator opened the create flow.

### Placement and layout

| Check | Emulator (light and dark) | Phone (dark) |
| --- | --- | --- |
| Create node bounds, before → after | [1188,183][1332,327], unchanged | [944,85][1070,211] → [943,84][1069,210], 1 px (0.4 dp) up and left |
| Button centre, before → after | (1260, 254), unchanged | (1006, 147) → (1005, 146) |
| Avatar node | [12,183][156,327], unchanged | [12,85][138,211], unchanged |
| Title and the bar between the title and the button | pixel-identical | pixel-identical (left 80 % of the bar) |
| Top-bar height: first row starts at | y 375, unchanged | y 252, unchanged |
| Pool rows | pixel-identical | pixel-identical |

- **Avatar photo on the emulator.** It differs only inside its 32 dp photo circle (up to 76 of 255 in one channel on 389 pixels). The photo was decoded and scaled again after the reinstall. The avatar bounds, the circle and the title are unchanged.
- **Bar layout.** The 48 dp container fits the bar's 48 dp action slot, so the bar's height and the avatar's and title's positions did not shift. The phone's 1 px offset is a rounding difference of the 48 dp slot at 2.625 px/dp.
- **Drawer (emulator, dark).** Compared with the dark drawer captured in 4.1–4.3, the open drawer panel differs by at most 1 of 255 in any channel and the pushed content by at most 2, which is not visible. The create button is off-screen while the drawer is open.

### Single flow opening

- **Phone:** one centre tap opened "Elige una plantilla para crear tu polla". One Back-arrow tap returned to "Mis pollas".
- **Emulator:** one edge tap opened "Choose a template to create your pool". One Back-arrow tap returned to "My pools".
- The accessible names are still "Crear polla" on the phone and "Create pool" on the emulator (`uiautomator dump`), on the icon inside the button, which Compose merges into the button for TalkBack.

### Captures

- `verification/4.4-create-button-before-after-emulator-light-dark-sm-g955f-dark.png`: top bars before (left) and after (right), for emulator light, emulator dark and phone dark (account data: avatar).
- `verification/4.4-create-button-screens-emulator-sm-g955f.png` (account data), left to right: emulator light, emulator dark, phone dark, the create flow opened on the phone, and the emulator drawer in dark.

### Settings

- The emulator's night mode was returned to `no`, and the emulator is still running and signed in.
- The phone's settings were not changed. It is still signed in with this build.

### Not verified

- **Light appearance on the phone:** verified later, after the user switched it to light; see the next subsection.
- **TalkBack speech:** checked only through the accessibility tree.
- **The pressed-state ripple colour on the filled button:** not measured.

### Follow-up: phone in light appearance (2026-09-28)

The user switched the phone to light: `ui_night_mode` reads `1` and `dumpsys uimode` shows `mNightMode=1`. No phone settings were changed here, and the phone was left in light.

| Check | Phone, light | Phone, dark (above) |
| --- | --- | --- |
| Fill / glyph | #2E7D32 / #FFFFFF, 5.13:1 | #4CAF50 / #000000, 7.56:1 |
| Circle | 126 px = 48.0 dp | 48.0 dp |
| Plus (expected 16.3 dp) | 42 px = 16.0 dp, bar 6 px = 2.29 dp, centred within half a pixel | 16.8 dp |
| Create node bounds | [943,84][1069,210] | [943,84][1069,210] |
| Circle centre | (1005, 146) | (1005, 146) |
| Avatar node | [12,85][138,211] | [12,85][138,211] |
| First pool row starts at | y 252 | y 252 |
| Accessible name (`uiautomator dump`) | "Crear polla" on the icon inside the button | "Crear polla" |

- **Against the light 4.1–4.3 capture of the same phone** (`after-mypools`, taken before 4.4), the title and the bar up to the button, and all the pool rows, are pixel-identical.
- **Taps:**
  - A centre tap at (1005, 146) opened "Elige una plantilla para crear tu polla" once, and one Back-arrow tap returned to "Mis pollas".
  - An edge tap at (1057, 146), 20 dp right of the centre, did the same.
- **Loading placeholders.** On launch, the pool-row placeholders appeared with the production rows and shimmer while the top bar already showed the new button.
- **Capture:** `verification/4.4-create-button-sm-g955f-light.png` (account data), left to right: My pools loaded, My pools loading, and the create flow opened by the centre tap.
- **State.** The app stays installed and signed in, and it was left on My pools.


Accepted without TalkBack speech and the pressed ripple colour for task 4.4 by the user on 2026-09-28.

## Integration — tasks 5.1–5.3 (2026-09-28)

Toolchain: Xcode 26.0.1 (17A400) on macOS 27.0. Everything below ran on the current tree, including the 2.5 create circle and the 4.4 Android filled button. Simulators:

- iPhone 17 on iOS 26.5 (`tmp-ci-repro-iPhone17-26.5`), signed in
- iPhone 16 Pro on iOS 18.1, signed in (stands in for pre-26)
- iPad Air 11-inch (M4) on iOS 27.0, signed in, in windowed-apps mode
- A throwaway iPad Air 11-inch (M4) on iOS 27.0, signed out, deleted afterwards
- iPad Air 11-inch (M2) on iOS 18.1 ("iPad Air 11-inch (M2) iOS 18.1", `F45A74E8-D2F2-4DF8-B5D3-DFFD4144E27D`), created for this task with the user's agreement; checked signed out (follow-up below), then signed in by the user

Prod was used read-only: the creator was left with Back, invitation sheets were closed unsent, join screens were left with Go to my pools, the username and prediction drafts were abandoned with Back or Cancel, the pool-name step was left without Done, and nobody was signed out. The runtime checks used a throwaway XCUITest (not committed). On the signed-in iPad the app never reports its animations idle, which stalled XCUITest before; the throwaway test replaced XCUITest's idle wait with a fixed 0.6 s pause on that device, and every step used bounded waits.

### 5.1 Tests and builds

| Check | Command | Result |
| --- | --- | --- |
| Build at the retained deployment target | `xcodebuild -workspace Tyche.xcworkspace -scheme Tyche -destination 'generic/platform=iOS Simulator' clean build` | `** BUILD SUCCEEDED **`. Account, Bet, Core, DataBet, DataPool, LazyPaging, Pool, Session, Tyche, UI, and ViewingState compiled for `arm64-apple-ios16.0-simulator` and `x86_64-apple-ios16.0-simulator`. All 9 `IPHONEOS_DEPLOYMENT_TARGET` entries are 16.0, and all 8 package manifests declare `.iOS(.v16)`. |
| Unit tests, iOS 18.1 | `xcodebuild test-without-building … -destination 'platform=iOS Simulator,id=<iPhone 16 Pro iOS 18.1>' -parallel-testing-enabled NO -skip-testing:TycheUITests` (built with `build-for-testing`, simulator signing) | Passed: 230 of 232, 0 failed. The 2 skips are the opt-in avatar wire probes (`TEST_RUNNER_AVATAR_WIRE_PROBE_ACCOUNT_ID`). Bundles: AccountTests, BetTests, CoreTests, DataBetTests, PoolTests, SessionTests, TycheTests, UITests. This covers the account, pool, bet, profile/username, drawer, input, row-accessibility, accent, and toolbar-circle suites. |
| Unit tests, iOS 26.5 | Same, also `-skip-testing:PoolTests` (ViewInspector crashes on local runtimes) | Passed: 197 of 203, 0 failed. Skips: the 2 wire probes and the 4 cases that run only below iOS 26 (`toolbarProminentFallbackGlyphIsLegibleOnTheAccent`, `liquidGlassSecondaryFallbackLabelIsLegible`) or only where offscreen renders draw glass (`liquidGlassProminentLabelIsLegibleOnTheAccent`, `disabledLiquidGlassProminentKeepsTheSystemPresentation`). |
| `DrawerPassUITests`, iOS 26.5 (`TEST_RUNNER_DRAWER_UI_PASS=1`) | `-only-testing:TycheUITests/DrawerPassUITests` | 5 of 5 passed. |
| `DrawerPassUITests`, iOS 26.5 with Reduce Motion | `testPoolListDrawer`, `testPoolListDrawerGestures`, `testPoolHomeDrawerGestures` | 3 of 3 passed. `ReduceMotionEnabled` was set to 1 for the run and restored to 0. |
| `DrawerPassUITests`, iOS 18.1 | Same | 4 of 5 passed. `testDrawerNavigationLayout` fails with the known pre-existing "avatar shifted within the pushed screen" (−324 vs 16), identical to 2.4, where the `HEAD` build failed the same way. |
| `DrawerPassUITests`, iPad | Not rerun | The known iPad failures (regular-width top tabs, floating-window coordinates; 2.4) were not re-observed. The iPad drawer behaviour was checked with the throwaway test in 5.2 instead. |
| Android | `./gradlew :ui:testDebugUnitTest --rerun :pool:testDebugUnitTest --rerun :app:testProdDebugUnitTest --rerun :app:assembleProdDebug` | BUILD SUCCESSFUL: ui 44, pool 2, app 60 tests, no failures. The Prod debug APK was up to date with the 4.4 sources. |
| No backend change | `git diff HEAD --stat` outside `iOS/`, `Android/`, `assets/`, and `openspec/` | Empty. The only untracked paths outside `iOS/` and `Android/` are `assets/icons/` and this change folder. |
| `Package.resolved` | Hashes before and after every run | Unchanged. |

An earlier run in this group, made while two simulators ran UI tests in parallel, failed `testPoolListDrawerGestures` on 26.5 and `testPoolHomeDrawer` on 18.1 once each; both passed when the simulators ran one at a time, and the results above come from sequential runs.

### 5.2 Cross-screen matrix

Earlier sections are still valid where this change hasn't touched the screen since. The create circle (2.5) was rechecked everywhere it appears.

| Area | iPhone iOS 26.5 | iPhone iOS 18.1 | iPad iOS 27.0 | iPad iOS 18.1 |
| --- | --- | --- | --- | --- |
| Welcome, email-link, email-and-password, sign-in-required join (signed out) | 3.1, 3.2, 3.8 on iOS 27.0 iPhone | 3.1, 3.8 (throwaway simulators) | **This section**, portrait and landscape | **This section** (follow-up below), portrait and landscape |
| Create circle: one control, localized name, full hit area, one route per tap | 2.5; **this section**: 7-point hit scan 7/7 in portrait, edge tap in portrait and landscape | 2.5; edge tap, portrait and landscape (this section) | 2.5 (full-screen portrait); **this section**: 375 pt window in landscape, and landscape windows of 747, 668, and 1180 pt | **This section**: 7-point hit scan 7/7, edge tap in portrait and landscape |
| Row invite sheet, list drawer, pool-home drawer, tabs | 2.3, 2.4; rechecked in portrait and landscape (this section) | 2.3, 2.4; rechecked in portrait and landscape (this section) | 2.x; **this section** at every window size above | **This section**, portrait and landscape |
| Join pair (join link, Go to my pools only) | 3.1; rechecked in portrait and landscape (this section) | 3.1; rechecked in portrait and landscape (734 × 50.3 pt each) | **This section**: 804 × 50.5 pt in portrait, 1164 × 50.5 pt in landscape, 2 buttons | **This section**: 804 × 50.5 / 1164 × 50.5 pt, 2 buttons |
| Forms and inputs | 1.3, 3.2 | 1.3, 3.2 | **This section** | **This section** (signed out and signed in) |
| Prediction row | 3.3 | 3.3 | **This section** | **This section** |
| Long-list scrolling | **This section**, portrait (before 2.5) and landscape (after 2.5) | Before 2.5 in this group (screen unchanged by 2.5) | **This section** | **This section**, portrait and landscape |
| Keyboard reachability | **This section**, portrait and landscape | **This section**, portrait and landscape | **This section** (hardware keyboard when signed in) | **This section**, software keyboard, portrait and landscape |
| Rotation | **This section** (after a simulator reboot) | **This section** | **This section** | **This section** |
| Window resizing | n/a | n/a | 2.x (375 pt and full screen); **this section** (intermediate widths) | Not verified (iOS 18 has no resizable windows; Split View and Slide Over need a second app) |
| Appearance and accessibility settings | 3.5, 3.7, 2.5 | 3.5, 3.7, 2.5 | 2.5 (create circle, light/dark/IC) | Light / Large only (the branch is the same as on the iOS 18.1 iPhone, covered in 3.5, 3.7, 2.5) |

**iPad, iOS 27.0, signed in** (captures `5.2-ipad27-*.png`)

- **Portrait, full screen (820 × 1180 pt).** The create circle is at (766, 32), 44 × 44 pt, the only "Create pool" button. An edge tap 17 pt right of its centre opened the creator once, and one Back returned. The row invite opened the share sheet without opening the pool, and it closed. Both drawers opened and closed; with the list drawer open, the avatar and the create circle were not hittable. The tabs are Scores, Bets, History at the top (the system's regular-width presentation).
- **Landscape.** In landscape the app opens in the floating 375 × 820 pt window left from the 2.x checks (at x 403). The same checks passed there: create circle at (724, 32), bottom tab bar, share sheet, both drawers.
- **Window resizing, landscape.** Dragging the floating window's corner produced widths of 747 pt, 668 pt, and 1180 pt (full width), and then 375 pt again. At each size the create circle stayed 44 pt and hittable at the trailing edge (910, 871, 1126, and 724 pt), the avatar stayed hittable, the creator opened once, the invite sheet opened within the window and closed, and the drawer opened and closed. In portrait, resizing still snaps between 375 pt and full screen (as in 2.x); both passed the same checks, and the window was restored to full screen.
- **Long lists.** On "Copa Mundial de la FIFA 2026", fast swipes scrolled Scores (13 gamblers) and History in portrait and in the landscape window. The Bets tab has no pending predictions in this pool.
  - In portrait at full screen, the regular-width top tabs move up with the navigation bar when the large title collapses (Scores tab y 142 → 90) and return when scrolled back. They keep their size (86.5 × 36 pt) and order. This is the system's collapsing large-title behaviour, not tab minimization. The bottom tab bar in the 375 pt window kept its frame.
- **Keyboard.** The signed-in iPad has a hardware keyboard connected, so the software keyboard doesn't appear (keyboard frame height 0). Save username (y 360), prediction Cancel/Save (y 381.5), and pool-name Done (y 256) were hittable with focus in the field, in portrait and in the landscape window. Prediction Cancel returned the row to Edit.
- **Captures:** `5.2-ipad27-create-circle-portrait-landscape-375-window.png` (portrait list; landscape window: list, pool home, share sheet), `5.2-ipad27-landscape-window-resize-747-668-1180-375.png` (747 pt list, 668 pt share sheet, 1180 pt list and drawer, 375 pt list), `5.2-ipad27-long-list-keyboard-hardware.png` (Scores and History mid-scroll, username with the hardware keyboard, prediction in the landscape window). They show account data.

**iPad, iOS 27.0, signed out** (throwaway simulator, deleted)

- Welcome, portrait: Email and Email and password are 788 × 50.5 pt, with the Google button outside the pair; exactly three buttons (`google_logo`, `Email`, `Email and password`). Landscape: 1148 × 50.5 pt, same order.
- This simulator shows the software keyboard (279 pt in portrait, 364 pt in landscape). With it up, email-link Sign in (enabled for `qa@example.com`) and email-and-password Sign in (disabled without a password) sit above the keyboard at y 156 and 217 pt in both orientations. Nothing was submitted.
- The sign-in-required join screen (join link) shows Got it at 804 × 50.5 pt, and Got it returned to the welcome screen.
- Capture: `5.2-ipad27-signed-out-welcome-forms-keyboard.png`.

**iPhone** (captures `5.2-iphone-ios18.1-landscape-create-circle-navigation.png`)

- **iOS 18.1, portrait and landscape.** The create circle is 44 × 44 pt at (342, 56.3) in portrait and (752, 0) in landscape. An edge tap opened the creator once in both. The invite sheet, both drawers, the three bottom tabs, and the join pair passed in both orientations.
- **iOS 26.5, portrait.** Create circle at (338, 62), 44 × 44 pt. A 7-point scan (centre, ±20 pt on each axis, and ±14 pt diagonals) opened the creator 7/7, each time returning with one Back. The edge tap, invite sheet, drawers, tabs, and join pair passed.
- **iOS 26.5, landscape.** The simulator stopped rotating the app partway through this session; after a simulator reboot (it stayed signed in) it rotated again. In landscape (874 × 402 pt): the create circle is 44 × 44 pt at (788, 24), the only "Create pool" button, and an edge tap opened the creator once; the row invite sheet opened without opening the pool and closed; the list drawer opened (avatar and circle not hittable) and closed; the three bottom tabs are in order; Scores and History scrolled with the tab bar fixed (89 × 36 pt Scores tab); the join pair measured 734 × 50.3 pt. With the keyboard up, prediction Cancel/Save and Save username are covered at first and became hittable after one scroll of the content (Save username y 244 → 152, prediction row y 257 → 187), with the draft and keyboard kept. On iOS 18.1 the same Save username scroll gave y 234 → 141.
- **Long lists and keyboard, before 2.5, both iPhones.** In portrait every action was above the keyboard (prediction row y 331–347, Save username y 395–411, Done y 270–286, keyboard top y 583). In landscape the keyboard covers the prediction row at first; one drag of the content lifts Cancel and Save above it. 2.5 changed only the toolbar item, and these screens don't show it.

**iPad, iOS 18.1, signed in** (capture `5.2-ipad18.1-signed-in-create-circle-sheets-lists-keyboard.png`, account data)

- **Create circle (filled fallback).** 44 × 44 pt at (756, 27) in portrait and (1116, 27) in landscape, the only "Create pool" button, next to an unchanged 32 pt avatar at (20, 33). The 7-point scan opened the creator 7/7, each time returning with one Back; an edge tap 17 pt right of the centre also opened it in both orientations.
- **Sheets, drawers, tabs.** The row invite opened the share sheet (a centred form sheet) without opening the pool and closed. Both drawers opened and closed, with the avatar and circle not hittable while open. The tabs are Scores, Bets, History at the top (regular width) in both orientations.
- **Join pair.** 804 × 50.5 pt in portrait and 1164 × 50.5 pt in landscape, exactly 2 buttons; Go to my pools returned without joining.
- **Long lists.** Scores and History scrolled in portrait and landscape. As on iOS 27, the regular-width top tabs move up with the collapsing large title (y 135 → 83) and return, keeping their size and order.
- **Keyboard (software).** 279 pt in portrait and 364 pt in landscape. Save username (y 348), prediction Cancel/Save (y 369.5), and pool-name Done (y 244) were all above the keyboard and hittable in both orientations. Prediction Cancel returned the row to Edit; nothing was saved.
- **Window sizes.** iOS 18 has no resizable app windows. Split View and Slide Over need a second app arranged with multitasking gestures, which the test couldn't drive, so no narrower width was exercised on this runtime.

### 5.3 Review against the delta specs

**Glass in content and bars.** `grep` over `iOS/` Swift sources (excluding tests):

- `.liquidGlass` / `.liquidGlassProminent` appear only in the standalone groups: welcome (`HomeView`), email-link Get started and Retry (`EmailLinkSignInView`), join and join failure (`PoolJoinerView`), and Got it (`PoolJoinRequiresSignInView`). `GlassEffectContainer` appears only inside `LiquidGlassContainer`.
- `LiquidGlassTextFieldStyle` is gone; no text field, form action, row action, invite, or paging Retry uses glass.
- `.glassEffect` appears once in app code: `ToolbarProminentButtonStyle`, used only by the create-pool action in `PoolScoreListRouter`. This is the spec's single permitted app-drawn glass control in a bar.
- `sharedBackgroundVisibility(.hidden)` appears only in `PlainToolbarItem`, used by the two avatars and the create circle.
- No `toolbarBackground`, `presentationBackground`, `UINavigationBarAppearance`, `UITabBarAppearance`, appearance proxy, `tabBarMinimizeBehavior`, `backgroundExtensionEffect`, or `glassEffectID`. The only bar-related modifier added is `.toolbarColorScheme(.dark, for: .navigationBar)` on the welcome screen, which sets light status-bar content (the 3.8 white-header decision) and draws no layer. `LoadingContainerView`'s `.thinMaterial` overlay is pre-existing and unchanged.

**Icon assets.** Every change belongs to the create glyph or the accent:

| Asset | Change | In scope because |
| --- | --- | --- |
| `assets/icons/add.svg`, `README.md`, `MATERIAL_SYMBOLS_LICENSE.txt` | Added | Canonical create glyph (2.1) |
| `iOS/UI/…/Icons/add.imageset` | Added | Derived iOS glyph (2.1) |
| `iOS/UI/…/Icons/filled_add_circle.imageset` | Removed | Replaced create glyph, no other consumer (2.1) |
| `Android/ui/…/drawable/add.xml` | Added | Derived Android glyph (4.1) |
| `Android/ui/…/drawable/filled_add.xml` | Removed | Replaced create glyph, no other consumer (4.1) |
| `iOS/Tyche/Tyche/tyche.icon/icon.json` | Background fill #4CAF50 → #2E7D32 | App icon accent (3.7) |
| `TycheLogoSplash.svg` | Green path → #2E7D32 | iOS launch logo (3.8) |
| `ic_launcher_background.xml`, `ic_tyche_logo_splash.xml` | #4CAF50 → #2E7D32 | Android launcher and splash (4.3) |

No other image, colour asset, or drawable changed except the colour sets named in the spec (`AccentColor`, `OnPrimaryColor`, `PrimaryContainerColor`). Navigation structure is unchanged: same destinations, three tabs in the same order, the same drawer, and sheets with the same detents.

**Recorded decisions against the code.**

| Decision | Code |
| --- | --- |
| Large glass buttons | `LiquidGlassButtonStyle` and `LiquidGlassProminentButtonStyle` apply `.controlSize(.large)` on both branches |
| Tap anywhere in the field to focus | `StandardTextFieldStyle`: its own `@FocusState` plus a tap handler on the capsule background |
| Input fill | `tertiarySystemFill` with a 3:1 `systemGray` boundary |
| Create action | `.toolbarProminent` in a `PlainToolbarItem`: 44 pt circle, 28 pt glyph, `onPrimary` label, accent-tinted interactive glass on iOS 26+, accent fill below; accessibility label `create_pool_action`; `toolbarActionTint()` removed |
| Primary-tinted secondaries | Row invite and prediction Cancel `.tint(.primary)`; `.liquidGlass` pre-26 fallback `.tint(.primary)` |
| Accent values | `AccentColor` #2E7D32 / #4CAF50, high-contrast variants equal; `OnPrimaryColor` white / black; `PrimaryContainerColor` #1B5E20; Android `Color.kt` matches |
| White welcome headers | iOS `HomeView` `.foregroundStyle(.white)` and `.toolbarColorScheme(.dark, …)`; Android `HomeView` white tint and light status-bar icons |
| Android create action and TalkBack label | 48 dp `FilledIconButton` (`primary` / `onPrimary`, 28 dp glyph), `contentDescription` `create_pool_action` in en, es, es-ES |

Fixed during this review (comments and documentation only): `iOS/UI/README.md` said the high-contrast variants keep Increase Contrast from lightening the fill; it now says the native styles still adjust the fill and the label stays at least 7:1, as design section 7 and 3.7 record. A `HomeView.swift` comment claimed "9.1:1 or more" for the white header; it now says at least 5:1, matching the 5.19:1 measured in 3.8.

**Requirement-to-evidence map.** "IC" means Increase Contrast; section names refer to this file.

| Requirement / scenario | Evidence | Recorded acceptance or gap |
| --- | --- | --- |
| *OS support*: opening on iOS 26+ | 3.1, 3.7, 2.5; 5.2 on iOS 26.5 and iPad 27 | — |
| *OS support*: opening on an older version | 1.2, 3.1, 3.8 follow-up, 2.5 on iOS 18.1; 5.1 build at 16.0 | iOS 16 runtime accepted as not verified for the whole change (1.x) |
| *Native navigation*: creating a pool from the toolbar | 2.5 (contrast in light, dark, and IC on three devices; hit-area scan; one route per tap; names in three locales); 5.2 hit scans on iPhone 26.5 and iPad 18.1, edge taps in portrait, landscape, and resized windows on all four devices | 4 pt left offset on iOS 26 iPhones accepted (2.5); VoiceOver speech accepted (2.2, 2.5) |
| *Native navigation*: opening the drawer through the avatar | 2.2, 2.4, 2.5; 5.1 `DrawerPassUITests` | — |
| *Native navigation*: scrolling beneath native navigation | 2.3 (tab-bar frame before and after scrolling); 2.6 (tab lists run beneath the floating tab bar with the native scroll-edge effect, last row reachable, on four devices); 2.8 (each tab collapses its own title, rows pass beneath the navigation bar, on four devices, light and dark on iPhone 26.5 and iPad 18.1) | iPad regular-width tabs now sit in the navigation bar row, accepted (2.8); Bets rows beneath the bars not shown, no pool has enough pending predictions (accepted, 2.6, 2.8) |
| *Native navigation*: only the create circle draws bar glass | 5.3 grep | — |
| *Content-layer controls*: editing a prediction | 1.3, 3.3; `PendingBetItemViewModelTests`; 5.2 keyboard checks | Paging while editing accepted (3.x) |
| *Content-layer controls*: retrying a failed prediction | 3.3 (view-model tests and rendered states) | Live failure and retry accepted (3.x) |
| *Content-layer controls*: editing and saving a username | 1.3, 3.2; `UsernameEditorViewModelTests`; 5.2 keyboard checks | Live saving and failure accepted (3.x); unfocused padding tap on 26.5 accepted (1.3) |
| *Content-layer controls*: inviting from a pool row | 3.4; 2.3; 5.2 | — |
| *Standalone groups*: choosing a sign-in method | 3.1, 3.8 follow-up; 5.2 signed-out iPad | Join and email-link loading states accepted (3.x) |
| *Standalone groups*: confirming or cancelling a join | 3.1; 5.2 join pair on three devices | Join itself not tapped (Prod read-only) |
| *Sheets and structure*: presenting an invitation sheet | 2.3; 2.x iPad; 5.2 at every iPad window size; 2.6–2.8 runs of `testPoolHomeDrawer` (drawer Invite sheet) after the restructure | — |
| *Sheets and structure*: requirement text (destinations, tab count, labels, order, tab-bar visibility, opaque drawer, no minimization) | 5.3 refresh below (code review of the per-tab stacks); 2.8 results | iPad tab placement in the navigation bar row accepted (2.8) |
| *Sheets and structure*: operating the drawer and tabs | 2.4, 2.x iPad; 5.1 drawer suites; 2.7 (angle sweeps on four devices: 0–55° from vertical scrolls, 62–90° toward the trailing edge opens, leading drags over a closed drawer do nothing and open no row; `DrawerDragDirectionTests`, `DrawerInteractionTests`); 2.8 (destinations and Back on four devices; `DrawerPassUITests` after the erase: iPhone 26.5 5 of 5, iPhone 18.1 known failures only; iPad top-tab drags keep their tab selection and don't open the drawer) | iPad floating-window edge back-swipe and intermediate portrait widths accepted (2.3, 2.4, 2.8); 2.7 gaps accepted (iOS 16/17 fallback, downward drags, horizontally scrolling controls) |
| *Accessibility*: transparency and contrast preferences | 3.5, 3.7, 2.5 | — |
| *Accessibility*: VoiceOver | Accessibility tree in 2.2, 3.1, 3.4, 3.5, 2.5, 5.2 | Speech accepted as not verified (2.2, 3.x, 2.5) |
| *Accessibility*: large text or resized window | 1.3, 3.2–3.4 (AX Large); 2.x and 5.2 (iPad 27 375 pt to full width, landscape 668 and 747 pt); 5.2 keyboard reachability on four devices | iPad 18.1 resizing not possible on that runtime (Not verified) |
| *Accessibility*: reducing motion | 3.3, 3.5; 5.1 and 2.7 drawer suites with Reduce Motion | — |
| *Accessibility*: retrying a failed page load | 3.6 | Reload after Retry blocked by the pre-existing `lazy-paging-swift` issue, accepted (3.6) |
| *Accent labels*: primary action in light | 3.7, 2.5; Android 4.3, 4.4; `AccentFilledLabelTests`, `PrimaryColorTest` | — |
| *Accent labels*: dark with and without IC | 3.7, 2.5; 4.3, 4.4 | System IC fill adjustment accepted and requirement reworded (3.7) |
| *Accent labels*: launching the app | 3.7 (compiled icon), 3.8 (launch screen), 4.3 (launcher, splash) | iOS 26 home-screen icon accepted (3.7); Android dark-mode splash branding gap tracked outside this change (below) |
| *Canonical icons*: preparing the create glyph | 2.1, 4.1 (byte and path equality, rendered geometry); `assets/icons/README.md` | — |
| *Placeholders*: loading My pools | 3.4 (render test, `PoolScoreItemAccessibilityTests`); Android 4.4 follow-up | Live iOS placeholders accepted (3.x) |
| *pool-score-list*: sighted user sees no difference | 3.4 (row pitch before and after); 2.5 (avatar and title pixel-identical); 4.2, 4.4 (Android pixel diffs) | — |
| *pool-score-list*: pool with exactly one member | `PoolScoreItemAccessibilityTests` (5.1); "1 member" visible in 5.2 captures | — |
| *pool-score-list*: surrounding screen untouched | 2.3, 2.5; 5.2 | — |
| *pool-score-list*: Android pool rows retain their appearance | 4.2, 4.4 | Placeholder pixel comparison accepted (4.x) |

**Remaining runtime-validation limitations** (each recorded above or in its task section):

- iOS 16 runtime: not available; iOS 18.1 stands in (accepted).
- VoiceOver and TalkBack speech: only the accessibility trees were read (accepted).
- Live writes on Prod: saving, joining, failure injection, and paging while editing are covered by unit and render tests (accepted in 3.x).
- `lazy-paging-swift` Retry no-op in short lists: pre-existing library bug (accepted in 3.6).
- Android dark-mode splash branding: a known gap, tracked as a separate task outside this change.
- iOS 18.1 `testDrawerNavigationLayout` and the iPad `DrawerPassUITests`: pre-existing, reproduced on `HEAD` (2.4).
- iPad window resizing on iOS 18.1 (no resizable windows on that runtime), and the iPad 18.1 in other appearance and accessibility settings (same pre-26 branch as the iOS 18.1 iPhone, which covers them).
- The iPad 27 software keyboard while signed in (a hardware keyboard is connected there); covered on the signed-out iPad 27, the iPad 18.1, and both iPhones.

### Simulator settings

- Reduce Motion on the iOS 26.5 iPhone was set to 1 for one run and restored to 0. Appearance, text size, and Increase Contrast weren't changed: all three signed-in simulators are at light, Large, Increase Contrast off.
- The iPad's portrait window was resized during the checks and restored to full screen. Its landscape window was resized and left at the 375 pt floating window it had before. Orientation was returned to portrait on every device.
- The iOS 26.5 iPhone was rebooted once to restore rotation; it stayed signed in.
- The signed-in simulators run the current debug build and remain signed in, including the new iPad 18.1 (kept). The throwaway signed-out iPad 27 was deleted.
- The scratch DerivedData folder was deleted, and the throwaway test was removed.

### Not verified

- iOS 16 runtime: not available; iOS 18.1 stands in (accepted for the whole change).
- VoiceOver speech: the simulators have no VoiceOver; the accessibility trees were read (accepted).
- iPad window resizing on iOS 18.1: that runtime has no resizable windows, and Split View / Slide Over need a second app arranged by multitasking gestures.
- iPad 18.1 in dark appearance, Increase Contrast, Reduce Transparency, Reduce Motion, and large text: only light / Large was run. The same pre-26 code paths are covered on the iOS 18.1 iPhone (3.5, 3.7, 2.5).
- iPad 27 software keyboard while signed in: a hardware keyboard is connected on that simulator. The software keyboard was covered on the signed-out iPad 27, the iPad 18.1, and both iPhones.
- iPad intermediate widths in portrait on iOS 27: resizing snaps between 375 pt and full screen (as recorded in 2.x); intermediate widths were exercised in landscape.
- The previously accepted gaps: live Prod writes, paging while editing, live placeholders, the `lazy-paging-swift` Retry issue, and the iPad floating-window edge back-swipe.

### Follow-up: iPad on iOS 18.1, signed out (2026-09-28)

A new iPad Air 11-inch (M2) simulator on iOS 18.1 ("iPad Air 11-inch (M2) iOS 18.1", `F45A74E8-D2F2-4DF8-B5D3-DFFD4144E27D`) was created with the user's agreement and the current debug build installed. Before the user signs in, the signed-out screens were checked with the same throwaway test (nothing submitted):

- Welcome, portrait: Email and Email and password are 788 × 50.5 pt (bordered fallback, primary-tinted secondary), with the Google button outside the pair; exactly three buttons. Landscape: 1148 × 50.5 pt, same order.
- Software keyboard (279 pt portrait, 364 pt landscape): email-link Sign in (enabled for `qa@example.com`) and email-and-password Sign in (disabled without a password) are above it at y 144 and 205 pt in both orientations.
- The sign-in-required join screen shows Got it (804 × 50.5 pt), and Got it returned to the welcome screen.
- On the first keyboard presentation in portrait, iOS showed its own keyboard onboarding card ("Type English and Spanish") over the keyboard area. It is system UI, not the app's.
- Capture: `verification/5.2-ipad18.1-signed-out-welcome-forms-keyboard.png`.

The user then signed in; the signed-in results are in the iPad iOS 18.1 part of 5.2 above.

### 5.3 refresh (2026-09-29)

Tasks 2.6, 2.7, and 2.8 changed pool home and the drawer after 5.1–5.3 were first recorded. This refresh reviews them against both delta specs and the navigation-drawer contract (`openspec/specs/navigation-drawer/spec.md`), and updates the evidence map above. The pool-home results in 5.2 (long lists, tabs, pool-home drawer) were taken before these tasks; 2.6–2.8 supersede them for pool home.

**Tests rerun on the current tree.** 2.8's follow-up reran only the UI package and `TycheTests`, so the full unit suite was rerun here: `xcodebuild build-for-testing -workspace Tyche.xcworkspace -scheme Tyche -destination 'generic/platform=iOS Simulator'` (Xcode 26.5, scratch DerivedData), then `xcodebuild test-without-building … -destination 'platform=iOS Simulator,id=<iPhone 16 Pro iOS 18.1>' -parallel-testing-enabled NO -skip-testing:TycheUITests`: passed, 236 of 238, 0 failed, 2 skips (the opt-in avatar wire probes). The run covered AccountTests, BetTests, CoreTests, DataBetTests, PoolTests, SessionTests, TycheTests, and UITests. `Package.resolved` files were unchanged. No other simulator work was done.

**Navigation structure (2.8) against "Existing sheets and navigation structure remain native".**

- **Destinations:** the same five routes (bet timeline, match bets, manage gamblers, Profile, username editor), now registered by `PoolHomeDestinations` on each tab's stack, with the same closures. The pre-change router also had five. (2.8's record says "six" but lists five; the code has five.)
- **Tabs:** three, in the same order, with the same tags, labels, icons, and titles (Scores, Bets, History), and the same toolbar on each tab (avatar in `PlainToolbarItem`, native change-pool `ToolbarItem`).
- **Tab-bar visibility:** each destination sets `.toolbar(.hidden, for: .tabBar)`, so destinations still cover the tabs; 2.8's tables show the tabs hidden on every destination on four devices. Only the selected tab's stack is bound to `DrawerHostNavigation.path`, and tabs can't change while a destination is shown.
- **Drawer:** `.drawer(… allowsDragging: navigation.isHostVisible, stabilizesNavigationLayout: true)` wraps the tab view unchanged; `DrawerNavigationLayout` now stabilises the selected tab's navigation controller. `drawerTabBarBoundary()` moved from each list to each tab's stack; `excludesTabBarFromDrawerDrags()` is unchanged.
- **iPad:** regular-width tabs now sit in the navigation bar row instead of below the large title. This is the iPadOS placement for a root `TabView`; the user accepted it in 2.8.
- **Sheets:** the invitation `.sheet` with `[.medium, .large]` detents and no background override is unchanged.

**Bar and glass re-audit** (diff against `753c448e`, and all non-test Swift sources):

- The only added bar-related modifiers are `.toolbar(.hidden, for: .tabBar)` on the five destinations (visibility, as above) and the welcome screen's `.toolbarColorScheme(.dark, for: .navigationBar)` (status-bar content).
- `glassEffect` still appears only in `ToolbarProminentButtonStyle` (the create-circle exception). `sharedBackgroundVisibility` is only in `PlainToolbarItem`.
- No `toolbarBackground`, `presentationBackground`, appearance proxies, `tabViewStyle`, `tabBarMinimizeBehavior`, `scrollEdgeEffect…`, or `backgroundExtensionEffect` in app code. The 2.6 and 2.8 list changes only move padding into `contentInsets`.

**Drawer gesture (2.7) against the navigation-drawer contract.**

| Contract point | Code | Evidence |
| --- | --- | --- |
| A predominantly horizontal drag from anywhere, including a row, opens or moves the drawer | `DrawerReveal.takesDrag`: within 30° of horizontal; `DrawerPanGesture` is attached over the whole container; translation is converted to logical direction for RTL | 2.7 sweeps (62–90° R opened on four devices, from rows); `DrawerDragDirectionTests`; `DrawerPassUITests` swipe tests |
| Vertical scrolling keeps its behaviour | Scroll views' pans are required to wait for `DrawerPanGesture`, which fails at once for any drag it doesn't take | 2.7 sweeps: 0–55° from vertical scrolled on four devices and on My pools at AX XXXL |
| Once a drawer drag is recognized, the start control doesn't activate | `UIPanGestureRecognizer` cancels touches in the view once it begins; a leading drag over a closed drawer is taken (`takesDrag`) but not followed (`claimsDrag`) | 2.7: 65–90° L did nothing and opened no row; `DrawerPassUITests` (row not opened by a swipe) |
| System-rendered bars keep their drags | `takesDrag` leaves a drag starting in the host's reserved tab-bar band while the drawer rests closed | `testPoolHomeDrawerGestures` (tab tap and slide) after 2.7 and after 2.8 on iPhone 26.5 and 18.1; 2.8 iPad top-tab drags kept their selection |
| System Back owns its edge; no drawer drags once a destination is open, including while the drawer is closing | `isEnabled` follows `allowsDragging` = `isHostVisible` (`path.isEmpty`); `open` / `openFromDrawer` append to the path synchronously, so dragging is disabled as soon as a destination is chosen | 2.8 destination tables (edge back-swipes return with the drawer closed on four devices); `testPoolListDrawerGestures` (Profile back-swipes) |
| Existing horizontal controls that handle their own drags keep them | Not handled: every scroll view's pan, including a horizontally scrolling one, waits for the drawer's recognizer, which takes clearly horizontal drags | No drawer host contains such a control today; accepted in 2.7 (see Findings) |

**Findings** (no product code changed):

1. **Horizontal scroll views inside a drawer host (latent).** `DrawerPanGesture.gestureRecognizer(_:shouldBeRequiredToFailBy:)` makes every `UIScrollView` pan wait, so a horizontally scrolling list added to a host later would lose horizontal drags to the drawer, contrary to the contract's "existing horizontal controls that handle their own drags". None exists now, and 2.7 recorded this. Proposed fix when one is added: in `gestureRecognizerShouldBegin`, decline when the touch starts in a scroll view that can scroll horizontally (content wider than its bounds), or let such a scroll view's pan win instead of waiting.
2. **Second finger during a drawer drag (low risk, not observed).** `gestureRecognizer(_:shouldReceive:)` records `touchDown` for every touch it is offered. If UIKit offers a second finger's touch mid-drag, the translation would be measured from that finger and the reveal could jump. Proposed fix: record `touchDown` only while the recognizer is `.possible` and has no touches. Not reproduced; multi-touch wasn't exercised.
3. **Record wording.** 2.8's Code section says "six `navigationDestination`s" and lists five; the code and the pre-change router have five. Earlier sections were left as recorded.

**Remaining runtime-validation limitations, added by 2.6–2.8** (in addition to the list above):

- **Simulator storage incident and erase (2.8).** On 2026-09-29 at about 18:27 the device folders of all four simulators disappeared from `~/Library/Developer/CoreSimulator/Devices/` during 2.8's iPad 18.1 checks; the cause wasn't identified. At the user's direction, `xcrun simctl erase` was run on all four (none deleted or recreated), the debug build was reinstalled, and the user signed in again. Signed-in results recorded in this section before that date (5.2, including the iPad 18.1 signed-in checks) came from the pre-erase sessions and weren't repeated; 2.8's follow-ups reran the pool-home, destination, and drawer checks after the erase.
- **2.6:** dark appearance on iPhone 18.1 and the iPads, loading placeholders on the iPads, Increase Contrast, Reduce Transparency, and large text over the tab bar (accepted).
- **2.7:** the iOS 16/17 SwiftUI-drag fallback, a physical device and real finger paths, downward drags, horizontally scrolling controls, and a single unreproduced missed 90° drag on iPad 27 (accepted).
- **2.8:** Bets rows beneath the bars (no pool has enough pending predictions) and the edge back-swipe inside the iPad floating 375 pt window (accepted).
- **First-run `testPoolHomeDrawer` failure on iPhone 18.1** ("share sheet did not dismiss") recurs in 2.6, 2.7, 2.8, and 5.1 and passes when rerun alone; test flakiness, not a product failure.

**Not verified, final** (this change as a whole):

- iOS 16 and 17 runtimes: not installed; iOS 18.1 stands in, and the pre-18 drawer drag was not measured (accepted).
- VoiceOver and TalkBack speech: accessibility trees only (accepted).
- Live Prod writes (saving predictions and usernames, joining, pool creation), injected failures, paging while editing, and live My pools placeholders: covered by unit and render tests (accepted).
- Paging Retry reload in short lists: blocked by the pre-existing `lazy-paging-swift` issue (accepted).
- Android dark-mode splash branding: tracked outside this change.
- iPad `DrawerPassUITests` and the iPhone 18.1 `testDrawerNavigationLayout`: pre-existing failures reproduced on `HEAD` (2.4).
- iPad: window resizing on iOS 18.1 (no resizable windows), intermediate portrait widths on iOS 27, the floating-window edge back-swipe, the iPad 18.1 in accessibility settings, and the iPad 27 software keyboard while signed in.
- 2.6–2.8 gaps listed just above, including Bets rows beneath the bars, horizontally scrolling controls in a drawer host, downward and real-finger drags, and multi-touch during a drawer drag.
- Signed-in checks from before the 2026-09-29 erase were not repeated beyond 2.8's follow-ups.

## iOS — task 2.6 (2026-09-29)

Toolchain: Xcode 26.0.1 (17A400) on macOS 27.0. Simulators, all signed in to Prod and used read-only (no predictions, pools, usernames, invites, or joins; nobody signed out):

- iPhone 17 on iOS 26.5 (`tmp-ci-repro-iPhone17-26.5`)
- iPhone 16 Pro on iOS 18.1
- iPad Air 11-inch (M4) on iOS 27.0
- iPad Air 11-inch (M2) on iOS 18.1

The runtime checks used a throwaway XCUITest (not committed). It opened "Copa Mundial de la FIFA 2026" from My pools, captured the first frames of pool home (loading placeholders), then on each tab dragged the list, swiped to the end, and logged the frames of the tab bar, the navigation bar, and the tab's scroll view. On both iPads it replaced XCUITest's idle wait with a fixed 0.6 s pause, and every run had a hard `timeout`.

### Cause

The three tab roots padded their paging list from outside the scroll view: `GamblerScoreListView`, `PendingBetListView`, and `FinishedBetListView` each ended with `.padding(.vertical, boxSpacing.medium)`. SwiftUI lets a scroll view run beneath a bar only when the scroll view reaches that bar's safe-area edge. The 8 pt padding kept it off the tab bar's edge, so the list stopped above the bar.

Measured before the change on iOS 26.5: the scroll view's frame ended at y 783, 8 pt above the tab bar's safe-area edge at y 791 (screen height 874). The rows were clipped there, and the floating bar sat over an empty strip.

The drawer's UIKit hosting boundary (`DrawerNavigationHost` in `DrawerNavigationLayout.swift`) doesn't contribute:

- It changes the navigation controller's layout margins and additional safe-area insets only while the drawer is displaced. When the drawer is closed, it restores the originals.
- With the wrapper in place and unchanged, moving the bottom spacing inside the scroll view extends the scroll view to y 874 on every tab.

### Code

- `GamblerScoreList`, `PendingBetList`, `FinishedBetList`: the bottom spacing moved inside the scroll view as the library's `contentInsets` (`bottom: boxSpacing.medium`). The loading, empty, error, and append content live inside the same `LazyVStack`, so the placeholders get the same treatment. They are unchanged production rows with the shared shimmer.
- `GamblerScoreListView`, `PendingBetListView`, `FinishedBetListView`: `.padding(.vertical, …)` became `.padding(.top, …)`, with a comment explaining why the top stays outside.
- No tab arrangement, tab-bar modifier, bar material, glass, or drawer code changed.

**Why the top padding stays outside.** A first version also moved the top spacing inside, so rows ran beneath the navigation bar too. On iOS 26.5 this showed that the navigation bar's large title never collapses on the History and Bets tabs; only Scores collapses.

- The navigation bar stayed 106 pt tall with the large title at y 120 across three 150 pt drags.
- The same happens on the pre-change build (the change temporarily reverted and rebuilt): the History title stays at y 120.
- With the top inside, History rows scrolled beneath the uncollapsed large title and overlapped it.

The top spacing therefore stays where it was, and the top edge behaves as before. See Open questions.

### Results

| Check | iPhone 26.5 | iPhone 18.1 | iPad 27.0 | iPad 18.1 |
| --- | --- | --- | --- | --- |
| Scroll view reaches the bottom edge (frame maxY, before → after) | 783 → 874 on Scores, Bets, History | → 874 (tab bar at 791) | Full screen (top tabs): → 1180. 375 pt window (bottom tab bar at y 1103, 77 pt): → 1180 | Full screen (top tabs): → 1180 |
| Rows pass behind the tab bar | Yes, with the native scroll-edge fade (light and dark) | Yes, behind the standard translucent bar | 375 pt window: yes. Full screen: rows run to the screen edge under the home indicator | Rows run to the screen edge |
| Loading placeholders behind the bar | Yes (captured during load) | Captured during load at a light shimmer phase | Not captured separately | Not captured separately |
| Last row reachable at the scroll limit | Scores: row 13 fully above the bar, with its divider above the bar's top edge | Same | Scores in the 375 pt window: row 13 above the bar | Scores: row 13 above the bottom edge |
| Tab bar legible over content | Light and dark: the glass bar's labels stay legible over rows | Light: the standard material bar | Light | Light |
| Tab arrangement | Scores, Bets, History unchanged; bar frame 402 × 83 pt throughout | Same | Top tabs 86.5 × 36 pt; bottom bar in the window | Top tabs |
| Top edge (unchanged) | Scroll view starts below the navigation bar as before | Same | Same | Same |

- The Bets tab in this pool has no pending predictions ("Nothing to show"). Its empty state sits inside the same scroll view, and its scroll view frame measured the same as the other tabs.
- History has pinned date headers with an opaque background. They still pin at the scroll view's top, as before.
- On iPad 27 the 375 pt window was made by dragging the window's corner in portrait, then restored to full screen (820 × 1180 pt at the origin). Landscape was also probed; the app filled the screen there (1180 pt), and the scroll view extended to the bottom edge.

### Drawer and tests

| Check | Command | Result |
| --- | --- | --- |
| Build | `xcodebuild build-for-testing -workspace Tyche.xcworkspace -scheme Tyche -destination 'generic/platform=iOS Simulator'` (scratch DerivedData) | Succeeded |
| `DrawerPassUITests`, iOS 26.5 | `TEST_RUNNER_DRAWER_UI_PASS=1 xcodebuild test-without-building … -only-testing:TycheUITests/DrawerPassUITests` | 5 of 5 passed, including `testPoolHomeDrawerGestures`: tab tap, a finger sliding along the tab bar switches tabs without opening the drawer, and the leaderboard scrolls |
| `DrawerPassUITests`, iOS 18.1 | Same | 3 of 5 passed in the first run. `testDrawerNavigationLayout` failed with the known pre-existing "avatar shifted within the pushed screen" (−324 vs 16). `testPoolHomeDrawer` failed once with "share sheet did not dismiss" and passed when rerun alone; 5.1 saw the same one-off failure. `testPoolHomeDrawerGestures` passed |
| Unit tests, iOS 18.1 | `xcodebuild test-without-building … -skip-testing:TycheUITests` | `** TEST EXECUTE SUCCEEDED **`, no failures, including `DrawerTabBarBandTests`, `DrawerInteractionTests`, and `DrawerRevealTests` |
| `Package.resolved` | Hashes before and after | Unchanged |

`DrawerPassUITests` were not run on iPad; their known iPad failures are recorded in 2.4.

### Captures

- `verification/2.6-scores-before-after-ios26.5-light-dark.png`: Scores mid-scroll, left to right: before light, after light, before dark, after dark. Before, rows stop above the bar. After, they pass behind it.
- `verification/2.6-scroll-limit-history-placeholders-ios26.5-light-dark.png`: Scores at the scroll limit (light, dark), History mid-scroll (light, dark), and the loading placeholders behind the bar (light).
- `verification/2.6-iphone-ios18.1-light.png`: Scores mid-scroll and at the limit, History mid-scroll and at the limit, and the loading frame.
- `verification/2.6-ipad27-375-window-light.png`: iPad 27 in a 375 pt window: Scores at rest, at the limit, and History.
- `verification/2.6-ipad27-ipad18.1-full-screen-light.png`: full screen with top tabs. The top row is iPad 27 (Scores at the limit, History). The bottom row is iPad 18.1 (Scores at the limit, History at the end).

The captures show account data.

### Other scroll views

- **My pools** (`PoolScoreListView`): there is no tab bar, and no padding sits outside its scroll view.
- **Pushed destinations with the same outer-padding pattern:** `ManageGamblersListView` and `MatchBetListView` use `.padding(.vertical, …)`, and `BetTimelineListView` uses `.padding(boxSpacing.medium)`. They have no tab bar, so their lists stop short of the home-indicator edge and the navigation bar. That is outside 2.6's scope, so they were not changed.

### Simulator settings

- The iPhone 26.5 appearance was switched to dark for the dark runs and restored to light.
- The iPad 27 window was resized to 375 pt and restored to full screen. Its orientation was returned to portrait after the landscape run.
- All four simulators run the current debug build with this change, remain signed in, and were shut down afterwards; none was booted at the start.
- The scratch DerivedData was deleted, and the throwaway test was removed from the repo.

### Not verified

- Dark appearance on the iPhone 18.1 and both iPads (only light was run there). The same code path is covered in dark on iPhone 26.5.
- Loading placeholders on the iPads: they are in the same scroll view with the same insets, but the loading frames weren't captured there.
- The Bets tab with pending predictions: the probed pool had none, and saving predictions isn't allowed on Prod.
- Increase Contrast, Reduce Transparency, and large text over the tab bar: not rerun for this change.
- VoiceOver: no semantics changed.

### Open questions

- **History and Bets large title doesn't collapse (pre-existing).** On iOS 26.5, pool home's navigation bar collapses its large title only while Scores is scrolled; on History and Bets it stays large, on both the pre-change and changed builds. The tabs share one navigation bar, because the `TabView` sits inside the drawer host's `NavigationStack`. The likely cause is that the navigation bar tracks only one tab's scroll view. Letting the lists also run beneath the navigation bar needs that fixed first, for example with a navigation stack per tab. That would change navigation structure, so it is left for a decision.

Accepted without dark appearance on iPhone 18.1 and the iPads, loading placeholders on iPad, the Bets tab with pending predictions, Increase Contrast, Reduce Transparency, large text over the bar, and VoiceOver for task 2.6 by the user on 2026-09-29.

## iOS — task 2.7 (2026-09-29)

Toolchain: `xcode-select` pointed at Xcode 26.0.1 (17A400) when the measurements began and at Xcode 26.5 (17F42) for the later builds; the change was outside this task. The fix was built and every result below after "Fix" ran on Xcode 26.5.

Simulators, all signed in to Prod and used read-only. Nobody was signed out.

- iPhone 17 on iOS 26.5
- iPhone 16 Pro on iOS 18.1
- iPad Air 11-inch (M4) on iOS 27.0, full screen, with tabs at the top
- iPad Air 11-inch (M2) on iOS 18.1, full screen

### How drags were measured

A throwaway XCUITest (not committed) opened "Copa Mundial de la FIFA 2026" (pool home, Scores), or stayed on My pools, and synthesized single-finger drags. Each drag was 180 or 200 pt at 500 pt/s, with 60 samples a second, starting to move at touch-down the way a finger does. The angle is measured from vertical, and the finger always moved upward. R leans toward the trailing edge, and L toward the leading edge.

After each drag, the test recorded one of three outcomes:

- **SCROLL**: a row moved.
- **DRAWER**: Invite or Profile became hittable. The test closed the drawer again.
- **NONE**: neither happened.

The list was dragged back to the top between drags, and every drag started with row 1 at the same position.

- My pools was measured at AX XXXL text (launch argument) so its three pools overflow the screen.
- Both iPads used fixed pauses instead of XCUITest's idle wait.
- XCUITest's own `press(forDuration:thenDragTo:)` was not used for the sweeps. It rests on the finger for 50 ms before moving, and those drags scrolled reliably even before the fix, so they don't reproduce the report.

### Cause

**Before, iPhone 26.5, pool home (two runs):**

| Angle | 0° | 10° R/L | 20° R/L | 30° R/L | 40° R/L | 50° R/L | 60° R/L |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Run 1 | NONE | SCROLL / SCROLL | SCROLL / NONE | NONE / NONE | NONE / SCROLL | DRAWER / NONE | DRAWER / NONE |
| Run 2 | NONE | NONE / SCROLL | NONE / SCROLL | SCROLL / NONE | SCROLL / SCROLL | DRAWER (the list also moved) / SCROLL | DRAWER / NONE |

**Before, iPhone 18.1:** 0°, 10°, and 20° were NONE in both directions. 30° R and 40° R scrolled; 30° L and 40° L did not. From 50° R the drawer opened.

About half of the drags failed to scroll, even perfectly vertical ones. That matches the user's report.

**The drawer's drag causes it.** On iOS 18.1 the drawer's drag was temporarily detached: its `highPriorityGesture` mask was forced to `.subviews`, with nothing else changed, then reverted. With the drag detached, every drag from 0° to 60°, in both directions, scrolled.

**Mechanism.** The drawer followed drags with a SwiftUI `DragGesture` (minimum distance 20 pt) attached as a `highPriorityGesture` over the whole container.

- That gesture is recognized once the finger has travelled 20 pt, whatever the direction. Its "decline" of a vertical drag is only a decision in the reveal model.
- When a finger moves from touch-down, the scroll view's pan often hasn't begun by the time the drawer's drag is recognized. That pan then fails for the rest of the touch.
- Detaching the drawer's gesture afterwards (`including: .subviews`) comes too late to hand the touch back.
- The 45° direction rule (`abs(width) > abs(height)`) was a second, smaller problem: a drag at 50° from vertical both scrolled and opened the drawer.

**The drawer's UIKit wrapper plays no part.** `DrawerNavigationHost` in `DrawerNavigationLayout.swift` was unchanged in the detached-drag experiment, and every drag scrolled there. It only adjusts navigation margins and insets while the drawer is displaced.

### Code (`iOS/UI`)

- **`DrawerReveal`** now has two separate rules:
  - `takesDrag` decides whether the drawer's gesture takes the touch at all. It takes only a drag within 30° of horizontal (`maximumDragAngle`). While the drawer rests closed, it also leaves a drag that starts on the host's reserved tab-bar band to the bar.
  - `claimsDrag` decides whether the reveal follows a drag it took. While the drawer rests closed, it doesn't follow a drag toward the leading edge.
  - `updateDrag` makes its first-update decision through `claimsDrag`, so the direction is still locked once per drag.
- **`DrawerPanGesture`** (new, iOS 18 and later, `UIGestureRecognizerRepresentable`): a `UIPanGestureRecognizer` follows the drawer's drag.
  - It begins only for a drag the drawer takes (`gestureRecognizerShouldBegin`). Otherwise it fails at once.
  - Scroll views' pan recognizers are required to wait for it to fail. So a drag the drawer declines scrolls from its first movement, and a drag the drawer takes never scrolls.
  - Once it begins, UIKit cancels the touch for the control underneath. A leading-edge swipe over a closed drawer is still taken but not followed, so it doesn't activate the row it crosses, as before.
  - Translation is measured from the touch-down location in window coordinates, because the pan's own translation leaves out part of the distance travelled before it began. Release uses a scroll view's normal deceleration to project where the drag would end.
  - `isEnabled` follows `allowsDragging`, so no drawer drag starts once a destination is open.
- **`DrawerView`**: the handler takes a `DrawerDragValue`, and `DrawerDragGesture` attaches `DrawerPanGesture` on iOS 18+. Below iOS 18 it keeps the SwiftUI drag, now with the 30° rule.
- Reveal, animation, Reduce Motion, focus, dismissal, the tab-bar band, and the navigation host are unchanged.

A first version began the recognizer only for drags the reveal would follow. A leading-edge horizontal swipe over a closed drawer then reached the row and opened that gambler's Timeline. The pre-change build, rebuilt temporarily, didn't do that (NONE at 75° L and 90° L). This led to separating `takesDrag` from `claimsDrag`.

### Results after the fix

Pool home, Scores, synthesized drags at 500 pt/s:

| Device | 0°–55°, R and L | 62°–65° R | 75°–90° R | 65°–90° L (drawer closed) |
| --- | --- | --- | --- | --- |
| iPhone 26.5 | SCROLL at 0, 10, 20, 30, 40, 50, 55 | DRAWER | DRAWER | NONE (no scroll, no row opened) |
| iPhone 18.1 | SCROLL at 0, 10, 20, 30, 40, 50, 55 | DRAWER | DRAWER | NONE |
| iPad 27.0 | SCROLL at 0, 20, 40, 55 | DRAWER | DRAWER 7 of 8 | NONE |
| iPad 18.1 | SCROLL at 0, 40, 55 | DRAWER | DRAWER | NONE |

My pools at AX XXXL, 500 pt/s:

| Device | 0°, 20°, 40°, 55° (R and L) | 65° R, 90° R | 65° L, 90° L |
| --- | --- | --- | --- |
| iPhone 26.5 | SCROLL | DRAWER | NONE |
| iPhone 18.1 | SCROLL | DRAWER | NONE |

- No drag both scrolled and opened the drawer.
- The boundary sits between 55° and 62° from vertical, as the 30°-from-horizontal rule intends.
- A 30° drag at 1500 pt/s scrolled on iPhone 26.5 in both directions. That run's 0° measurement was lost because the list hadn't loaded yet, and the run was stopped afterwards: fast flicks slow the probe down.
- **iPad 27.0, one miss.** One 90° R drag, right after a 65° R drag had opened and closed the drawer, did nothing. The same sequence repeated twice and 90° R on its own (8 drags) all opened the drawer, so the miss was not reproduced.

### Tests

| Check | Command | Result |
| --- | --- | --- |
| Build | `xcodebuild build-for-testing -workspace Tyche.xcworkspace -scheme Tyche -destination 'generic/platform=iOS Simulator'` (scratch DerivedData) | Succeeded |
| UI package tests, iOS 18.1 | `xcodebuild test-without-building … -only-testing:UITests` | 77 tests in 13 suites passed (Swift Testing) |
| UI package tests, iOS 26.5 | Same | 77 passed |
| `DrawerPassUITests`, iOS 26.5 | `TEST_RUNNER_DRAWER_UI_PASS=1 … -only-testing:TycheUITests/DrawerPassUITests` | 5 of 5 passed |
| `DrawerPassUITests`, iOS 26.5 with Reduce Motion | `testPoolHomeDrawerGestures`, `testPoolListDrawerGestures` (`ReduceMotionEnabled` 0 → 1 → 0) | 2 of 2 passed |
| `DrawerPassUITests`, iOS 18.1 | Same as the 26.5 run | 3 of 5 passed in the first run. `testDrawerNavigationLayout` failed with the known pre-existing −324 vs 16 result. `testPoolHomeDrawer` failed with "share sheet did not dismiss" and passed when rerun alone; that is the same first-run failure seen in 2.6 and 5.1 |
| `Package.resolved` | Hashes before and after | Unchanged |

**Test changes.**

- New `DrawerDragDirectionTests` (Swift Testing, in `DrawerRevealTests.swift`):
  - The closed drawer opens for drags at 0–29° from horizontal.
  - Drags at 31–90° are not taken, open or closed, in either direction.
  - A leading drag over a closed drawer is taken but not followed.
  - The open drawer follows drags in both directions.
  - The reserved bar keeps its drags.
- `DrawerInteractionTests.DragWiring` now drives the container's handler and checks the attached modifier's `isEnabled`. The old tests inspected the SwiftUI `highPriorityGesture`, which iOS 18+ no longer attaches. They cover the same decisions, plus a new test for a 35° drag (declined) and a 25° drag (taken).

The drawer tests cover these contract points: horizontal drags open and close from rows, from Profile and Invite, and from the pushed strip; a recognized drag doesn't activate its row; the tab bar's tap and slide keep their behavior; the leaderboard scrolls; Profile's and a gambler's edge and content-area back-swipes return without opening the drawer.

### Simulator settings

- Reduce Motion on the iPhone 26.5 was turned on for one run and restored to off.
- No appearance, text-size, or window changes. AX XXXL text came from launch arguments only.
- All four simulators have the fixed build installed, remain signed in, and are shut down.
- The scratch DerivedData was deleted, and the throwaway test and the temporary A/B edits were removed.

### Not verified

- **iOS 16 and 17:** those runtimes aren't installed. Below iOS 18 the SwiftUI drag remains, with the stricter 30° rule. Whether a scroll view's pan is also held back there wasn't measured.
- **A physical device and a real finger:** the sweeps used synthesized straight drags. Curved thumb paths weren't synthesized.
- **Downward drags:** only upward finger drags were measured.
- **iPad in a compact floating window, and iPad `DrawerPassUITests`:** both have the known iPad failures recorded in 2.4.
- **Horizontal controls that handle their own drags:** no drawer host contains one. The recognizer doesn't yet leave a horizontally scrolling scroll view to itself.

The user confirmed on 2026-09-29 that scrolling now feels right. Accepted without the iOS 16/17 fallback, downward drags, iPad DrawerPassUITests and the compact floating window, horizontally scrolling controls, and the single iPad 27 missed horizontal drag for task 2.7 by the user on 2026-09-29.

## iOS — task 2.8 (2026-09-29)

Toolchain: Xcode 26.5 (17F42), selected with `xcode-select`. The simulators were all signed in to Prod until the storage incident described at the end of this section. Prod was used read-only: destinations were opened and left with Back, and the drawer was only opened and closed.

The runtime checks used throwaway XCUITests, which were not committed. On both iPads they used fixed pauses instead of XCUITest's idle wait.

### Hypothesis check

I built a variant of the pre-change tree that starts on History (`selectedTab` initialised to `.historyBet`, then reverted), on iPhone 26.5.

- History's large title collapsed: bar 54 pt, title at y 67.7.
- Scores' bar then stayed at whatever History had left it.
- In the unchanged build, only Scores collapses: History and Bets stay at 106 pt (2.6).

The shared navigation bar follows the scroll view of the tab it showed first, not the selected one. Pool home's `TabView` sat inside the drawer host's single `NavigationStack`. That is a tab bar controller inside a navigation controller, a combination UIKit doesn't re-target when the selected tab changes.

### Choice

**Chosen: one `NavigationStack` per tab, with the `TabView` at pool home's root.** That is the platform's standard structure, and each tab's bar then follows its own list.

**Rejected:**
- **iOS 26 scroll-edge APIs** (`scrollEdgeEffectStyle` and related): they style the edge effect but don't change which scroll view the navigation bar observes.
- **A UIKit bridge that calls `setContentScrollView(_:for:)` on tab changes:** it would have to reach into SwiftUI's hosting controllers.
- **Nested stacks inside the shared stack:** SwiftUI doesn't support them.

### Code

**`PoolHomeView`**
- Now generic over a `Destinations` view modifier, and takes the host's `path` binding.
- Each tab is a `NavigationStack` carrying:
  - its own title (Scores / Bets / History, unchanged);
  - the same toolbar: the avatar in a `PlainToolbarItem` with `drawerOpener()`, and the change-pool button;
  - the destinations;
  - `drawerTabBarBoundary()`.
- The selected tab's stack is bound to `DrawerHostNavigation.path`; the others get a constant empty path. The tab bar is hidden while a destination is shown, so tabs only change at the root, and the path is empty then.
- Tab count, order, labels, icons, and `excludesTabBarFromDrawerDrags()` are unchanged.

**`PoolHomeRouter`**
- The router no longer wraps pool home in a `NavigationStack`.
- The five `navigationDestination`s (timeline, match bets, manage gamblers, profile, username editor) moved into a `PoolHomeDestinations` modifier. Each destination hides the tab bar (`.toolbar(.hidden, for: .tabBar)`), so it covers the tabs as before.
- Routing still goes through `DrawerHostNavigation` (`open`, `openFromDrawer`, the path, `isHostVisible` → `allowsDragging`), so no drawer drag starts once a destination is open.

**`DrawerNavigationLayout`** (UI package)
- The stabilisation searches the host's controller hierarchy for the first navigation controller. Inside a `UITabBarController` it now searches only the selected tab. Nothing else changed.
- Stabilisation is still needed and still applies, now to the selected tab's stack.

**Tab lists** (`GamblerScoreList`, `PendingBetList`, `FinishedBetList` and their views)
- The top spacing moved inside the scroll view (`contentInsets.top`), next to the bottom spacing from 2.6. The outer `.padding(.top, …)` and its comment are gone.

### Results

**iPhone 26.5, light and dark.** After three drags of the list, each tab's bar collapses to 54 pt with an inline centred title at y 73.7: "Scores" 55 pt wide, "Bets" 36 pt, "History" 58 pt. Before, History and Bets stayed at 106 pt.

- Rows pass beneath the navigation bar's scroll-edge effect, as they do beneath the floating tab bar, and don't overlap the title.
- History's pinned date header pins just below the bar.
- The tab lists' scroll views span the whole screen (0–874 pt), so 2.6's rows beneath the tab bar still hold.
- Bets in "Copa Mundial de la FIFA 2026" shows the empty state, and its bar now collapses too. The one pool with a pending prediction ("Copa Mundial de la FIFA prur") has a single row, which doesn't scroll even at AX XXXL, so no Bets rows could be shown beneath the bar.
- **Collapsed Scores title (reported at y 67 but not shown inline): fixed as a side effect.** The title is now the inline title.

**iPhone 18.1, light.** All three tabs collapse to the standard 44 pt bar with inline titles, and rows pass beneath the translucent bar.

**iPad 27.0 and iPad 18.1, full screen.**
- Visible change: with the `TabView` at the root, iPadOS shows the regular-width tabs in the navigation bar row, beside the avatar and change-pool button (y 36 on iPad 27, y 33 on iPad 18.1). Before, they sat below the large title (y 142 / 135).
- Each tab's large title collapses with its own list. The bar is 54 pt on iPad 27 and 64 pt on iPad 18.1. No inline title is shown, because the tabs occupy the centre of the bar.
- Tab count, order, and labels are unchanged.

**Destinations, Back, drawer.** Throwaway probe on iPhone 26.5, iPhone 18.1, and iPad 27.0:

| Check | iPhone 26.5 | iPhone 18.1 | iPad 27.0 |
| --- | --- | --- | --- |
| Drawer opened on History; avatar keeps its place within the pushed bar (offset closed / open) | 22 / 22 pt | 16 / −324 pt; the capture shows the avatar moving with the screen. This is the known iOS 18.1 XCUITest frame quirk, the same as `testDrawerNavigationLayout` there | 16 / 16 pt |
| Profile from the drawer on History: tabs hidden; the edge back-swipe returns to History with the drawer closed | Pass | Pass | Pass |
| A match from History: tabs hidden, title "Score"; the back button returns to History | Pass | Pass | Pass |
| A gambler from Scores: Timeline, tabs hidden; the edge back-swipe returns to Scores with the drawer closed | Pass | Pass | Pass (tabs hidden in the capture) |
| The drawer reopens on the root afterwards | Pass | Pass | Pass |

The iPad 18.1 destination check didn't run; see the storage incident below.

### Tests

| Check | Result |
| --- | --- |
| Build (`build-for-testing`, workspace, scheme Tyche, generic iOS Simulator) | Succeeded |
| `DrawerPassUITests`, iPhone 26.5 (`TEST_RUNNER_DRAWER_UI_PASS=1`) | 5 of 5 passed, including `testDrawerNavigationLayout` for the pool-home host in three orientations |
| `DrawerPassUITests`, iPhone 18.1 | 3 of 5 passed in the first run. `testDrawerNavigationLayout` failed with the known pre-existing −324 vs 16. `testPoolHomeDrawer` failed with "share sheet did not dismiss" and passed when rerun alone, the same first-run failure as in 2.6 and 2.7 |
| 2.7 angle sweep, iPhone 26.5, pool home | 0°, 30° L/R, and 55° L/R scrolled. 65° R and 90° R opened the drawer. 65° L and 90° L: nothing, with no row opened |
| UI package and app unit tests after this change | Not run (storage incident) |
| `Package.resolved` | Unchanged |

### Captures

- `verification/2.8-top-bar-before-after-ios26.5-light.png`, left to right:
  - Before: Scores, Bets (empty), History, captured by 2.6's final run on the same tree without 2.8.
  - After: Scores, Bets, History.
- `verification/2.8-top-bar-before-after-ios26.5-dark.png`, left to right:
  - Before: Scores, History.
  - After: Scores, Bets, History.
  - Bets had no dark capture before. It shows the same empty state there.

### Simulator storage incident

At about 18:27, while the iPad 18.1 checks were running, the device folders under `~/Library/Developer/CoreSimulator/Devices/` disappeared for all four simulators. This task didn't do it: its commands only booted, shut down, and ran tests on simulators, and it deletes no files outside its scratch folder.

What followed:
- The next two installs of the test runner on the iPad 18.1 failed ("Unable to Install TycheUITests-Runner", `createTemporaryDirectoryInDirectoryURL`).
- A reboot and retry stalled.
- Booting the iPhone 26.5 then failed: "cannot be located on disk".
- `simctl` still lists all four devices. My later boot attempts left new, near-empty folders for the iPhone 26.5 and the iPad 18.1.

The signed-in state and the installed builds on those simulators are gone. I stopped using simulators there and did not erase, recreate, or repair any of them.

### Simulator settings

- iPhone 26.5 appearance was switched to dark for one run and restored to light, before the incident.
- No other settings changed.
- The scratch DerivedData was deleted, and the throwaway tests were removed.

### Not verified

- **UI package and app unit tests after this change** (including the `DrawerNavigationLayout` edit): stopped by the storage incident.
- **iPad 18.1:** destinations and Back.
- **iPad 27 in a 375 pt window** (bottom tabs).
- **iPad 18.1 in dark.**
- **Bets rows beneath the bars:** no pool has enough pending predictions to scroll.
- **The tab-bar drag band on iPad in regular width:** the tabs now sit in the navigation bar row, so where the band is measured changed. Tab taps worked, but horizontal drags starting on the top tabs weren't checked.

### Follow-up: simulators erased and unit tests (2026-09-29)

**User decisions**

- The user accepted the iPad regular-width tabs in the navigation bar row (the iPadOS standard) for this task.
- At the user's direction, `xcrun simctl erase` was run on all four simulators, and each erase succeeded:
  - iPhone 26.5: `FC9C4A7E…`
  - iPhone 18.1: `A2F5EF6F…`
  - iPad 27.0: `838A5F55…`
  - iPad 18.1: `F45A74E8…`
- None of the four was deleted or recreated.

**Reinstalled build**

- A fresh `build-for-testing` (Xcode 26.5, scratch DerivedData) succeeded.
- The resulting debug `Tyche.app` was installed on all four simulators.
- The iPhone 26.5, iPad 18.1, and iPad 27.0 were booted with the app on its welcome screen, signed out, for the user to sign in.
- The first launch on the iPad 18.1 timed out while the erased simulator finished its first boot. A later launch succeeded.

**Unit tests** (iPhone 18.1, no sign-in needed)

| Check | Command | Result |
| --- | --- | --- |
| UI package and app unit tests | `xcodebuild test-without-building -workspace Tyche.xcworkspace -scheme Tyche -destination 'platform=iOS Simulator,id=<iPhone 16 Pro iOS 18.1>' -parallel-testing-enabled NO -only-testing:UITests -only-testing:TycheTests` | `** TEST EXECUTE SUCCEEDED **`. UITests: 77 tests in 13 suites passed, including the drawer suites. TycheTests: 46 Swift Testing tests in 8 suites and the XCTest cases all passed, with 0 failures |

This covers the `DrawerNavigationLayout` change.

**Not run yet**

`DrawerPassUITests` need a signed-in session with pools, so they were not rerun after the erase. Their results from before the storage incident are in the 2.8 table above.

### Follow-up: signed-in checks after the erase (2026-09-29)

Toolchain: Xcode 26.5 (17F42), using the same scratch build as the previous follow-up, rebuilt for the probes. The runtime checks used throwaway XCUITests, which were not committed. They ran one simulator at a time, with fixed pauses instead of XCUITest's idle wait on the iPads. Prod was read-only: destinations were opened and left with Back or back-swipes, and nothing was saved or joined.

**iPad 18.1: destinations and Back** (signed in, full screen, light)

| Check | Result |
| --- | --- |
| Drawer opened on History | Opens. The avatar moves with the pushed screen in the capture. XCUITest reports −320 pt, the known iOS 18.1 untransformed-frame quirk |
| Profile from the drawer on History | Tabs hidden. The Back button reads "History". The edge back-swipe returns with the drawer closed |
| A match from History | Tabs hidden, title "Score". Back returns |
| A gambler from Scores | Timeline opens with tabs hidden and a "Scores" Back button. The edge back-swipe returns with the drawer closed |
| The drawer reopens on the root | Pass |

After the Profile Back, History's rows were on screen (the probe found and opened a History match row). The regular-width top tabs don't report `isSelected` to XCUITest on this runtime, so the probe's "selected" readings are false here. Capture: `verification/2.8-ipad18.1-destinations-light.png`.

**iPad 18.1: dark**

- Scores and History each collapse their own title to a 64 pt bar, with rows passing beneath it.
- The tab lists span the full 1180 pt height.
- Appearance was set to dark for the run and restored to light (confirmed light before and after).
- Capture: `verification/2.8-ipad18.1-tabs-dark.png`.

**Horizontal drags starting on the iPad top tabs** (iPad 18.1, full screen)

Synthesized 200 pt drags started on the Bets tab:

| Drag | Drawer | Selected tab after the drag |
| --- | --- | --- |
| 90° R (toward the trailing edge) | Did not open | History |
| 80° R | Did not open | History |
| 90° L | Did not open | Scores |
| 80° L | Did not open | Scores |

The top tab control keeps its own sliding selection. Capture: `verification/2.8-ipad18.1-top-tab-drags.png`.

**Blocked: sign-in missing**

- **iPad 27.0:** the run stopped at "Open menu" not found. A screenshot then showed the welcome screen: signed out.
- **iPhone 26.5:** `DrawerPassUITests` failed all 5 at their first step ("pool list did not appear" / "no pools to open"): signed out. The simulator's home screen shows Fortuna installed.
- **iPhone 18.1:** the app couldn't be launched by `simctl` ("denied by service delegate"). Its home-screen icon shows the placeholder of a never-launched app.

Before these runs, I shut down the iPhone 26.5 and the iPad 27.0 with `simctl shutdown`, one simulator at a time, while the iPad 18.1 checks ran. The iPad 18.1 was never shut down between the sign-in and its checks, and it stayed signed in through several test installs. I can't tell whether that shutdown lost the other two sessions or whether they were not signed in; this was not investigated further. No sign-in or sign-out was attempted.

**Package.resolved:** unchanged against the hashes recorded before this follow-up's build.

**Not verified yet** (blocked on sign-in)

- `DrawerPassUITests` on iPhone 26.5 and iPhone 18.1 after the erase. The runs before the storage incident are in the 2.8 table.
- iPad 27.0 in the 375 pt window.
- Top-tab drags on iPad 27.0. They were exercised on iPad 18.1 only.

### Follow-up: remaining checks after the user signed in again (2026-09-29)

Toolchain: Xcode 26.5 (17F42), same scratch build as the previous follow-up. Before each device's run I checked its sign-in state from a screenshot. No simulator was shut down, rebooted, or erased, and no sign-in or sign-out was attempted.

**Sign-in state**

- iPhone 26.5: signed in (My pools shown).
- iPad 27.0: signed in (My pools shown).
- iPhone 18.1: signed out. Its home screen showed the never-opened app icon. Launching the app opened the welcome screen, so its checks were not run.

**`DrawerPassUITests`, iPhone 26.5** (`TEST_RUNNER_DRAWER_UI_PASS=1`, after the erase): 5 of 5 passed.

- `testDrawerNavigationLayout`
- `testPoolHomeDrawer`
- `testPoolHomeDrawerGestures`
- `testPoolListDrawer`
- `testPoolListDrawerGestures`

**iPad 27.0 in a 375 pt window** (portrait, light)

The window corner was dragged to 375 pt; the window then sat at x 223. In the window:

- Pool home shows the bottom tab bar (375 × 77 pt at y 1103).
- The tab lists span the full 1180 pt height.
- Scores and History each collapse their own title to an inline title (54 pt bar), and rows pass beneath both bars.
- Capture: `verification/2.8-ipad27-375-window-light.png`.

The destinations probe in the window got as far as Profile, which opened with the tabs hidden. Two of its readings are artifacts of the floating window's coordinates, not of this change. The same two readings are recorded in 2.4 against the pre-change build:

- The avatar offset read 84 → 16 pt.
- The probe's edge back-swipe, aimed at screen-normalized x 0.005, lands outside the window, so it didn't go back.

The window was then restored to full screen (820 × 1180 pt at the origin). Orientation stayed portrait throughout, and appearance stayed light.

**Top-tab drags, iPad 27.0, full screen**

Synthesized 200 pt drags started on the Bets tab:

| Drag | Drawer | Selected tab after the drag |
| --- | --- | --- |
| 90° R | Did not open | Bets |
| 90° L | Did not open | Scores |
| 80° R | Did not open | History |
| 80° L | Did not open | Scores |

As on the iPad 18.1, the tab control keeps its drags.

**Cleanup**

- `Package.resolved`: unchanged.
- The scratch DerivedData was deleted.
- The throwaway tests are not in the repo.
- iPhone 26.5, iPhone 18.1, and iPad 27.0 were left booted. The iPad 18.1 is shut down, still signed in, since the previous follow-up.

**Not verified**

- `DrawerPassUITests` on iPhone 18.1 after the erase: the app there is signed out. Before the storage incident, the 2.8 run on iPhone 18.1 had only the known failures.
- Edge back-swipe inside the iPad floating 375 pt window: probe coordinates. The same result was recorded as pre-existing in 2.4.

### Follow-up: `DrawerPassUITests` on iPhone 18.1 after the erase (2026-09-29)

**Sign-in**

A screenshot confirmed the iPhone 16 Pro (iOS 18.1) was signed in, showing My pools with its three pools. No simulator was shut down, rebooted, or erased, and all three stay booted.

**Build**

A fresh `build-for-testing` on Xcode 26.5 (17F42), using scratch DerivedData, succeeded.

**Results**

`TEST_RUNNER_DRAWER_UI_PASS=1 xcodebuild test-without-building … -only-testing:TycheUITests/DrawerPassUITests`:

| Test | Result |
| --- | --- |
| `testPoolHomeDrawerGestures` | Passed |
| `testPoolListDrawer` | Passed |
| `testPoolListDrawerGestures` | Passed |
| `testDrawerNavigationLayout` | Failed with the known pre-existing "avatar shifted within the pushed screen" (−324 vs 16) |
| `testPoolHomeDrawer` | Failed with "share sheet did not dismiss", then passed when rerun alone. This is the same first-run failure seen in 2.6, 2.7, and 2.8 |

The run reported "Executed 5 tests, with 2 failures" and then hung while finishing. The bounded timeout ended it after all five results were in.

**Cleanup**

- The scratch DerivedData was deleted.
- `Package.resolved` is unchanged.
- No throwaway tests are in the repo.

The user accepted the iPad tab placement in the navigation bar row for task 2.8. Accepted without Bets rows scrolling beneath the bars (no pool has enough pending predictions) and the edge back-swipe inside the iPad floating 375 pt window for task 2.8 by the user on 2026-09-29.

## iOS — task 2.9 (2026-09-29)

Toolchain: Xcode 26.5 (17F42), scratch DerivedData (deleted afterwards). Simulators: iPhone 17 on iOS 26.5 and iPhone 16 Pro on iOS 18.1, both signed in. Sign-in was confirmed from the pool list before each device's runs: a screenshot, or `DrawerPassUITests` reaching the pools. No simulator was shut down, rebooted, or erased, and all three stay booted. Prod was read-only.

The runtime checks used a throwaway XCUITest (not committed).

### Code

`iOS/UI/Sources/UI/DrawerPanGesture.swift`:

- `gestureRecognizer(_:shouldReceive:)` records the drag's start point only for a touch that begins a drag, which is the 5.3 refresh finding 2.
- The rule is the new `DrawerPanStart.isFirstTouch(state:trackedTouches:)`: the recognizer is `.possible` and follows no touch yet.
- It is a plain enum outside the iOS 18-only recognizer, so it can be unit-tested on every runtime.
- The delegate still accepts every touch. Nothing else changed.

### Tests

**New unit test.** `DrawerPanStartTests` (Swift Testing, in `DrawerRevealTests.swift`) checks three cases:

- A touch while the recognizer is `.possible` with no touches sets the start.
- A second touch during `.began` or `.changed` doesn't.
- A second touch before recognition (`.possible`, one touch) doesn't.

**Runs**

| Check | Result |
| --- | --- |
| Build (`build-for-testing`, workspace, scheme Tyche, generic iOS Simulator) | Succeeded |
| UI package tests, iPhone 26.5 (`-only-testing:UITests`) | 80 tests in 14 suites passed, including `DrawerPanStartTests`; `** TEST EXECUTE SUCCEEDED **` |
| UI package tests, iPhone 18.1 | 80 tests in 14 suites passed. xcodebuild then hung while finishing, as in 2.8's 18.1 run, and the bounded timeout ended it |
| `DrawerPassUITests`, iPhone 26.5 (`TEST_RUNNER_DRAWER_UI_PASS=1`) | 5 of 5 passed |
| `DrawerPassUITests`, iPhone 18.1 | 4 of 5 passed. `testDrawerNavigationLayout` failed with the known pre-existing −324 vs 16. The share-sheet failure didn't occur this time |
| Angle sweep, pool home, iPhone 26.5 (synthesized 180 pt drags at 500 pt/s, as in 2.7) | 0°, 30° L/R, and 55° L/R scrolled. 65° R and 90° R opened the drawer. 65° L and 90° L did nothing and opened no row. Same as 2.7 and 2.8 |
| `Package.resolved` | Unchanged |

### Second finger

The probe synthesized a two-finger event on My pools (iPhone 26.5):

- Finger 1 drags right across the middle of the screen, from 25% to 75% of the width.
- Finger 2 touches down at 95% of the width halfway through, and holds until finger 1 lifts.

With the fix, the drawer opened on both two-finger drags, as it did on single-finger drags before and after them.

I also ran an A/B with the guard temporarily removed and rebuilt, then restored. The same sequence also opened the drawer. So this probe doesn't reproduce a jump: either UIKit doesn't offer the second touch to the one-touch pan in this case, or it doesn't move the reveal. The guard stays as a defensive fix for the reviewed code path, and the jump itself is recorded as not reproduced.

### Record correction

The 2.8 Code section's "six `navigationDestination`s" now reads "five", matching the five routes it lists and the code (5.3 refresh finding 3).

### Not verified

- A second finger actually reaching the drawer's pan mid-drag, and the reveal jumping without the guard: not reproduced with synthesized touches, even with the guard removed.
- iOS 16 and 17: the guard is in the iOS 18+ UIKit recognizer only. Below iOS 18 the SwiftUI drag is unchanged.

Accepted without reproducing a second-finger jump (the guard is kept as a precaution) for task 2.9 by the user on 2026-09-29.
