## Context

The pool leaderboard is the first tab inside `PoolHomeView`. Both clients page `PoolGamblerScoreModel` records through `GamblerScoreListView`; each record already contains the pool name, gambler count, current and previous position, score, gambler id, and username. The current row is a generic horizontal list item with a small position indicator and trailing trend. It has no avatar or explicit current-user state.

The approved implementation reference is preserved with this change:

- `assets/leaderboard-phone.png` — one ordered column.

The app already stores avatars at the public deterministic URL `avatars/<accountId>.jpg`. Uploaded objects and the existing image loaders use cache revalidation, so leaderboard avatars require no API or database change.

## Goals / Non-Goals

**Goals:**

- Reproduce the references' list-row hierarchy and rhythm on iOS and Android while using native semantic colors and typography.
- Keep rank order and score comparison immediately scannable.
- Make each gambler identifiable through an avatar and make the signed-in gambler unmistakable through text as well as color.
- Preserve paging, pull-to-refresh, retry, and navigation to another gambler's bet timeline.
- Define testable behavior for missing data, long content, dynamic type, screen readers, and image failures.

**Non-Goals:**

- Changing scoring, ranking, tie-breaking, paging, or pool-membership rules.
- Adding or changing backend endpoints, DynamoDB items, or the S3 avatar key.
- Changing the existing Scores/Puntajes title, subtitle/header content, top navigation bar, drawer/profile access, pool-switching action, or bottom tab navigation.
- Redesigning the Bets and History tabs.
- Adding search, filters, podium cards, prizes, or animated rank transitions.

## Decisions

### 1. Apply the references only to the leaderboard list

The reference assets define the visual treatment from the first leaderboard list row through its separators and paging states. They do not authorize changes to content or controls outside `GamblerScoreListView`/`GamblerScoreList` and `GamblerScoreItem`.

The existing Scores/Puntajes title, subtitle/header content, top navigation bar, drawer/profile access, pool-switching action, and bottom tab navigation remain visually, structurally, textually, and behaviorally unchanged. Any title, subtitle, header, or navigation element visible in a reference crop is ignored for implementation purposes. Existing header or column-label content outside the list boundary is neither added, removed, moved, nor restyled by this change.

- *Why*: the requested change is specifically a list redesign. Preserving the surrounding screen chrome avoids an unrelated information-architecture and navigation change.
- *Alternative considered*: adopt the reference crop's title, back arrow, or header simplification. Rejected because those elements are outside the confirmed list-only scope.

### 2. Retain the existing refreshable paging lists

The redesign changes the content and styling of each leaderboard row without replacing the list container or its paging orchestration:

```text
┌─────────────────────────────────────┐
│ [ 1 ]  (A) AstroDelBalon        200 │
│   —                                 │
├─────────────────────────────────────┤
│ [ 2 ]  (T) TiburonDelArea       160 │
├─────────────────────────────────────┤
│▓[ 3 ]▓ (E) ElGoleador · You ▓▓ 150▓│
└─────────────────────────────────────┘
```

- iOS continues to use the existing `RefreshableLazyPagingVStack`.
- Android continues to use the existing `RefreshableLazyPagingColumn`.
- Items remain in server/pager order.
- Existing initial loading, empty, refresh, append loading, append error, and retry behavior remains owned by those paging containers.

*Alternative considered*: replace the paging container as part of the row redesign. Rejected because the current containers already provide the required paging and refresh behavior, and replacing them would expand the change without improving the approved row design.

### 3. Add a dedicated non-Material `currentUser` color family

The neutral competition table continues to use platform theme tokens:

- **Canvas**: the base background inherited from the list's container (not painted by the list itself) — SwiftUI's system background on iOS (`#FFFFFF` light / `#000000` dark) and the Material container background on Android (`#FFFFFF` / `#121212`).
- **Primary ink**: on-surface (`#212121` / `#E0E0E0`).
- **Muted ink and separators**: on-surface-variant and outline semantics.
- **Rank and avatar tiles**: surface-variant (`#F5F5F5` / `#1E1E1E`).
- **Steady/down accents**: amber (`#FBC02D` / `#FFEB3B`) and red (`#D32F2F` / `#F44336`).

