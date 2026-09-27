# Android edge-to-edge verification

## Android — tasks 1.1–1.3 (2026-09-26)

### 1.1 Target SDK and activity configuration

- `targetSdk = projectTargetSdk.toInt()` added to the app's `defaultConfig`, with `projectTargetSdk=36` in `android/gradle.properties` beside the existing compile and min SDK properties. Min SDK stays 28.
- MainActivity keeps `enableEdgeToEdge()` before `super.onCreate` and `isNavigationBarContrastEnforced = false` on API 29+; no code change there.
- `android:windowSoftInputMode="adjustResize"` added to MainActivity in the manifest.
- Merged manifests (`app/build/intermediates/merged_manifest/<variant>/.../AndroidManifest.xml`): prodDebug, prodRelease, localDebug and localRelease all show `minSdkVersion="28"`, `targetSdkVersion="36"` and MainActivity `windowSoftInputMode="adjustResize"`.
- The pre-change builds already installed on the Pixel_10_Pro_XL AVD and the SM-G955F both reported `targetSdk=36` in `dumpsys package`, so AGP was already defaulting the target to the compile SDK. Declaring it explicitly does not change the effective target.
- `./gradlew :app:assembleProdDebug :app:assembleLocalDebug` → success. `./gradlew build` (all modules and variants, including lint and unit tests) → BUILD SUCCESSFUL.

### 1.2 Username editor inset ownership

- Removed the route's runtime soft-input workaround (`ResizeWindowForKeyboard` and its window-attribute helpers).
- Owner: `UsernameEditorScaffold`. Its content modifier is `fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding).fitInside(WindowInsetsRulers.Ime.current)`. `fitInside` and `WindowInsetsRulers` exist in the resolved Compose BOM 2026.05.00 (foundation-layout and ui 1.11.1), so the ruler strategy is used rather than the `imePadding` fallback. The editor's `imePadding()` after `verticalScroll()` was removed, so the column only scrolls.
- Device: SM-G955F, API 28, es-CO, three-button navigation, dark theme, prodDebug installed over the existing signed-in app with `adb install -r`. Short window and large text: `wm size 1080x1500` (the prior override was 1080x2220) and `font_scale 1.3`, restored to `1080x2220` and `1.0` afterwards.
  - Pool list drawer → Perfil → Nombre de usuario: with the keyboard open, the top bar and back arrow stayed in place and the focused field was brought above the keyboard (`verification/1.2-api28-poollist-entry-keyboard-open.png`). Scrolling brought "Guardar nombre de usuario" into view below the helper text and above the keyboard (`…-save-above-keyboard.png`). Typing `zz` and closing the keyboard with Back kept the draft `felipearpazz`, and the content returned to full height with no keyboard-sized gap (`…-keyboard-closed-draft-kept.png`). Toolbar back returned to Perfil with the username still `felipearpa`.
  - Pool home drawer → Perfil → Nombre de usuario: same keyboard layout (`verification/1.2-api28-poolhome-entry-keyboard-open.png`, `…-save-above-keyboard.png`). Tapping Save with the keyboard open saved `felipearpazz` to production and returned to Perfil.
  - **Production write restored:** the username was changed back to `felipearpa` through the same editor, and Perfil showed `felipearpa` afterwards.
- Not covered on a device in this group: API 35 or 36 with the signed-in app (see the incident below), landscape, gesture navigation, and light theme. These belong to group 6.

### 1.3 Regression coverage

- New `app/src/androidTest/.../UsernameEditorKeyboardTest.kt`. Before each test, the test activity is made edge to edge and given MainActivity's manifest soft-input mode, read from `PackageManager`. Content renders at 2× font scale and 1.35× display size so the editor overflows with the keyboard open. The tests cover:
  - MainActivity declares `adjustResize`.
  - With the keyboard open, the back button is displayed, enabled and inside the window. Save can be scrolled to and ends above the keyboard.
  - Save stays below the helper text and moves with the scrolled content.
  - After the keyboard closes and reopens, the draft and a 1–4 selection are preserved. The scrolling viewport ends at the keyboard top while the keyboard is open, and at the navigation bar with no gap after it closes.
  - With the keyboard open, Saving leaves back displayed and disabled, and tapping it does nothing. Failure re-enables back, and tapping it navigates once.
- `UsernameEditorNavigationTest`: removed `theEditorAsksForAResizingWindowAndRestoresTheModeWhenItIsLeft`, which asserted the removed runtime workaround. The other 9 cases are unchanged.
- `ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedProdDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.felipearpa.tyche.UsernameEditorKeyboardTest,com.felipearpa.tyche.UsernameEditorNavigationTest` on Pixel_10_Pro_XL (API 36, en-US, gesture navigation) → 14 tests, 0 failures.
- Negative check: with `fitInside(WindowInsetsRulers.Ime.current)` removed temporarily, 4 of the 5 keyboard tests failed (all except the manifest check). The line was then restored.
- `./gradlew :app:testProdDebugUnitTest` → 60 tests, 0 failures.

### Incident: Pixel_10_Pro_XL AVD lost its signed-in Fortuna install

`connectedProdDebugAndroidTest` uninstalls the app and test APKs after the run, which removed Fortuna and its sign-in from the Pixel_10_Pro_XL AVD. The emulator had been started with `-no-snapshot-save`, but reloading the `default_boot` snapshot failed with "Error -1 from the snapshot callback". The AVD cold-booted without Fortuna. Felipe needs to reinstall Fortuna and sign in on that AVD again. Pixel_8_API_35 and Pixel_7_API_34 were checked and had no Fortuna install, so the on-device editor checks used the SM-G955F.

Accepted without API 35/36 signed-in-app, landscape, gesture-navigation, and light-theme username-editor checks (deferred to group 6) by the user on 2026-09-26.

## Android — tasks 2.1–2.3 (2026-09-26)

### 2.1 List inset owners

Each screen's Material 3 `Scaffold` owns the system insets. Its content padding goes to the scrolling list as `contentPadding`, and `consumeWindowInsets(innerPadding)` sits on the list's container, so nothing below applies the same inset again. Design spacing is added to the content padding and is not consumed.

| Screen | Top and side insets | Bottom inset |
| --- | --- | --- |
| My pools (`PoolScoreListView` → `PoolScoreList`) | List content padding: the list scrolls under the collapsing top app bar | List content padding |
| Manage gamblers (`ManageGamblersView`) | List content padding: the list scrolls under the top app bar | List content padding; the removal-failure banner is padded by the same insets |
| Bet timeline (`BetTimelineListView`) | Fixed username header (`excludingBottom()`) | List content padding (`onlyBottom()`) |
| Match bets (`MatchBetListView`) | Fixed match header (`excludingBottom()`) | List content padding (`onlyBottom()`), or the open-predictions message's scrolling padding |
| Pool templates, creation step one (`StepOneView` → `PoolFromLayoutCreatorList`) | Fixed instruction text (`excludingBottom()`) | List content padding (`onlyBottom()`) |

- `excludingBottom()` and `onlyBottom()` in `ui/PaddingValuesSides.kt` split scaffold padding when a fixed header sits between the app bar and the list. They read the scaffold padding lazily, so the collapsing app bars keep updating layout.
- The pool-creation stepper now passes its padding to each step. Step two applies it unchanged as `Modifier.padding(contentPadding)`, the same bounds it had before. Its keyboard handling belongs to group 4.
- `RefreshableLazyPagingColumn` in `ui/lazy` now pairs the lazy-paging library's `LazyPagingColumn` with its own `PullToRefreshBox`. The pull indicator and the inline refresh indicator start below `contentPadding`'s top edge. The library's refreshable column always pins its pull indicator to the viewport top, which on My pools and Manage gamblers is now behind the top app bar.

### 2.2 Loaded, loading, empty, and error states

- All states render inside the same `LazyColumn` and use the same content padding.
- Full-viewport empty and error items (shared `lazyPagingColumnEmpty`, `lazyPagingColumnError`, My pools' error with Retry, and Manage gamblers' error) now use `ViewportFillingItem`. It fills the padded viewport and centers the message. When the message is taller than the viewport, it scrolls inside the item instead of being clipped. Before this change, My pools' Retry was clipped and unreachable in a short window with large text.
- The match-bet screen's own failure branch (with Retry) and its open-predictions message now use `CenteredScrollableColumn`, the non-list form of the same layout. Before this change, Retry was clipped in a 1080×850 window at font scale 1.6.
- Placeholders remain production rows populated with a placeholder model and the shared `Modifier.shimmer()`. Pending-bet, match-gambler-bet, match-header, and pool-template placeholders exposed their filler text to TalkBack (runs of "X", 100–100 scores). Each placeholder wrapper now applies `clearAndSetSemantics {}`. None of them had a click action. The pool-template placeholder now comes from one shared `PoolFromLayoutCreatorFakeItem`, used by both the creation list and the My pools empty state. My pools and leaderboard placeholders already cleared their semantics.
- The pending-bet placeholder change also reaches the pending-bet tab list, which belongs to group 3. It is covered by the placeholder test below.

### Device checks

The SM-G955F (API 28, es-CO, three-button navigation, dark theme) ran prodDebug, installed with `adb install -r` over the signed-in app. To make lists scroll with real data, the window was overridden with `wm size` to 1080×850, 1080×700 (both wider than tall, so the navigation bar moves to the right edge), and 1080×1600 (navigation bar at the bottom), with `font_scale` 1.3–1.6. Error states used `svc wifi disable` and `svc data disable`. Afterwards the display was restored to the prior 1080×2220 override and font scale 1.0, and Wi-Fi and mobile data were re-enabled.

