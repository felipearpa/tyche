# Design

## Context

See proposal.md for motivation and specs/android-edge-to-edge/spec.md for the behavioral contract. Exploration found one app activity, MainActivity, already using `androidx.activity.enableEdgeToEdge()` and disabling navigation-bar contrast enforcement on API 29+. The project uses Compose and Material 3, compiles against SDK 36, and has no explicit target SDK in the app build file. Its manifest lacks `adjustResize`.

Screens commonly apply scaffold padding to a parent before rendering a list. The shared `RefreshableLazyPagingColumn` already forwards `contentPadding`, so the missing connection is primarily between screen scaffolds and feature list composables. PoolHomeView wraps every tab in a padded Box; its PrimaryTabRow applies navigation-bar padding. UsernameEditorScreen changes window soft-input flags while mounted, and UsernameEditor places `imePadding()` after `verticalScroll()` without consuming scaffold padding. Other text-entry routes generally have no explicit IME handling.

The existing profile specification requires a visible back control and a save action in the username editor's scrolling content. The navigation-drawer specification already covers safe bounds and system gesture priority. These contracts are preserved. The earlier pool-list accessibility pass's appearance constraint is scoped to that pass; row styling and semantics stay unchanged here while screen scrolling bounds change.

## Goals / Non-Goals

**Goals:** Establish traceable ownership of system and keyboard insets from each scaffold to its content, reuse existing Material 3 behavior, and verify actual interaction at window edges.

**Non-Goals:** Introducing NavigationSuiteScaffold, changing navigation architecture, redesigning icons or rows, altering business logic, or migrating iOS. Insets do not require a global wrapper that shrinks the navigation host.

## Decisions

### 1. Configure the activity once

Set `targetSdk = 36` explicitly in the app configuration, retain the existing minimum SDK, keep ComponentActivity's `enableEdgeToEdge()` before content creation, and add `android:windowSoftInputMode="adjustResize"` to MainActivity. Remove the route-local runtime soft-input adjustment and restoration code after replacing its layout handling. Do not use `SOFT_INPUT_ADJUST_RESIZE` at runtime.

Keep `isNavigationBarContrastEnforced = false` on API 29+ because pool home draws bottom navigation chrome. Retain ComponentActivity's automatic system icon appearance rather than adding duplicate WindowCompat theme side effects. Target 35 was considered; 36 matches the existing compile SDK and gives an explicit current project target. The implementation must verify merged manifest values and target-dependent behavior.

### 2. Keep inset ownership at the screen boundary

Use each existing Material 3 Scaffold and its inset-aware app bars. Forward scaffold padding to child content and consume it where applied. Static content can use `padding(innerPadding).consumeWindowInsets(innerPadding)`; list content must receive the values as `contentPadding` with consumption on the list modifier. Preserve design spacing separately so it is not accidentally counted as system inset consumption.

For screens without a scaffold, apply safe-drawing insets to the controls or content container while leaving the background full-window. Cover horizontal cutouts and caption bars as well as top and bottom bars. Avoid combining manual system-bar padding with Material components that already own the same inset. A single padded wrapper around the NavHost was rejected because it would constrain every background and list viewport.

### 3. Propagate content padding through list APIs

Add or forward `PaddingValues` through feature lists to the shared paging component and direct LazyColumn instances. Cover pool scores, gambler scores, pending and finished bets, bet timelines, match bets, Manage gamblers, and pool-layout selection. Remove outer system-inset padding that constrains those scrolling viewports. Preserve deliberate row spacing, fixed headers, paging, and refresh behavior; position refresh indicators clear of app chrome.

Change the pool-home content slot to pass padding to each tab rather than padding its enclosing Box. Tab lists consume that padding at their scrolling boundary. Loading, empty, and error branches use the same inset policy. Where model-backed placeholders are affected, use the production row populated with a placeholder model and the existing shared shimmer, with placeholder-only suppression of fetching, actions, and accessibility. Do not add skeleton-only layouts.

