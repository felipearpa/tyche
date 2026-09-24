# Verification record

## iOS — tasks 2.1–2.9 (2026-09-22)

Xcode 26.0.1 (`/Applications/Xcode_26.0.1.app`, iPhoneSimulator 26.0 SDK). Captures live in
[`verification/ios/`](verification/ios/).

| Simulator | Runtime | Used for |
| --- | --- | --- |
| iPhone 18 Pro `4DC816B1-…` | iOS 27.0 | Felipe's signed-in session, production backend, working-tree Debug build of `Tyche` |
| iPhone 17 `4D209B45-…` | iOS 27.0 | Workspace test suite; the throwaway drawer harness (no Fortuna data) |
| iPhone 16 Pro `A2F5EF6F-…` | iOS 18.1 | UI package tests (ViewInspector traps on the iOS 26/27 runtimes) |

### Instruments (not committed)

- **Harness app.** A throwaway app compiled with `swiftc -target arm64-apple-ios16.0-simulator`
  from the unchanged `DrawerView.swift`, `DrawerReveal.swift`, and `DrawerStyle.swift`, plus
  stand-ins for the two generated catalog symbols. It hosts a `NavigationStack` list with a toolbar
  opener, a drawer menu (optionally 30 extra rows), scripted programmatic requests, a
  `requestGeometryUpdate` rotation trigger, and a dump of the UIAccessibility hierarchy. Compiling at
  an iOS 16 target also proves the container uses no unguarded newer API. A second build changes only
  the spring response (0.35 s → 3 s) so an in-flight transition can be grabbed by a scripted touch.
- **Accessibility dump for the real app.** A dylib injected with
  `SIMCTL_CHILD_DYLD_INSERT_LIBRARIES` that walks the UIAccessibility hierarchy the way VoiceOver
  does (skipping `accessibilityElementsHidden` subtrees) and prints labels, traits, and frames.
- **Network shim.** A second injected dylib that delays (12 s) or cancels only
  `GET pools/<id>/gamblers/<id>` — the drawer's pool summary — to stage loading and failure.