The leaderboard list does not paint its own canvas. Like the app's other paged lists — the Bets tab (`MatchBetList`/`PendingBetList`) and the History tab (`FinishedBetList`) — it inherits the base background from its container across rows, inter-row spacing, loading placeholders, empty and error states, and any remaining scrollable area. On iOS this resolves to the platform system background (pure black in dark, white in light); on Android it resolves to the Material container background the surrounding `Surface`/`Scaffold` already supplies (the app's dark `#121212`). Inheriting rather than overriding is what makes the leaderboard match the surrounding screens on each platform; the earlier explicit `surface` paint was the sole reason the list read as a lighter panel over iOS's black sibling tabs. The neutral (non-signed-in) rows and loading placeholders were also filling `surface` per row, so they are made transparent to show the inherited canvas; only the signed-in gambler's row keeps an opaque fill (its `currentUserContainer` highlight), and the rank tile keeps its `surfaceVariant` fill.

Because the canvas is inherited, the list background naturally stops at the existing list boundary. `PoolHomeView`, its title or subtitle/header content, top navigation, and bottom navigation keep their existing surfaces, and the list neither ignores safe areas nor extends behind those surrounding elements.

Current-user identity is not a Material role and is not the same semantic state as positive rank movement. It gets a project-level custom quartet following the existing non-Material color conventions:

| Token | Light | Dark | Use |
|---|---|---|---|
| `currentUser` | `#176B3A` | `#7EDB9A` | Fallback avatar fill and current-user accent on the base surface |
| `onCurrentUser` | `#FFFFFF` | `#12351F` | Content on the filled fallback avatar |
| `currentUserContainer` | `#D4F3DE` | `#143C24` | Full current-user row background |
| `onCurrentUserContainer` | `#176B3A` | `#B3F0C3` | Rank, username, "You", and score on the highlighted row |

Android adds these four fields to `ExtendedColorScheme` and supplies them through `LocalExtendedColorScheme`. iOS adds matching light/dark color sets (`CurrentUserColor`, `OnCurrentUserColor`, `CurrentUserContainer`, `OnCurrentUserContainer`) to the shared UI asset catalog. This keeps the state semantic and consistent across platforms without overloading Material `primary`, rank-gain green, or the amber brand accent.

The rank tile inside the highlighted row is a stronger nested surface produced by compositing `currentUser` at 14% over `currentUserContainer`; this yields approximately `#B9E0C7` in light mode, as shown in the prototype, without adding a fifth token.

All text/surface pairs exceed WCAG AA for normal text: 6.56:1 for the light avatar pair, 5.52:1 for light row content, 8.03:1 for the dark avatar pair, and 9.50:1 for dark row content.

System type remains the app-wide body family. Within the list, hierarchy comes from native styles: body-semibold for username, title2/headline-small tabular figures for scores, and caption for rank movement and "You".

The signature element is the **current-user band**: one quiet container joins rank, avatar, identity, and score into a single unmistakable self marker. Within every row, the rank rail still ties the rounded-square position tile to its movement directly underneath.

- *Alternative considered*: keep the list painting its own `surface` canvas (`#121212`). Rejected because it is the only canvas override in the app and renders the list as a lighter grey panel over iOS's pure-black sibling tabs, which is the exact inconsistency this revision removes.
- *Alternative considered*: force the list to a fixed pure black on both platforms (a literal black or a `leaderboardCanvas` token). Rejected because Android's Bets/History tabs are `#121212`, not black, so a forced black would make the leaderboard darker than its own neighbors on Android and add a list-specific color role the other lists do not use. Matching each platform's inherited base background keeps the leaderboard consistent with its siblings without touching the app-wide theme.

### 4. Give every row a stable four-part anatomy

Each item reserves space for:

1. Rank rail: 44×44 rounded tile plus movement beneath it.
2. Identity avatar: 40×40 circle.
3. Identity text: one-line username; a second localized "You" line only for the signed-in gambler.
4. Trailing score: bold tabular figures, trailing-aligned.