### 4. Handle IME bounds without duplicate padding

Retain default scaffold system-bar insets for form screens and prefer `fitInside(WindowInsetsRulers.Ime.current)` on the content container if supported by the resolved Compose version. Verify availability before introducing imports. The supported fallback is `consumeWindowInsets(innerPadding)` followed by `imePadding()` before `verticalScroll()`; do not combine either padding strategy with a parent already accounting for IME through `contentWindowInsets = WindowInsets.safeDrawing`.

Apply the policy to email sign-in, password sign-in, pool naming, both username-editor entry contexts, and the pending-bet list. Make short form bodies scrollable as needed. Keep the username top bar outside the scrolling body and preserve focus/selection behavior and in-flow Save placement.

For pending bets, reduce the list's usable area for the IME so focused score fields can be brought into view. Keep pool tabs at their existing window-edge position; they may be covered by the keyboard, but they must not obscure the active field or required bet actions. Do not stack full navigation-bar, tab-bar, and keyboard heights as unrelated bottom margins: use consumption/ruler fitting to account for their overlap. Verify this on-device rather than relying on modifier inspection alone.

### 5. Protect system bars at the surface that draws beneath them

Let inset-aware top app bars supply their existing coverage. Where scrolling content is exposed under the status bar, add a theme-aware translucent or gradient protection sized from actual status-bar insets; do not install an unconditional opaque top strip on every screen. Draw the pool bottom-bar surface through the navigation region and apply navigation insets inside that surface, around its controls. ComponentActivity remains responsible for system icon appearance.

Audit HomeView, ProfileView, avatar crop, email-link and pool-join states, and PushDrawer for duplicated insets or missing horizontal safe bounds. Preserve drawer system-gesture handling. Audit app-owned sheets and dialogs; any full-screen Dialog using `usePlatformDefaultWidth = false` and `fillMaxSize()` must also use `decorFitsSystemWindows = false` and manage content insets. Do not convert ordinary dialogs into full-screen ones. No FAB or adaptive scaffold was found in exploration; if one is found during implementation, use its own safe-area contract without adding parent-wide padding.

## Risks / Trade-offs

- Target SDK 36 can expose behavior beyond layout → inspect the merged manifest and smoke-test authentication, deep links, photo selection/crop, and back navigation alongside the inset checks.
- Nested containers can double-count system and IME space → document the owner at each screen boundary and test keyboard open/close transitions with bottom tabs present.
- Moving list padding can alter refresh indicators and fixed-header placement → preserve intentional screen structure and verify refresh, pagination, and scroll endpoints.
- Removing the username workaround can reintroduce whole-window panning → verify the existing profile scenarios on a short viewport with large text before marking the migration complete.
- Compose ruler API availability depends on resolved artifacts → prefer rulers when available, otherwise use the explicit consumed-padding fallback without requiring a dependency upgrade solely for this change.
- Screenshot-only checks can miss input and gesture regressions → pair visual checks with actual field focus, scrolling, button activation, drawer gestures, and system Back.

## Migration Plan

1. Establish the explicit target and activity keyboard configuration, then update affected inset ownership in the same implementation change.
2. Migrate list boundaries, form handling, and system-bar surfaces in feature groups without changing data contracts.
3. Run `./gradlew build` from Android and targeted existing instrumentation checks; add focused regression coverage where needed for keyboard visibility and list endpoints.
4. Verify at least API 35 and 36 plus an available supported pre-35 device/emulator. Cover both navigation modes and themes, portrait/landscape, a cutout, resized windows, and large font settings. Exercise loaded/loading/empty/error states and all text-entry flows. Record configurations, results, and any unavailable coverage.
5. Ship as an ordinary Android update after verification. No server or data migration is required. Before release, revert the activity configuration and dependent layout changes together if rollback is needed; after release, ship a corrected build while retaining the published target SDK.