- **My pools:** the first row starts below the app bar, and at the end the last row is fully revealed while the app bar collapses (`2.1-api28-poolscore-first-row-below-app-bar.png`, `…-last-row-revealed.png`). The pull-to-refresh indicator appears below the app bar (`…-pull-refresh-indicator-below-app-bar.png`). Offline, the error message scrolls to Retry (`2.2-api28-poolscore-error-top.png`, `…-error-retry-reached-by-scrolling.png`). Tapping Retry after reconnecting loaded the list. On a cold start, a screen recording shows the placeholder rows and then the loaded rows at the same inset and divider positions (`2.2-api28-poolscore-loading-then-loaded.png`).
- **Bet timeline:** rows pass under the translucent navigation bar while scrolling, and the last item ends 8 dp (`LocalBoxSpacing.medium`) above it (`2.1-api28-timeline-rows-pass-under-nav-bar.png`, `…-last-item-above-nav-bar.png`). With the navigation bar on the right edge, the first and last items were checked too (`2.1-api28-sidenav-timeline-*.png`).
- **Match bets:** rows pass under the navigation bar below the fixed match header, and the last row is fully revealed (`2.1-api28-matchbets-*.png`). A match opened offline showed the failure state, and in the 1080×850 window its Retry could be scrolled into view (`2.2-api28-matchbets-error-retry-reached-by-scrolling-short-window.png`). Retry after reconnecting loaded the header. The list had failed offline, so the list error appeared below the fixed header, centered above the navigation bar (`2.2-api28-matchbets-list-error-*.png`). Pull-to-refresh recovered it (`…-list-recovered-by-pull-to-refresh.png`).
- **Manage gamblers** (Prueba, owner): in the 1080×700 window the first row scrolls under the app bar and the last (owner) row is fully revealed. Row backgrounds stop at the right-edge navigation bar (`2.1-api28-managegamblers-*.png`). Offline, pull-to-refresh showed the error, which scrolls under the app bar. Pull-to-refresh after reconnecting recovered it (`2.2-api28-managegamblers-*.png`).
- **Pool templates** (creation step one): production has no open templates, so the empty state appeared. It is centered below the fixed instruction and stays centered while the app bar collapses (`2.2-api28-pooltemplates-*.png`). The loaded template list endpoints could not be staged with real data. They are covered by the shared list test below.
- **API 35** (Pixel_8_API_35 emulator, gesture navigation, light theme, landscape with 560 dpi density and font scale 2.0, all restored afterwards): My pools rows pass under the gesture handle, and the last row is fully revealed above it with the app bar collapsed (`2.1-api35-poolscore-*.png`). The emulator was too loaded to be responsive. The second capture has a "Fortuna isn't responding" dialog on top. The trace showed the main thread in layout under software rendering (GPU frame 90th percentile 4.9 s), and the app used no CPU while idle.
- Placeholder rows in production were not checked with TalkBack. Their inertness is covered by the tests below.

### 2.3 Tests

- New `ui/src/test/.../PaddingValuesSidesTest.kt` (JUnit 5, Kotest). `./gradlew :ui:testDebugUnitTest --tests com.felipearpa.tyche.ui.PaddingValuesSidesTest` → 5 tests, 0 failures.
- New `ui/src/androidTest/.../lazy/RefreshableLazyPagingColumnInsetsTest.kt`: the viewport keeps its full height, the first row starts at the top padding, the last row ends at the bottom padding, an empty state is centered in the padded area, and a tall error scrolls its recovery action into the padded area.
- New `pool/src/androidTest/.../poolscore/PoolScoreListInsetsTest.kt`: the last pool ends above the bottom padding. A failed first load in a 280 dp viewport scrolls to Retry, and tapping Retry reloads the list. The template placeholder exposes no text or click action.
- New `bet/src/androidTest/.../BetPlaceholderAccessibilityTest.kt`: pending-bet, match-gambler-bet, and match-header placeholders expose no filler text and no click action.
- Instrumented runs used a separate signed-out Pixel_7_API_34 AVD (API 34, en-US, headless, no Fortuna installed) through `ANDROID_SERIAL=emulator-5560`. It was shut down afterwards.
  - `:ui:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.felipearpa.tyche.ui.lazy.RefreshableLazyPagingColumnInsetsTest` → 4 tests, 0 failures.
  - `:pool:connectedDebugAndroidTest` → 25 tests, 0 failures. This includes the existing pool-score and gambler-score list, item, and placeholder tests.
  - `:bet:connectedDebugAndroidTest` → 6 tests, 3 failures. The 3 new placeholder tests pass. The 3 existing `BetTextFieldTest` cases fail because they look for the test tag `textField`, which `BetTextField` does not set. Neither file is touched by this change.
- Negative checks: with `clearAndSetSemantics {}` removed from `MatchGamblerBetPlaceholderItem`, its placeholder test failed. With the `verticalScroll` removed from `CenteredScrollableColumn`, the My pools Retry test failed. Both were restored, and the three new classes passed again.
- `./gradlew build` → BUILD SUCCESSFUL.

Accepted without on-device TalkBack checks of placeholders and without the My pools empty state on a device by the user on 2026-09-26. The Manage gamblers hand-built loading skeleton is left as is (not model-backed; out of scope) by the user on 2026-09-26.

### 2.1 follow-up: loaded pool templates and API 36 (2026-09-26)

- **Pool templates with data**, checked on the SM-G955F after production gained an open pool layout (one template, "Copa Mundial de la FIFA 2026"). Only swipes were used on this screen: no template was selected and no pool was created.
  - Full window: the template card sits below the fixed instruction and the app bar (`2.1-api28-pooltemplates-loaded-full-window.png`).
  - 1080×520 window at font scale 1.6 (navigation bar on the right edge): the list viewport runs to the bottom window edge, and the card is cut by the window edge rather than by a margin (`…-row-passes-window-edge-short-window.png`). Scrolling collapses the app bar, and the card is then fully revealed below the instruction and clear of the side navigation bar (`…-row-revealed-app-bar-collapsed.png`).
  - 1080×1150 at 560 dpi and font scale 1.3 (navigation bar at the bottom): the card sits above the navigation bar (`…-row-above-bottom-nav-bar.png`).
  - With a single template, "first" and "last" are the same row. The template list could not be scrolled further than shown here.
  - Restored afterwards: size override 1080×2220 and font scale 1.0. `wm density reset` left an override of 560 dpi on this Samsung. `wm density 420` restored the original effective density: `wm density` again reports only "Physical density: 420", with `display_density_forced=420`.
- **API 36** (emulator-5554, Pixel_10_Pro_XL, gesture navigation, light theme). Updated to the current build with `adb install -r`. Content was enlarged with `wm density 900` and font scale 1.5; both were restored to 480 dpi and 1.0.
  - My pools: rows pass under the gesture handle (`2.1-api36-poolscore-rows-pass-under-gesture-handle.png`). At the end of the list, the last pool is fully revealed above the handle, and the collapsed app bar covers the status bar (`…-last-row-above-gesture-handle.png`).
  - Pool templates: the card sits below the fixed instruction and the app bar (`2.1-api36-pooltemplates-row-below-fixed-header.png`).
  - Bet timeline: the emulator's network dropped while the timeline loaded, so the list error appeared below the fixed username header (`2.1-api36-timeline-error-below-fixed-header.png`). A slow pull showed the refresh indicator below the header (`…-pull-indicator-below-fixed-header.png`) and loaded the timeline, whose rows pass under the gesture handle (`…-recovered-rows-pass-under-gesture-handle.png`). Faster swipes did not start a refresh on this emulator. No request was sent for them.
  - Match bets: a swipe registered as a tap and opened a match. Loading placeholders appeared below the placeholder match header and ran under the gesture handle (`2.1-api36-matchbets-loading-placeholders-under-gesture-handle.png`). The loaded rows were captured behind an ANR dialog (`…-loaded-rows-under-gesture-handle-anr-dialog.png`).
  - Not observed on API 36: the last timeline item and the last match-bet row at the end of the list. The emulator repeatedly showed "Fortuna isn't responding" and then "Process system isn't responding" dialogs (load average above 20). Its system server restarted twice, at 13:46 and 13:56; Fortuna's crash log records only `DeadSystemException`. After the restarts, Fortuna is still installed and its data directory still has its `datastore`. The app did not return to the foreground on the overloaded emulator, so its sign-in was not rechecked. The API 28 phone checks above cover these list endings.

Accepted without API 36 checks of the last bet-timeline item and last match-bet row (deferred to group 6) and without a multi-row pool-template list (production has one template) by the user on 2026-09-26.

## Android — tasks 3.1–3.4 (2026-09-26)

### 3.1 Pool-home tab inset owners

- Pool home's `Scaffold` owns the system insets. `PoolHomeContent`'s content slot now takes the scaffold padding and passes it to the selected tab. The padded `Box` around the tabs is gone, so each tab's list viewport runs from the window top to the window bottom, under the top app bar and the tab bar.
- Scores (`GamblerScoreListView` → `GamblerScoreList`) and History (`FinishedBetListView` → `FinishedBetList`) take the padding as list content padding, with `consumeWindowInsets(contentPadding)` on the list. Scores adds its existing 8 dp vertical spacing to the content padding instead of padding the list's container.
- Bets (`PendingBetListView` → `PendingBetList`) does the same, plus the keyboard handling in 3.2. Its existing 8 dp vertical spacing also moved into the content padding.
- The empty states of Bets and History and the Bets error state now use `ViewportFillingItem`, so they are centered in the padded viewport and scroll when taller than it.

### 3.2 Pending-bet keyboard bounds