Rows keep a minimum 82pt/dp content height and a full-column separator. Long usernames ellipsize before the score and remain available in the accessibility label. Missing rank or score renders an em dash in the reserved position rather than collapsing the layout.

The signed-in gambler's entire row uses `currentUserContainer`; rank, username, "You", and score use `onCurrentUserContainer`. Movement retains its gain/drop/steady semantic color and symbol. The fallback avatar uses the `currentUser`/`onCurrentUser` pair. An uploaded photo remains untinted so the user's chosen identity image is not altered.

Movement is `beforePosition - position`: positive is up/green, negative is down/red, zero is a steady amber dash, and absent input yields no movement text. The number is the absolute number of places moved.

*Alternative considered*: keep movement beside the score. Rejected because movement describes rank, not points, and the reference deliberately nests it under the rank tile.

### 5. Refactor the existing avatar stack to support username-backed fallbacks

`AccountAvatar` remains the single component responsible for loading, revalidating, cropping, and displaying photos from `avatars/<accountId>.jpg`. Its fallback input is generalized so a caller can supply identity text and a stable color key instead of requiring an email. Existing email-based initializers/call sites remain source-compatible and keep their current rendering.

The initial, deterministic color, contrast, and missing-initial icon rendering currently implemented inside `EmailAvatar` is extracted into a shared `InitialAvatar` primitive. `EmailAvatar` becomes a thin compatibility wrapper that derives its initial and stable key from email and delegates to `InitialAvatar`; its public behavior and existing callers do not change.

Leaderboard rows compose the existing `AccountAvatar` with `gamblerId` for photo loading and a username-backed `InitialAvatar` for fallback. The fallback uses the uppercase first user-perceived letter or number and the same deterministic palette/contrast behavior as `EmailAvatar`; a username without a usable initial displays the existing person icon. The signed-in gambler overrides the generated fallback palette with `currentUser`/`onCurrentUser`. The leaderboard response therefore does not need to expose email.

- *Why*: separating the generic initial rendering from the email adapter lets both existing account surfaces and leaderboard rows share one photo-loading path and one fallback implementation without misusing an email-named API or creating a leaderboard-specific avatar component.
- *Alternative considered*: extend the score API with avatar URLs or account email. Rejected because the deterministic avatar URL is already derivable from `gamblerId`, and exposing email would be unnecessary personal data.

### 6. Keep the redesigned list independent of surrounding header content

The redesigned list owns only its ranked rows, row separators, loading placeholders, and paging states. It does not own or modify the existing screen title, subtitle, pool metadata, column labels, or navigation chrome that the containing screen renders outside the list.

The list layout begins at the same boundary supplied by its current container and fits below the unchanged surrounding header without requiring header removal, relocation, or restyling.

*Alternative considered*: remove or simplify surrounding subtitle, metadata, or column labels to create more room for the new rows. Rejected because those elements are explicitly outside this change.

### 7. Preserve interaction semantics and add reusable native press feedback

Rows for other gamblers remain tappable and open that gambler's bet timeline. The signed-in gambler row is not announced as a button and performs no navigation. Pull-to-refresh, append loading, and inline retry continue to use the existing paging components.

Each actionable row is one full-row semantic activation target and gives immediate feedback from press-down until release or cancellation:

- Android retains `Modifier.clickable` and its theme-aware Material ripple/indication.
- iOS replaces the gesture-only activation with a semantic `Button` and a shared reusable row `ButtonStyle` driven by `configuration.isPressed`, producing the platform-appropriate pressed/highlight state across the full row.

The interaction contract is shared—prompt feedback, full-row target, activation on a completed press, and no activation after cancellation—but its visual expression remains native to each platform. iOS does not imitate an Android ripple, and routine row navigation adds no haptic feedback. The shared iOS row style belongs in the common UI layer so later list redesigns can adopt the same behavior without copying leaderboard code; Android continues to reuse the platform's existing clickable primitive rather than wrapping it in a leaderboard-specific component.

Dragging away or otherwise cancelling a press clears its visual state and performs no navigation. Because the signed-in gambler row has no action, it receives neither pressed feedback nor button/click semantics.

Each row becomes one logical accessibility element in this order: rank, username, "You" when applicable, score with localized points, and movement. Trend color and arrows are supplementary; the spoken label includes "up", "down", or "unchanged".