- **Motion.** `simctl io recordVideo` (variable frame rate), frames extracted with
  `ffmpeg -fps_mode passthrough`, timestamps from `ffprobe`, and per-frame tracking of a colored
  marker on the pushed screen (rank tiles or the selected tab). Endpoints are exact: the marker
  moves by exactly the 340 pt drawer width. *(Corrected by
  [iOS — navigation title position](#ios--navigation-title-position-2026-09-24): these markers
  move with the pushed screen in portrait, but its large title does not, and in landscape neither
  do its rows or menu button.)*

### Automated coverage (2.8)

```bash
# UI package: new DrawerRevealTests, DrawerPhaseTests, DrawerLayoutTests, DrawerInteractionTests
cd iOS/UI && xcodebuild test -scheme UI -destination 'id=A2F5EF6F-BA25-4035-9494-F6ACE0144047'

# Workspace suite (PoolTests trap locally on these runtimes — known ViewInspector issue)
cd iOS && xcodebuild test -workspace Tyche.xcworkspace -scheme Tyche \
  -destination 'id=4D209B45-123A-426F-8ABC-B882E7211E6B' -parallel-testing-enabled NO \
  -skip-testing:PoolTests

# Opt-in drawer pass against the signed-in session (self-skips without the variable)
cd iOS && TEST_RUNNER_DRAWER_UI_PASS=1 xcodebuild test -workspace Tyche.xcworkspace -scheme Tyche \
  -destination 'id=4DC816B1-F082-4C29-98D2-1E6CE4B80185' -parallel-testing-enabled NO \
  -only-testing:TycheUITests/DrawerPassUITests
```

- UI package, iOS 18.1: **33 passed, 0 failed** — 24 new Swift Testing cases in 4 suites plus the
  9 existing XCTest cases. The new cases cover requests settling at the requested endpoint,
  reversal, drag takeover from the presented progress,
  release by displacement and by velocity, cancellation returning to the settled endpoint,
  vertical drags ignored, a request during a drag, clamping, right-to-left axes, phase derivation,
  width bounds, and — through ViewInspector — hit isolation, hidden accessibility content,
  dismissal, and single destination dispatch at the closed, open, and mid-transition states
  (one tap each; see [iOS — double dispatch fix](#ios--double-dispatch-fix-2026-09-24) for two
  activations before the next update).
- Workspace suite, iPhone 17 (iOS 27.0): **159 tests — 148 passed, 6 skipped, 5 trapped**. The
  skips are the opt-in live passes (`AvatarSanityPassUITests` ×2, `DrawerPassUITests` ×2) and the
  two read-only avatar wire probes in `AccountTests`. The 5 traps are all of
  `DrawerInteractionTests`, which ran inside the scheme's UI package bundle on this runtime:
  `Fatal error: Can't unsafeBitCast between types of different sizes` — the ViewInspector 0.10.2
  toolchain issue that already traps `PoolTests` on the local iOS 26/27 runtimes. The same five
  pass on iOS 18.1 (above); CI's Xcode 26.6 was not run. Locally, add
  `-skip-testing:UITests/DrawerInteractionTests` on iOS 26/27 runtimes, as for `PoolTests`.
- `DrawerPassUITests`, signed-in iPhone 18 Pro: **2 passed, 0 failed** (pool list 17.7 s, pool home
  56.1 s over three pools: one as a member, two as owner). It checks that drawer actions exist only
  while open, the pushed screen cannot be hit while open, a tap on the visible strip over a pool
  row closes the drawer without opening the pool, Profile opens once and returns with the drawer
  closed, Sign out is reachable (never tapped), Gamblers and Delete pool appear only for the owner,
  Delete pool asks for confirmation (cancelled), and Invite closes the drawer and opens the share
  sheet (closed unsent).
- The pass checks the pushed screen with `isHittable`, not `exists`: XCUITest's automation tree
  still lists the UIKit-hosted `NavigationStack` that the drawer hides from VoiceOver, while it
  does drop the drawer's own hidden SwiftUI content. The VoiceOver-visible tree is recorded
  separately below.
- Builds: the UI package (`platforms: [.iOS(.v16)]`) and the app (deployment target 16.0) build
  with no new warnings in the touched files. No `Package.swift`, `Package.resolved`, or project
  file changed; the drawer files contain no `UIViewRepresentable`, gesture recognizer, display
  link, `TimelineView`, or UIKit color. `Package.resolved` was checked after every `xcodebuild`
  run and never changed.

### 2.1 — Interruptible reveal state

Harness, iPhone 17, normal motion, recording `reverse` (opener x in points; closed ≈ 41–45,
open ≈ 380):

| Script | Observed |
| --- | --- |
| open, close after 0.12 s | 117 → 155 → 182.7 (peak) → 173 → … → 51 → closed; never approached open |
| open, close after 0.10 s, open again 0.06 s later | 190 → 212 (peak) → 199 → 192.7 → 220 → … → 379.8, settled open |
| close | settled closed; final frame matches the initial closed frame |

Each reversal continues from the displayed position (no jump to the opposite endpoint) and settles
at the requested one. The binding API (`drawer(isShowing:content:)`) and the style entry points
(`drawerStyle(_:)`, `DrawerStyle`, `DrawerStyleConfiguration`) are unchanged.

### 2.2 — Group reveal and pushed-screen treatment

> **Corrected by [iOS — navigation title position](#ios--navigation-title-position-2026-09-24).**
> The pushed screen does not keep its layout while it moves. In the strips below, its large title
> sits at its leading edge in the ≈25 %, ≈50 %, ≈75 %, and open frames instead of 16 pt in. In
> landscape, not shown here, its rows, menu button, and title also lose the leading safe-area
> inset. This is accepted as a known iOS limitation. What this section says about the drawer's
> content group, and about the offset, scrim, corners, and edge moving with the same progress,
> still holds.

- Strips from recordings at closed, ≈25 %, ≈50 %, ≈75 %, and open:
  [pool list, light](verification/ios/reveal-strip-pool-list-light.jpg),
  [pool home, light](verification/ios/reveal-strip-pool-home-light.jpg),
  [pool home, dark](verification/ios/reveal-strip-pool-home-dark.jpg). The content group grows
  from 0.95 scale and fades in as one unit (header, summary, rows, footer); the opaque surface
  never scales or fades; line breaks are identical in every frame (scale is a render transform, so
  no reflow). The pushed screen's offset, black scrim (0.18 light / 0.24 dark; the former white
  wash in dark mode is gone), 24 pt leading corners, and hairline edge move with the same progress.
- Recordings: [pool list open/close](verification/ios/list-light-open-close.mp4),
  [pool home open, drag reversal, close](verification/ios/home-light-open-drag-close.mp4),
  [pool home dark](verification/ios/home-dark-open-close.mp4).
- Settling: opening takes ≈ 0.45 s and closing ≈ 0.6 s; the tracked marker never passes either
  endpoint (at most 0.1 pt, within tracking noise), and the content stays visible until the closing
  frame that reaches the endpoint.
- Previews: `#Preview` blocks declare the closed, half-open (`DrawerReveal(heldAt: 0.5)`), open,
  dark, and right-to-left container states. They were not rendered in Xcode here; the strips above
  are the rendered equivalents from the running app.

### 2.3 — Direct manipulation

> **Superseded by [2.10](#ios--task-210-2026-09-22) for drag regions.** 2.3 limited drawer drags
> to the strip and the drawer's non-interactive space and had no opening swipe. The **Release**
> region wording, the drawer-row half of **Vertical scrolling**, and all of **Navigation stack**
> below describe that arrangement and no longer hold. Takeover, release settling, cancellation,
> and right-to-left were re-checked with the 2.10 arrangement.

- **Grabbing an in-flight transition** (harness, 3 s spring): the opening was 92 % on screen when
  the drag was recognized. From then on each frame moved by exactly the finger's travel (−18, −20,
  −20, −20, −10 pt); released at ≈ 67 % with no velocity, it settled open from the displayed
  position. [Clip](verification/ios/harness-takeover-slow-spring.mp4).
- **Release** *(regions superseded by 2.10)*: a 105 pt flick on the strip or on non-interactive
  drawer space closes; a drag toward closed and back within one gesture settles open (app, pool
  home: the tracked tab followed the finger 274 → 214 → 274 → 364 pt, then settled open).
- **Cancellation**: a drag held at ≈ 20 % (a release there would close) was cancelled by
  backgrounding the app; on return the drawer was at its previously settled open endpoint, fully
  interactive, and the binding never changed. Rotating mid-drag also cancels and returns open.
- **Vertical scrolling** *(drawer-row behavior superseded by 2.10)*: long drawer content scrolls in
  portrait and in a 402 pt tall landscape window without moving the drawer; horizontal drags from
  the header or empty space close it. ~~A drag that starts on a drawer row stays with the row (the
  row activates on release, the drawer does not move) — the design limits drawer drags to
  non-interactive regions.~~ An earlier simultaneous-gesture variant activated the row *and*
  closed the drawer; that was replaced.
- **Right-to-left**: the drawer reveals from the right; a rightward drag closes it and a leftward
  drag keeps it open (harness and app).
- ~~**Navigation stack**: with the drawer closed there is no drawer gesture on the screen, so the
  leading-edge pop from a pushed destination works (harness) and a horizontal swipe on the closed
  pool list does not open the drawer. Opening is by the menu opener only, which the design allows.~~
  *(Superseded by 2.10: hosts now open by swipe, and destinations detach the drawer's drag.)*

### 2.4 — Dismissal isolation and destinations

- Tapping the pushed screen exactly over its (moved) opener closes the drawer without activating
  the opener (harness log). In the app, `DrawerPassUITests` taps the strip over a pool row: the
  drawer closes and the pool does not open.
- Five taps across the pushed screen and the drawer while the reveal was kept permanently in flight
  (toggled every 0.25 s) activated nothing — no row, opener, toolbar button, Profile, or Sign out.
- Profile dispatched exactly once, closed the drawer, and pushed its destination; the edge pop
  returned to the root with the drawer closed (harness log, and app via `DrawerPassUITests`).
  *(Single taps only. Two activations handled before SwiftUI's next update were not covered here;
  see [iOS — double dispatch fix](#ios--double-dispatch-fix-2026-09-24).)*
- Invite closes the drawer and opens the share sheet
  ([capture](verification/ios/pool-home-invite-share-sheet.jpg)); Delete pool presents the existing
  confirmation, cancelled in the pass. No pool was deleted.

### 2.5 — Accessibility and Reduce Motion

Real app, VoiceOver-style traversal via the injected dump:

- Closed: the pool list's rows, "Open menu" (new label; it previously read as the avatar's initial),
  the create button, and "My pools". No drawer content.
- Open (pool list): "felipearpa, felipearcila@gmail.com", Profile, Sign out, Close menu. The pushed
  screen — navigation bar, rows, opener — is absent.
- Open (pool home, owner): account, Profile, "Playing now, Copa Mundial de la FIFA prur, Rank 1,
  41 points", "THE POOL" (header), Invite, "Gamblers, 1", Delete pool, Sign out, Close menu.
- Harness: VoiceOver's escape walk from a focused drawer element is handled one container up and
  closes the drawer; escape and activation on "Close menu" close it too. The escape action had to
  sit inside the drawer's scroll view — the scroll view is a platform view, and SwiftUI containers
  above it are not in the chain VoiceOver walks.
- Reduce Motion (app and harness): closed → open and open → closed are each a single frame, with no
  scale or spatial animation ([clip](verification/ios/list-reduced-motion.mp4)); the open endpoint's
  accessibility tree and interaction are reached without animation frames; a drag release also
  settles immediately. The spec allows "an immediate change or brief non-spatial fade"; a fade was
  tried first, but animating only opacity still slid the pushed screen because its offset changes in
  the same animated transaction, and scoping that away needs iOS 17's `animation(_:body:)`.
- **Not verified:** VoiceOver focus movement. The container sets `@AccessibilityFocusState` to the
  element marked `drawerInitialFocus()` (the account identity) once opening settles, and to the
  control marked `drawerOpener()` once closing settles. The simulator has no VoiceOver and the
  device cannot be driven from here, so neither move was observed.

### 2.6 — Layout

- [Pool list, light](verification/ios/pool-list-open-light.jpg),
  [pool home owner, light](verification/ios/pool-home-owner-open-light.jpg),
  [owner, dark](verification/ios/pool-home-owner-open-dark.jpg),
  [member, dark](verification/ios/pool-home-member-open-dark.jpg); before the change:
  [pool list](verification/ios/before-pool-list-open.jpg),
  [pool home](verification/ios/before-pool-home-open.jpg).
- Width: 340 pt on the 402 pt phone (62 pt dismissal strip); unit tests pin 318.75 pt at 375 pt,
  the 340 pt bound at 1024 pt, 340 + leading inset in landscape, and ≥ 44 pt of strip at 200 pt.
  Harness landscape ([short window, scrolled](verification/ios/harness-short-window-scrolled.jpg)):
  a 402 pt surface (340 + 62 pt inset) with content clear of the inset, scrolling to Sign out.
- Largest accessibility size (`accessibility-extra-extra-extra-large`):
  [pool list](verification/ios/pool-list-open-largest-text.jpg),
  [pool home, top](verification/ios/pool-home-open-largest-text-top.jpg),
  [scrolled](verification/ios/pool-home-open-largest-text-scrolled.jpg). The identity stacks
  vertically, every action including the separated Sign out is reachable by scrolling, and the
  drawer stays open while scrolling. Two fixes came out of this pass: row icons now grow to at most
  36 pt, and at accessibility sizes the Gamblers count moves below its label ("Gamblers" had
  wrapped mid-word).
- [Right-to-left](verification/ios/pool-list-open-rtl.jpg) (`-AppleTextDirection YES
  -NSForceRightToLeftWritingDirection YES`): mirrored layout and gestures.
- Supporting text (email, section title, statistics) uses on-surface at 70 %: 5.95:1 on the light
  surface, 5.72:1 on the light group, 7.36:1 and 6.83:1 in dark. The system secondary label it
  replaces measures 3.4:1 on light surfaces. Delete pool: 7.33:1 light, 5.20:1 dark. The accent
  trophy is decorative (the "Playing now" text carries the meaning).
- `DefaultDrawerStyle` now uses the shared `Surface` color asset; the `Color(uiColor:)` and
  `Color(.separator)` bridges are gone.

### 2.7 — Pool summary, data, and icons

- Loaded ([light](verification/ios/pool-home-owner-open-light.jpg)): inset `SurfaceVariant` group,
  accent trophy, pool name, "1º · 41 points", one combined announcement.
- Loading ([capture](verification/ios/pool-home-summary-loading.jpg), request delayed 12 s): the
  same `PoolSummaryItem` rendered from `poolGamblerScorePlaceholderModel()` under the shared
  `.shimmer()`; absent from the accessibility tree while every other action stays available; loads
  in place when the request completes.
- Failure ([capture](verification/ios/pool-home-summary-failure.jpg), request cancelled): the
  existing `ErrorView` inside the same group; Profile, Invite, Gamblers, Delete pool, and Sign out
  remain available.
- Owner and member visibility: see 2.4 and the member capture.
- Cached reopening: after cold start settled, three open/close cycles on the pool list issued no
  network task at all (CFNetwork diagnostics; the preceding 25 s cold-start window, as a control,
  showed 2 API calls and 2 avatar revalidations answered `304`). The header kept its photo, name,
  and email. The drawer content stays in the hierarchy, and its body is not re-evaluated per
  animation frame (the reveal is applied through animatable modifiers).
- Icons: no icon was added or replaced. The rows reuse the committed `filled_person`, `person_add`,
  `group`, `delete_forever`, `trophy`, and `log_out` assets, so the canonical-vector requirement is
  not triggered.

### 2.9 — Frame pacing and remaining issues

Frame intervals while the pushed screen was between endpoints (simulator recordings, variable frame
rate):

| Capture | Frames in motion | Median interval | Max interval | Intervals > 25 ms |
| --- | --- | --- | --- | --- |
| Pool list open, light | 26 | 16.7 ms | 25.0 ms | 0 |
| Pool list close, light | 28 | 16.7 ms | 33.3 ms | 1 |
| Pool home open, light | 10 (tab leaves the screen) | 16.7 ms | 20.0 ms | 0 |
| Pool home close, light | 29 | 16.7 ms | 21.7 ms | 0 |
| Pool home close, dark | 29 | 16.7 ms | 30.0 ms | 1 |

Remaining issues and limits:

- Simulator pacing on a Mac is not device pacing; no physical device was measured.
- VoiceOver focus movement is unobserved (see 2.5).
- iOS 16 and 17 behavior is covered only by compiling at an iOS 16 target; no such runtime is
  installed.
- Resizing mid-*animation* could not be staged: the scripted rotation took effect only after the
  opening settled. Rotating mid-drag cancels the drag and lands on the settled endpoint with
  aligned interaction bounds ([capture](verification/ios/harness-rotated-during-drag.jpg)). No
  iPad or Stage Manager window was exercised; the signed-in session exists only on the iPhone 18 Pro.
- Pre-existing, outside this change: `ErrorView`'s icon and the pool list's create button announce
  their asset names ("sentiment_dissatisfied", "filled_add_circle"), and the `log_out` asset does
  not mirror in right-to-left.

## iOS — task 2.10 (2026-09-22)

Revised gesture rules: swipe open from anywhere on a host while its navigation path is empty, drag
closed from anywhere on the open drawer or the strip, a recognized drawer drag cancels the control
it started on, and destinations detach the drawer's drag. Same Xcode and simulators as above;
the review-fix harness checks also used an iPhone Air and an iPad Pro 11-inch (M5), both iOS 27.0.
Captures are in [`verification/ios/`](verification/ios/).

A review of the first 2.10 implementation asked for five fixes, made on 2026-09-23.
[Review fixes](#review-fixes-2026-09-23) says what each changed; statements below that a fix
changed are marked with it.

### Arrangement

- One `DragGesture` on the drawer container (above the drawer, the pushed screen, and the
  dismissal surface), attached with `highPriorityGesture(_:including:)` and a 20 pt minimum
  distance. High priority is what cancels the row, opener, or strip button under the finger once
  the drag is recognized; a tap never travels 20 pt, so it still activates.
- When the drawer declines a drag — vertical first, toward the leading edge while resting closed,
  or starting on a host's tab bar while closed — the gesture's mask switches to `.subviews` for
  the rest of that touch. Detaching hands the touch back to UIKit views beneath, such as the tab
  bar; a gesture attached mid-touch waits for the next touch. *(Fix 2)* The declined flag lives
  in the gesture's `@GestureState` together with whether a touch is active, so SwiftUI resets it
  when the touch ends, is cancelled, or the gesture detaches; no separate state waits for a render
  to be reset. A drag the reveal model still holds when a new touch starts (left by a touch that
  ended before any render observed it) is finished as a cancellation first.
- *(Fix 1)* A cancelled drag returns to the endpoint the reveal last came to rest at, and the
  binding follows, even when the drag had taken over a transition heading for the other endpoint.
  2.10 first returned to the latest requested endpoint.
- `.drawer(isShowing:allowsDragging:content:)` is additive (default `true`). Both routers pass
  `path.isEmpty`; `false` detaches the gesture through the same mask, so a destination's back
  button and back-swipes never compete with it, including while the drawer finishes closing after
  Profile, Gamblers, or another destination was chosen.
- *(Fix 3)* `excludesTabBarFromDrawerDrags()` on pool home's `TabView` and
  `drawerTabBarBoundary()` on each of its three tab roots (new, UI package) replace 2.10's
  `excludesBottomBarFromDrawerDrags()`, which assumed the bar sits in the bottom safe area. They
  locate the bar from two safe frames: the tab view's, which excludes the bar, and a tab root's,
  which ends at it. A bar below the content (iPhone) is reserved from its top to the screen's
  bottom edge; a bar above the content (iPad in regular width) from the tab view's safe top to the
  content. While closed, the drawer declines drags that start in that band. Open, the band is part
  of the pushed screen and drags there close the drawer.
- No environment value is read by row or button styles, and no host control changed. No UIKit
  bridge, pop-gesture override, timer, or dispatch was added (checked by grep over the drawer
  files and routers).

### How the arrangement was chosen

Throwaway harness (`swiftc -target arm64-apple-ios16.0-simulator`, the drawer files plus a
`NavigationStack` host mirroring the pool list and pool home: tap-gesture rows holding a small
button, full-width button rows, a toolbar opener, a `TabView`, a `.refreshable` scroll view,
optionally a horizontal chip scroller), driven by real touch paths on the iPhone 17 (iOS 27.0) and
iPhone 16 Pro (iOS 18.1) simulators, with activations and drawer phases logged.

| Container drag | iOS 27 result | iOS 18.1 result |
| --- | --- | --- |
| High priority, 10 pt | Vertical drags never scroll (the drag is recognized before the scroll view) | Not run |
| High priority, 15 pt and 20 pt | Scroll views keep vertical drags; drags from button rows, tap rows, and the opener move the drawer and activate nothing | Same |
| Simultaneous | Scrolling works; a drag from a button row opens the drawer *and* pushes the row's destination; a 40 pt drag from the opener also toggles it | Not run |
| Normal priority | Tab bar keeps its drag; a drag from a tap row reaches the drawer only at release (no tracking), and one from a button row stays with the button | Not run |
| High or simultaneous, drag starting on the iOS 26+ tab bar | The drawer takes the drag and the tab bar's slide-to-select stops working | iOS 18 tab bar has no drag |
| High, gesture detached when a drag is declined | The tab bar resumes its slide mid-touch and selects the tab | — |

Measured on iOS 27: the scroll view's own threshold lies between 10 and 15 pt; 20 pt leaves a
margin. SwiftUI buttons under a recognized high-priority drag are cancelled (they do not resume
when it detaches); UIKit recognizers wait and resume. That margin holds only while a touch moves
in small steps: the review-fix checks found that a first move of 20 pt or more loses the scroll
(see [Remaining issues](#remaining-issues-and-decisions)).

### Automated coverage

```bash
# UI package (iOS 18.1): DrawerRevealTests, DrawerPhaseTests, DrawerLayoutTests,
# DrawerTabBarBandTests, and DrawerInteractionTests with its nested DragWiring suite
cd iOS/UI && xcodebuild test -scheme UI -destination 'id=A2F5EF6F-BA25-4035-9494-F6ACE0144047'

# Workspace suite (iOS 27.0, no Fortuna data); skipping DrawerInteractionTests also skips
# DragWiring, which needs the same ViewInspector-compatible runtime
cd iOS && xcodebuild test -workspace Tyche.xcworkspace -scheme Tyche \
  -destination 'id=4D209B45-123A-426F-8ABC-B882E7211E6B' -parallel-testing-enabled NO \
  -skip-testing:PoolTests -skip-testing:UITests/DrawerInteractionTests

# Drawer pass with real synthesized drags, signed-in session (iOS 27.0)
cd iOS && TEST_RUNNER_DRAWER_UI_PASS=1 xcodebuild test -workspace Tyche.xcworkspace -scheme Tyche \
  -destination 'id=4DC816B1-F082-4C29-98D2-1E6CE4B80185' -parallel-testing-enabled NO \
  -only-testing:TycheUITests/DrawerPassUITests
```

**As first implemented (2026-09-22).** *(Count corrected by fix 5: this record said
`DrawerRevealTests` gained 6 cases.)*

- UI package, iOS 18.1: **38 passed, 0 failed** (29 Swift Testing cases in 4 suites, including
  the 5 ViewInspector `DrawerInteractionTests`, plus 9 XCTest cases). `DrawerRevealTests` gained
  **5 new** cases: a closed drawer claims a trailing drag and tracks from 0; declines a leading
  drag (and stays declined if the finger turns back); declines a drag from the reserved bar; an
  open drawer claims drags from the bar area and trailing drags; a closing drawer claims a leading
  drag from its presented progress. **2 cases from 2.8 were modified**: the vertical-drag case
  now also checks that its first update reports the decline, and the request-during-a-drag case
  checks that the drawer keeps the touch.
- Workspace suite, iPhone 17: **161 tests — 153 passed, 8 skipped, 0 failed**. Skips are the
  opt-in live passes (`AvatarSanityPassUITests` ×2, `DrawerPassUITests` ×4) and the two
  read-only avatar wire probes.
- `DrawerPassUITests`, signed-in iPhone 18 Pro: **4 passed, 0 failed** — the two 2.9 passes
  unchanged (pool list 17.2 s, pool home 69.6 s) and two new gesture passes:
  - `testPoolListDrawerGestures` (33.2 s): a 260 pt swipe starting on a pool row opens the drawer
    and does not open the pool; a drag starting on Profile closes it and does not open Profile; a
    leading swipe on a row and a vertical drag leave the drawer closed and open nothing; Profile,
    opened by tap, returns to the list with the drawer closed after both a content-area
    back-swipe and a left-edge back-swipe.
  - `testPoolHomeDrawerGestures` (42.7 s): a swipe starting on another gambler's leaderboard row
    opens the drawer without opening that gambler; a drag starting on Invite closes it without
    presenting the share sheet; tapping a tab selects it and sliding a finger along the tab bar
    selects the third tab without opening the drawer; a vertical drag scrolls the leaderboard
    (first row moved up more than 50 pt) without opening the drawer or a gambler; tapping a
    gambler opens their bets once and a content-area back-swipe returns with the drawer closed.

**After the review fixes (2026-09-23), final code.**

- UI package, iOS 18.1: **52 passed, 0 failed** — 43 Swift Testing tests in 6 suites plus the
  9 XCTest cases. Changes since the first implementation:
  - `DrawerRevealTests`: **3 new** — cancelling a grabbed opening returns closed and cancelling a
    grabbed closing returns open, where each last rested (fix 1); a swipe opens after a declined
    drag finishes (fix 2). **3 modified** — the cancellation case checks the endpoint it returns
    (fix 1); finishing after a release returns no endpoint (fix 1); with no measured width, a
    claimed drag is kept without moving the reveal, because claim and decline are now decided
    before the width guard (fix 2).
  - `DrawerTabBarBandTests` (**new suite, 3 tests**, fix 3): a bottom bar's band reaches the
    screen edge; a top bar's band spans from the tab view's safe top to the content and leaves
    the bottom strip open; no band without a bar or before layout. The frames are the ones
    measured in the harness on the iPhone Air and the iPad.
  - `DrawerInteractionTests.DragWiring` (**new nested suite, 8 tests, 10 cases**; fixes 1, 2, 3,
    4a, 4d). It feeds synthesized values through the gesture the container attaches, so it
    covers wiring and decisions, not arbitration. With `allowsDragging: false` the gesture mask
    is `.subviews`, against `.all` for a host, whether the drawer rests closed, rests open, or is
    still closing after a destination was chosen. Cancelling a grabbed opening or closing through
    the container's handler returns the binding to closed or open and dispatches nothing. A drag
    an earlier touch left in the model does not decide the next touch. Through the container's
    handler, a closed drawer claims a trailing drag and declines a leading one, declines a drag
    that starts in the reported tab-bar band and claims one just above it, and, open, claims a
    drag that starts in the band. Hosted, the tab-bar modifiers report the band within half a
    point of the bar's top, in the global coordinates the drag's start location uses.
- Workspace suite, iPhone 17: **167 tests — 159 passed, 8 skipped, 0 failed**. The six added
  tests are the three new `DrawerRevealTests` cases and `DrawerTabBarBandTests`; `DragWiring`
  is skipped with `DrawerInteractionTests`. The skips are the same eight as before.
- `DrawerPassUITests`, signed-in iPhone 18 Pro: **4 passed, 0 failed** (pool list 17.5 s, pool
  home 61.0 s, pool list gestures 50.6 s, pool home gestures 52.4 s). New steps:
  - Pool list (fix 2): after a quick leading flick and a quick vertical flick on a pool row, a
    swipe from that row opens the drawer, and a 200 pt drag starting on the strip closes it; no
    pool opens.
  - Pool list (fix 4b): the vertical drag now runs after relaunching with the largest
    accessibility text, where the three pools overflow the screen, and asserts that the first row
    moved up more than 50 pt. The step waits 3 s after the relaunch and drags slowly; see the
    second failed run below.
  - Pool list and pool home (fix 4c): the content-area back-swipe runs only on iOS 26 and later;
    the left-edge back-swipe runs everywhere.
  - Pool home (fix 2): after the tab-bar slide and the vertical drags, all declined by the drawer,
    a swipe from a gambler's row opens the drawer and a tap on the strip closes it without
    opening the gambler.
- Two earlier runs of the updated pass each failed one step, and the test now handles both:
  - `testPoolHomeDrawer`, unchanged since 2.9: right after the share sheet was dismissed, XCUITest
    reported the app's window at `{inf, inf}` and failed the hittability check on the opener
    outright. The screen recording ended on pool home with the drawer closed. The pass now waits
    until the window reports a frame before checking hittability.
  - The largest-text scroll step, immediately after the relaunch: the list did not move (its
    scroll bar stayed at 0 %). The same step had passed in the run before. The harness then
    showed why (see Remaining issues): a first move of 20 pt or more, which a main-thread stall
    during the relaunch can produce, loses the scroll to the drawer's drag.
- Builds: the files these runs recompiled (the drawer files and the UI pass) produced no compiler
  warnings; the harness compiled the drawer files at an iOS 16 target. `Package.resolved` was
  checked after every `xcodebuild` run and never changed.

### Real drags in the app (signed-in iPhone 18 Pro, iOS 27.0)

Recorded with `simctl io recordVideo`; a colored marker on the pushed screen was tracked per frame.

- **Pool list** ([clip](verification/ios/list-swipe-from-row-and-profile.mp4)): a drag starting
  on the first pool row, finger 70 → 200 pt, moved the row's rank tile 24 → 134 pt in exact 10 pt
  steps for each 10 pt of finger travel (the first 20 pt are the recognition distance); moving
  back to 120 pt brought it to 54 pt; releasing past the midpoint settled open at 364 pt (the
  340 pt drawer width). A drag starting on Profile, finger 300 → 150 pt, moved it 364 → 234 pt in
  step, then settled closed at 24 pt on release. Neither the pool nor Profile opened.
- **Pool home** ([clip](verification/ios/home-tab-slide-swipe-from-rows-scroll.mp4),
  [frames](verification/ios/home-gesture-strip.jpg)): sliding along the tab bar from Scores to
  History selected History with the drawer closed; a drag from El mono's row tracked 1:1
  (avatar 99.6 → 229.6 pt for 150 pt of finger travel) and settled open; a drag from Invite
  tracked 1:1 and settled closed with no share sheet; a vertical drag scrolled the leaderboard.
- **Taps**: a tap on a leaderboard row opened that gambler's timeline once; a content-area
  back-swipe returned to pool home with the drawer closed.
- These recordings predate the review fixes, which did not change how a claimed drag tracks
  without main-thread load. The final code was exercised in the app through the
  `DrawerPassUITests` run above.

### Harness checks with the 2.10 files

| Check | iOS 27 | iOS 18.1 |
| --- | --- | --- |
| Tap on a button row: activates once, pushes | ✓ | ✓ |
| Tap on a drawer row: activates once | ✓ | ✓ |
| Swipe from a button row opens; row does not activate | ✓ | ✓ |
| Swipe from a tap-gesture row opens; row does not activate | ✓ (app pool rows) | ✓ |
| Drag from a drawer row closes; row does not activate | ✓ | ✓ |
| Drag from the opener (40 pt, released short) moves the drawer, opener does not toggle | ✓ | ✓ |
| Drag on the strip closes; a short strip drag settles open (no dismissal tap) | ✓ | short drag ✓ |
| Leading swipe on a closed host: nothing opens or activates | ✓ | ✓ |
| Vertical scroll of the host and of a long drawer | ✓ | ✓ (host) |
| Tab bar: tap selects; slide and flick select (iOS 26+); no drawer | ✓ | tap ✓; drag on the bar opens nothing |
| Swipe just above the tab bar opens the drawer | ✓ | ✓ |
| Destination: content-area back-swipe (iOS 26+) and left-edge back-swipe pop; drawer stays closed | ✓ | edge ✓ |
| Profile chosen from the drawer: dispatched once; back-swipe returns closed | ✓ | — |
| Grabbing an in-flight opening (3 s spring copy): recognized at 98 % presented, then 1:1 with no jump ([clip](verification/ios/harness-takeover-container-drag.mp4)) | ✓ | — |
| Drag held past the midpoint, cancelled by backgrounding: returns open, binding unchanged, row not activated | ✓ | — |
| Right-to-left (landscape): leftward opens from the right, rightward closes, rightward while closed is declined | ✓ | — |

### Review fixes (2026-09-23)

The harness was rebuilt from the final drawer files. For these checks a second copy of
`DrawerView.swift` added log lines at the drag's updates, declines, end, and finish, and a third
changed only the spring response to 20 s so a transition can be grabbed and held (none of them
committed). Touches were real touch paths on the simulators; "backgrounding" means launching
Settings while the finger was held, which cancels the touch.

1. **Cancellation restores the last settled endpoint.** The reveal model records the endpoint the
   reveal last came to rest at (only when the model still rests there, so a phase change that
   arrives after a drag took over does not count). A cancelled drag returns there, and the binding
   is set to match, so the caller's state agrees with where the drawer ends up. Unit-tested in
   both directions (model and container handler). Harness, iPhone Air, 20 s spring:
   - Opening grabbed 6 s after tapping the opener, held past the midpoint toward open, then
     backgrounded: no release reached the drawer (no `onEnded`); it returned closed and the
     binding became `false` ([capture](verification/ios/harness-cancel-grabbed-opening.jpg)).
   - From open, a tap on the strip started the closing; the closing was grabbed, held past the
     midpoint toward closed, then backgrounded: it returned open and the binding became `true`
     ([capture](verification/ios/harness-cancel-grabbed-closing.jpg)).
2. **A declined drag cannot leave the drawer stuck.** The declined flag moved from a view state,
   which one gesture-state change had to reset, into the gesture's `@GestureState`, and a drag
   the model still holds at the start of a new touch is finished first. Instrumented harness,
   iPhone Air, main thread blocked for 45 ms of every 50 ms:
   - A quick vertical flick arrived as one batch: the drag was recognized at 90 pt, declined, and
     ended before any render. The model held no drag by the next render, and a trailing swipe
     right after it opened the drawer. The same happened after a quick leading flick.
   - Tab host: a slide along the tab bar was declined, the gesture detached, its state reset
     (logged at the next render), and the bar selected History; a trailing swipe on a row then
     opened the drawer. Without load, the same sequence works on the iPad below.
   - In the app, `DrawerPassUITests` opens the drawer by swipe after declined flicks (pool list)
     and after the declined tab-bar slide and vertical drags (pool home).
   - The instrumented runs also showed that under this load a second update of the same touch
     can arrive before a render and start again from the uncommitted gesture state. It is then
     decided afresh from the whole translation, which gives the same decision unless the finger
     turned in between. The reveal then trails the finger by the distance travelled at that later
     update (70 pt instead of 30 pt in one run) instead of by the recognition distance. The code
     comments now say so; see Remaining issues.
3. **The tab bar is reserved wherever the system puts it.** See Arrangement. Harness:
   - iPad Pro 11-inch (M5), iOS 27.0, portrait (regular width, bar at the top): sliding along the
     bar from Scores to History was declined by the drawer and selected History; a trailing swipe
     starting just below the bar opened the drawer, and so did one starting in the bottom
     home-indicator strip ([capture](verification/ios/harness-ipad-top-tab-bar-swipe.jpg)); a
     trailing swipe starting beside the floating bar, inside its band, was declined.
   - iPhone Air, iOS 27.0: a trailing swipe starting in the 21 pt below the floating bar, and one
     starting beside it, were declined; with the reservation removed, the same swipe from below
     the bar opened the drawer. See Remaining issues.
   - Not observed: the app itself on iPad. Felipe's signed-in session exists only on the iPhone 18
     Pro, so the iPad was checked in the harness and by `DrawerTabBarBandTests`.
4. **Test gaps.** (a) `DragWiring` checks the gesture mask for `allowsDragging` true against false
   in all three drawer states, including closing after a destination was chosen. In the harness
   (20 s spring), with Profile chosen and the drawer still closing, leading and trailing drags on
   the pushed screen never reached the drawer's gesture (no update logged); Profile was dispatched
   once, and after the drawer closed a content-area back-swipe popped Profile. (b) The pool-list
   vertical drag asserts that the list scrolled. (c) Content-area back-swipes are gated to
   iOS 26 and later; the edge back-swipe runs everywhere. (d) `DragWiring` routes the tab-bar
   band through the container's handler and checks the modifiers' reported band in a hosted view.
5. **This record.** The first-implementation test count is corrected above and new cases are
   told apart from modified ones; the decisions below are recorded; the coverage numbers are from
   the final runs.

### Remaining issues and decisions

- **Press-and-hold feedback on host and drawer buttons.** A high-priority drag delays every
  SwiftUI control beneath it until the drag fails, which is at touch-up. Taps still activate
  once, but a finger held on a row shows no pressed state until release: in the app, a leaderboard
  row held for 1.5 s stayed at its unpressed white, and the harness logged `isPressed` only at
  release (without the drawer's drag it turned on about 150 ms after touch-down). This applies
  only on the two hosts while their path is empty; destinations are unaffected. The design's
  fallback (a simultaneous drag plus an environment value honoured by row and button styles)
  would keep the pressed state but needs every affected host control — row styles, toolbar
  buttons, the pool row's invite button, pending-bet buttons — to opt in, because simultaneous
  drags let buttons and the opener fire. **Decision (Felipe, 2026-09-22): trade-off accepted;
  he declined an experiment to restore the pressed state.**
- **Confirmed (Felipe, 2026-09-22):** the 20 pt minimum distance, and
  `excludesBottomBarFromDrawerDrags()` on pool home's tabs. Fix 3 replaced that modifier with
  `excludesTabBarFromDrawerDrags()` on the tab view and `drawerTabBarBoundary()` on the same three
  tab roots, so the bar is also found at the top; its role is unchanged.
- **A first move of 20 pt or more loses the scroll.** The list's pan recognizes within
  10–15 pt and keeps a vertical drag only if it gets there before the drawer's 20 pt drag. When
  the first move event already covers 20 pt, the drawer's drag is recognized first, declines,
  and detaches, and the list does not resume for that touch. Harness, iPhone Air, no load: first
  moves of 5, 10, and 15 pt scrolled; 20 pt and 25 pt did not; with the drawer's drag detached,
  the 25 pt drag scrolled. A main-thread stall while a finger starts moving can produce such a
  first move, the likely cause of the app's largest-text step failing once right after a
  relaunch; a later touch that starts in smaller steps scrolls normally. Raising the distance
  narrows this but slows drawer recognition; avoiding it within the design's constraints needs
  its simultaneous-drag fallback. **Decision (Felipe, 2026-09-23): accepted. A vertical drag
  whose first move event already covers 20 pt or more, for example after a main-thread stall,
  may lose the scroll to the drawer's drag for that touch. The minimum distance stays at 20 pt,
  and no simultaneous-drag fallback is added.**
- **The tab-bar band spans the full width.** SwiftUI reports where the bar begins but not
  its horizontal extent or, for the iOS 26+ floating bar, where it ends. On iPhone the band
  therefore runs from the bar's top to the screen edge. On iOS 18 that is exactly the tab bar's
  hit area. On iOS 26 and later it also covers the host content beside the floating bar and the
  21 pt below it (the home-indicator strip), where trailing swipes are declined although the rule
  says "anywhere on the host". On iPad the band covers the content beside the floating top bar.
  Bounding it would need a hard-coded bar margin or a UIKit lookup, which the design excludes.
  **Decision (Felipe, 2026-09-23): accepted. On iPhone with iOS 26 and later, trailing swipes
  that start in the 21 pt home-indicator strip below the floating tab bar, or beside the bar,
  don't open the drawer. No hard-coded bar margin is added.** **Decision (Felipe, 2026-09-23):
  the iPad case is accepted too. On iPad, where pool home's tab bar floats at the top, trailing
  swipes that start inside the top band beside the floating bar don't open the drawer. No
  hard-coded bar margin is added there either.**
- **Updates delivered before a render.** Under heavy main-thread load, SwiftUI delivered a second
  update of the same touch before committing the gesture state, and the container handled it as
  a touch's first update: the drag is decided again from its whole translation, and the reveal
  trails the finger by the travel at that update. The decision changes only if the finger turned
  within that stall. Observed only with the 45 ms-per-50 ms load.
- **Horizontal child scroll views.** A trailing drag that starts on a horizontal scroll view while
  the drawer is closed opens the drawer instead of scrolling it (harness chip row); leading drags
  still scroll it. Neither iOS host has such a control today; one added later would need the
  same kind of reservation as the tab bar.
- **Tab bar reservation timing.** On iOS 18.1, with the 2.10 modifier, a tab's content reported
  zero safe-area insets on its first appearance, which cleared the reservation until its layout
  settled; a later drag on that tab's bar was declined as expected. Neither this nor a drag in
  the first frames after switching tabs was re-staged with the fix-3 modifiers.
- Release projection uses SwiftUI's `predictedEndTranslation`. On the simulator a finger held
  still before lifting keeps its earlier velocity (no move events arrive), so a slow drag that was
  held can still settle by that velocity.
- Not observed: iOS 16 and 17 runtimes (compile-only, as before); a physical device; the app on
  iPad (harness only).

## Android — tasks 3.1–3.5 (2026-09-23)

Shared container only: `Android/ui/src/main/java/com/felipearpa/tyche/ui/PushDrawer.kt` and the new
`DrawerReveal.kt` in the same package. The drawer views (3.6), the pool summary (3.7), and the
device pass (3.9) are not part of this record. The public `PushDrawer(isOpen, onOpenChange,
drawerContent, modifier, content)` signature is unchanged, so `PoolScoreListView` and
`PoolHomeView` compile and call it as before. Captures are in
[`verification/android/`](verification/android/).

| Device | Used for |
| --- | --- |
| AVD `Pixel_10_Pro_XL`, Android 16 (API 36), en-US, 1344 × 2992 px at 480 dpi, gesture navigation, software rendering (SwiftShader) | `:ui` instrumented tests; Felipe's signed-in session with the working-tree `prodDebug` build installed by `adb install -r` |

Compose BOM 2026.05.00 resolves Compose UI and Foundation 1.11.1; Activity 1.13.0 (reached through
`navigation-compose`) supplies `PredictiveBackHandler`. No dependency, version catalog, or lockfile
changed. The drawer uses no Android View or XML widget and no third-party animation library.

### Arrangement

- **One reveal state.** `DrawerReveal` holds the progress on screen (0 closed, 1 open), the
  endpoint it heads for, the endpoint it last came to rest at, and an optional drag. Requests run
  Compose's suspending `animate` from the progress on screen with a critically damped spring
  (stiffness 500); a reversal keeps its momentum, and a start faster than the spring can absorb
  is capped so it never overshoots. A drag stops the running transition where it is and moves the
  same progress. The phase (closed, opening, open, closing, dragging) is derived from those values.
- **Caller ownership.** The caller's `isOpen` is the requested endpoint. A drag release, Back, or
  Escape animates to its endpoint and reports it through `onOpenChange`; if the caller keeps its
  value, the drawer returns to it. A tap or accessibility click on the pushed screen only asks the
  caller to close.
- **Visuals from the one progress.** Group scale 0.96 → 1 anchored at the logical leading center
  and alpha 0 → 1 (`graphicsLayer` after layout; scale dropped when system animations are off);
  pushed screen translation by the drawer width, leading corners 0 → 24 dp, a black scrim of
  0.18 (light) / 0.24 (dark) × progress inside its clip, and a 1 dp `outlineVariant` edge at
  alpha = progress. The progress is read only in layer and draw blocks, so animating it does not
  recompose. Closed, the drawer content stays composed but is not placed.
- **Drags.** One detector on the container, in the default Main pass, so descendants see each
  event first. After touch slop it decides: predominantly vertical is left alone; horizontal is
  consumed (which cancels the click of the row, tab, or button under the finger) and moves the
  reveal, except a leading drag while the drawer rests closed. The finger moves the drawer from
  the slop onward (the distance past slop in the claiming event counts). Release settles by
  velocity (400 dp/s) or else the nearer endpoint; a cancelled or interrupted gesture returns to
  the endpoint the reveal last rested at. Drags that start in the window's system-gesture insets
  are left to the system. Directions use the layout direction.
- **Isolation and dismissal.** Outside the closed endpoint the pushed screen is covered by a
  dismissal surface (drawn above it, moving with it), is cleared from semantics, and is closed to
  keyboard focus; drawer actions take touches only at the open endpoint. The dismissal surface
  follows Material's drawer scrim: a tap or an accessibility click (label "Close menu", button
  role) closes the drawer, but it is no keyboard focus stop.
- **Back.** `PredictiveBackHandler`, enabled while the drawer is not closed, inside the host
  destination, so it runs before the navigation host's own handler. Predictive progress moves
  the reveal; cancelling returns it to where it last rested; committing closes it.
- **Semantics and focus.** Pane title "Menu" on the drawer only while open; drawer content
  exposed only while open; traversal puts the drawer before the dismissal action. Keyboard focus
  moves into the drawer once it settles open and back to the pushed screen's previously focused
  control once it settles closed, if the host route is still resumed. Escape from a focused
  drawer control does what Back does (Compose would otherwise use it to move focus out of the
  control first). *(Superseded by the [review fix](#android--talkback-focus-review-fix-2026-09-23): no pane title, and "Close menu" is exposed only once
  the drawer has settled open. Keyboard focus superseded by the
  [keyboard focus fix](#android--keyboard-focus-review-fix-2026-09-24): focus in the drawer is
  cleared as soon as it starts to close, however it got there, returns once the route resumes,
  and keyboard input that begins while the drawer is open moves focus into it.)*
- **Animation duration.** Compose's animations follow the system duration scale, so with
  animations off every request and release settles at once, and focus and input endpoints follow
  the same frame; the decorative scale is also dropped then, including during a drag.
- New strings `close_menu_action` ("Close menu" / "Cerrar menú") and `menu_pane_title` ("Menu" /
  "Menú") in `values`, `values-es`, and `values-es-rES`. *(`menu_pane_title` was removed in the
  [review fix](#android--talkback-focus-review-fix-2026-09-23).)*

### Automated coverage

```bash
cd Android
./gradlew :ui:testDebugUnitTest
# One class per run (a hang stays contained); the AVD is emulator-5554.
ANDROID_SERIAL=emulator-5554 ./gradlew :ui:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.felipearpa.tyche.ui.<Class>
./gradlew :app:assembleProdDebug :app:testProdDebugUnitTest :pool:testDebugUnitTest
```

- `:ui:testDebugUnitTest`: `DrawerRevealTest` (JUnit 5, Kotest) **17 passed, 0 failed**.
- `:ui:connectedDebugAndroidTest`, final run of each class on the AVD, **44 passed, 0 failed**:

  | Class | Tests | Final run |
  | --- | --- | --- |
  | `PushDrawerRevealTest` | 5 | 5 passed |
  | `PushDrawerGestureTest` | 20 | 20 passed |
  | `PushDrawerDismissalTest` | 7 | 7 passed |
  | `PushDrawerAccessibilityTest` | 6 | 6 passed |
  | `PushDrawerDisabledAnimationsTest` | 2 | 2 passed |
  | `GestureDisableModifierTest`, `ProgressIndicatorTest` (existing) | 1 + 3 | 4 passed |

- `:app:assembleProdDebug` built; `:app:testProdDebugUnitTest` **54 passed** and
  `:pool:testDebugUnitTest` **2 passed**, 0 failed (the two host screens' modules).
- Earlier runs in this pass also failed on the environment. The AVD renders in software and the
  host's load average was about 14: two root captures missed Compose's one-second pixel-copy
  deadline, and one activity teardown timed out (`Activity never becomes requested state
  [DESTROYED]`) after its test's assertions had passed. The harness now retries a failed capture
  up to five times with a pause; the final reveal run needed no retry.
- One earlier class run failed the tabbing test: the first Tab already reported focus on the
  opener with the drawer closed, and the remaining Tabs walked the host. It passed alone (with
  logging: Tab cycled Profile ↔ Sign out) and in the final run, and it was not reproduced. The
  test now asserts that the drawer is open before tabbing, so a recurrence would show where it
  goes wrong.

The instrumented tests use a harness host (`PushDrawerHarness.kt`) with a clickable row, an opener,
a Material `PrimaryTabRow`, a horizontal `LazyRow`, and a vertical `LazyColumn`, and a menu with a
wrapping label, two rows, and a colored bar used to measure the group's scale and alpha.

### The previous attempt's run

- **The five accessibility failures were the assertions.** They looked for hidden nodes with
  `useUnmergedTree = true`. The unmerged tree keeps the descendants of a node with cleared
  semantics and nodes that are not placed; Compose documents it as the tree for testing and
  debugging, while accessibility uses the one that drops both (`SemanticsNode.replacedChildren`,
  and `getAllUncoveredSemanticsNodes` skips unplaced nodes). The found "Profile" and "hostRow"
  nodes were exactly those. The assertions now read the merged tree, and each state is also
  checked against the tree an accessibility service receives, read through `UiAutomation`.
- **The two keyboard failures were the input method.** `performKeyInput` hands keys straight to
  the Compose view, which in touch mode does not hold view focus, so Tab did nothing. The tests
  now press keys through the system input pipeline (`Instrumentation.sendKeyDownUpSync`), which
  leaves touch mode as a hardware keyboard does.
- **The hang was the emulator, not a test.** In that run's logcat no process on the device logs
  anything from 13:09:12 to 13:15:37 (the tab-row test took 394 s), and the capture for the next
  test, `given_a_closed_drawer_when_swiping_toward_the_trailing_edge_from_a_host_row_…`, ends at
  13:19:07, twelve minutes before the run was cancelled. That test passed alone (1/1) and in
  every later class run. The drawer starts animations only on a request or a release, and no
  test waits without a bound.

### 3.1 — One interruptible reveal state

- Unit tests (`DrawerRevealTest`, manual frame clock): an opening settles open and a closing
  settles closed, both monotonic; a close request mid-opening turns around from the displayed
  value and never reaches open; a drag stops a transition and holds its value; a request takes a
  drag over; a fast release toward an endpoint never passes it.
- Instrumented, host-driven (`PushDrawerRevealTest`, paused clock):
  - Close requested 6 frames into an opening: `0, 0, 0.051, 0.161, 0.291, 0.419` → `0.534,
    0.534, 0.582, 0.552, 0.488, 0.413, 0.338, …` → 0. The request reaches the drawer a frame
    later, momentum carries it 0.16 further, and it turns around; no step exceeds the opening's
    largest (0.13).
  - Reopen requested 6 frames into a closing: `1, 1, 0.949, 0.839, 0.709, 0.581` → `0.466, 0.466,
    0.418, 0.448, 0.512, 0.587, …` → 1.
  - Open then close each settle at the endpoint with no `onOpenChange` report (the host asked).
- Grabbing an opening (`PushDrawerGestureTest`): presented and grabbed progress agree, the
  transition stops following while the finger rests, a 0.25-width move moves progress by 0.25,
  and cancelling returns closed (the opening never came to rest) and reports it.
- A host that refuses a swipe's `true` gets the drawer back closed.

### 3.2 — Group reveal and pushed-screen treatment

- **Controlled frames** (`given_a_drawer_when_it_opens_and_closes_frame_by_frame_…`, 40 + 40 paused
  frames, pixel reads per frame): in every frame between 5 % and 95 % the pushed screen's left
  edge is within 4 px of drawer width × progress, its marker is dimmed by 0.18 × progress (±0.02),
  the drawer surface behind the content is exactly white, the bar's alpha is the progress (±0.04),
  and its width is the scaled width (±3 px). The content is still drawn at 12–30 % of the exit, and
  the wrapping label was never laid out again during 80 frames. Dark theme: the pushed screen is
  dimmed by 0.24 and the drawer surface is `#121212`.
- **Recording, app, pool list, light** ([mp4](verification/android/list-light-open-close.mp4),
  `animator_duration_scale` 10, 672 × 1496 screen recording, about 20–25 fps while moving):
  [opening strip](verification/android/list-light-opening-strip.jpg) and
  [closing strip](verification/android/list-light-closing-strip.jpg). A rank tile on the pushed
  screen was tracked in all 174 distinct frames: 24 px closed, 596 px open (the 571 px drawer), never
  outside that range, so neither endpoint is overshot. The pushed screen's background matched
  255 × (1 − 0.18 × progress) within 3 levels in every frame, and the drawer surface stayed at
  252–253 (white after compression). The account header, Profile, and the Log out button fade and
  grow together, and the header is still faintly visible at 9 % of the exit.
- **Dark, app** ([closed and open](verification/android/list-dark-closed-open.jpg)): the pushed
  screen goes from 17 to 12 (0.76 ×, the black scrim; the former white wash is gone) while the
  drawer surface stays at 16–17 (`#121212`). This recording caught only 11 frames (the AVD rendered
  about one frame a second after the dark-mode switch), so it is a before/after pair, not a strip.
- Previews: closed, half-open (`DrawerReveal.heldAt(0.5f)`), open, and right-to-left open, in light
  and dark. Not rendered here.

### 3.3 — Direct manipulation

Instrumented (`PushDrawerGestureTest`, 20 tests): a trailing swipe from the host row opens and the
row does not activate; a slow drag released at 0.35 settles closed with no report and one released
past the midpoint settles open; short fast flicks open from closed and close from open; drags from
a drawer row and from the pushed-screen strip close without activating the row; taps on the host
row and the drawer row each activate once; cancelling a drag held past the midpoint returns to the
previous endpoint from either side without activating the row; a grabbed opening continues from the
presented progress; the host list scrolls vertically and a nearly vertical drag leaves the drawer
closed; a horizontal `LazyRow` keeps its drags both ways; a tab tap selects the tab, and a trailing
swipe across the tab row opens the drawer without selecting a tab; a leading swipe on a closed host
does nothing; in right-to-left a leftward swipe opens, a rightward one on the closed drawer does
nothing, and a rightward one closes it; a drag from a system-gesture inset is left alone while one
just inside opens.

App (AVD, `adb shell input motionevent` finger paths):

| Check | Result |
| --- | --- |
| Pool list: swipe from the first pool row | Drawer opened; pool did not open ([held mid-drag](verification/android/list-drag-from-pool-row-held.jpg): the pushed screen trails the finger by the 24 px slop) |
| Pool list: 500 px drag from Profile toward closed | Released past the midpoint below the fling speed: settled open; Profile did not open |
| Pool list: 850 px drag from Profile toward closed | Settled closed; Profile did not open |
| Pool list: tap the opener, then tap Profile | Profile opened once; Back returned to the list with the drawer closed |
| Pool home: vertical drag on the leaderboard | Scrolled (Rank 4 moved from y 1152 to 267); drawer closed; no gambler opened |
| Pool home: swipe from a leaderboard row | Drawer opened; the gambler's bets did not open |
| Pool home: tap History, then swipe from the Scores tab across the tab row | Drawer opened; after Back, History was still selected |
| Pool home: tap Scores | Selected |

`adb shell input swipe` of 400–600 ms did not reach the drawer on this AVD; a 4 s one tracked and
opened it. The shell waits for each injected event to be handled, so on the software-rendered
emulator a short swipe sends one or two moves and then an up far away. Compose drags, including
this one, ignore movement carried by the up event, so the drawer saw a 50 px drag.

### 3.4 — Isolation, dismissal, and Back

Instrumented (`PushDrawerDismissalTest`): a tap over the pushed host row closes the drawer and
reports `false` without activating the row or the opener; two taps where the row and the opener are
drawn mid-closing activate nothing, and only the tap after the close reaches the row; the
accessibility click on "Close menu" closes it; inside a `NavHost`, the first Back closes the drawer
and keeps the route and the second pops it; a predictive Back progressed to 0.4 moves the reveal to
0.6 and a cancel returns it open with no report; a committed one closes it; a closed drawer
registers no enabled Back callback.

App (AVD, gesture navigation):

- Back key with the drawer open (pool list and pool home, both back-stack roots): the drawer closed
  and Fortuna stayed the resumed activity.
- Right-edge back swipe with the drawer open: the system started back navigation to the app's
  callback (`BackNavigationInfo{mType=TYPE_CALLBACK}`); midway the reveal was at 0.53, and the
  commit closed the drawer on the pool list.
- Right-edge swipe held at 0.68 ([capture](verification/android/list-predictive-back-held.jpg)),
  then returned to the edge: the system reported `triggerBack=false` and the drawer came back open.
- Not tried: a left-edge swipe with the drawer closed, which would exit the app from these roots,
  and three-button navigation (3.9).

### 3.5 — Semantics, focus, and animation duration

Instrumented (`PushDrawerAccessibilityTest`):

- Closed: no drawer node, no "Close menu", no "Menu" pane in either the merged tree or the
  accessibility-service tree; "Open menu", "Host row", and the list are.
- Opening (paused mid-transition): only "Close menu" is reachable; neither drawer actions nor any
  host control. *(Since the [review fix](#android--talkback-focus-review-fix-2026-09-23), nothing is reachable while the drawer opens.)*
- Open: Profile and Sign out are clickable nodes, the "Menu" pane title is present, "Close menu"
  is a button with traversal index 1, and no host control is reachable. *(Since the [review fix](#android--talkback-focus-review-fix-2026-09-23), there is
  no pane title.)*
- Keyboard (keys sent through the system input pipeline; the first Tab leaves touch mode, then the
  opener is focused): Enter opens and focus lands on Profile; Back closes and focus returns to the
  opener. The same with Escape. Eight Tabs in the open drawer cycle between Profile
  and Sign out and never reach the pushed screen; Enter then activates nothing on the host.

Instrumented (`PushDrawerDisabledAnimationsTest`, duration scale 0): a host request is open within
two frames (one for the request to reach the drawer, one to end the transition), and so is the
dismissal click's close, after which the host row takes the very next tap; a drag still follows
the finger (0.58 held) and its release settles within two frames.

App (AVD, `uiautomator dump`, the tree TalkBack reads):

- Closed pool list: the three pool rows, the opener, "My pools", and the create button. No drawer
  node.
- Open pool list: "felipearpa", the email, Profile, "Log out", and "Close menu" (bounds
  1142–1344 px, the strip). No pool row, title, or opener.
- Open pool home (member): account, Profile, the pool summary ("Playing now", pool name, 8º,
  589 points), "POOL", Invite, "Log out", and "Close menu"; no leaderboard row, tab, or title.
- Mid-transition trees could not be dumped in the app: `uiautomator dump` waits for the UI to
  idle, which never happens while the reveal animates (tried at `animator_duration_scale` 100).
  The instrumented test above covers that state.
- `animator_duration_scale` 0: the opener opened the drawer at once; after a strip tap, a tap on
  the opener 0.5 s later opened it again. A second tap sent right after the strip tap (same shell
  command) was absorbed, because it arrived before the AVD rendered the closing frame; the tree
  showed no "Close menu" afterwards.

### Settings and cleanup

The AVD's `animator_duration_scale` was set to 10, 100, and 0 during the app checks and deleted
afterwards (it was unset before); night mode was switched on for the dark capture and back off.
Navigation mode stayed gestural. The app was left on the pool list, signed in.

### Remaining issues and decisions

- **TalkBack focus is not moved programmatically.** Compose has no API to place accessibility
  focus. Opening announces the "Menu" pane and removes the pushed screen, including the opener,
  from the tree; the drawer is first in traversal order. Material 3's own drawer does the same.
  Keyboard focus moves in and back as specified. Live TalkBack was not run, so neither where
  TalkBack's cursor lands after opening nor after closing was observed.
  *(Observed live in [3.9](#android--tasks-3839-2026-09-23): opened from the opener with TalkBack,
  the cursor lands on "Close menu"; closing returns it to the opener. Fixed in the [review fix](#android--talkback-focus-review-fix-2026-09-23): TalkBack
  now lands on the account identity.)*
- **The dismissal surface is not a keyboard focus stop.** Keyboard users close with Back or Escape,
  as with Material's drawer scrim, which is not focusable either. An earlier focusable version
  (a `clickable`), activated with Enter, closed the drawer but left focus off the opener in two
  runs: the focused surface leaves composition at the closed endpoint, and moving focus back did
  not hold.
- ~~**The drawer width is still 85 % of the window.**~~ *(Addressed in
  [3.6](#android--tasks-3637-2026-09-23): bounded at 360 dp.)* Bounding it (design Decision 5) is
  listed under 3.6; the tests take the width from the container's `drawerWidth()` so they follow
  that change.
- **The pool list opener has no accessible label** (`uiautomator` shows an unlabeled clickable
  avatar), and the create button reads "Localized description". Both are consumer views (3.6).
  *(The opener on both hosts reads "Open menu" since
  [3.6](#android--tasks-3637-2026-09-23); the create button is unchanged.)*
- Not observed here: a physical device, frame pacing (the AVD renders in software), three-button
  navigation, a resize during a transition, and right-to-left in the app (the harness covers it).

## Android — tasks 3.6–3.7 (2026-09-23)

The drawer content on both hosts, built on the 3.1–3.5 container. Same AVD as above
(`Pixel_10_Pro_XL`, API 36, 1344 × 2992 px at 480 dpi, so 1 dp = 3 px), Felipe's signed-in session,
the working-tree `prodDebug` build installed with `adb install -r`. Captures are in
[`verification/android/`](verification/android/), at half the device resolution.

### What changed

- **Shared hierarchy.** New `DrawerMenu` (app, the counterpart of iOS's `DrawerMenu.swift`):
  account identity, Profile directly below it, the host's sections, then a sign-out footer
  separated by a divider. The menu is a vertical scroll whose minimum height is the drawer's, so
  the footer rests at the bottom when there is room and scrolls with everything else when there is
  not. Both drawer views use it; the pool list adds no sections. The two copies of the outlined
  Sign out button are gone.
- **Rows.** `DrawerButtonRow` is a Material `clickable` with button semantics and the theme's
  ripple, at least 48 dp tall, growing with the font. Its 24 dp icon is centred in a leading column
  as wide as the account avatar (64 dp, the existing Android size), so labels line up with the
  account name. It takes an optional trailing accessory (the Gamblers count). Disabled rows keep
  their place at Material's 38 % content opacity.
- **Account identity.** `AccountIdentityRow` is one merged node (name and email). The avatar is
  cleared from semantics, so the letter avatar's initial is not read. The email wraps to two lines.
- **Pool summary.** The production `PoolSummaryItem` is now an inset `surfaceVariant` Material
  surface with 12 dp corners: an accent trophy, "Playing now", the pool name (`titleMedium`), and
  "1º · 41 points" on one line. It is announced as one element, for example "Playing now, Copa
  Mundial de la FIFA prur, Rank 1, 41 points". "Rank" reuses the pool module's
  `leaderboard_rank_accessibility` string, so "1º" is not read as a symbol. Loading passes
  `poolGamblerScorePlaceholderModel()` to the same component with the shared `Modifier.shimmer()`
  on each value, and clears its semantics. A failure shows the existing `ExceptionView` inside the
  same surface. The trophy is sized in `sp`, so it grows with the caption beside it.
- **Section title.** "POOL" is a heading, in `labelMedium` at the supporting colour.
- **Supporting text** (email, section title, statistics): `onSurface` at 70 %, measured 5.92:1 on
  the light surface, 5.68:1 on the light group, 7.34:1 and 6.85:1 in dark. The section title used
  60 % before, which measures 4.29:1 on white. Delete pool: 4.98:1 light, 5.20:1 dark.
- **Container (`PushDrawer.kt`).** `drawerWidth()` is 85 % of the window, bounded at 360 dp plus
  the leading inset the content is padded by, and always leaves at least 48 dp of the pushed
  screen before the trailing inset. The content group is padded by the system bars and the display
  cutout on the top, bottom, and leading sides only. It used to pad all four sides by the system
  bars, which in landscape would also have padded the drawer's trailing side by a right-hand
  navigation bar.
- **Opener label.** The avatar opener on both hosts reads "Open menu" (new `open_menu_action` in
  `:ui`: "Abrir menú" in `values-es` and `values-es-rES`), and the avatar inside it is cleared from
  semantics.
- **Previews.** Both drawer views are previewed inside an open `PushDrawer`: light, dark, a wide
  window (Pixel Tablet), a short window (phone landscape), the largest font scale (2.0), and right
  to left. Pool home adds member, without position, loading, failure, and deleting previews. **They
  were not rendered in Android Studio**; the device pass below covers the same configurations in
  the running app.

Unchanged: the account and avatar sources (the view models read `CurrentAccountCoordinator`;
`AccountAvatar` keeps its 64 dp size and therefore its pixel bucket), every action callback, the
delete confirmation dialog, `enabled = !isDeleting`, and the host's deletion overlay and error
dialog. No icon was added or replaced.

### Automated coverage

```bash
cd Android
./gradlew :ui:testDebugUnitTest :app:testProdDebugUnitTest :app:assembleProdDebug :pool:testDebugUnitTest
# One class per run; the AVD is emulator-5554.
ANDROID_SERIAL=emulator-5554 ./gradlew :ui:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.felipearpa.tyche.ui.<Class>
```

- `:ui:testDebugUnitTest`: **new `DrawerWidthTest`, 7 passed**. It checks 85 % at 360 dp, the
  360 dp bound at 448 dp (this AVD's portrait width) and at 1280 dp, 360 + 48 dp with a 48 dp
  leading inset, exactly 48 dp of strip at 280 dp with and without a 24 dp trailing inset, and every
  width from 0 to 1600 dp in 10 dp steps (never negative, never less than 48 dp of strip).
  `DrawerRevealTest` 17 passed.
- `:app:testProdDebugUnitTest`: **new `DrawerViewModelTest`, 4 passed**. The pool's creator is an
  owner with the gambler count, another gambler is a member, a deletion in flight reports
  `isDeleting` until the request answers (the state that disables Delete pool) and then clears it
  without telling the host, and a successful deletion tells the host once. The 54 existing app
  tests passed as well.
- `:pool:testDebugUnitTest` 2 passed (the pool list host changed only its opener label).
- `:app:assembleProdDebug` built. No compiler warnings in the touched files.
- `:ui:connectedDebugAndroidTest`, re-run because the drawer width and insets changed (the tests
  take the width from `drawerWidth()`):

  | Class | Result |
  | --- | --- |
  | `PushDrawerRevealTest` | 4 of 5 passed in the class run; see below |
  | `PushDrawerGestureTest` | 20 passed |
  | `PushDrawerDismissalTest` | 7 passed |
  | `PushDrawerAccessibilityTest` | 6 passed |
  | `PushDrawerDisabledAnimationsTest` | 2 passed |

  `PushDrawerRevealTest`'s frame-by-frame test (81 root captures on a paused clock) failed in the
  class run and in two runs on its own. Every failure was a capture, before any assertion: first a
  `ComposeTimeoutException` in `forceRedraw` (which the harness does not retry), then twice
  `Failed waiting for PixelCopy!` on five consecutive attempts for one frame (4 and 17 retries
  logged across the run). The AVD renders with SwiftShader at a host load average of 14–16. The
  other four reveal tests passed. One of them, the dark-theme test, samples the pushed screen at
  exactly `drawerWidth()`, so it checks the bounded open endpoint by pixel. The gesture and
  dismissal tests drive drags by fractions of the same width. **The frame-level geometry with the
  new width is therefore not verified.**
  *(Verified in [3.8](#android--tasks-3839-2026-09-23): the test passed at the 1080 px width in two
  runs, with no capture retries.)*

### 3.6 — Layout, themes, and reachability (app, AVD)

Touch targets are read from `uiautomator dump` bounds. The drawer is `[0,0][1080,…]` in portrait:
360 dp of a 448 dp window, where 85 % would be 381 dp. That leaves an 88 dp dismissal strip.

| Configuration | Result |
| --- | --- |
| Pool list, light ([capture](verification/android/drawer-list-open-light.jpg)) | Identity (one node), Profile, and Log out below a divider at the bottom, clear of the gesture bar (24 dp inset). Rows 144 px (48 dp) tall and 360 dp wide. |
| Pool list, dark ([capture](verification/android/drawer-list-open-dark.jpg)) | Surface `#121212`; the pushed screen is dimmed by the black scrim. |
| Pool home, owner, light ([capture](verification/android/drawer-home-owner-open-light.jpg)) | Summary group `#F5F5F5` on the `#FFFFFF` surface, then POOL, Invite, Gamblers with its count, and Delete pool in the error colour. The footer is at the bottom. |
| Pool home, member, dark ([capture](verification/android/drawer-home-member-open-dark.jpg)) | Surface `#121212`, group `#1E1E1E` (sampled). Invite only. |
| Font scale 2.0 (Android's maximum), portrait: [pool list](verification/android/drawer-list-open-font-2.jpg), [pool home](verification/android/drawer-home-owner-open-font-2.jpg) | The email wraps at ".com", the pool name at a word boundary, and rows grow to 171 px (57 dp). Everything fits on this tall screen, including the footer. Icons stay 24 dp; the trophy grows with its caption. |
| Landscape, default font: wide (997 dp) and short (448 dp) window ([capture](verification/android/drawer-home-owner-landscape.jpg)) | The drawer is 1239 px (413 dp): the 360 dp bound plus the 53 dp camera-cutout inset on its leading side. Content starts at x = 159 px, clear of the cutout. The rest of the window stays visible and dismisses. The menu scrolls. |
| Landscape at 2.0: [top](verification/android/drawer-home-owner-landscape-font-2-top.jpg), [scrolled to the end](verification/android/drawer-home-owner-landscape-font-2-scrolled.jpg) | Vertical drags on the menu scroll it without moving the drawer. Scrolled content is clipped at the status bar. Every action through Log out is reachable. |
| Right to left (forced RTL, English strings): [pool list](verification/android/drawer-list-open-rtl.jpg), [pool home](verification/android/drawer-home-owner-open-rtl.jpg) | The drawer is `[264,…][1344,…]` with the strip on the left. Text, icons, the count badge, and the summary are mirrored. |

- The opener: `[12,183][156,327]`, 48 dp, labelled "Open menu" on both hosts.
- Forced RTL needed a configuration update to apply. `settings put global debug.force_rtl 1` plus
  `setprop debug.force_rtl true` did nothing until the app's per-app locale was set to `en-US`,
  which recomputes the layout direction with RTL forced and keeps the English strings.

### 3.7 — Pool summary, data, and actions (app, AVD)

- **Loaded:** one element, "Playing now, Copa Mundial de la FIFA prur, Rank 1, 41 points" (owner)
  and "…FIFA 2026, Rank 8, 589 points" (member).
- **Loading** ([capture](verification/android/drawer-home-summary-loading.jpg)): the same summary
  surface rendered from the placeholder model under the shimmer. The tree dump in this state
  (taken with `animator_duration_scale` 0 so the UI could idle) has no summary node at all: no
  "Playing now", no placeholder "XXXX…" text. Profile, Invite, Gamblers, Delete pool, Log out, and
  "Close menu" are all present.
- **Failure** ([capture](verification/android/drawer-home-summary-failure.jpg)): the existing
  `ExceptionView` ("Network error" with its explanation) inside the summary surface. Every other
  action stays present and enabled in the dump.
- **How loading and failure were staged.** The emulator's `network delay` (6 s, then 30 s) did not
  slow this AVD's traffic, and `gsm data off` only moved it onto the virtual Wi-Fi, so both were
  reverted. Instead a temporary build held the view model in `Loading` for 45 s and then emitted
  `Failure(NetworkException.RemoteCommunication)`. The procedure: back up `DrawerViewModel.kt`
  (SHA-256 recorded), add the three marked lines, build, install, and capture. Then restore from
  the backup (`shasum -c` OK, `git status` clean for the file), rebuild with `--rerun-tasks`, and
  reinstall. The device's `base.apk` SHA-256 equals the local APK (`75e1d39e…`).
- **Cached reopening:** after the pool home settled, three open/close cycles produced no Ktor line
  in logcat (0) and no change to Coil's disk-cache journal (117 lines before and after). The
  header kept the photo, name, and email. Positive control: leaving for the pool list right after
  logged 22 Ktor lines (`GET …/gamblers/<id>/pools`, `GET …/pool-layouts/open`).
- **Single dispatch:** Gamblers opened Manage gamblers, and one Back returned to pool home with the
  drawer closed. Profile from pool home likewise. Invite closed the drawer and opened the system
  share sheet (`com.android.intentresolver`); Back dismissed it unsent and left the drawer closed.
- **Owner and member:** owner of "Copa Mundial de la FIFA prur": Invite, Gamblers (1), Delete pool.
  Member of "Copa Mundial de la FIFA 2026": Invite only.
- **Deletion:** Delete pool opened the existing confirmation
  ([capture](verification/android/drawer-home-delete-confirmation.jpg)); Cancel dismissed it and
  the drawer stayed open. No pool was deleted.
- **Pending deletion was not observed on the device.** Staging it means confirming a real deletion.
  It rests on `DrawerViewModelTest` (pending while the request is in flight) and the unchanged
  `enabled = !isDeleting` binding (the "Deleting" preview shows the dimmed row).
- **Icons:** none added or replaced. The rows reuse `filled_person`, `person_add`, `group`,
  `delete_forever`, and `trophy` (`:ui`) and `sign_out` (app), so the canonical-vector requirement
  is not triggered.

### Environment incidents

- **ANRs during the device pass.** Fortuna raised four "isn't responding" dialogs. Their stacks
  (from `dumpsys dropbox --print data_app_anr`):
  - One was taken after the main thread had recovered (idle in `nativePollOnce`).
  - One had the main thread composing pool home's own leaderboard placeholder rows
    (`GamblerScorePlaceholderItem` with the accompanist shimmer), outside the drawer.
  - One had the main thread building `AccessibilityNodeInfo`s for a `uiautomator dump` that I
    requested while the list's 50 placeholder rows were animating.

  The host's load average was 14–16, driven by iOS Simulator runtime processes (diagnosticd,
  searchd, spotlight) that had been running for 19 hours, and the device reported CPU pressure
  avg10 = 74 %. One relaunch was killed by the system with "start timeout". Idle with the drawer
  open, the app used 1 CPU tick in 10 s, so nothing loops. From then on, dumps were taken only
  while the UI was idle. Some of the taps meant for the drawer instead landed on an ANR dialog's
  Close app, and once on the launcher (opening Calendar). Each time, the app was relaunched and
  the check repeated.
- A cold restart of the AVD (`emu kill`, then boot with `-no-snapshot-save`) resumed its quick-boot
  snapshot, which held an older Fortuna install, so the build was reinstalled.
- The pool list once showed "Unexpected error" right after the activity was recreated for the
  locale change. Retry loaded it.

### Settings and cleanup

Changed on the AVD and restored: night mode (on → off), `font_scale` (2.0 → 1.0), `user_rotation`
and `accelerometer_rotation` (landscape lock → 0 and 1, as found), `debug.force_rtl` (setting
deleted, property `false`), Fortuna's per-app locale (`en-US` → none), `animator_duration_scale`
(0 → deleted, as found), and the emulator's `network delay` (→ none) and `gsm data` (→ on).
Navigation stayed gestural. The app was left signed in. No pool was deleted, no share was sent,
and the account was never signed out. The emulator is shut down. Its next boot resumes the
quick-boot snapshot, which holds an older Fortuna install, so reinstall the working-tree build
first.

### Remaining issues and decisions

- **Leading column.** Android keeps its 64 dp avatar, so labels start at 88 dp (iOS: 48 pt avatar,
  labels at 72 pt). Keeping the size also keeps `AccountAvatar`'s pixel bucket, and so its memory
  and disk cache keys.
- **Sign out is a row, not an outlined button**, in the separated footer, as on iOS.
- **The drawer menu keeps its scroll position** across closing, reopening, and configuration
  changes (`rememberScrollState` is saveable).
- Pre-existing, not changed:
  - The pool list's create button reads "Localized description", and pool home's pool-change
    button has an empty label.
  - The `sign_out` asset does not mirror in right to left, like iOS's `log_out`.
  - The `settings` drawable is now unused; it served only the old `DrawerButtonRow` preview.
- `PushDrawerHarness.captureRoot()` retries on `AssertionError` and `RuntimeException`, but
  `ComposeTimeoutException`, which is what the capture threw here, extends `Throwable`, so it was
  not retried.
- The placeholder lists on both hosts (50 shimmer rows) are the heaviest thing composed on this AVD
  in a debug build; a physical device (3.9) will show whether they matter outside the emulator.
- **Not observed:**
  - Live TalkBack: the merged identity announcement and the POOL heading were checked only in the
    tree.
  - Previews in Android Studio.
  - A physical device, a tablet or foldable window (the landscape phone, 997 dp, is the wide case
    here), and resizing mid-transition (3.9).
  - A genuinely slow network: loading was staged with the temporary build.
  - The frame-by-frame reveal test at the new width, which could not capture every frame on this
    AVD (see Automated coverage). *(Passed in [3.8](#android--tasks-3839-2026-09-23); resizing
    mid-transition and live TalkBack are recorded there too.)*

## Android — tasks 3.8–3.9 (2026-09-23)

Focused Compose coverage (3.8) and a device pass over both drawer hosts (3.9), on the uncommitted
3.1–3.7 code. Only test files changed; no production code. Captures are in
[`verification/android/`](verification/android/).

| Device | Used for |
| --- | --- |
| AVD `Pixel_10_Pro_XL`, Android 16 (API 36), en-US, 1344 × 2992 px at 480 dpi (1 dp = 3 px) | `:ui` instrumented tests; Felipe's signed-in session with the working-tree `prodDebug` build (`base.apk` SHA-256 `75e1d39e…`, equal to the local APK, so it was not reinstalled) |
| Felipe's phone SM-G955F | Not attached during this pass (`adb devices` listed only the AVD) |

**Renderer.** Unlike the earlier passes, the emulator chose the host GPU on this boot
(`gles_mode_selected:host`, "Android Emulator OpenGL ES Translator (Apple M1 Pro) … Metal";
`hw.gpu.mode=auto`) instead of SwiftShader. In its visible window, frame timing collapsed a few
minutes in: the GPU 50th percentile reached 4.95 s on the pool list, which had measured 17 ms
shortly before, and pool home's UI thread took 18–65 ms per frame. The cause was not confirmed;
the emulator window was behind other apps (Claude was frontmost), and no thermal warning was
recorded. The emulator was restarted headless (`-no-window -gpu host`) for every timing and
motion measurement below.

### Automated coverage (3.8)

```bash
cd Android
./gradlew :ui:testDebugUnitTest --rerun :app:testProdDebugUnitTest --rerun \
  :pool:testDebugUnitTest --rerun :app:assembleProdDebug
# One class per run; the AVD is emulator-5554.
ANDROID_SERIAL=emulator-5554 ./gradlew :ui:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.felipearpa.tyche.ui.<Class>
```

- JVM: `:ui` **34 passed** (`DrawerRevealTest` 17, `DrawerWidthTest` 7, 10 others), `:app`
  **58 passed** (including `DrawerViewModelTest` 4), `:pool` **2 passed**, 0 failed.
  `:app:assembleProdDebug` built.
- `:ui:connectedDebugAndroidTest`, final run of every class after the last harness change,
  **54 passed, 0 failed** (50 drawer tests, 10 of them new):

  | Class | Tests | New in this pass |
  | --- | --- | --- |
  | `PushDrawerRevealTest` | 5 | — |
  | `PushDrawerGestureTest` | 24 | 4 |
  | `PushDrawerDismissalTest` | 8 | 1 |
  | `PushDrawerAccessibilityTest` | 7 | 1 |
  | `PushDrawerDisabledAnimationsTest` | 4 | 2 |
  | `PushDrawerResizeTest` (new class) | 2 | 2 |
  | `GestureDisableModifierTest`, `ProgressIndicatorTest` (existing) | 1 + 3 | — |

- **The frame-by-frame reveal test now passes at the bounded width**, in both runs of its class
  in this pass, with no capture retry. In the 26 frames between 5 % and 95 % at the 1080 px (360 dp) drawer width:
  pushed-screen edge within 2.09 px of width × progress, dimming within 0.0035 of 0.18 ×
  progress, content alpha within 0.0039 of the progress, and bar width within 0.84 px of the
  scaled width.
- Harness (`PushDrawerHarness.kt`): a synthetic horizontal control (a `draggable` strip, standing
  for a slider-like control neither host has) on the pushed screen and in the menu; a `modifier`
  on `DrawerHarnessContent` so a test can resize the host; `captureRoot()` also retries
  `ComposeTimeoutException`, which extends `Throwable`.
- No build file, version catalog, lockfile, or layout XML changed. The drawer and host sources
  import no `AndroidView`, `DrawerLayout`, navigation-drawer widget, `LayoutInflater`, or
  animation library; the drawer is Compose layout, `graphicsLayer`, pointer input, and
  `PredictiveBackHandler` only.

Coverage for each item the task names (new tests in bold):

| Item | Tests |
| --- | --- |
| Swipe opening | A swipe from the host row, a slow drag past the midpoint, a short fast flick, a swipe across the tab row, right to left; a drag from a system-gesture inset is left alone |
| Drag-to-close from an action row without activating it | A drag from the drawer row closes it with no click; a cancelled drag from it returns open with no click; a tap still activates once |
| A synthetic horizontal child control keeps its drag | **The synthetic control on the closed host takes 916.8 px of each 0.7-width swipe, trailing then leading, with no report**; **in the open menu it takes a 732 px leading drag and the drawer stays open**; the `LazyRow` scrolls both ways; **at its start, a trailing swipe on it stays with it (overscroll) and the drawer stays closed** |
| Interruption | Host reversal mid-opening and mid-closing; a drag grabs an opening; **a drag grabs a closing (presented 0.709, grabbed 0.603) and a cancel returns it open, reporting `true`**; **Back mid-opening turns it around** |
| Cancellation | Cancelled drags past the midpoint from either endpoint; a cancelled grab of an opening; **of a closing**; a cancelled predictive Back |
| Background hit isolation | A tap over the host row; two taps mid-closing where the row and opener are drawn; **a tap on the strip beside a drawer widened mid-opening** |
| Back precedence | In a `NavHost`, the first Back closes and the second pops; **Back 4 frames into an opening never reaches open (below the displayed progress + 0.15) and keeps the route**; predictive Back progress, cancel, and commit; no enabled callback while closed |
| Accessibility visibility | Closed, opening, **closing**, and open trees, both merged and as `UiAutomation` reads them; keyboard entry, return, Escape, and Tab containment |
| Disabled animations | Requests and dismissal settle within two frames; a drag release settles on the next frame; **a drag held at 0.58 shows the full 360 px bar (no decorative scale) at alpha = progress**; **Back closes within two frames** |
| Resize (for 3.9) | **A drag held at 0.478 keeps its progress when the host narrows from 448 to 360 dp; the strip moves from 516 to 438.6 px (918 px × 0.478), the next 0.2-width move adds exactly 0.2, and it settles open at the narrow width**; **an opening widened at 0.291 puts the strip at 314.5 px (1080 × 0.291) on that frame, settles at 1080 px, and a strip tap dismisses with no row click** |

### 3.9 — Frame pacing (AVD, headless, host GPU)

Per host: 3 warm-up cycles, then 5 cycles of opener tap → 1.2 s → strip tap with
`dumpsys gfxinfo … framestats` after each, then 10 cycles for the summary. "In-motion intervals"
excludes each request's first and last interval, which are the tap and the end of the settle
(33 ms on every opening), not motion.

| Host | In-motion intervals (10 transitions) | Skipped vsyncs | UI thread per frame | `gfxinfo`, 10 cycles |
| --- | --- | --- | --- | --- |
| Pool list | 229, median 16.7 ms | 4 (max 50 ms) | median 2.4, p95 4.2, max 8.9 ms | 789 frames, 2.66 % janky; p50 17, p90 18, p95 18, p99 24 ms; 0 missed vsync, 0 slow UI thread |
| Pool home (13 gamblers) | 235, median 16.7 ms | 0 | median 2.3, p95 3.5, max 5.3 ms | 789 frames, 3.04 % janky; p50 17, p90 22, p95 23, p99 29 ms; 0 missed vsync, 0 slow UI thread |

- An opening is 27 frames over 467 ms; a closing 26 frames over 417 ms. The janky frames are all
  "frame deadline missed" on the GPU side of the emulator's GL translation; the UI thread never
  exceeded 9 ms.
- The screen recordings below are variable-frame-rate and add encoding load, so they support
  rather than measure pacing: while the drawer moved, their frames came a median 17 ms apart.
  The dark taps stayed within 21 ms and the light tap reversal within 19 ms; the longest gap was
  71 ms, in the last 4 px of one light opening's settle.
- **Not measured on a physical device**: the phone was not attached. Drag-driven pacing was not
  measured either, because `input motionevent` delivers a move about every 32 ms.

### 3.9 — Motion and reversal (pool home, light and dark)

[Recording](verification/android/home-light-gestures.mp4) (member pool, 13 gamblers), with the
pushed screen tracked per frame by the Scores label (closed x = 82, open x = 622 in the
672 px-wide recording, i.e. the 1080 px drawer):

| Gesture | Tracked progress |
| --- | --- |
| Swipe from Val Cardo's row, 50 px steps | 0 → 1 in steps of 0.046 per step (1:1), settled at exactly 1.0 |
| Drag from Invite toward closed, 40 px steps | steps of −0.037 down to 0.467, then settled at 0; no share sheet |
| Opener tap, then strip tap | [opening](verification/android/home-light-opening-strip.jpg) and [closing](verification/android/home-light-closing-strip.jpg) strips at 0, ≈ 0.3, 0.55, 0.73, 1 |
| **Fast reversal:** opener tap, strip tap about 0.15 s later | rose to 0.796, held one frame, turned and settled at 0; never reached open ([strip](verification/android/home-fast-reversal-strip.jpg)) |
| Drag out to 0.626 and back, released at 0.117 | followed the finger both ways, settled at 0 |

No frame passed either endpoint. Dark ([recording](verification/android/home-dark-open-close.mp4),
[opening](verification/android/home-dark-opening-strip.jpg),
[closing](verification/android/home-dark-closing-strip.jpg)): the same synchronized movement
with the black scrim on the `#121212` screen; the earlier pass could capture only a before/after
pair.

### 3.9 — Swipe opening, drag closing, and taps (AVD, gesture navigation)

Each gesture was a `motionevent` finger path; after each, the tree showed whether the drawer was
open and which screen was in front.

| Host | Start of the gesture | Result |
| --- | --- | --- |
| Pool list | Title "My pools"; the opener's right half (x = 130 px); the empty area below the pools; just above the bottom gesture inset (y = 2860 px) | Opened each time; the opener did not toggle, nothing opened |
| Pool list | Leading drag from a pool row's invite button, drawer closed | Nothing: no drawer, no share sheet |
| Pool home | Top bar title; the bottom navigation bar across Bets; a leaderboard row (own and others') | Opened; no tab selected, no gambler opened |
| Pool home | Vertical drag on the leaderboard | Scrolled (Rank 3 at the top afterwards); drawer closed, no gambler opened |
| Pool list drawer | Drag toward closed from the account identity, Profile, and empty menu space | Closed; Profile did not open |
| Pool home drawer (owner) | From Profile, the pool summary, Invite, Gamblers, and Delete pool | Closed; no destination, share sheet, or confirmation |
| Font scale 2.0, pool home | Swipe open from a row; drag closed from Delete pool | Opened and closed; no confirmation |

- Taps still activate once: Profile (pool list, and pool home at font 2.0) opened once and Back
  returned with the drawer closed; Gamblers opened Manage gamblers once.
- **Invite and delete:** Invite closed the drawer and opened the system share sheet
  (`ChooserActivityLauncher`); Back dismissed it unsent and left the drawer closed. Delete pool
  opened the existing "Delete pool?" confirmation; Cancel dismissed it and the drawer stayed open
  with Delete pool enabled. No pool was deleted and nothing was shared.
- **Sign-out** is reachable at the bottom of both drawers ("Log out", `[0,2752][1080,2896]`, and
  at font 2.0 `[0,2725][1080,2896]`), but was neither tapped nor dragged: the footer row signs
  out without a confirmation. Its callback path (`viewModel.logout()` then `onSignOut()`) is
  unchanged, and a drag starting on a drawer row is covered by the instrumented row tests.
- **Destination:** on Profile, a trailing swipe mid-screen did nothing (no drawer is composed
  there), and a left-edge back swipe returned to the pool list with the drawer closed.
- **Press feedback:** a finger resting on a pool row and on the Profile row shows the Material
  ripple ([capture](verification/android/press-feedback-held.jpg); both touches ended with
  CANCEL, so nothing activated). The iOS hosts show no pressed state while held.
- **Gesture-inset trap, as specified:** a drag that starts on the opener's leading 30 dp
  (x < 90 px) is inside the left back-gesture inset. The system took it as Back, which from the
  pool list sent Fortuna to the launcher; my following gestures then landed on the launcher and
  opened Chrome's first-run screen. Chrome was force-stopped without accepting anything and
  Fortuna relaunched; every later gesture first checked that Fortuna was the resumed activity.

### 3.9 — Gesture and three-button navigation (pool home and pool list)

| Mode | Gesture | Result |
| --- | --- | --- |
| Gesture | Left-edge swipe, drawer open | System Back closed the drawer; pool home stayed |
| Gesture | Right-edge swipe, drawer open | Same |
| Gesture | Left-edge swipe held midway, drawer open, then returned to the edge | The reveal followed predictive Back (about 0.54, [capture](verification/android/home-gesture-nav-left-edge-open-held.jpg)); returning left it open |
| Gesture | Left-edge swipe held midway, drawer closed, then returned to the edge | The system's back-to-home preview, no drawer ([capture](verification/android/home-gesture-nav-left-edge-closed-held.jpg)); Fortuna stayed on pool home with the drawer closed |
| Three-button | Left-edge drag from x = 4 px, drawer closed (pool home and pool list) | Ordinary drawer drag: tracked (held at 0.53, [capture](verification/android/home-three-button-left-edge-drag-held.jpg)) and opened |
| Three-button | Back button, drawer open | Closed; the route stayed |
| Three-button | Leading drag from the right edge (x = 1340 px), drawer open | Closed |

Switched with `cmd overlay enable …navbar.threebutton` and back with `…navbar.gestural` plus
`cmd overlay disable …navbar.threebutton` (enabling one did not disable the other);
`navigation_mode` is 2 again.

### 3.9 — Resizing

- **In place, without recreation:** with `animator_duration_scale` 10, the display was narrowed
  with `wm size 1200x2992` 1 s into an opening. The activity was not recreated: the drawer kept
  opening and settled at 1020 px (340 dp = 85 % of 400 dp), with "Close menu" at
  `[1020,0][1200,2992]`, and a tap on that strip closed it. `wm size reset` 0.4 s into the next
  opening: the reveal continued (captured at about 0.54) and settled at 1080 px with the strip
  at `[1080,0][1344,2992]`, and a strip tap closed it
  ([capture](verification/android/list-resize-during-opening.jpg)). `PushDrawerResizeTest`
  checks the same with a held drag and with exact values.
- **Rotation recreates the activity** (the manifest declares no `configChanges`). Rotating to
  landscape mid-opening, and back to portrait while a finger held the drawer at about 0.5,
  each came back with the drawer closed; afterwards the opener opened it and the strip closed
  it. Both hosts keep `isDrawerOpen` in `remember` (unchanged by this change), so
  even a fully open drawer closes on rotation. On iOS, rotation cancels a drag and returns to
  the settled endpoint.

### 3.9 — TalkBack (pool list, live)

TalkBack 16.0 was enabled on the AVD through `enabled_accessibility_services`. Shell-injected
taps and swipes bypass TalkBack's touch exploration and act as plain touches, so TalkBack was
driven through the emulator console's touchscreen (`adb emu event mouse`): a touch moves the
cursor, a quick left-to-right swipe moves to the next item, and a double tap activates. The cursor was read from TalkBack's green outline in screenshots
([panel](verification/android/list-talkback-focus.jpg)). No double tap was sent unless a
screenshot showed the cursor on the intended element.

| Step | TalkBack's cursor |
| --- | --- |
| Touch the opener | "Open menu" |
| Double tap (drawer opens) | **"Close menu" (the strip)**, in 4 of 4 openings *(fixed in the [review fix](#android--talkback-focus-review-fix-2026-09-23): the account identity)* |
| Back (drawer closes) | The opener, in 3 of 3 |
| Double tap on "Close menu" (drawer closes) | The opener |
| Swipe forward from Profile | Log out, then "Close menu", then stays on "Close menu"; host controls were never reached |
| Drawer opened by a plain tap while the cursor was on a pool row | The account identity; closing returned the cursor to that pool row |

- **Opening does not put TalkBack on the account.** During the opening transition only
  "Close menu" is in the tree (drawer actions appear once it settles open), so TalkBack moves its
  cursor from the vanished opener to "Close menu" and keeps it there. The spec asks for the
  drawer's account or first actionable element; a TalkBack user has to swipe backward from the
  last item. See Remaining issues. *(Fixed in the [review fix](#android--talkback-focus-review-fix-2026-09-23).)*
- Closing returns TalkBack to the opener. That return is TalkBack's own memory of its previous
  position in the window; keyboard focus is returned by the container (instrumented).
- The Android Accessibility Suite asked to post notifications when TalkBack started, and kept
  asking after being declined (Back the first time, then "Don't allow"). With TalkBack off, its
  permission-request task was force-stopped to end the loop. The permission was never granted,
  and its user-set flag was cleared afterwards.
- **Pool home with TalkBack was not run live**, because the permission prompt kept returning. Its
  reading order is recorded from the accessibility tree in 3.5 and 3.7. *(Run live in the [review fix](#android--talkback-focus-review-fix-2026-09-23).)*

### Comparison with iOS

| Behavior | iOS (2.9, 2.10) | Android (this pass) |
| --- | --- | --- |
| Opening a host by swipe | From anywhere while the path is empty; 20 pt recognition; the tab-bar band is reserved | From anywhere; touch slop (8 dp); the system gesture insets are reserved; a swipe across pool home's bottom navigation bar opens the drawer without selecting a tab (accepted by Felipe, 2026-09-24; tab taps still select) |
| Drag from a drawer action or host row | Moves the drawer; the control does not activate; taps activate once | Same |
| Pressed feedback while held | None on the hosts (accepted) | Material ripple |
| Horizontal child scrollers | A trailing drag on one opens the drawer (none in the hosts) | The scroller keeps its drags both ways, even at its start (none in the hosts) |
| Screen edges | Destinations keep their back swipes; the drawer's drag detaches | Gesture navigation: both edges are system Back, which closes an open drawer (with predictive progress) and otherwise leaves the app; three-button: edges are ordinary drawer drags |
| Dismissal without touch | Accessibility escape | Back (key, button, gesture, predictive), Escape key, "Close menu" |
| Settling | About 0.45 s opening, 0.6 s closing | 467 ms opening, 417 ms closing (critically damped, stiffness 500) |
| Screen reader after opening | Focus set on the account identity (not observed with VoiceOver) | ~~TalkBack lands on "Close menu"~~ TalkBack lands on the account identity ([review fix](#android--talkback-focus-review-fix-2026-09-23)) |
| Screen reader after closing | Focus set on the opener (not observed) | TalkBack returns to the opener |
| Reduce motion / animations off | One frame, no scale or movement | Settles within two frames; a drag moves without the decorative scale |
| Resize | Rotation mid-drag returns to the settled endpoint | An in-place resize keeps progress and alignment; rotation recreates the activity and closes the drawer |
| Frame pacing | Simulator: median 16.7 ms, at most 1 interval over 25 ms per capture | Emulator (host GPU): median 16.7 ms, 0–4 skipped vsyncs per 230 intervals, UI thread under 9 ms |

### Settings and cleanup

Changed on the AVD and restored to what was found: `animator_duration_scale` (10 → deleted),
`user_rotation` / `accelerometer_rotation` (landscape lock → 0 / 1), `font_scale` (2.0 → 1.0),
night mode (on → off), navigation (three-button → gestural), `wm size` (1200 × 2992 → reset),
TalkBack (`enabled_accessibility_services` → deleted, `accessibility_enabled` → 0,
`touch_exploration_enabled` back to 0), and the Accessibility Suite's notification permission
flag.
Chrome's first-run screen was left without accepting anything. The app was left signed in on the
pool list; no pool was deleted, nothing was shared, and sign-out was never touched. The emulator
is shut down and the Gradle daemons are stopped; no screen recording or logcat stream is left
running.

### Remaining issues and decisions

- ~~**TalkBack lands on "Close menu" after opening from the opener.**~~ *(Fixed in the [review fix](#android--talkback-focus-review-fix-2026-09-23):
  "Close menu" stays out of the tree until the drawer settles open, and the pane title is gone.)*
  The spec's scenario asks for
  the account or first actionable element. Options are in the container's (3.5) scope, for
  example exposing the drawer content without actions during the transition, or keeping
  "Close menu" out of the tree until the drawer settles, which would leave the transition
  without an accessible dismissal. Not changed here.
- **Rotation closes the drawer**, because both hosts keep `isDrawerOpen` in `remember`
  (unchanged). It is a valid state; `rememberSaveable` in the two hosts would keep it open.
  **Decision (Felipe, 2026-09-24): accepted. Rotation closes an open drawer; the hosts keep
  `isDrawerOpen` in `remember`.**
- **Horizontal child controls keep trailing drags, even at their start**, so a trailing swipe
  that starts on one does not open the drawer. Neither host has one; iOS behaves the opposite
  way.
- **A drag that starts on the opener's leading 30 dp is system Back** under gesture navigation,
  as the spec requires for reserved edges; from these back-stack roots it leaves the app.
- **Not observed:** a physical device (the phone was not attached), so frame pacing is from the
  emulator only; live TalkBack on pool home *(run in the [review fix](#android--talkback-focus-review-fix-2026-09-23))*; drag-driven frame pacing; a
  tablet, foldable, or multi-window resize (the display override stood in for an in-place
  resize).

## Android — TalkBack focus review fix (2026-09-23)

Review finding: after a TalkBack user opened the drawer from the opener, TalkBack's cursor sat on
"Close menu" (the last item) instead of the account identity, which fails the spec's "VoiceOver or
TalkBack navigation" scenario (3.5, 3.9). Fixed in the shared container; the drawer views, the
public `PushDrawer` signature, and keyboard focus handling are unchanged. Captures are in
[`verification/android/`](verification/android/).

### Cause

TalkBack's focus management (read in the open-source `google/talkback` repository for this fix)
recovers a lost cursor in two places:

- About 150 ms after a subtree change, if its focused node has left the tree, it focuses the
  node last focused in the same window and pane (when that can still be found), otherwise the
  input-focused node, otherwise the first focusable node in traversal order.
- When a pane title appears, it treats the window as changed and, unless the cursor is on a
  valid node, restores the node last focused in that pane before falling back to the first
  content.

The opener leaves the tree on the first opening frame. While the drawer opened, "Close menu" was
the only reachable node, so TalkBack moved there, and it kept that still-valid node when the drawer
settled. By the same code, the "Menu" pane title would have sent a reopening TalkBack back to
whatever it last focused in the drawer, such as "Close menu" after closing with it; that path was
not tried live with the pane title in place.

### What changed

- `PushDrawer.kt`: the dismissal surface's semantics ("Close menu", button role, click action)
  are exposed from the moment the drawer settles open until it is closed again (open, and closing
  or dragging after it has settled open). While a closed drawer opens, by request or under a
  finger, nothing in the window is reachable, so TalkBack's cursor goes to the drawer's first
  element once the drawer settles; the dismissal follows it in traversal order. Taps on the strip
  still dismiss throughout, and Back stays enabled whenever the drawer is not closed.
- The "Menu" pane title and its `menu_pane_title` strings (`values`, `values-es`,
  `values-es-rES`) are removed.
- `DrawerReveal.settledIsOpen` is snapshot state, so composition reads it.

### Automated coverage

```bash
cd Android
./gradlew :ui:testDebugUnitTest --rerun
./gradlew :app:assembleProdDebug :app:testProdDebugUnitTest --rerun :pool:testDebugUnitTest --rerun
# One class per run; the AVD is emulator-5554.
ANDROID_SERIAL=emulator-5554 ./gradlew :ui:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.felipearpa.tyche.ui.<Class>
```

- JVM: `:ui` **34 passed**, `:app` **58 passed**, `:pool` **2 passed**, 0 failed.
  `:app:assembleProdDebug` built.
- `:ui:connectedDebugAndroidTest`, one run of every class after the change, **55 passed, 0
  failed**: `PushDrawerAccessibilityTest` 8 (run three times, all passed), `PushDrawerGestureTest`
  24, `PushDrawerDismissalTest` 8, `PushDrawerRevealTest` 5, `PushDrawerDisabledAnimationsTest`
  4, `PushDrawerResizeTest` 2, `GestureDisableModifierTest` 1, `ProgressIndicatorTest` 3.
- **Regression check:** with the old rule (dismissal exposed whenever the drawer is not closed)
  temporarily restored, the new opening test failed with `List should be empty but has 1
  elements, first being: "Close menu"`; with the fix it passes.
- Test changes:
  - `PushDrawerAccessibilityTest`: the opening test now asserts that the tree an accessibility
    service reads has no labelled or clickable node mid-opening, and that once open the menu's
    first element comes first and "Close menu" last. A new test holds a drag from closed at 0.5
    and finds no "Close menu" until the drawer settles open. The pane-title assertions became
    "no pane title".
  - `PushDrawerResizeTest`: mid-drag and mid-opening positions are read from the pushed screen
    (the widest node with cleared semantics), since the dismissal node is not exposed then; at
    the open endpoint both the dismissal and the pushed screen are checked at the new width.
  - `PushDrawerHarness.kt`: the menu's first label is a named constant.

### TalkBack, live (AVD, TalkBack 16.0)

`prodDebug` with the fix installed by `adb install -r` on the signed-in AVD (host GPU,
headless). TalkBack was driven through the emulator console's touchscreen as in 3.9, and the
cursor was read from TalkBack's outline in screenshots. No double tap was sent unless a screenshot
showed the cursor on the intended element; Log out, Invite, and Delete pool were never activated.

Pool list ([panel](verification/android/list-talkback-open-lands-on-account.jpg)):

| Step | TalkBack's cursor |
| --- | --- |
| Touch the opener, double tap | Account identity (name and email) |
| Swipe forward | Profile, Log out, "Close menu"; no host control |
| Double tap on "Close menu" | Drawer closed; the opener |
| Double tap (reopen) | Account identity |
| Swipe to Log out (not activated), Back | Drawer closed; the opener |
| Double tap (reopen), Back, double tap sent at the opener's position | Account identity both times |
| Swipe to Profile, double tap; Back from Profile | Profile opened once; back on the list with the drawer closed; the opener |
| Double tap (reopen) | Account identity |
| `animator_duration_scale` 0: open; close with "Close menu"; reopen | Account identity; the opener; account identity |
| Plain tap on the opener while the cursor is on the first pool row; Back | Account identity; the opener |

Every opening landed on the account identity: 7 of 7 from the opener and 1 by plain tap.

Pool home, member pool with 13 gamblers
([panel](verification/android/home-talkback-open-and-order.jpg)):

| Step | TalkBack's cursor |
| --- | --- |
| Touch the opener, double tap | Account identity |
| Swipe forward | Profile, the pool summary ("Playing now", pool name, 8º · 589 points), "POOL", Invite, Log out, "Close menu"; no leaderboard row, tab, or top-bar control |
| Double tap on "Close menu" | Drawer closed; the opener |
| Double tap (reopen) | Account identity |
| Back | Drawer closed, pool home still shown; the opener |

- **Closing returns TalkBack to the opener** on both hosts, including after a plain-tap opening
  (3.9 recorded the pool row there). Without a pane title, TalkBack's recovery takes the first
  focusable node on the host, and on both hosts that is the opener.
- The Accessibility Suite's notification prompt appeared once when TalkBack started and was
  declined with "Don't allow"; it did not return. The permission stays denied.
- Pool home's switch-pool button (⇄) has no accessible label (empty in the tree), and TalkBack
  touches on it did not move the cursor; the pass returned to the pool list with TalkBack off.
  It is a host control, outside the drawer.

### Decisions

- **No dismissal node while a closed drawer opens.** For about 0.47 s with normal motion (one or
  two frames with animations off), and while a finger drags a closed drawer open, TalkBack has no
  "Close menu" to reach. Strip taps still dismiss, and Back (TalkBack's back gesture, the key, or
  the button) dismisses throughout. The spec asks for an accessible dismissal action while
  modal; this reads Back as that action during the opening. **Decision (Felipe, 2026-09-24):
  accepted, as the trade-off that fixed TalkBack landing on "Close menu".**
- **No "Menu" pane title.** TalkBack announces the account identity when the drawer opens
  instead of "Menu".

### Settings and cleanup

The emulator was booted headless for this pass and shut down afterwards; the Gradle daemons are
stopped. TalkBack was enabled and then turned off again (`enabled_accessibility_services`
deleted, `accessibility_enabled` 0, `touch_exploration_enabled` back to 0), and
`animator_duration_scale` 0 was deleted (it was unset). The app was left signed in on the pool
list; nothing was shared or deleted, and sign-out was never touched.

## Android — keyboard focus review fix (2026-09-24)

Review findings 1 and 2 (the same defect, should-fix): keyboard focus could stay on a hidden
drawer action after the drawer closed, and Enter then ran that action with no drawer visible,
which fails the spec's "Drawer is closed" scenario (keyboard) and "Closing SHALL restore focus to
the opener when the route remains active". Fixed in the shared container (`PushDrawer.kt`); the
drawer views, the hosts, and the public `PushDrawer` signature are unchanged. No capture was
added: the results below are read from the accessibility tree (`focused="true"`).

### Cause

- Focus left the drawer only at the closed endpoint, and only if the drawer had taken focus
  itself when it settled open and the host route was resumed at that moment. After a touch
  opening the drawer's actions cannot take focus, so a later Tab into them was never recorded;
  and Invite's share sheet pauses the activity before the close finishes, so the return was
  skipped.
- The drawer's focus properties only refuse focus *entering* it. Compose UI 1.11.1 keeps focus on
  a control that is no longer placed and still dispatches keys to it, so Enter reached the hidden
  action's click. Touching the screen cleared that focus; key-only closes (Escape, the Back key)
  did not.

### What changed

- The drawer content group reports whether it holds focus.
- Focus held anywhere in the drawer is cleared as soon as the caller asks it to close (from the
  composition that sees `isOpen = false`, a frame before the closing phase is composed) or the
  drawer leaves the open endpoint by itself (drag, predictive Back), and at the closed endpoint
  when animations are off and no closing phase is composed. From that same composition the drawer
  also refuses focus entry, so the system's refocus after clearing cannot put focus back on it.
- Once the drawer settles closed, the cleared focus returns to the pushed screen through its
  focus restorer: the control that held it before (the opener after a keyboard opening), or else
  the pushed screen's default entry. While the host route is not resumed, the return waits for it,
  so after the share sheet focus comes back to the opener; a route that navigated away is disposed
  first, and the destination keeps its own focus handling.
- **Found in the live check below and fixed with it:** in the app, a Tab after a touch opening
  reached nothing. With nothing focused, the system's first focus search (Compose's search from
  the window's top-left corner, read in the 1.11.1 sources) prefers the pushed screen's opener,
  which is nearer the top than Profile below the account header; the pushed screen refuses that
  entry while the drawer is modal, and the search does not try the next candidate. Escape then did
  nothing either, since no Compose node had focus to receive it. Keyboard input that begins while
  the drawer is open (Compose's input mode switching to keyboard) now moves focus into the drawer,
  as a keyboard opening does. The harness did not show this, because its menu's first action is
  nearer the top than any host control.

### Automated coverage

```bash
cd Android
./gradlew :ui:testDebugUnitTest --rerun :app:testProdDebugUnitTest --rerun \
  :pool:testDebugUnitTest --rerun :app:assembleProdDebug
# One class per run; the AVD is emulator-5554.
ANDROID_SERIAL=emulator-5554 ./gradlew :ui:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.felipearpa.tyche.ui.<Class>
```

- JVM: `:ui` **34 passed**, `:app` **58 passed**, `:pool` **2 passed**, 0 failed.
  `:app:assembleProdDebug` built.
- `:ui:connectedDebugAndroidTest`, final run of every class on the final code, **63 passed, 0
  failed**: `PushDrawerAccessibilityTest` 15 (7 new), `PushDrawerDisabledAnimationsTest` 5 (1
  new), `PushDrawerGestureTest` 24, `PushDrawerDismissalTest` 8, `PushDrawerRevealTest` 5,
  `PushDrawerResizeTest` 2, `GestureDisableModifierTest` 1, `ProgressIndicatorTest` 3.

| New test | Shows |
| --- | --- |
| Touch opening, Tab into the drawer, Escape; the same with Back; the same with animations off | Nothing is focused after the touch opening; once closed, focus is on the pushed screen's default entry (the host row), and Enter clicks it, not Profile or Sign out |
| Keyboard focus on Profile when the host closes the drawer | Four frames into the close nothing is focused and Enter clicks nothing; once closed, focus is on the opener |
| One frame after an Enter that closes the drawer, Enter again | Nothing is focused after that frame, and the action ran once |
| An action that closes the drawer and pauses the route (as Invite's share sheet does) | While paused and closed nothing is focused; once resumed, focus is on the opener and Enter reopens the drawer instead of running the action again |
| An action that navigates inside a `NavHost`, Enter again four frames into the close | One navigation (the back stack is host, destination); no focus returns to the departed host |
| Touch opening with the menu's first action below the host controls (`menuTopPadding` 200 dp), Tab, Escape | Tab puts focus on Profile; Escape closes and focus goes to the host row |

Regression checks, each replacing only the named part of `PushDrawer.kt`:

- **The previous focus handling restored** (as before this fix; the added focus tracking left in,
  unused): 6 of the new tests failed (this run came before the last two tests were written).
  After the touch openings, focus stayed on "Profile" once closed and Enter clicked it (1 click
  instead of the host row's); Enter during the close reached Profile (1 click instead of 0); with
  the paused route, focus stayed on Profile and the last Enter ran it again (2 clicks) instead of
  reopening the drawer; inside the `NavHost` the second Enter navigated again (2 clicks, the
  destination pushed twice).
- **Without the input-mode entry:** the menu-below test failed: after Tab nothing was focused, and
  Escape then left the drawer open.
- **Without the release on the caller's request:** the one-frame test failed: Profile was still
  focused after the frame, and the second Enter ran it again (2 clicks).
- Harness (`PushDrawerHarness.kt`): the drawer row's action can be set (`onDrawerRowClick`), Sign
  out counts its clicks, the host can be given its own lifecycle owner (`HarnessLifecycleOwner`),
  and `menuTopPadding` moves the menu's actions down. `focusedNodeLabels(useUnmergedTree = true)`
  also finds focus held by hidden or unplaced nodes.

### Live, app (AVD, final build)

`prodDebug` with the final code installed by `adb install -r` on the signed-in AVD (host GPU,
headless). Keys were sent with `adb shell input keyevent` (Tab, Enter, Escape, Back). Before every
Enter, the tree showed which node had focus; Enter was never sent with focus on Log out. After a
last comment-only edit, the rebuilt APK was reinstalled and the first two pool list rows rechecked
with the same results.

| Host | Steps | Result |
| --- | --- | --- |
| Pool list | Tap the opener (touch), Tab | Focus on Profile |
| Pool list | Tab (to Log out), Escape | Drawer closed; focus on "Open menu" |
| Pool list | Enter | The drawer opened with focus on Profile: Enter reached the opener, not the hidden Log out |
| Pool list | Keyboard opening (Tab to the opener, Enter), Tab to Log out, Escape | Focus on "Open menu" |
| Pool list | Enter on Profile, then a second Enter about 120 ms later and, in another run, at least 150 ms later | Profile opened once each time; one Back returned to the list with the drawer closed |
| Pool list | Both Enters in one `input keyevent` call, so the second arrives right after the first is handled | Profile pushed twice (two Backs needed); see Decisions *(fixed in the [double dispatch fix](#android--double-dispatch-fix-2026-09-24))* |
| Pool home (member) | Keyboard opening, Tab to Invite, Enter | Drawer closed and the share sheet opened |
| Pool home | Back (dismisses the sheet, nothing shared) | Focus on "Open menu" |
| Pool home | Enter | The drawer opened with focus on Profile; no share sheet |
| Pool home | Touch opening, Tab (Profile), Tab (Invite), Enter, Back from the sheet | Focus on "Open menu"; Enter then opened the drawer, not the sheet |

With an intermediate build (focus cleared only once the closing phase was composed, and no
input-mode entry), the same pool list steps showed the live gap: after a touch opening, three Tabs
left nothing focused and Escape did nothing. That build also let a second Enter 150 ms after
choosing Profile reach Profile's own back button, which the destination had just focused during the
route transition, so the app returned to the list; with the final build, focus is cleared a frame
earlier and the same timing left Profile open once.

### Decisions

- ~~**A second activation before the next frame still runs the action twice.**~~ *(Fixed in the
  [double dispatch fix](#android--double-dispatch-fix-2026-09-24), as Felipe decided on 2026-09-24.)* The drawer learns
  of the close request only when the host recomposes, so two Enters handled before that composition
  both reach the action; two taps that close together would do the same through touch. The host
  navigation callbacks have no guard against repeated activation (unchanged by this change); a
  guard such as `dropUnlessResumed` in the hosts' destination callbacks would cover both. Not
  changed here.
- **After a touch opening, closing sends keyboard focus to the pushed screen's default entry**,
  since no control on the pushed screen held it; on both hosts that is the opener.
- **Keyboard input that begins while the drawer is open moves focus into the drawer.** This goes
  beyond the two findings; without it, a keyboard user who opened the drawer by touch could not
  reach its actions or close it with Escape. **Decision (Felipe, 2026-09-24): accepted and
  kept.**
- While a destination's route transition runs, the destination can take keyboard focus by its own
  default handling once the drawer gives focus up (seen with the intermediate build).

### Settings and cleanup

The emulator was booted headless for this pass and shut down afterwards; the Gradle daemons are
stopped. No device setting was changed; the last tap returned the AVD to touch mode after the key
input. Fortuna was sent to the launcher once by a Back on the pool list and relaunched. The
app was left signed in on the pool list; nothing was shared or deleted, and sign-out was never
touched.

## Android — double dispatch fix (2026-09-24)

Review finding (recorded as a decision in the
[keyboard focus fix](#android--keyboard-focus-review-fix-2026-09-24), then flagged by reviewers):
two activations handled before the host's next composition, such as two Enters in one injection or
two taps close together, ran a destination twice. That fails "Choosing a destination SHALL close
the drawer and invoke the existing action once" and the scenario "Selecting an existing
destination" (task 3.7, single navigation dispatch). Felipe decided on 2026-09-24 to fix it. The
fix is in the hosts' navigation callbacks; `PushDrawer`'s behavior and public signature are
unchanged. No capture was added: the results are read from the accessibility tree and the activity
stack.

### Cause

- The drawer and its host learn of a choice only when they recompose. Until then the chosen row
  stays placed, enabled, and focused, and the host screen stays composed. `NavController.navigate`
  pushes synchronously on every call, so a second activation pushed the destination again.
- On this AVD the window is wider than a frame. Before the fix, two taps on Profile sent as
  separate `input motionevent` calls, about 60 ms apart, still pushed it twice: the drawer blocks its rows
  only after a composition has started the closing phase, and the frames right after a navigation
  are slow here. A pool row stays tappable through the route's 700 ms exit fade.
- The delete confirmation had the same gap: its dialog leaves at the next composition, and
  `DrawerViewModel.deletePool` started a request on every call.

### What changed

- **`runIfStarted`** (new, `:ui`, `RouteActions.kt`): an extension on the route's
  `LifecycleOwner`, which is its `NavBackStackEntry` and also the `LocalLifecycleOwner` of its
  content. It runs the action only while the entry is at least started. Navigating away moves the
  departing entry below started synchronously (to created; read in the Navigation 2.9.8 sources),
  while its screen stays composed, so a second activation does nothing. It checks started, not
  resumed as androidx's `dropUnlessResumed` does, so a host that is fading in after navigation or
  Back keeps responding: its entry is started, not resumed, until the NavHost's 700 ms fade ends.
- **Pool list nav provider:** Profile (drawer), open a pool, create a pool, and pick a template run
  through it. **Pool home nav provider:** switch pool, Gamblers and Profile (drawer), open a
  gambler's bets, and open a match.
- **Sign-out** runs through it in both drawer views, around the logout and the navigation, so a
  second press neither logs out again nor navigates again.
- **`PoolHomeView`** takes `onPoolDeleted` separately from `onPoolChange`. The switcher is guarded;
  the navigation that follows a completed deletion is not, so it still reaches the pool list if the
  gambler opened another screen or left the app meanwhile. The delete flow is unchanged.
- **`DrawerViewModel.deletePool`** drops a request while one is pending. It now sets the pending
  state before it starts the request; under `Dispatchers.Main.immediate` it was already set
  synchronously, so a single deletion behaves as before.
- Already once, unchanged: Invite and the pool list's share action set the same pending share URL,
  which launches the share sheet once. The Delete pool row only opens the confirmation. Back and
  the opener are not navigation callbacks.
- `PushDrawer`'s KDoc says that drawer actions that leave the route should run through
  `runIfStarted`.

### Automated coverage

```bash
cd Android
./gradlew :ui:testDebugUnitTest --rerun :app:testProdDebugUnitTest --rerun :app:assembleProdDebug
# One class per run; the AVD is emulator-5554.
ANDROID_SERIAL=emulator-5554 ./gradlew :ui:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.felipearpa.tyche.ui.<Class>
```

- JVM: `:ui` **34 passed**, `:app` **60 passed** (2 new), 0 failed. `:app:assembleProdDebug`
  built.
- `:ui:connectedDebugAndroidTest`, final run of every class on the final code, **68 passed, 0
  failed**:
  `PushDrawerNavigationTest` 5 (new), `PushDrawerAccessibilityTest` 15, `PushDrawerGestureTest` 24,
  `PushDrawerDismissalTest` 8, `PushDrawerRevealTest` 5, `PushDrawerDisabledAnimationsTest` 5,
  `PushDrawerResizeTest` 2, `GestureDisableModifierTest` 1, `ProgressIndicatorTest` 3.

| New test | Shows |
| --- | --- |
| `PushDrawerNavigationTest`: keyboard focus on a drawer action that closes the drawer and navigates; two Enters with the test clock paused | Both Enters reach the action (2 clicks, and the drawer has not composed the close); the destination is pushed once |
| The same action tapped twice through the system input pipeline (`Instrumentation.sendPointerSync`) with the clock paused | 2 clicks before the close is composed; one navigation; the drawer rests closed |
| A host row that navigates, tapped twice the same way | 2 clicks before the destination is composed; one navigation |
| Back to the host, reopen the drawer and choose again; Back, then tap the host row | Each navigates once more (back stack: host, destination) |
| Back, then a host row tap six frames later, while the host fades in (its entry started, not resumed) | Navigates |
| `DrawerViewModelTest`: a second deletion request while the first is pending | One request, one success callback |
| A deletion request after a failed one | A new request starts |

The navigation host in `PushDrawerNavigationTest` wires the harness's drawer row and host row as
both drawer hosts do: through `runIfStarted` on the host's back stack entry. The pool list and pool
home themselves need Koin and live data, so their wiring was checked in the app (below).

Regression checks, each replacing only the named part:

- **`runIfStarted` running the action unconditionally:** the three double-activation tests failed
  with `expected:<["host", "destination"]> but was:<["destination", "destination"]>`; the other two
  passed.
- **`runIfStarted` requiring resumed:** the fade-in test failed (`but was:<[null, "host"]>`, the
  tap was dropped); the other four passed.
- **`DrawerViewModel.deletePool` from `HEAD`:** the pending-request test failed
  (`2 matching calls found, but needs exactly 1`).
- A first version of the two tap tests used `performTouchInput { click(); click() }`. Compose's
  test input dispatcher advances the paused clock between events, so frames ran between the taps
  (the drawer had composed the close). They still failed unguarded, which shows the window lasts
  more than a frame, but they now inject through the system pipeline, so no frame comes between.
- Harness (`PushDrawerHarness.kt`): the host row's action can be set (`onHostRowClick`), and
  `tapTwiceWithinOneFrame` taps a node twice through the system input pipeline.

### Live, app (AVD)

`prodDebug` on the signed-in AVD (host GPU, headless). "Before" is the previous build still
installed at the start (`base.apk` SHA-256 `ca610edd…`, the keyboard focus fix); "after" is the
final build installed by `adb install -r` (`fe8de401…`, equal to the local APK). Counting: Back
until the list or pool home returned, or, from pool home (a back stack root), until Fortuna left the
screen. Two taps in one emulator console session (`event mouse`, several commands per connection)
arrive a few milliseconds apart; shell-level writes to `/dev/input` are refused on this image, and
`adb root` was not used. Keys were sent with `adb shell input keyevent`, and focus was read from the
tree before every Enter. Log out and Delete pool were never activated.

| Host | Check | Before | After |
| --- | --- | --- | --- |
| Pool list | Drawer open; two taps on Profile in one console session | Profile pushed twice, 5 of 5 | Once, 5 of 5 |
| Pool list | Two taps on Profile with `input motionevent` (about 60 ms apart) | Twice, 2 of 2 | Once, 2 of 2 |
| Pool list | Tab to Profile; both Enters in one `input keyevent` call | Twice in 1 of 5 (depends on timing) | Once, 6 of 6 |
| Pool list | Two taps on a pool row, console | Pool home pushed twice, 2 of 2 | Once, 2 of 2 |
| Pool list | Two taps on a pool row, `input motionevent` | Twice, 2 of 2 | Once, 2 of 2 |
| Pool list | Two taps on create pool, console | — | Creator once |
| Pool home (owner) | Two taps on Gamblers, console | — | Manage gamblers once, 3 of 3 |
| Pool home (owner) | Two taps on Profile, console | — | Once, 2 of 2 |
| Pool home (owner) | Tab to Gamblers; both Enters in one call | — | Once |
| Pool home (owner) | Two taps on Invite, console | — | One share sheet; Back dismissed it (nothing shared) and no second sheet came |
| Pool home (owner) | Two taps on the pool switcher, console | — | Pool list once (one Back left the app) |
| Pool home (member) | Two taps on another gambler's leaderboard row, console | — | Their bets once |

- **Later choices still work.** Every trial started after returning from the previous one and
  chose again from the drawer or the host. A switcher tap sent 0.3 s after the tap that opened a
  pool (plus the shell's own start-up), while pool home faded in, returned to the pool list.
- A pool row tap sent right after Back from Profile, in the same shell command, left the list on
  screen. During a pop the NavHost draws the departing screen above the returning one, so Profile's
  surface took the tap. This is the NavHost's behavior, not the guard (the harness test, whose
  destination is plain text, lets the tap through and navigates).
- A pool row tap followed about 100 ms later by a tap where both screens have a top-right button
  (create pool on the list, the switcher on pool home) left pool home on screen once and opened
  nothing else: pool home was not composed yet, so the second tap reached the departing list, whose
  create button no longer navigates. This was not run on the previous build.

### iOS (read only, not changed)

*(Superseded: reproduced and fixed in
[iOS — double dispatch fix](#ios--double-dispatch-fix-2026-09-24).)*

Unclear: the code has the same unguarded structure, but a double activation was not staged. The
routers' destination callbacks append to the navigation path with no guard
(`PoolScoreListRouter.swift:146-147` Profile; `PoolHomeRouter.swift:198-199` Gamblers, `209-210`
Profile; host controls at `PoolScoreListRouter.swift:83`, `85`, `166` and
`PoolHomeRouter.swift:99`, `109`). The drawer starts closing only in the view update that follows
(`DrawerView.swift:75`, `.onChange(of: isShowing)`), and its actions stop taking touches when that
update recomputes the phase (`DrawerView.swift:225`, `.allowsHitTesting(phase == .open)`). So two
actions that run before SwiftUI's next update would both append. Whether UIKit delivers two taps,
or two Full Keyboard Access presses, before that update was not tested; iOS 2.4 observed single
dispatch with single taps only. `PoolHomeDrawerViewModel.deletePool` also has no pending guard
(`PoolHomeDrawerViewModel.swift:105-107`), but its confirmation is a system alert.

### Remaining issues and decisions

Felipe's decisions of 2026-09-24:

1. **Double navigation dispatch within one frame: fixed** in this pass (above).
2. **Accepted:** while a closed drawer is opening or being dragged open (about 0.5 s), TalkBack has
   no "Close menu" dismissal node; Back dismisses throughout. This is the trade-off that fixed
   TalkBack landing on "Close menu"
   ([TalkBack focus review fix](#android--talkback-focus-review-fix-2026-09-23)).
3. **Accepted:** a horizontal swipe across pool home's tab row opens the drawer on Android; tab taps
   still select. iOS reserves its tab bar.
4. **Accepted:** rotation closes an open drawer (pre-existing; both hosts keep `isDrawerOpen` in
   `remember`).
5. **Accepted and kept:** keyboard input that begins while the drawer is open moves focus into it
   (added in the [keyboard focus fix](#android--keyboard-focus-review-fix-2026-09-24)).
6. **The verification captures may keep Felipe's personal information** (photo, email, pool
   names); they are not blurred.

Also from this pass:

- The guard is a lifecycle check, not a timer: nothing waits, and nothing stays blocked once the
  route is started again. While a route is left for a destination that floats above it (a dialog
  destination), its entry stays started, so the guard would not apply there; neither host opens
  one.
- Not observed: a physical device, and the Delete confirmation pressed twice in the app (that
  would delete a real pool); the view model test covers it.

### Settings and cleanup

The emulator was booted headless for this pass and shut down afterwards; the Gradle daemons are
stopped. No device setting was changed. The final build (`fe8de401…`) stays installed; Fortuna was
left signed in on the pool list. It left the screen several times through Back from a back stack
root and was relaunched each time. Nothing was shared or deleted, and sign-out was never touched.

## iOS — double dispatch fix (2026-09-24)

The [Android fix](#android--double-dispatch-fix-2026-09-24) left the same question open on iOS:
the routers' callbacks appended to the navigation path with no guard, so two activations handled
before SwiftUI's next update could both navigate. Felipe decided on 2026-09-24 to fix it on iOS
too. The requirement is "Choosing a destination SHALL close the drawer and invoke the existing
action once" and the scenario "Selecting an existing destination" (task 2.4, "destination
callbacks run once"). The fix is in the routers' callbacks, through a new UI package type; the
drawer container's behavior is unchanged.

Same Xcode (26.0.1) and simulators as the earlier iOS passes: the signed-in iPhone 18 Pro
(iOS 27.0), the iPhone 17 (iOS 27.0) for the harness and the workspace suite, and the iPhone 16 Pro
(iOS 18.1) for the UI package.

### Reproduction (before the fix)

**Instrument (not committed).** A scratch XCUITest synthesized touch sequences with exact offsets
through XCTest's private event record (`XCSynthesizedEventRecord` and `XCPointerEventPath`, called
through `objc_msgSend`), so two touches can land within one frame. It drove two targets:

- **Harness** (iPhone 17): an app compiled with `swiftc` from the working-tree `DrawerView.swift`,
  `DrawerReveal.swift`, and `DrawerStyle.swift`, with two hosts and a root that mirror the pool
  list and pool home routers and `PoolContent`, callback for callback. It counts every action
  closure that runs and logs each action, render, and path change with a millisecond clock.
- **App** (signed-in iPhone 18 Pro, working-tree Debug build installed by the test run). The pass
  counted the screens above the host by edge back-swiping until the host's menu button was hittable
  again, and recorded a drawer or share sheet left on screen. Sign out and Delete pool were never
  tapped; the share sheets were closed unsent.

Touch patterns, two rounds each:

| Pattern | Touches |
| --- | --- |
| Sequential | One finger, two taps whose touch-downs are 5, 10, 16, 25, 40, or 100 ms apart |
| `doubleTap()` | XCTest's own double tap |
| Two fingers, same control | Both on the control, 40 pt apart: lifting together, or overlapping |
| Two fingers, two controls | Lifting together (down together, up 30 ms later), or overlapping (A down at 0 and up at 20 ms, B down at 10 ms and up at 60 ms) |

Harness results:

- **One finger never activated twice.** 96 sequential trials and 18 `doubleTap()` trials over
  Profile, create, a pool row, a leaderboard row, Gamblers, switch pool, and the delete
  confirmation all ran their action once. The log shows why: SwiftUI re-evaluated the host's body
  within about 1 ms of the first action, before the second touch arrived; by then the drawer's
  rows no longer took touches, and the host had pushed or been replaced.
- **Two fingers on the same control activated nothing** (SwiftUI buttons and tap-gesture rows),
  except overlapping fingers on a toolbar button or the alert's Delete, which activated once.
- **Two fingers on two controls lifting together ran both actions:** two leaderboard rows pushed
  two screens; Gamblers and Profile pushed both; Profile and Sign out pushed Profile and ran
  sign-out; create and the menu button pushed the creator and opened the drawer above it; a
  leaderboard row and the menu button did the same; two pool rows both opened a pool, and the
  last one stayed. The log shows the render between them: for two leaderboard rows the second
  action ran 3.4 ms after the first, 2.5 ms after the host re-rendered with the first route
  appended.
- **Overlapping fingers:** a touch that began before the first action still activated its
  control after that action's render. Gamblers and Profile, lifted 40 ms apart, pushed both (2 of
  2); the other pairs ran at most one action.
- **The delete confirmation never confirmed twice:** at most one Delete in all 31 trials,
  including Delete and Cancel lifting together.

App results (before):

| Host | Pattern | Result |
| --- | --- | --- |
| Pool list | Profile, create, a pool row: sequential and `doubleTap()` | One screen every time (38 trials) |
| Pool list | Create and the menu button, lifting together | Creator pushed and the drawer open above it, 2 of 2 |
| Pool list | Two pool rows, lifting together / overlapping | The first row's pool, 4 of 4 / the second row's, 4 of 4 |
| Pool home | Two leaderboard rows, lifting together | Two screens pushed, 4 of 4 |
| Pool home | Two leaderboard rows, overlapping | Two screens pushed, 4 of 4 |
| Pool home (owner) | Gamblers and Profile, lifting together | Two screens pushed, 2 of 2 (overlapping: one) |
| Pool home | Profile and Invite, lifting together | Profile pushed and the share sheet presented above it, 3 of 3 (overlapping: the share sheet only) |
| Both | One control, two fingers | Nothing activated |
| Pool home | Profile, a leaderboard row, Gamblers: sequential and `doubleTap()` | One screen every time (40 trials) |

So on iOS the double dispatch needs two touches in progress at once, or ending in the same event,
such as two fingers. One finger tapping quickly did not reproduce it, even 5 ms apart. Full
Keyboard Access was not staged: it was turned on in the harness simulator, but XCTest's key presses
did not move keyboard focus onto the rows (the focused-element query found the menu button once and
nothing after), so no key activation was delivered.

### Cause

A control responds according to the last update SwiftUI rendered, and a touch keeps the control it
began on. Two actions handled before the next update, or a second touch that began before it, both
run against the routers' state, and the routers appended to `path` and set `drawerVisible`
unconditionally. `@State` writes are visible at once to the next action closure; nothing read them.

### What changed

- **`DrawerHostNavigation`** (new, UI package, `DrawerHostNavigation.swift`): a drawer host's
  `path` and `isDrawerOpen`, with members that check this state when the action runs, where the
  first activation is already recorded. Nothing waits and nothing stays blocked: once the host is
  visible again after Back, or the drawer reopens, the next choice goes through.
  - `open(_:)` pushes from a host control only while the host is interactive: nothing above it
    (`isHostVisible`, the empty path) and its drawer closed.
  - `openFromDrawer(_:)` and `closeDrawerForChoice()` run a drawer choice only while the drawer is
    still open, closing it; the first choice closes it, so a second finds it closed.
  - `toggleDrawer()` runs the menu button only while the host is visible, so the drawer never
    opens above a destination.
- **Both routers** keep a `DrawerHostNavigation` instead of `path` and `drawerVisible`, bind the
  stack and the drawer to it, and pass `isHostVisible` as `allowsDragging`. Profile, Gamblers,
  Invite, and sign-out are drawer choices; create pool, pick template, a gambler's bets, and a
  match open through `open`; open pool and switch pool, which replace the host rather than push,
  check `isHostInteractive` first; the menu button uses `toggleDrawer()`.
- **Sign-out**: the routers now call the drawer view model's sign-out after
  `closeDrawerForChoice()`, then the existing `onSignOut`. The drawer views pass their sign-out
  row straight to the router; before, they started the logout themselves, ahead of any check.
- **`PoolContent`** keeps the first pool chosen: a second pool row in the same update finds a pool
  already selected. Switch pool sets the selection to none, which a second activation repeats
  without effect.
- **Unchanged:** Back, the destinations' own controls (a match from a gambler's bets, the username
  editor from Profile, `onHome`), the delete confirmation and its pending state, the completed
  deletion's navigation (not guarded, so it always reaches the pool list), and the drawer
  container. No pending guard was added to `PoolHomeDrawerViewModel.deletePool`: the system alert
  confirmed at most once in every pattern above.
- `drawer(isShowing:allowsDragging:content:)`'s documentation names `isHostVisible`.

### Automated coverage

```bash
cd iOS/UI && xcodebuild test -scheme UI -destination 'id=A2F5EF6F-BA25-4035-9494-F6ACE0144047'
cd iOS && xcodebuild test -workspace Tyche.xcworkspace -scheme Tyche \
  -destination 'id=4D209B45-123A-426F-8ABC-B882E7211E6B' -parallel-testing-enabled NO \
  -skip-testing:PoolTests -skip-testing:UITests/DrawerInteractionTests
cd iOS && TEST_RUNNER_DRAWER_UI_PASS=1 xcodebuild test -workspace Tyche.xcworkspace -scheme Tyche \
  -destination 'id=4DC816B1-F082-4C29-98D2-1E6CE4B80185' -parallel-testing-enabled NO \
  -only-testing:TycheUITests/DrawerPassUITests
```

- UI package, iOS 18.1: **68 passed, 0 failed** — 59 Swift Testing tests in 8 suites plus the 9
  XCTest cases (52 before). New:
  - `DrawerHostNavigationTests` (**new suite, 11 tests**, no ViewInspector, so it runs on every
    runtime): each case runs a second action against the state the first left. A second host
    control opens nothing until Back; the menu button after a host row leaves the drawer closed,
    and a host row after the menu button opens nothing; a drawer choice closes the drawer and
    opens its destination once; a choice that leaves without a destination (sign-out) runs once
    per opening; a destination after another choice, a host control after a drawer choice, and a
    drawer choice after a tap on the pushed screen open nothing; after Back, and after reopening
    the drawer, the next host control, menu button, and drawer choice go through.
  - `DrawerInteractionTests.HostNavigationWiring` (**new nested suite, 5 tests**): a host wired
    as both routers are, with a row, a menu button, and Profile and Sign out in the drawer.
    ViewInspector activates the rendered controls with no render in between: Profile twice opens
    it once and closes the drawer; Profile then Sign out runs only Profile; a host row twice
    opens once; a host row then the menu button leaves the drawer closed; after Back and
    reopening, Sign out runs and the host row opens again.
- Workspace suite, iPhone 17: **178 tests — 170 passed, 8 skipped, 0 failed** (167, 159, and 8
  before; the 11 added are `DrawerHostNavigationTests`, and the wiring suite is skipped with
  `DrawerInteractionTests`). The skips are the same eight opt-in passes and wire probes.
- `DrawerPassUITests`, signed-in iPhone 18 Pro: **4 passed, 0 failed** (pool list 17.0 s, pool
  home 55.4 s, pool list gestures 49.0 s, pool home gestures 49.9 s), unchanged: Profile opens
  once and returns closed, Invite closes the drawer and presents the share sheet (closed unsent),
  Delete pool asks for confirmation (cancelled), and the gesture steps hold.
- Builds: the app and the UI package built with no warnings in the touched files; the harness
  compiled `DrawerHostNavigation.swift` at an iOS 16 target. `Package.resolved` was checked after
  every `xcodebuild` run and never changed.

Regression check: `DrawerHostNavigation`'s members replaced by unguarded versions (append,
toggle, and close unconditionally, as the routers did), then restored:

- 12 tests failed: 8 of the 11 in `DrawerHostNavigationTests` and 4 of the 5 wiring tests, for
  example `aDrawerDestinationActivatedTwiceBeforeTheNextUpdateOpensOnce` with a path of
  `[profile, profile]` and `twoDrawerRowsActivatedBeforeTheNextUpdateRunOnlyTheFirst` with one
  sign-out. The four that passed are the "later choice goes through" cases, which hold either way.
- Restored, byte-identical to the version that passed, the final run passed (68 of 68, above).
- No test covers `PoolContent`'s first-pool check or the routers' wiring directly: the routers
  need live dependencies and `TycheTests` has no ViewInspector. Both were checked in the app
  (below).

### Live, app and harness (after)

**Harness** (guarded build: the same mirrors acting through `DrawerHostNavigation` and the
first-pool check, compiled with the working-tree `DrawerHostNavigation.swift`), the same patterns
with one sequential gap (16 ms), two rounds each. The counters show that both action closures still
ran; the checks let only the first act:

| Pair lifting together | Before | After |
| --- | --- | --- |
| Two leaderboard rows | Two screens | One screen, 2 of 2 (overlapping: one) |
| Gamblers and Profile | Two screens | One screen, 2 of 2 (overlapping: one, 2 of 2) |
| Profile and Sign out | Profile pushed and sign-out ran | Sign-out ran, Profile not pushed, 2 of 2 |
| Create and the menu button | Creator pushed, drawer opened above it | Creator pushed, drawer stayed closed, 2 of 2 |
| A leaderboard row and the menu button | Bets pushed, drawer opened above them | Drawer opened, the row did nothing, 2 of 2 |
| Two pool rows | Both opened a pool, the last one stayed | One pool opened, 2 of 2 |
| Profile and Invite | (not run before in the harness) | Invite ran, Profile not pushed, 2 of 2 |
| Profile and Delete pool | (not run before in the harness) | Profile pushed **and** the delete confirmation shown above it, 2 of 2 (see Remaining issues) |

Sequential taps, `doubleTap()`, and two fingers on one control behaved as before (one activation
or none), and the delete confirmation still confirmed at most once. In the overlapping Gamblers and
Profile trial the log shows the two action closures 67 ms apart, with two renders and Profile's
appearance between them; the second found the drawer closed and did nothing.

**App** (signed-in iPhone 18 Pro, fixed working-tree build installed by the run):

| Host | Pattern | Before | After |
| --- | --- | --- | --- |
| Pool list | Profile: sequential (5, 16, 40, 100 ms) and `doubleTap()` | One screen | One screen, 10 of 10 |
| Pool list | A pool row: sequential and `doubleTap()` | Pool home, nothing above it | Pool home, nothing above it, 10 of 10 |
| Pool list | Create: sequential and `doubleTap()` | One screen | One screen, 10 of 10 |
| Pool list | Create and the menu button, lifting together | Creator pushed, drawer open above it | Creator pushed, drawer closed, 2 of 2 |
| Pool list | Two pool rows, lifting together | The first row's pool, 4 of 4 | The second row's pool, 2 of 2 |
| Pool home | Two leaderboard rows, lifting together / overlapping | Two screens, 4 of 4 / 4 of 4 | One screen, 2 of 2 / 2 of 2 |
| Pool home (owner) | Gamblers and Profile, lifting together / overlapping | Two screens, 2 of 2 / one | One screen, 2 of 2 / 2 of 2 |
| Pool home | Profile and Invite, lifting together | Profile pushed, share sheet above it, 3 of 3 | Share sheet only, Profile not pushed, 2 of 2 |
| Pool home | Profile, a leaderboard row, Gamblers: sequential and `doubleTap()` | One screen | One screen, 30 of 30 |

- **Fast double taps open one screen.** A double tap on Profile or on a pool row, whether
  `doubleTap()` or two synthesized taps 5 to 100 ms apart, opened Profile or pool home once, with
  nothing above it, before and after the fix.
- **Two pool rows:** in both builds the second row's action ran first when the fingers lifted
  together. Before, the first row's action then replaced it; now it is dropped, so the first
  choice stays. Overlapping fingers opened the second row's pool in both builds, a single action.
- The overlapping pairs that ran one action before, create with the menu button and Profile with
  Invite, ran the same single action after.
- In one Profile-and-Invite trial after the fix, and one before it, the pass still found the
  drawer's dismissal surface right after closing the share sheet; a tap on the strip removed it,
  and the next trial started normally. `DrawerPassUITests` saw the app's window unmeasurable at
  the same point in 2.10.

### Remaining issues and decisions

For Felipe:

1. **A drawer choice and Delete pool lifting together** open the choice and present the delete
   confirmation above it (harness, 2 of 2; not staged in the app, where the confirmation belongs
   to a real pool). Nothing is deleted without confirming. The Delete pool row only asks for
   confirmation and keeps the drawer open, so it is not a drawer choice; stopping the pair would
   need the drawer view to check the drawer's state when the row fires and the host to track a
   pending confirmation. Left as is: the brief keeps the delete confirmation unchanged.
2. **Invite is now a drawer choice.** The brief kept invite and share unchanged: a single Invite
   still closes the drawer and presents the share sheet (`DrawerPassUITests`). What changed is
   that Invite and another choice in the same event no longer both run; before, Profile and Invite
   pushed Profile and presented the share sheet above it.
3. **The menu button is checked too** (`toggleDrawer()`), although it is not a destination. The iOS
   drawer wraps the navigation stack, so a menu tap in the same event as a host control opened the
   drawer above the new destination (reproduced in the app). Android's drawer lives inside the host
   route and does not have this case.
4. **No pending guard on `deletePool`**: the system alert confirmed at most once in every pattern
   (31 harness trials, including Delete and Cancel together), so a double confirmation could not
   start two deletions. Android's view model has one; adding it here would be for parity only.
5. **Sign-out's logout call moved** from the drawer views into the routers, after the check. The
   order is unchanged: the logout starts, then the session router shows sign-in.

Also from this pass:

- The destinations' own controls stay unchecked: a match from a gambler's bets, a gambler from a
  match's bets, the username editor from Profile, and `onHome`. Two fingers on two rows of a
  gambler's bets could still push twice. They are outside the two drawer hosts; the Android fix
  covered the same host callbacks.
- The reproduction instrument uses XCTest's private event API, so it is not committed, and no
  committed UI test reproduces touches within one frame. `DrawerPassUITests` is unchanged.
- Not observed: a physical device; VoiceOver, where a double tap is one activation; the pool list's
  template picks and the Bets and History tabs' match rows (the same `open` as create and the
  leaderboard rows); sign-out in the app (harness only); iOS 16 and 17 runtimes (the harness
  compiled the new file at an iOS 16 target).

### Settings and cleanup

The harness app was uninstalled from the iPhone 17 and the scratch UI test removed from
`TycheUITests` before the final runs; neither is committed. Full Keyboard Access was turned on in
the iPhone 17 for the attempt above; its three preference keys, absent before the pass, were
deleted afterwards. The simulators this pass booted (iPhone 17, iPhone 18 Pro, iPhone 16 Pro) were
shut down; the iPhone Air was already running and was left as found. On the iPhone 18 Pro the
session is intact and the fixed build stays installed. Nothing was shared or deleted, no delete
confirmation was shown in the app, and sign-out was never touched.

## iOS — navigation title position (2026-09-24)

Felipe reported on the running app that while the drawer opens, and while it is open, the pushed
screen's navigation title moves a little to the left, as if its leading padding were removed. It
does, on both hosts, and in landscape more of the pushed screen moves. The cause is how UIKit lays
out the navigation stack that SwiftUI hosts, not one of the drawer's modifiers. **Felipe accepted
it as a known iOS limitation on 2026-09-24; no product code changed** (see the decision below).

Same Xcode (26.0.1) as the earlier iOS passes. The app was measured on the signed-in iPhone 18 Pro
(iOS 27.0); a throwaway harness ran on the iPhone 17 (iOS 27.0) and the iPhone 16 Pro (iOS 18.1).
Captures, all taken before any decision: [pool list, portrait](verification/ios/nav-title-list-portrait.jpg)
(closed, drag held at about half, open), [pool list, landscape](verification/ios/nav-title-list-landscape.jpg)
(closed above, open below), and [pool home, landscape, dark](verification/ios/nav-title-home-landscape-dark.jpg)
(closed above, open below).

### Cause

The drawer moves the pushed screen with `.offset(x:)` (`DrawerForegroundReveal`). Each host's
screen is a `NavigationStack`, which SwiftUI hosts as a UIKit navigation controller, and SwiftUI
applies the offset by moving that hosted view: with the drawer open, the view that contains it
sits at x = 340 pt in portrait. UIKit derives the navigation controller's layout margins and
safe-area insets from where its view sits in the window, so moving the view changes its layout:

- **Portrait, iOS 26 and later.** The navigation controller's leading layout margin becomes
  max(0, 16 − x) for a displacement of x pt. The large title is laid out on that margin, while the
  toolbar buttons keep a fixed 16 pt inset. The title therefore loses its 16 pt inset during the
  first 16 pt of travel and stays at the pushed screen's edge until the drawer closes. Nothing else
  on the two hosts moves in portrait.
- **Landscape, every version (iOS 18.1 checked in the harness).** UIKit recomputes the safe area
  from the window: the 62 pt leading inset on the iPhone 18 Pro becomes max(0, 62 − x). The rows
  and the menu button hold their positions on screen for the first part of the travel (62 pt for
  the rows, 22 pt for the menu button), then move with the pushed screen, closer to its edge than
  before. The inline title ends 11 pt nearer the edge.
- **Portrait, iOS 18.1 (harness).** The large title keeps its place: that navigation bar lays it
  out with its own margin.

Everything returns to its closed position when the drawer closes: the measurements after closing
equal the closed ones.

### Measurements in the app

Signed-in iPhone 18 Pro, iOS 27.0. Positions are in points from the pushed screen's leading edge,
taken as its navigation bar's leading edge. "Frame" values are XCUITest element frames with the
drawer settled. "Pixels" values come from screenshots and give the first pixel of the glyph or
tile, so the title's includes its glyph's side bearing; the "held" column is a drag held at about
half the reveal, with the pushed screen's edge between 124 and 170 pt. Pool list and pool home, in
light and dark appearance, gave the same frames.

Portrait:

| Element | Closed | Held | Open |
| --- | --- | --- | --- |
| Large title (frame) | 16.0 | — | 0.0 |
| Large title (pixels) | 18.0 list, 17.0 home | 1.3–2.0 | 1.3–2.0 |
| Menu button (frame; held: pixels) | 22.0 | 21–22 | 22.0 |
| First row's tile (pixels) | 24.0 list, 42.3 home | 23–24 list, 41.3–42.3 home | 24.0 list, 42.3 home |

Landscape:

| Element | Closed | Held | Open |
| --- | --- | --- | --- |
| First row (frame) | 62.0 | — | 0.0 |
| First row's tile (pixels) | 86.0 list, 104.3 home | 23–24 list, 41.3–42.3 home | 24.0 list, 42.3 home |
| Menu button (frame; held: pixels) | 44.0 | 21–22 | 22.0 |
| Inline title (frame) | 401.0 list, 409.3 home | — | 390.0 list, 398.3 home |

A screenshot taken while pool home was closing, with the pushed screen's edge at 16 pt, had the
first row's tile at 88.3 pt and the menu button at 28.0 pt: both 16 pt less than when closed, so
still at their closed positions on screen.

### How the cause was isolated

The throwaway harness (not committed) compiled the drawer files, `PlainToolbarItem.swift`, and
`ParentSizeKey.swift` with two hosts that mirror the pool list and pool home: a `NavigationStack`
with a large title and plain toolbar items, the second with a `TabView`. It logged the hosted
navigation controller's position, safe area, and layout margins every 50 ms.

- Reveals held with the pushed screen moved by 6.7, 13.7, and 34 pt gave leading margins of 9.3,
  2.3, and 0 pt on iOS 27. Animated opening and closing followed the same curve, and the toolbar
  buttons stayed at 16 pt.
- Removing the clip, the scrim, the edge line, the hit-testing and accessibility-hiding changes, the
  dismissal surface, or the container drag, one at a time and all together, still left the margin
  at 0 pt with the drawer open. Removing only the offset kept it at 16 pt.
- Landscape, iOS 27: with the drawer open, the navigation controller's leading safe area went from
  62 to 0 pt and its bar's leading margin from 78 to 0 pt.
- iOS 18.1: the same in landscape (leading safe area 62 → 0 pt, bar margin 70 → 8 pt, the inline
  title 31 pt nearer the leading edge, and the content's leading safe area 62 → 0 pt). In portrait
  the large title stayed at 16 pt.

### SwiftUI-only alternatives tried

None keeps the layout, because UIKit reacts to where the hosted view appears on screen, however
SwiftUI moves it:

- `transformEffect`, `projectionEffect`, and `visualEffect` with an offset, in place of `.offset`:
  all three collapse the margin the same way. For the first two, the view tree showed why: SwiftUI
  turns a pure translation into the same frame move (the containing view at x = 340 pt with an
  identity transform).
- Transforms that SwiftUI cannot turn into a frame move (the translation combined with a 10⁻⁶
  scale, a 10⁻⁶ rad 3D rotation, or a 10⁻⁹ perspective term): the containing view keeps its frame
  and the move is applied as a transform, but UIKit still reduces the margin.
- `.ignoresSafeArea()` on the navigation stack itself: no change.
- `.safeAreaInset(edge: .leading)` on the navigation stack, to give back the lost inset: it never
  reaches the hosted navigation controller, whose safe area and `additionalSafeAreaInsets` stay
  unchanged. Even if it did, compensating through the safe area cannot restore the iOS 26 margin
  without moving the content that respects the safe area.

### UIKit options and the decision

Tried only in the harness, never in the app. On the hosted `UINavigationController`, turning off
`viewRespectsSystemMinimumLayoutMargins` and pinning `directionalLayoutMargins` to their closed
values kept the large title at 16 pt through the whole reveal. Adding the dropped inset back
through `additionalSafeAreaInsets` (62 pt, at the open endpoint in landscape) put the first row's
tile back at 78 pt and the bar's leading margin back at 78 pt. A fix along these lines needs a
representable to reach the navigation controller that SwiftUI creates, and while the screen moves,
an inset update on every frame (min(62, x)).

**Decision (Felipe, 2026-09-24): the UIKit options are declined, to keep design Decision 3 (SwiftUI
only, no UIKit bridges). The shift is accepted as a known iOS platform limitation: while the
drawer is open or moving, the pushed screen's large title sits at that screen's leading edge on
iOS 26 and later, and in landscape the pushed screen's rows, menu button, and title lose the
notch-side safe-area inset (about 62 pt on the iPhone 18 Pro) on every iOS version. Everything
returns when the drawer closes. No product code changed, and `DrawerPassUITests` gained no check
for it.** The notes in [2.2](#22--group-reveal-and-pushed-screen-treatment) and the motion
instrument note under [Instruments](#instruments-not-committed) are marked as corrected by this
section.

### Not checked

iPad; right-to-left, where the pushed screen moves toward the left and the same rule would act on
its trailing side (not observed); a physical device; iOS 16 and 17 runtimes.

### Settings and cleanup

The app was measured through a scratch XCUITest (removed from `TycheUITests`; not committed) and
`simctl io screenshot` captures taken during the held drags. The test rotated the simulator and
switched its appearance through `XCUIDevice`, and left the iPhone 18 Pro in portrait and light
appearance, as it was. Nothing was injected into the signed-in app. The harness was uninstalled
from the iPhone 17 and the iPhone 16 Pro. The simulators this pass booted (iPhone 18 Pro, iPhone
17, iPhone 16 Pro) were shut down; the iPhone Air was already running and was left as found. No
recording or log stream was left running. `Package.resolved` was checked after both `xcodebuild`
runs and never changed. On the iPhone 18 Pro the session is intact; nothing was shared or deleted,
and sign-out was never touched.