- `PendingBetList`'s modifier is `consumeWindowInsets(contentPadding).fitInside(WindowInsetsRulers.Ime.current)`. The list viewport ends at the keyboard top while the keyboard is open. The tab bar stays at the window's bottom edge and the keyboard covers it.
- The list's bottom content padding is the scaffold's bottom padding minus the keyboard height, never below zero (`PaddingValues.bottomUncoveredBy` in `ui/PaddingValuesSides.kt`), plus the 8 dp design spacing. The tab-bar and navigation-bar heights are not added on top of the keyboard. With the keyboard closed, the padding is the full tab-bar height.
- `BetTextField` brings itself into view once the keyboard has finished opening while it has focus. Without it, the focused score near the end of the list stayed behind the keyboard in 1 of 3 instrumented runs (see 3.4).
- On a device: no production pool has open pending bets (the tournament has finished; the Bets tab showed "Nothing to show" in all three of Felipe's pools), so score entry near the end of the list could not be exercised with real data. No bet was edited or saved. The instrumented test below covers it.

### 3.3 Tab bar through the navigation-bar area

- The tab bar is a `Surface` in the tab row's container color that fills the scaffold's bottom slot to the window edge. The `PrimaryTabRow` inside it takes the navigation-bar insets (bottom and horizontal) as padding, so the tabs sit above the navigation bar and the surface draws behind it. Previously the tab row itself was padded, leaving the navigation area to the window background.
- Device: emulator-5554 (Pixel_10_Pro_XL, API 36, en-US), prodDebug installed with `adb install -r` over the signed-in app.
  - Dark theme, three-button navigation: the tab surface color (#121212) continues without a band to the bottom edge behind the navigation buttons (sampled at several rows from the tab row down to the last pixel row). The tabs end above the buttons (`3.3-api36-dark-threebutton-tabbar-through-nav-area.png`).
  - Light theme, three-button navigation: same, in white, with dark navigation buttons (`3.3-api36-light-threebutton-tabbar-through-nav-area.png`). Tapping the lower part of the Scores tab, just above the navigation buttons, switched tabs (`3.3-api36-light-threebutton-scores-tab-tapped.png`).
  - Light theme, gesture navigation: the tabs sit above the gesture handle, and the surface reaches the bottom edge (the 3.1 captures).
  - In both themes the tab surface and the screen background share the same color, so the surface's extent is visible only as the absence of a contrasting band.
  - Restored afterwards: gesture navigation (`navigation_mode` 2, only the gestural overlay enabled) and light theme, as found.
- The SM-G955F (API 28) was not used: it was locked with a PIN. Its settings were not changed (1080×2220 override, 420 dpi, font scale 1.0, as found).

### 3.1 device checks (emulator-5554, API 36, gesture navigation, light theme)

- Scores, 13-member pool: the first row starts below the expanded app bar (`3.1-api36-scores-first-row-below-app-bar.png`). At the end of the list the app bar is collapsed and the last gambler (13th) is fully revealed above the tabs (`3.1-api36-scores-last-row-above-tabs.png`).
- History: the first match sits below the expanded app bar (`3.1-api36-history-first-row-below-app-bar.png`). After paging through the whole tournament, the opening match (6/11) is fully revealed above the tabs (`3.1-api36-history-last-row-above-tabs.png`).
- Bets: the empty state is centered between the app bar and the tabs, with the app bar collapsed and expanded (`3.1-api36-bets-empty-centered-between-bars.png`, `3.1-api36-bets-empty-app-bar-expanded.png`).
- Two swipes on the emulator registered as taps and opened match screens. System Back from the match screen returned to pool home each time. Nothing was changed.

### 3.4 Tests

- New `app/src/androidTest/.../PoolHomePendingBetKeyboardTest.kt`. It hosts the production `PendingBetList` inside the real `PoolHomeContent` scaffold, in an edge-to-edge test activity given MainActivity's manifest soft-input mode. Twenty pending bets; only the last is open for betting. Nothing is saved. The tests cover:
  - With the keyboard closed, the list viewport reaches the window bottom and the last bet's Edit ends above the tabs.
  - Focusing the last bet's away score opens the keyboard. The list viewport then ends at the keyboard top, and the field is above the keyboard. The tabs keep their window-edge position under the keyboard. With a draft entered, Save and Cancel are enabled and end above the keyboard, and the gap between them and the keyboard is smaller than the tab-bar height (no stacked tab-bar padding).
  - Closing the keyboard returns the list viewport to the window bottom. The draft (2–7) is kept, and Save, enabled, ends above the tabs.
- Runs on a separate signed-out Pixel_7_API_34 AVD (API 34, en-US, no Fortuna installed) through `ANDROID_SERIAL=emulator-5560`, shut down afterwards:
  - `:app:connectedProdDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.felipearpa.tyche.PoolHomePendingBetKeyboardTest`, gesture navigation → 3 tests, 0 failures, five consecutive runs (15 of 15 passed).
  - The same with three-button navigation → 3 tests, 0 failures. The AVD was returned to gesture navigation.
  - `:pool:connectedDebugAndroidTest` → 25 tests, 0 failures (includes the gambler-score list tests).
  - `:bet:connectedDebugAndroidTest` → 6 tests, 3 failures. The 3 failures are the existing `BetTextFieldTest` cases that look for the `textField` test tag, as in group 2.
- Negative checks, each restored afterwards:
  - Bottom padding using the full scaffold padding instead of `bottomUncoveredBy`: the focus test failed with "The gap above the keyboard (266.0) must not include the tab bar (211.0)".
  - Without `fitInside(WindowInsetsRulers.Ime.current)`: the focus and keyboard-closing tests failed, with the list ending at the window bottom (2400 px) instead of the keyboard top (1633 px).
  - Without `BetTextField`'s bring-into-view on keyboard open: 1 of 3 runs failed with the focused score (bottom 2208 px) behind the keyboard (top 1633 px).
- `./gradlew :ui:testDebugUnitTest --tests com.felipearpa.tyche.ui.PaddingValuesSidesTest` → 8 tests, 0 failures (3 cover `bottomUncoveredBy`).
- `./gradlew build` → BUILD SUCCESSFUL.
- Not checked on a device at first: keyboard over a real pending bet (no open bets in production), the API 28 phone (locked), and TalkBack. See the follow-up below.

### 3.2–3.4 follow-up: real pending bet, API 28 phone, TalkBack (2026-09-26)

The "Copa Mundial de la FIFA prur" pool (1 member) gained an open bet: Colombia vs Portugal, 10/2/26 8:00 PM, with no score yet. It is the only pending bet, so it is also the last row of the list. Every edit below was discarded with Cancel. **No bet was saved.**

**3.2 on emulator-5554 (API 36, gesture navigation, light theme, English)**

- First refresh of the Bets tab: the API answered 401 (`{"message":"Unauthorized"}`), probably an expired sign-in token after about 2 hours idle. The list showed "Unexpected error" and was not refreshed. Restarting the app (`am force-stop`, then launch; sign-in kept) loaded the bet.
- Full window: Edit, then focus on the Portugal (lower) score. The keyboard opened with the top app bar in place. The draft (3–1), the focused field, and Cancel and Save (enabled) were all above the keyboard. The keyboard covered the tabs (`3.2-api36-open-bet-loaded.png`, `3.2-api36-keyboard-open-field-and-actions-above.png`). Hiding the keyboard with its own hide key kept the draft 3–1. The list returned to full height, ending at the tabs with no keyboard-sized gap (`3.2-api36-keyboard-closed-draft-kept.png`).
- Short window with large text (`wm size 1344x1400`, font scale 1.5):
  - Focusing the Portugal score scrolled the list, and the app bar collapsed. The field with its draft "1" sat fully above the keyboard, and the tops of Cancel and Save showed at the keyboard edge (`3.2-api36-short-window-focused-field-above-keyboard.png`).
  - Scrolling with the keyboard open brought Cancel and Save fully above the keyboard. The list's end (divider plus 8 dp) sat directly on the keyboard top, with no tab-bar-sized gap (`3.2-api36-short-window-actions-above-keyboard.png`). Save was disabled only because the Colombia score was empty.
  - Hiding the keyboard kept the draft and the caret. The list returned to ending at the tabs (`3.2-api36-short-window-keyboard-closed-draft-kept.png`).
  - Restored: `wm size reset` (physical 1344×2992, 480 dpi) and font scale 1.0.
- The short-window captures were taken with the final build: the `./gradlew build` APK, reinstalled with `adb install -r` at 19:06. The full-window captures were taken with the build installed at 16:30. That build was assembled during the last negative check, so it may have lacked `BetTextField`'s bring-into-view helper. The full-window layout doesn't depend on the helper, because the field was already above the keyboard. With the final build, the short-window steps gave the same results as the full window.
- The font-scale change recreates the activity, which drops an unsaved bet edit. This behavior is unchanged by this change.
- During the relaunch at font scale 1.5, the overloaded emulator showed "Fortuna isn't responding" twice (host load about 14). The app was closed from the dialog and relaunched, and it was idle (0% CPU) before that.

**3.2 on the SM-G955F (API 28, es-CO, three-button navigation, dark theme)**, updated to the final build with `adb install -r`:

- Apuestas, then Editar, then focus on the Portugal score and type "1". The field, Cancelar and Guardar were above the Samsung keyboard. The top bar stayed in place, and the keyboard covered the tabs (`3.2-api28-keyboard-open-field-and-actions-above.png`).
- Hiding the keyboard with the navigation bar's hide key kept the draft and caret. The list returned to ending at the tabs (`3.2-api28-keyboard-closed-draft-kept.png`). Cancelar discarded the edit.

**3.3 on the SM-G955F**

- Dark theme: the tabs sit above the navigation buttons. Tapping the lower part of Historia, just above the buttons, switched tabs (`3.3-api28-dark-historia-tab-tapped-above-nav-buttons.png`).
- Background: **there is a faint band** (`3.3-api28-dark-tabbar-nav-scrim-band.png`). The tab bar is #121212 down to its bottom edge, but the navigation-bar area is #171717. This matches the translucent dark scrim (`#801B1B1B` over #121212) that `enableEdgeToEdge()`'s default navigation-bar style draws on API 28 and below, where contrast enforcement can't be turned off. The tab surface itself does reach the window edge: API 36 shows no band in either theme. The contrast ratio of the band is about 1.03:1. MainActivity's system-bar style (group 1 and group 5 scope) was not changed.
- Light theme: not checked. `cmd uimode night no` has no effect on this Android 9 Samsung (it still reports "Night mode: yes", with `ui_night_mode` 2, and the app stays dark). Switching further would need a reboot or One UI settings, and a reboot would lock the phone. Nothing was changed. The phone stayed unlocked and wasn't asked for a PIN.

**3.4 TalkBack on emulator-5554**

- TalkBack is installed (`com.google.android.marvin.talkback`). Before: `enabled_accessibility_services` null and `accessibility_enabled` 0. TalkBack was turned on through those settings, with Edit open and the Portugal score focused with a draft "2".
- Turning it on raised "Allow Android Accessibility Suite to send you notifications?" twice. It was declined both times, which closed the keyboard until the prompt was gone.
- With the keyboard open, TalkBack's focus outline was on the Portugal score field, above the keyboard (`3.4-api36-talkback-focus-on-score-keyboard-open.png`).
- **Finding (not caused by this change):** the score fields have no accessible label. `uiautomator` shows both EditTexts with empty content description and hint, and the empty one is flagged NAF (not accessibility friendly). TalkBack can announce only the value and "edit box", not which team the score is for. `BetTextField`'s semantics are unchanged by this change.
- Announcements and TalkBack navigation were not verified. adb-injected swipes did not move TalkBack focus. An injected tap on Cancel bypassed explore-by-touch and activated it, discarding the draft (nothing saved).
- Restored: TalkBack off (`enabled_accessibility_services` deleted, back to null; `accessibility_enabled` 0; touch exploration off). The Accessibility Suite notification-permission flags set by the two declines were cleared (`pm clear-permission-flags … user-set user-fixed`), leaving the permission as before (not granted, flags `USER_SENSITIVE_WHEN_GRANTED`).

Accepted without the light-theme tab-bar check on the API 28 phone (the API ≤28 navigation-bar scrim band is deferred to group 5) and without TalkBack spoken-output/swipe-navigation checks by the user on 2026-09-26.

## Android — tasks 4.1–4.3 (2026-09-26)

### 4.1 Sign-in form inset owners

Both sign-in screens follow the username editor's pattern. Each screen's Material 3 `Scaffold` owns the system insets and its top app bar stays pinned. The form's modifier is `fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding).fitInside(WindowInsetsRulers.Ime.current).verticalScroll(…)`, with the design spacing inside the scrolling content. With the keyboard closed, the form ends at the navigation bar. With it open, the form ends at the keyboard top and scrolls within the remaining space. The navigation-bar and keyboard heights are not added together.

| Screen | Form states that use it | Design spacing inside the scroll |
| --- | --- | --- |
| Email sign-in (`EmailSignInView`) | Idle, sending, failure (form under the error dialog) | Horizontal 16 dp, as before |
| Email and password sign-in (`EmailAndPasswordSignInView`) | Idle, signing in, signed in, failure | 16 dp on all sides, as before |

- Neither form has inline validation messages. Both use the fields without validation (`EmailTextField(validation = null)`, `RawEmailTextField`, `RawPasswordTextField`), so "validation" here is the Sign in button's enabled state. The password screen's no-recovery notice is the last item in its form.
- The email-sent confirmation (the loaded state of email sign-in) is unchanged. Tests showed it doesn't overflow on a Pixel 7-sized window, even in landscape at font scale 2.0 or in portrait at the largest text and display sizes, so no scrolling was added.
- **Device check:** Pixel_7_API_34 AVD (API 34, en-US, signed out, gesture navigation, light theme), prodDebug installed with `adb install -r`. The AVD was set to landscape (`user_rotation 1`, auto-rotate off) with `font_scale 1.3`. Only fake input was typed. Nothing was submitted and no sign-in email was sent. In landscape, the display cutout is on the left edge. The form and top app bar start at x = 157 px, inside the cutout inset, and the screen background reaches the window edges.
  - Email: with the keyboard open, the top bar and back arrow stay in place and the field sits directly above the keyboard (`4.1-api34-email-landscape-font1.3-field-above-keyboard.png`). The space between the app bar and the keyboard is 152 px (58 dp), so the field's floating label is partly behind the bar. Dragging the form brought Sign in (disabled for the invalid address) fully above the keyboard (`…-signin-above-keyboard.png`). Closing the keyboard with Back kept the draft and the caret. The form returned to full height with no keyboard-sized gap (`…-keyboard-closed-draft-kept.png`).
  - Email and password: focusing the email field and then the password field brought each above the keyboard, with the top bar in place (`4.1-api34-password-landscape-font1.3-email-field-above-keyboard.png`, `…-password-field-above-keyboard.png`). Scrolling reached Sign in (`…-signin-above-keyboard.png`) and then the form's end, where the no-recovery notice sits above the keyboard (`…-warning-at-form-end-above-keyboard.png`). Closing the keyboard kept both drafts (`…-keyboard-closed-drafts-kept.png`).
  - Restored afterwards: `font_scale` deleted (it was unset before), `user_rotation 0`, auto-rotate on.

### 4.2 Pool-name step inset owner

- The creation screen's `Scaffold` owns the system insets. The stepper consumes the scaffold padding and passes it to each step. Step one (templates) receives it plus the 16 dp design spacing and applies it as in group 2, unchanged.
- Step two (`StepTwoView`) is called by the stepper directly; it is no longer a lambda parameter. Its column is `fillMaxSize().padding(contentPadding).fitInside(WindowInsetsRulers.Ime.current).verticalScroll(…).padding(16 dp)`, the same pattern as the sign-in forms. The title, field, validation message, and Done scroll together above the keyboard.
- The top app bar is pinned on step two. Step two neither passes the collapsing scroll behavior to the bar nor attaches its nested-scroll connection, so scrolling the name form (including bringing the focused field into view) cannot collapse the bar and its back control. Step one still collapses the bar with the template list. Previously, a bar collapsed on step one stayed collapsed on step two, where nothing could scroll it back.
- The name draft is the step's `remember` state and is unaffected by keyboard transitions. Re-entering step two from the templates still resets the name to the chosen template's name, as before. A rotation or font-scale change recreates the activity and returns to step one. This is existing behavior: the step and draft are not saved across recreation.
- **Device check:** emulator-5554 (Pixel_10_Pro_XL, API 36, en-US, signed in, gesture navigation, light theme), prodDebug installed with `adb install -r` over the signed-in app. Done was never tapped and no pool was created.
  - Portrait, font scale 1.0: My pools → create → the World Cup template → name step (`4.2-api36-portrait-name-step.png`). Focusing the name and typing kept the top bar in place. The field and Done (enabled) were above the keyboard (`4.2-api36-portrait-keyboard-open-field-and-done-above.png`). With the keyboard open, the toolbar back returned to the templates and closed the keyboard. Choosing the template again returned to the name step.
  - Landscape, font scale 1.3: focusing the name kept the top bar pinned, and the field sat directly above the keyboard (`4.2-api36-landscape-font1.3-field-above-keyboard.png`).
  - Not observed on the device in landscape: Done above the keyboard, and the draft after closing the keyboard. First the emulator showed "Process system isn't responding" and stopped accepting taps, so it was rebooted with `adb reboot` (data and sign-in kept). Then "Fortuna isn't responding" appeared twice on the name step (`4.2-api36-landscape-font1.3-anr-dialog-over-name-step.png`), and after that "Pixel Launcher isn't responding". The ANR reason was "Input dispatching timed out … Waited 5028ms for KeyEvent" for an injected key, and system_server logged 0.5–2.3 s watchdog lock waits. During the ANR, Fortuna's main thread was sleeping, and its CPU time did not change over 5 s, so it was not in a layout loop. Host load averages were 13–19 at the time. The same landscape keyboard flow passed in every instrumented run below (4 runs of each case in `PoolNameKeyboardTest`, plus the full `:pool` suite). The system killed Fortuna after the second ANR ("user request after error"); its data was kept.
  - Restored afterwards: font scale 1.0, `user_rotation 0`, auto-rotate on. Size (1344×2992), density (480), light theme and gesture navigation were not changed.
- The SM-G955F phone was not used.

### 4.3 Tests

- New `account/src/androidTest/.../SignInKeyboardTest.kt` and `pool/src/androidTest/.../creator/PoolNameKeyboardTest.kt`. Both are parameterized over two constrained windows: landscape at font scale 1.3, and portrait at font scale 2.0 and display size 1.35. The test activity is turned to the case's orientation, made edge to edge, and given the keyboard-resizing soft-input mode. `UsernameEditorKeyboardTest` asserts that MainActivity's manifest declares that mode; these library modules can't read MainActivity. In landscape, each test asserts that the form overflows with the keyboard open, so scrolling is exercised. The state-driven overloads of the two sign-in views and of `PoolFromLayoutCreatorView` became `internal` so the tests can host them without view models.
  - Sign-in, 4 tests × 2 windows:
    - Email: the field and Sign in are above the keyboard and the top bar stays in place.
    - Email: the draft and a 2–6 selection survive closing and reopening the keyboard.
    - Email and password: each field, Sign in (disabled, then enabled) and the no-recovery notice are reachable above the keyboard.
    - Email and password: both drafts and the password selection survive closing and reopening the keyboard.
    - After each keyboard change, the form must end at the keyboard top, or at the navigation-bar top once the keyboard closes, with no gap.
  - Pool name, 3 tests × 2 windows. The real creation screen hosts the production template step with fixed templates.
    - With the keyboard open, the name field, its length validation message (after clearing the name) and Done (enabled, then disabled) are above the keyboard. The pinned back control stays above the form.
    - The draft and a 2–6 selection survive closing and reopening the keyboard.
    - With the keyboard open, back returns to the templates without leaving the creator, and choosing another template shows its name. Nothing is created.
  - A field taller than the space between the bar and the keyboard (a floating-label field in the landscape case: 58 dp of space) must fill that space instead of fitting inside it.
- Runs on the signed-out Pixel_7_API_34 AVD (API 34, en-US, no signed-in Fortuna) through `ANDROID_SERIAL=emulator-5560`, which was shut down afterwards:
  - `:account:connectedDebugAndroidTest --rerun -Pandroid.testInstrumentationRunnerArguments.class=com.felipearpa.tyche.account.SignInKeyboardTest` → 8 tests, 0 failures, on each of 3 consecutive runs, plus once more at the end.
  - `:pool:connectedDebugAndroidTest --rerun -Pandroid.testInstrumentationRunnerArguments.class=com.felipearpa.tyche.pool.creator.PoolNameKeyboardTest` → 6 tests, 0 failures, on each of 3 consecutive runs, plus once more at the end.
  - Full suites: `:account:connectedDebugAndroidTest` → 8 tests, 0 failures. `:pool:connectedDebugAndroidTest` → 31 tests, 0 failures (the 25 existing tests plus the 6 new ones).
- Negative check: with `fitInside(WindowInsetsRulers.Ime.current)` removed from both sign-in forms and the name step, all 8 sign-in and all 6 pool-name cases failed. For example, "Form bottom expected 394.0 but was 1017.0" in landscape: the form ran under the keyboard to the window bottom. The lines were restored.
- `./gradlew build` → BUILD SUCCESSFUL (includes `:app:testProdDebugUnitTest`, 60 tests, 0 failures).

Accepted without the landscape on-device check of Done above the keyboard and draft retention on the pool-name step (deferred to group 6) by the user on 2026-09-26. The user chose to keep the pinned top app bar on the pool-name step on 2026-09-26.

## Android — tasks 5.1–5.4 (2026-09-26)

### 5.1 Remaining screens: inset owners and fixes

| Screen | Inset owner | Finding | Change |
| --- | --- | --- | --- |
| Home (`HomeView`) | Material 3 `Scaffold` (system bars and display cutout) | Correct insets, but the three sections were laid out with `SpaceBetween` in a fixed-height column. In a landscape phone window their total height exceeds the safe area, so the sign-in buttons were pushed past the bottom edge with no way to reach them. | The column now sits in the scaffold padding (consumed) and scrolls, with a minimum height of the safe area. Weighted spacers keep the old spread when everything fits. |
| Profile (`ProfileView`) | `Scaffold` with the top app bar | Correct insets, but the content could not scroll, so large text or a short window could push the username row out of view. | The content consumes the scaffold padding and scrolls; 16 dp design spacing at its end. |
| Avatar crop (`AvatarCropView`) | The screen itself (no scaffold) | `navigationBarsPadding()` only: the crop area ran under the status bar, and in landscape the actions were not kept clear of a side display cutout (or caption controls). | `safeDrawingPadding()` on the column, after the background, so the dark surface still reaches every window edge. See 5.2 for its system-bar icons. |
| Email-link sign-in result and failure (`EmailLinkSignInView`) | The screen itself | `statusBarsPadding().navigationBarsPadding()`: no display-cutout or caption-bar bounds, so in landscape the action ran into a side cutout. The message was centered in a fixed-height area, so when it was taller than that area it overflowed onto the action and past the top. | `safeDrawingPadding()` plus the new shared `MessageWithActions` (`ui/MessageWithActions.kt`): the message is centered above the actions, and both scroll together when they don't fit. |
| Pool join: invitation, load failure, join failure (`PoolJoinerView`), sign-in required (`PoolJoinRequiresSignInView`) | The screen itself | Same as email-link sign-in. | Same change. `PoolJoinerContainer` became `internal` for the test below. |
| Loading overlays (`LoadingContainerView`) | None needed | Full-window blur and spinner with no controls; the background already reaches every edge. | None. |

### 5.2 System-bar legibility

- **Status bar.** Every list screen scrolls under a Material top app bar. The bar keeps covering the status bar when collapsed: on the bet timeline (API 28, dark), the status-bar area stays the bar's scrolled container color (#211f26) while rows pass below it (`verification/5.2-api28-dark-timeline-rows-under-transparent-three-button-nav.png`). Home, Profile, sign-in, pool-join, and email-link screens do not scroll content under the status bar: after 5.1 their scrolling areas start below it. The only screen that showed content under the status bar was the avatar crop (the photo under the scrim); since 5.1 it no longer does. No status-bar protection strip was added anywhere.
- **Avatar crop icons.** The crop surface is near-black in both themes. With `enableEdgeToEdge()`'s theme-based icons, light theme gave dark status and navigation icons on it. The crop view now sets light system-bar icons while it is shown and restores the previous appearance when it leaves (`LightSystemBarIcons`, a `DisposableEffect` using `WindowCompat.getInsetsController`). ComponentActivity still sets the icons everywhere else. On API 36 in light theme, the crop showed white status icons (20.5:1 against the surface) and a light gesture handle (17.2:1) (`5.2-api36-light-crop-light-system-icons-anr-dialog.png`; the emulator's "isn't responding" dialog covers the middle of the screen, not the bars). Restoring the icons on leaving could not be seen on the device because of the emulator incidents below; `AvatarCropSystemBarsTest` covers it.
- **API ≤28 navigation-bar band (carried over from group 3). Decision:** `MainActivity` now passes `navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)` to `enableEdgeToEdge()`. On API 28 and below, the navigation bar is transparent instead of the default translucent scrim (#E6FFFFFF in light theme, #801B1B1B in dark theme), and icon color still follows the theme through ComponentActivity. On API 29 and above nothing changes: `auto` styles were already transparent there, and `isNavigationBarContrastEnforced = false` is kept. This matches API 29+, where group 1 already turned off the system scrim.
  - SM-G955F (API 28, es-CO, three-button, dark), updated with `adb install -r`: the tab bar is #121212 from the tabs down to the last pixel row, with no #171717 band (`5.2-api28-dark-tabbar-nav-area-no-band.png`). The thin #49454f line above the navigation buttons is `PrimaryTabRow`'s own divider, not a band. The white navigation buttons are legible on #121212.
  - Screens without a bottom bar on the phone: Profile, crop, and the bet timeline in portrait, and Profile and crop in landscape with the navigation bar on the right edge. The white buttons stay legible over the dark background and over timeline rows passing beneath them (`5.2-api28-dark-timeline-…png`, `5.1-api28-dark-landscape-profile.png`).
  - API 36 (emulator-5554, three-button, dark): the tab bar is still #121212 to the bottom edge, unchanged (`5.2-api36-dark-threebutton-tabbar-unchanged.png`).
  - Not checked: light theme on API ≤28 (the phone's night mode can't be switched, as in group 3). With a transparent bar, light theme shows dark buttons over the light app surface, the same appearance API 29+ already had.

### 5.3 Drawers, sheets, dialogs, FAB

| Surface | Result |
| --- | --- |
| `PushDrawer` (both drawers) | No change. Drawer content is padded once by system bars ∪ display cutout on its vertical and start sides, and its width already accounts for them. `DrawerMenu` scrolls, adds no insets, and the pushed screen keeps its own scaffold insets. API 36 landscape (cutout on the left): the drawer content starts clear of the cutout and Log out sits above the gesture handle (`5.3-api36-light-landscape-drawer.png`; portrait `5.3-api36-light-drawer-portrait.png`). System Back with the drawer open closed it and stayed on My pools. |
| Photo-source sheet (`ModalBottomSheet`) | No change. M3's default sheet insets (safe drawing, top and bottom) keep the rows above the navigation bar while the sheet surface reaches the bottom edge. In landscape the sheet is width-capped and centered, clear of the side cutout (API 28 portrait/landscape and API 36 portrait/landscape at font 1.5: `5.3-*-photo-source-sheet.png`). |
| `AlertDialog`s (avatar upload error, delete pool, remove gambler, `ExceptionAlertDialog`) | No change. These are ordinary, platform-width dialogs, not full-screen. |
| `MinimalDialog` | No change. Platform-width `Dialog` without `usePlatformDefaultWidth = false`; it has no callers. |
| Full-screen dialogs | None found (no `usePlatformDefaultWidth = false` with `fillMaxSize`). |
| FAB, adaptive scaffold | None found. The My pools "+" is a top-app-bar action. |

### 5.4 Tests

- New instrumented tests. Each runs in an edge-to-edge test activity turned to landscape, with the display-cutout mode reapplied after the window is attached (`LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`, as MainActivity gets it from `enableEdgeToEdge()` before attachment). Content uses font scale 2.0. Nothing is joined, signed in, or uploaded.
  - `pool/…/joiner/PoolJoinSafeBoundsTest` (4 tests): invitation (Join, Go to my pools), load failure, join failure (Retry, Go to my pools), and sign-in required (Got it). Every action scrolls into view inside the area clear of the system bars and the cutout, and the message ends above the first action.
  - `account/…/EmailLinkSignInSafeBoundsTest` (2 tests): the verified confirmation (Get started) and the failure (Retry), with the same checks.
  - `app/…/AvatarCropSystemBarsTest` (2 tests): the crop view turns the system-bar icons light and restores the activity's appearance after it leaves; in landscape, Cancel and Use photo are inside the safe area.
- Runs on the signed-out Pixel_7_API_34 AVD (API 34, en-US, gesture navigation, light theme; display cutout 136 px, on the left in landscape) through `ANDROID_SERIAL=emulator-5560`. It was shut down afterwards.
  - `:pool:connectedDebugAndroidTest` → 35 tests, 0 failures (31 existing plus 4 new).
  - `:account:connectedDebugAndroidTest` → 10 tests, 0 failures (8 existing plus 2 new).
  - `:ui:connectedDebugAndroidTest` → 72 tests, 0 failures, including the existing `PushDrawer` accessibility, dismissal, gesture, navigation, resize, reveal, and disabled-animation tests.
  - `:app:connectedProdDebugAndroidTest` → 19 tests, 1 failure: `UsernameEditorKeyboardTest.closingAndReopeningTheKeyboardKeepsTheDraftAndSelectionWithoutAGap`, "expected TextRange(1, 4) but was TextRange(7, 7)". It also fails with this group's `MainActivity` change reverted, and the test hosts the editor in a plain test activity, so it is independent of this group. Group 1 ran it only on API 36. The selection reset on keyboard reopen is specific to this API 34 AVD run and is left open.
- Negative checks, each restored afterwards:
  - Pre-change pool-join and email-link views: all 6 new tests failed (no scrolling container to reach the actions).
  - New layout with `statusBarsPadding().navigationBarsPadding()` instead of `safeDrawingPadding()` on the sign-in-required screen: its test failed with "The action Rect.fromLTRB(21.0, 872.0, 2379.0, 994.0) must lie inside the safe area Rect.fromLTRB(136.0, 74.0, 2400.0, 1017.0)": Got it ran into the cutout.
  - Crop without `LightSystemBarIcons()` and with `navigationBarsPadding()`: both crop tests failed (light status-bar icons expected false; Cancel at x = 42 inside the 136 px cutout).
- `./gradlew build` → BUILD SUCCESSFUL. `:app:testProdDebugUnitTest` → 60 tests, 0 failures.

### Device checks

- **API 34 signed out** (Pixel_7_API_34, prodDebug installed with `adb install -r`; restored afterwards to font scale 1.0, rotation 0 with auto-rotate on, light theme):
  - Home, portrait: layout unchanged (`5.1-api34-light-home-portrait.png`).
  - Home, landscape: the sign-in buttons start at x = 199 px, clear of the 136 px cutout. Email and password was cut off at the viewport bottom and scrolled fully into view above the gesture handle (`5.1-api34-light-home-landscape-top.png`, `…-scrolled-to-sign-in-actions.png`). Dark theme: light status icons on the dark gradient (`5.1-api34-dark-home-landscape.png`).
  - Email-link failure: opened `https://tyche-588ce.web.app/sign-in/gambler@example.com` (fake address; the link itself is not a Firebase sign-in link, so no email is involved). In landscape, "Invalid sign-in link" and Retry sit inside the cutout inset (Retry from x = 157 px), at font scale 1.0 and 2.0 (`5.1-api34-light-landscape-email-link-failure.png`, `…-font2-email-link-failure.png`).
  - Pool join while signed out: `https://tyche-588ce.web.app/pools/01TEST00000000000000000000/join` showed the sign-in requirement with Got it inside the safe area at font 2.0 in landscape (`5.1-api34-light-landscape-font2-pool-join-requires-sign-in.png`).
- **API 28** (SM-G955F): Profile and crop in portrait and landscape (navigation bar on the right edge). The crop area stops at the status bar and at the right-edge navigation bar, and Cancelar and Usar foto sit above or beside the navigation buttons (`5.1-api28-dark-*.png`). A personal photo was picked and the crop was cancelled; nothing was uploaded. Rotation was restored to 0 with auto-rotate on. The window was also set to 1080×1300, 1080×1110, and 1080×1000 at font scale 1.6, then restored to 1080×2220 and font 1.0 (420 dpi and night mode unchanged).
- **API 36** (emulator-5554, signed in): Profile in landscape keeps its content right of the cutout (x = 255 px), and at font scale 1.5 it scrolls by its end spacing (`5.1-api36-light-landscape-profile.png`, `5.1-api36-landscape-font1.5-profile-scrolled-to-username.png`). A generated test image (`e2e-crop-sample.jpg`) was pushed to `/sdcard/Pictures` to reach the crop screen and deleted afterwards (media store rescanned, no images left). Restored: gesture navigation, light theme, rotation 0 with auto-rotate on, font 1.0, physical 1344×2992 at 480 dpi.

### Incidents

- The emulator-5554 host was overloaded (load average 13–22, mostly macOS `ANECompilerService` and then both emulators). "Pixel Launcher", "Photos & videos" (the photo picker), and "Fortuna isn't responding" dialogs appeared repeatedly. Fortuna's ANRs were "Input dispatching timed out … Waited 5001ms for FocusEvent" or for a MotionEvent. A main-thread dump taken as root (`adb root`, reverted with `adb unroot`) showed Fortuna idle in `Looper.pollOnce` at 0% CPU, and `dumpsys input` listed no focused window, so these were system-side stalls, not an app hang. The emulator was rebooted once with `adb reboot` (data and sign-in kept) and Fortuna was force-stopped once. The photo picker was closed from its ANR dialog twice.
- Not verified on a device because of these stalls: returning from the crop to Profile on API 36 with dark icons restored, and the crop in landscape on API 36 with the cutout. Both are covered by `AvatarCropSystemBarsTest` on API 34 with a cutout.

### Not verified

- Caption-bar and freeform or desktop windowing: not attempted on the overloaded emulator. Every screen changed here uses Scaffold insets or `safeDrawing`, and both include the caption bar.
- Email-link verified confirmation on a device (it needs a real sign-in link). It is covered by `EmailLinkSignInSafeBoundsTest`.
- Pool invitation and join failure for a signed-in account on a device (no invite link for a pool the account can view was opened, to avoid any join). They are covered by `PoolJoinSafeBoundsTest`.
- Light theme on API ≤28 (the phone's night mode can't be switched).
- TalkBack: not re-run, since no semantics changed.

### Findings not caused by this group

- The bet timeline's sticky date headers have no background, so a scrolled row's text shows through the pinned date (`5.2-api28-dark-timeline-…png`, top). The sticky headers predate this change.
- `UsernameEditorKeyboardTest` selection case fails on the API 34 AVD (see 5.4).

Accepted without caption-bar/freeform windowing checks, light theme on API ≤28, and an on-device check of icons returning to dark after the crop screen (all to be attempted in group 6) by the user on 2026-09-27. The user chose to keep the crop screen's temporary system-bar icon override and the new scrolling on Home and Profile on 2026-09-27.

### 5.4 follow-up: username-editor selection on API 34 (2026-09-27)

**Diagnosis: a test timing artifact, not an app bug.**

- `UsernameEditorKeyboardTest.closingAndReopeningTheKeyboardKeepsTheDraftAndSelectionWithoutAGap` failed on the Pixel_7_API_34 AVD with the selection at (7, 7) instead of (1, 4). To see why, a temporary log line was added to the editor's `onDraftChange` (with the caller's stack) and the test was re-run. The log line was removed afterwards; `UsernameEditor.kt` is unchanged by this follow-up. The field's value changed in this order:
  1. The test replaced the text: `neptune`, caret (7, 7).
  2. Gboard, which composes on a plain-text field, marked the word as its composing region: (7, 7), composition (0, 7).
  3. The test set the selection: (1, 4), no composition.
  4. About 145 ms later, Gboard's `finishComposingText`, sent as the keyboard hid, arrived through the field's input connection and set (7, 7).
- Compose's legacy text field resynchronizes its editing buffer with the field's value only when it recomposes (`CoreTextField` calls `EditProcessor.reset`). The test hid the keyboard right after setting the selection, before the next frame, so Gboard's closing edit was applied to a buffer that still held (7, 7). The sign-in keyboard tests don't hit this: Gboard doesn't compose in their email and password fields.
- A person can't move a selection and close the keyboard within the same frame, so the app behaviour is correct. The test now calls `composeTestRule.waitForIdle()` between setting the selection and hiding the keyboard, with a comment explaining why. The assertions are unchanged: draft and exact selection after closing, and again after reopening, plus the no-gap check.
- With the change, the log shows the selection kept at (1, 4) through closing and reopening. It collapses to (4, 4) only at teardown, when the field loses focus (`TextFieldSelectionManager.deselect`), after the assertions.

**Runs** (signed-out AVDs through `ANDROID_SERIAL`, each shut down afterwards; emulator-5554 was not used):

- The single test on Pixel_7_API_34 (API 34, Gboard, gesture navigation): 3 consecutive passes.
- `:app:connectedProdDebugAndroidTest` on Pixel_7_API_34 → 19 tests, 0 failures.
- `:app:connectedProdDebugAndroidTest` on a new signed-out AVD, `Pixel_7_API_36_signed_out` (created with `avdmanager` from `system-images;android-36;google_apis;arm64-v8a`, device `pixel_7`, port 5562, Gboard, gesture navigation) → 19 tests, 0 failures. The AVD is kept for reuse; it has no Fortuna install or account.

**Not verified:** the hand check on the SM-G955F (type, move the selection, hide and reopen the keyboard, discard with toolbar back). The phone was dozing with the keyguard showing (`isStatusBarKeyguard=true`), so it was not woken or unlocked, and nothing was changed on it.

Accepted without a hand check of username-editor selection retention on the SM-G955F (covered by group 6 text-entry smoke tests) by the user on 2026-09-27.

## Android — tasks 6.1–6.3 (2026-09-27)

No product code was changed in this group. Every device ran the same prodDebug APK (SHA-256 `65e22b95…fa370e2`), built by the `./gradlew build` below and installed with `adb install -r` on signed-in devices. Captures are under `verification/6.*`.

### Configuration matrix

| Device | API | Account | Navigation | Theme | Orientation | Window / density | Font | Used for |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| SM-G955F phone (es-CO) | 28 | Signed in | Three-button | Dark | Portrait, landscape | 1080×2220 override, 420 dpi | 1.0 | 6.2, 6.3 |
| Pixel_8_API_35 AVD (cutout) | 35 | Signed in | Gesture; three-button | Light; dark | Portrait, landscape | 1080×2400, 420 dpi | 1.0, 1.5 | 6.2, 6.3 |
| Pixel_10_Pro_XL AVD (cutout) | 36 | Signed in | Gesture; three-button | Light; dark | Portrait, landscape | 1344×2992 at 480 dpi; then 672×1496 at 240 dpi (same dp size); 672×1000 short window; freeform window | 1.0, 1.5 | 6.2, 6.3 |
| Pixel_7_API_36_signed_out AVD | 36 | Signed out | Gesture (tests); three-button (screens) | Light (tests); dark (screens) | Portrait | 1080×2400, 420 dpi | 1.0 | 6.1 suites, sign-in screens |
| Pixel_7_API_34 AVD | 34 | Signed out | Gesture | Light | Portrait | 1080×2400 | 1.0 | 6.1 suites (pre-35) |

Only one emulator ran at a time. The phone was unlocked throughout and never locked or asked for a PIN.

### 6.1 Build and instrumented suites

- `./gradlew build` (from `Android/`) → BUILD SUCCESSFUL. Rerun with `./gradlew build --rerun-tasks` → BUILD SUCCESSFUL, 1044 of 1044 tasks executed (lint, all variants, all unit tests). Unit tests: 634, 0 failures (`:app` prodDebug 60 and localDebug 60, `:account` 52, `:ui` 42, `:core` 10, `:session` 23, `:data:bet` 4, `:data:pool` 3, `:network:core` 376, `:network:ktor` 2, `:pool` 2).
- `ANDROID_SERIAL=<avd> ./gradlew --continue :app:connectedProdDebugAndroidTest :account:connectedDebugAndroidTest :pool:connectedDebugAndroidTest :bet:connectedDebugAndroidTest :ui:connectedDebugAndroidTest :session:connectedDebugAndroidTest :data:pool:connectedDebugAndroidTest :data:bet:connectedDebugAndroidTest`, on the two signed-out AVDs. These are all the modules with `androidTest` sources.

| Suite | API 36 (Pixel_7_API_36_signed_out) | API 34 (Pixel_7_API_34) |
| --- | --- | --- |
| `:app` (prodDebug) | 19 tests, 0 failures | 19, 0 |
| `:account` | 10, 0 | 10, 0 |
| `:pool` | 35, 0 | 35, 0 |
| `:ui` | 72, 0 | 72, 0 |
| `:bet` | 6, 3 failures | 6, 3 failures |
| `:session`, `:data:pool`, `:data:bet` | 1, 1 failure each | 1, 1 failure each |

- All failures predate this change and are in files it doesn't touch. The 3 `:bet` failures are the `BetTextFieldTest` cases that look for the missing `textField` test tag. The `:session`, `:data:pool` and `:data:bet` failures are the template `ExampleInstrumentedTest`s asserting a pre-rename package name (for example "expected `com.felipearpa.data.user.test` but was `com.felipearpa.tyche.session.test`"). The overall Gradle result is therefore BUILD FAILED for both runs, from these 6 tests only.
- The `:app` suite includes `UsernameEditorKeyboardTest` (5), `UsernameEditorNavigationTest` (9), `PoolHomePendingBetKeyboardTest` (3) and `AvatarCropSystemBarsTest` (2); all passed on both APIs.
- Instrumented suites were not run on the signed-in devices (by agreement). The API 34 AVD has no Fortuna install afterwards; Fortuna was uninstalled from the API 36 signed-out AVD after the screen checks, so both are as found.

### 6.2 Spec scenarios on devices

**Screens reach the window edges; controls stay clear of system UI.**

- API 36, landscape, gesture, light: My pools, drawer, Username, match bets and pool naming start right of the left cutout (content from x = 80–104 px at 240 dpi) and keep the screen background to the edges (`6.2-api36-light-gesture-landscape-mypools-cutout-left.png`, `…-landscape-drawer.png`).
- API 35, landscape, three-button, dark, font 1.5: pool home content is inset from the left cutout, and the tab row ends left of the right-edge navigation buttons (`6.2-api35-dark-threebutton-landscape-font1.5-bets.png`).
- API 36 and API 35, three-button, dark: the tab bar is #121212 down to the last pixel row behind the navigation buttons, with no band (sampled at 8 rows; `6.2-api36-dark-threebutton-poolhome-scores.png`, `6.2-api35-dark-threebutton-poolhome-bets.png`).

**List endpoints (deferred from group 2).**

- API 36, gesture, light: the last bet-timeline item (the opening match, 6/11) ends above the gesture handle below the fixed username header, after paging through the whole tournament (`6.2-api36-light-gesture-timeline-last-item-above-handle.png`). Three-button, dark: the same item ends above the navigation buttons, and the collapsed app-bar color covers the status bar (`6.2-api36-dark-threebutton-timeline-last-item-above-nav-buttons.png`).
- API 36 match bets (Noruega vs Inglaterra, 13 rows): in portrait, all 13 rows fit above the handle (`6.2-api36-light-gesture-matchbets-last-row-above-handle.png`). In landscape the list scrolls, and the last row is fully revealed above the gesture handle, below the fixed match header and right of the cutout (`…-landscape-matchbets-top.png`, `…-landscape-matchbets-last-row-revealed.png`).
- API 36, three-button, dark, 672×1000 short window at font 1.5: the 13th leaderboard row ends above the tabs, and rows pass under the collapsed app bar at the top (`6.2-api36-dark-threebutton-short-font1.5-scores-last-row-above-tabs.png`).

**Username editor, signed in (deferred from group 1).**

- API 36 portrait, gesture, light: with the keyboard open the top bar and back arrow stay in place, and the field, helper text and Save username (enabled after typing) are above the keyboard (`6.3-api36-light-gesture-username-editor-keyboard-open.png`).
- API 36 landscape, gesture, light: with the keyboard open the field sits directly under the pinned top bar; dragging the form brings Save username fully above the keyboard, with the top bar still in place (`6.2-api36-light-gesture-landscape-username-keyboard-open.png`, `…-save-above-keyboard.png`). Closing the keyboard with its hide key kept the draft and caret, and the form returned to full height with Save above the gesture handle and no keyboard-sized gap (`…-keyboard-closed.png`). Toolbar back discarded the draft; Profile still showed `felipearpa`.
- A rotation with a draft kept the draft (`6.3-api36-light-landscape-username-draft-kept-after-rotation-anr-dialog.png`, behind a system ANR dialog; see Incidents).
- API 35 (see Not verified): Gboard on this AVD opens as a floating stylus-mode keyboard, so no keyboard inset is applied. The editor correctly kept its full layout under the floating keyboard (`6.2-api35-light-gesture-username-floating-stylus-keyboard.png`).

**Pool-name step in landscape (deferred from group 4).**

- API 36, landscape, gesture, light: with the keyboard open the name field sits under the pinned top bar; dragging the form brings Done fully above the keyboard. Closing the keyboard kept the draft "Copa Mundial de la FIFA 2026 prueba" (`6.2-api36-light-gesture-landscape-pool-name-keyboard-open.png`, `…-done-above-keyboard.png`, `…-keyboard-closed-draft-kept.png`).
- API 28, landscape (navigation bar on the right), dark: the same result with the Samsung keyboard (`6.3-api28-dark-landscape-pool-name-*.png`). Done was never tapped on either device; no pool was created.

**Avatar crop icons (deferred from group 5).** API 35, gesture, light: on the crop screen the window appearance has no light-bar flags (status icons and handle mean luminance 228 and 235 on a #0A0A0A surface); after Cancel, Profile's appearance is `LIGHT_STATUS_BARS LIGHT_NAVIGATION_BARS` with dark icons (mean luminance 112 and 102 on white) (`6.3-api35-light-crop-light-icons.png`, `6.3-api35-light-profile-after-crop-dark-icons.png`). A generated image was pushed for this and deleted afterwards (media store rescanned, no images left).

**Freeform window.** API 36 with `enable_freeform_support` and `force_resizable_activities` set to 1 and `am start --windowingMode 5`: Fortuna opened in a 335×672 px freeform window with its top app bar at the window's top edge and no duplicate status-bar inset (`6.2-api36-dark-freeform-window-no-caption-bar.png`). This phone system image draws no caption bar and exposes no caption-bar inset source, so caption controls could not be checked (see Not verified). Both settings were deleted afterwards (back to `null`).

### 6.3 Smoke tests after the target SDK change

| Flow | Result |
| --- | --- |
| Session after update | API 28, API 35 and API 36 kept the signed-in session across `adb install -r`, a force-stop and cold start, and (API 36) an emulator reboot (`6.3-api28-dark-mypools-session-kept-after-update.png`, `6.3-api35-light-gesture-mypools-session-kept-after-update.png`). |
| Sign-in screens (signed-out API 36, dark, three-button) | Home, email and email-and-password forms with fake input (`gambler@example.com`): fields, Sign in and the no-recovery notice sit above the keyboard with the top bar in place (`6.3-api36so-*.png`). Nothing was submitted. |
| Deep links | `https://tyche-588ce.web.app/pools/01KR6QJ1NQXXP65HE719F8HTEN/join` (a pool the account already belongs to): API 28 showed the invitation with Unirte and Ir a mis pollas; Ir a mis pollas returned to My pools. API 36 showed the same screen with its actions above the navigation buttons, behind a system ANR dialog. Join was never tapped. Signed out (API 36): the same link showed "Sign-in required to join" with Got it, and `https://tyche-588ce.web.app/sign-in/gambler@example.com` showed "Invalid sign-in link" with Retry. No sign-in link was opened on a signed-in device. |
| Photo selection and crop | API 28: Elegir de la biblioteca → Google Play services picker → personal photo → crop, a drag, then Cancelar returned to Perfil (`6.3-api28-dark-crop-*.png`). API 35: system picker → generated image → crop → Cancel. Nothing was uploaded. |
| Pool navigation | My pools → pool home → tabs → timeline → match bets → Back on API 28, 35 and 36. |
| Drawer dismissal | With the pool-home drawer open, system Back closed it and stayed on pool home: API 28 (three-button, dark), API 35 and API 36 (three-button, dark) (`6.3-*-poolhome-drawer-open.png`). |
| Refresh and pagination | Pull-to-refresh sent a new request and got 200 on My pools (API 28, API 35) and History (API 28) (`6.3-api35-dark-threebutton-mypools-after-refresh.png`, `6.3-api28-dark-history-after-pull-refresh.png`). API 36 paged the bet timeline through the whole tournament (above). |
| Pending bet (prur pool, API 28) | Editar, then scores 2 and 1: the fields, Cancelar and Guardar sit above the Samsung keypad. Hiding the keyboard kept the draft, and the list ended at the tabs. Cancelar discarded it (`6.3-api28-dark-pendingbet-*.png`). **No bet was saved.** |
| Pending bet (API 35) | Scores typed through the stylus-mode keyboard (no on-screen keyboard appeared). System Back from the pool screen closed the app, as it does today, and the unsaved draft was dropped. After relaunch the bet still showed Edit with no scores. |
| Username selection hand check (API 28, deferred from 5.4) | Typed `zz`, moved the caret 3 places left, hid the keyboard, typed `x`: it was inserted at the kept caret (`felipearpxazz`). Reopening the keyboard by tapping the field kept the draft (the tap itself places the caret, so selection after reopening was not observed separately). Toolbar back discarded the draft; Perfil still shows `felipearpa` (`6.3-api28-dark-username-*.png`). **The username was never saved.** |
| Accessibility | Semantics were unchanged in this group. uiautomator on every signed-in screen above showed the same content descriptions as before (for example "Copa Mundial de la FIFA 2026, Rank 8, 589 points, 13 members, Rank unchanged" and the drawer's "Close menu" scrim). TalkBack was not run. |

### Settings changed and restored

- SM-G955F: rotation (landscape for the pool-name step), then `user_rotation 0`, auto-rotate on. Size 1080×2220 override, 420 dpi, font 1.0 and night mode were not changed.
- Pixel_10_Pro_XL: night mode, three-button navigation, `wm size`/`wm density` (672×1496 at 240 dpi, 672×1000), font 1.5, rotation, freeform settings; restored to physical 1344×2992, 480 dpi, font 1.0, light, gesture navigation, rotation 0 with auto-rotate on, freeform settings deleted. It was rebooted once with `adb reboot` (data and sign-in kept), shut down while the other AVDs ran, and restarted afterwards with its original arguments.
- Pixel_8_API_35 starting state: 1080×2400, 420 dpi, font 1.0, light, gesture navigation, rotation 0 with auto-rotate on, freeform `null`. Changed night mode, three-button navigation, font 1.5 and rotation; all restored to the starting state. Shut down afterwards.
- Pixel_7_API_36_signed_out: night mode and three-button navigation for the sign-in screens, restored to light and gesture; Fortuna uninstalled; shut down.

### Incidents

- The Pixel_10_Pro_XL AVD (software rendering, 2 GB RAM) was unstable again at full resolution: "Photos & videos isn't responding" in the photo picker, "Fortuna isn't responding" after rotations and deep links, and an ANR in `system_server` with `crash_dump64` running against it. Fortuna's reasons were "Waited … for FocusEvent" or "Application does not have a focused window"; its main thread was sleeping with unchanged CPU time, so these were system-side stalls, as in groups 2–5. After an `adb reboot`, a smaller logical resolution (672×1496 at 240 dpi, the same dp size) made it usable. Gboard's process was also killed once and restarted, after which it showed no keyboard until the editor was reopened.
- The picker ANR on API 36 closed Fortuna's task; the crop was then done on API 35 instead.

### Not verified

- ~~Light theme on API ≤28~~: checked in the follow-up below.
- **Caption bar**: the available system images draw no caption bar in freeform mode and report no caption-bar inset, so caption controls were not observed. Freeform resizing was checked only for window placement.
- **Keyboard insets on API 35**: Gboard on the Pixel_8_API_35 AVD opens in stylus mode (a floating keyboard or toolbar, no IME inset), because the AVD's touchscreens report a stylus source. The username editor, pending-bet list and pool-name step were therefore not checked with a docked keyboard on API 35. The docked-keyboard behavior is covered on API 28 and API 36 above and by the instrumented tests on API 34 and 36.
- **Photo crop on API 36**: the system photo picker stopped responding; the crop and its system-bar icons were checked on API 35 and API 28.
- **Pool-join invitation on API 36** was captured only behind an ANR dialog; the API 28 run shows it fully.
- **Real sign-in and sign-out**, and the email-link verified confirmation: not allowed; covered by `EmailLinkSignInSafeBoundsTest`.
- **TalkBack** announcements and navigation.
- **Instrumented suites on API 35**: the only API 35 AVD is signed in, so suites ran on API 34 and API 36.

Accepted without a real sign-in/sign-out submission and without a TalkBack pass by the user on 2026-09-27.

### 6.2 follow-up: light theme on API 28 (2026-09-27)

Felipe switched the SM-G955F to light theme in One UI settings (`ui_night_mode` 1) and will switch it back to dark himself. The phone was otherwise as before: es-CO, three-button navigation, 1080×2220 override, 420 dpi, font 1.0, portrait. The same prodDebug build was relaunched (force-stop, then launch). Nothing was saved or uploaded. The phone stayed unlocked.

Contrast ratios below come from the most common background and icon pixel colors in the status-bar strip (y 0–63) and the navigation-button strip (y 2110–2215).

| Check | Result |
| --- | --- |
| Pool tab bar (Copa Mundial de la FIFA 2026, Puntajes) | White (#FFFFFF) from the tabs down to the last pixel row, sampled at 9 rows from y 1960 to 2219, with no band. The only non-white row is `PrimaryTabRow`'s own divider (#CAC4D0, y 2091–2093) above the navigation buttons. The tabs end above the buttons (`6.2-api28-light-poolhome-scores-tabbar.png`). |
| Tab tapped just above the buttons | Tapping Historia at y 2085, just above the buttons, switched tabs (`6.2-api28-light-historia-tab-tapped-above-nav-buttons.png`). |
| Three-button icons, pool home | #949494 on white, 3.03:1. |
| Status bar while scrolling (Historia, app bar collapsed) | The collapsed app bar's scrolled color (#F3EDF7) covers the status bar while rows scroll beneath it. Icons #615F63, 5.49:1 (`6.2-api28-light-historia-scrolled-under-status-bar.png`). |
| Bet timeline (Val Cardo), rows under the buttons | Rows and a score ("+20") scroll beneath the transparent navigation bar. The buttons stay the same #949494 over the white row background, 3.03:1. The status bar keeps the #F3EDF7 app-bar color, icons 5.49:1 (`6.2-api28-light-timeline-rows-under-nav-buttons.png`). The sticky date header overlapping a row's time at the top is the existing finding from group 5. |
| Profile (no bottom bar) | Status icons #666666 on white, 5.74:1. Navigation buttons #949494 on white, 3.03:1 (`6.2-api28-light-profile-nav-buttons.png`). |
| Avatar crop (personal photo picked, then Cancelar) | Light system icons on the #0A0A0A crop surface: status icons #C0C0C0, 10.88:1; navigation buttons #959595, 6.61:1 (`6.2-api28-light-crop-light-icons.png`). After Cancelar, Perfil is back to dark icons: 5.74:1 and 3.03:1 (`6.2-api28-light-profile-after-crop-dark-icons.png`). |
| Drawer | Opens over pool home in light theme (`6.2-api28-light-poolhome-drawer-open.png`). |

- The navigation buttons are One UI's own light-mode color (#949494). They meet 3:1 against white with little margin. Before this change's transparent bar, the default light scrim was #E6FFFFFF over the same white surface, so the buttons' background and contrast are effectively unchanged.
- Unchanged afterwards: 1080×2220 override, 420 dpi, font 1.0, rotation 0 with auto-rotate on. The theme was left in light for Felipe to switch back.