### 8. Reuse and extend the existing position and trend indicators

The current leaderboard already composes the shared indicator implementations: iOS uses `PostionIndicator` from the Pool package and `TrendIndicator` from the UI package, while Android uses `PositionIndicator` from the pool module and `TrendIndicator` from the ui module. `GamblerScoreItem` SHALL continue to compose those existing components for the position tile and movement display.

If the new 44pt/dp rank rail, nested current-user surface, theme colors, spacing, or accessibility treatment needs additional configuration, the implementation extends the existing indicators with focused size/style/semantic inputs and backward-compatible defaults. Existing consumers such as `PoolScoreItem` keep their current appearance and behavior unless they explicitly select the new configuration.

Position formatting, tile shape, and position content remain owned by `PostionIndicator`/`PositionIndicator`. Movement classification (`> 0`, `< 0`, or `0`), absolute-value formatting, symbols, semantic gain/drop/steady colors, and their focused tests remain owned by `TrendIndicator`. The leaderboard row supplies data and layout context but does not recreate either rendering path or copy the trend component's internal up/down/stable implementations.

- *Why*: one implementation per platform prevents visual and semantic drift between leaderboard and pool-score surfaces while allowing the shared components to support the approved reference.
- *Alternative considered*: add leaderboard-only rank-tile and movement components tuned to the prototype. Rejected because they would duplicate existing behavior and create a second source of truth for rank and trend semantics.

## Risks / Trade-offs

- [Avatar requests multiply on a leaderboard] → Reuse the existing URL-keyed memory/disk cache and conditional revalidation; do not bypass cache or request presigned GETs.
- [Very long localized labels or accessibility text can collide] → Reserve the score width, allow the row to grow vertically for accessibility sizes, ellipsize only the visible username when necessary, and expose its full value to accessibility.
- [A full-row state can become decorative or low-contrast] → Use the dedicated semantic quartet, retain the literal "You" label, and test every foreground/container pair in both themes.
- [Extending shared indicators can regress other score surfaces] → Add only backward-compatible configuration with defaults matching the current rendering, and cover both default and leaderboard styles in the shared component previews/tests.
- [Custom row content can suppress or obscure native press feedback] → Apply the interaction to the full semantic row, keep Android's Material indication, centralize the iOS pressed style in shared UI, and verify feedback over both neutral and current-user-adjacent surfaces.
- [Reference crops can invite changes outside the list] → Treat the current screen chrome as a visual regression baseline and reject implementation diffs to its title, subtitle/header content, or navigation controls.
- [Overriding the list canvas makes it diverge from the sibling screens] → Do not paint a canvas color on `GamblerScoreList`; let it inherit the base background from its container like the Bets/History lists, and verify across all paging states that the leaderboard matches those tabs on each platform in light and dark.
- [Reference pixels differ from native rendering] → Match hierarchy, spacing, and token roles rather than hard-coding screenshot pixels; verify phone previews in light and dark themes.

## Migration Plan

1. Add the shared `currentUser` extended-color quartet on both platforms.
2. Extract `InitialAvatar` from `EmailAvatar`, generalize the existing `AccountAvatar` fallback input with compatibility defaults, and add tests proving existing email-avatar callers remain unchanged.
3. Extend the existing position and trend indicators with backward-compatible configuration only where the reference treatment requires it.
4. Add the shared iOS interactive-row button style and apply the native platform interaction treatment to actionable leaderboard rows on both clients.
5. Replace the compact leaderboard row and list on both clients by composing those shared indicators.
6. Remove the explicit canvas background from each `GamblerScoreList` so it inherits the base background from its container like the Bets/History lists.
7. Add platform previews/screenshot coverage for the redesigned rows in the existing paging lists.
8. Ship clients independently; no backend deployment or data migration is required.

Rollback restores the previous `GamblerScoreList` and `GamblerScoreItem` presentation. Avatar objects are unaffected, and compatibility wrappers preserve existing avatar call sites.

## Open Questions

None. The reference asset's list-only boundary, existing screen chrome, data contract, and paging behavior are resolved in this design.
