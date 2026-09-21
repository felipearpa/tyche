## Context

See proposal.md — Why. The relevant current state:

- **iOS** `PoolScoreItem` is a plain `HStack` with no accessibility modifiers. The open-pool tap lives one level up in `PoolScoreList`, on a `VStack { PoolScoreItem; Divider }` carrying `.padding(.horizontal, boxSpacing.medium)`, `.contentShape(Rectangle())`, and `.onTapGesture`. Shimmer is applied from outside by `PoolScorePlaceholderRow` via `.shimmer()`, so the item itself has no notion of being a placeholder.
- **Android** `PoolScoreItem` already takes a `placeholderModifier: Modifier`, so it knows when it is a placeholder, but applies no semantics. The open-pool click lives one level up in `PoolScoreList`, on a `Column` with `Modifier.clickable`. Because `clickable` merges descendants, the row is already a single TalkBack node — but its description is the raw traversal-order concatenation of the visible strings, and the rank tile and trend indicator contribute bare unlabeled numbers.
- `GamblerScoreItem` on both platforms is the reference: it builds its announcement from the model, exposes it through one element (`.accessibilityElement(children: .ignore)` on iOS, `clearAndSetSemantics` on Android), and suppresses that announcement entirely for placeholders.
- `PoolGamblerScoreModel` is identical on both platforms and already carries `poolName`, `position`, `beforePosition`, `score`, and `gamblerCount`. No data plumbing is needed.
- The leaderboard's rank/score/movement accessibility strings already exist in the `pool` module's string tables on both platforms, in all three locales.

## Goals / Non-Goals

**Goals:**
- One announcement per pool row, built from the model rather than scraped from the rendered visual strings.
- Both row actions — open the pool, invite to the pool — reachable and named from that one element.
- Announcement-building logic that is unit/instrumentation testable without a screen reader attached, the way `GamblerScoreItem.accessibilityLabel` and `gamblerScoreAccessibilityDescription` already are.
- iOS and Android `PoolScoreItem` converge on the same shape they already share on the leaderboard side.

**Non-Goals:**
- No change to the paging stack, the empty state, the error state, or the layout of any surface. The one visible change permitted is grammatical number at `n = 1` (D5b), which does reach the pool-home drawer; nothing else about those surfaces moves.
- No change to `PositionIndicator`, `TrendIndicator`, or any other shared UI component — they are wrapped, not modified.
- No new capability for the bet lists or the manage-gamblers list; the same treatment there is separate work.
- No accessibility audit of the empty-state hero, the template cards, or the "See all templates" action.

## Decisions

### D1: The announcement is composed in the row component, from the model

Each platform's `PoolScoreItem` gains a description builder that reads `PoolGamblerScoreModel` and joins localized fragments with `", "`, exactly as `GamblerScoreItem` does. Order: pool name, rank, points, member count, movement.

*Why:* it keeps the spoken text independent of layout and of the abbreviated visible copy (`"8 pts."`, `"101 members"`), and it makes the string assertable in a test that never renders a screen reader. *Alternative rejected:* letting the platform merge the visible children (`children: .combine` on iOS, plain `mergeDescendants` on Android). That is what Android does incidentally today and it is the defect — it speaks `"4, 1, …"` because the rank tile and trend indicator render bare numbers.

### D2: Pool name leads the announcement; the leaderboard leads with rank

`GamblerScoreItem` announces rank first, because on a leaderboard the user is scanning an ordered list and rank is the row's position in it. In "My pools" the rows are not a ranking — each row is a different pool, and the pool name is what distinguishes one row from the next.

*Why:* leading with rank here would make every row start with an interchangeable number, forcing the user to listen past it to find the pool. *Alternative rejected:* matching the leaderboard's order verbatim for symmetry; symmetry of order is not worth the scanning cost.

### D3: The open action is added alongside the existing gesture, not moved into the row

The visible tap target stays exactly where it is — the padded wrapper in `PoolScoreList`, which spans the row plus its divider and horizontal padding. `PoolScoreItem` additionally declares a button role and a default activation that calls the same `onOpen` closure.

*Why:* moving the gesture into `PoolScoreItem` would shrink the sighted hit area by the wrapper's horizontal padding and drop the divider strip, which the spec forbids. Declaring an explicit activation is also more reliable than hoping the platform synthesizes one: on iOS `.accessibilityElement(children: .ignore)` does not inherit an ancestor's `onTapGesture`. *Cost:* `onOpen` is referenced from two places. Accepted — the closure is the single source of truth and both paths call it.

### D4: iOS `PoolScoreItem` adopts the leaderboard's `placeholderModifier` parameter

iOS `PoolScoreItem` gains `placeholderModifier: (any ViewModifier)? = nil`, deriving `isPlaceholder` from it and applying `.accessibilityHidden(isPlaceholder)` — the same signature and derivation `GamblerScoreItem` uses, and the same shape Android's `PoolScoreItem` already has.

*Why:* it converges the two platforms' row components and the two iOS row components on one idiom, and it moves the shimmer from outside the row to inside it, where the placeholder flag lives. `PoolScorePlaceholderRow` in `PoolScoreList` passes `ShimmerModifier()` instead of calling `.shimmer()` on the result. Since the row paints no background, applying the shimmer to the content rather than the composed row is visually identical. *Alternative rejected:* a bare `isPlaceholder: Bool` flag — it would work, but leaves iOS's two row components with two different placeholder conventions. Android's `PoolScoreItem` is changed the same way, from `placeholderModifier: Modifier = Modifier` to `Modifier? = null`, so that it too can tell a placeholder from a loaded row — which its non-nullable default made impossible.

### D5: Reuse the leaderboard's rank/score/movement strings; add only what is new

`leaderboard_rank_accessibility`, `leaderboard_*_missing_accessibility`, and `leaderboard_movement_*_accessibility` are reused rather than duplicated. (They are reused *as keys*; D5a then reconciled their values across platforms and D5b gave three of them plural forms, so "verbatim" describes the intent at this point in the design, not what shipped.) The member count reuses the visible `gamblers_text` ("101 members" / "101 jugadores"), which is already spelled out rather than abbreviated, so it needs no accessibility twin. Only two new keys are added: `pool_score_open_accessibility_action` and `pool_score_invite_accessibility_action`.

*Why:* the phrases mean the same thing on both surfaces, and duplicating them would let the two lists drift apart in translation — which is also why the member count reuses `gamblers_text` rather than getting a key whose English value would be identical. *Trade-off:* the reused keys keep a `leaderboard_` prefix while being used outside the leaderboard. Renaming that prefix would touch shipped leaderboard code and three translated string tables for no user-audible gain, so it is accepted as historical. New keys are named for the pool list.

### D5a: The reused strings were reconciled across platforms

*Found during implementation:* the two platforms' shipped leaderboard strings did not agree, so reusing the keys would have inherited the divergence into this list. iOS said "Score unavailable", "down %lld places", "unchanged" against Android's "Points unavailable", "Down %1$d places", "Rank unchanged"; in Spanish iOS said "Posición"/"sube"/"baja" against Android's "Puesto"/"Subió"/"Bajó". This was initially scoped out and then reconciled at the user's direction.

The canonical table below was chosen per cell rather than by adopting one platform wholesale, and iOS moves on six of the seven keys while Android moves on one:

| key | en | es / es-ES | moved |
| --- | --- | --- | --- |
| `leaderboard_rank_accessibility` | `Rank %d` | `Puesto %d` | iOS |
| `leaderboard_rank_missing_accessibility` | `Rank unavailable` | `Puesto no disponible` | iOS |
| `leaderboard_points_accessibility` | `%d points` | `%d puntos` | neither |
| `leaderboard_score_missing_accessibility` | `Points unavailable` | `Puntos no disponibles` | both |
| `leaderboard_movement_up_accessibility` | `Up %d places` | `Subió %d puestos` | iOS |
| `leaderboard_movement_down_accessibility` | `Down %d places` | `Bajó %d puestos` | iOS |
| `leaderboard_movement_unchanged_accessibility` | `Rank unchanged` | `Puesto sin cambios` | iOS |

`%d` above stands for each platform's own specifier: iOS keeps `%lld`, Android keeps `%1$d`. Aligning the specifier itself would crash Android's formatter, so it is deliberately not aligned.

**This table is superseded by D5b for three of its rows.** `leaderboard_points_accessibility` and both movement rows are no longer flat strings — they carry `one`/`other` forms, and the values above are their `other` forms. A new locale must be translated from D5b's plural shape, not from this table, or it will ship flat strings that read "Up 1 places". The `moved` column likewise describes D5a only: under D5b, Android moves on `leaderboard_points_accessibility` and both movement keys as well.

Three choices are worth recording:

- **`Puesto`, not `Posición`.** One noun for the standing across rank, missing rank, movement and unchanged, so a single utterance never uses two words for one concept. It also avoids the football reading of "posición" (a player's position on the pitch) in a fragment heard with no visual context.
- **`Points unavailable` / `Puntos no disponibles`, which moved Android too.** The loaded state says "8 points"; the empty state must name the same column. In Spanish this also dissolves the `Puntaje` (Colombian) vs `Puntuación` (peninsular) split honestly instead of picking a region — "puntos" is native in both. This is the only row where aligning cost extra churn rather than saving it.
- **`Rank unchanged`, not bare `unchanged`.** The bare form has no antecedent and binds to whatever precedes it: "8 points, unchanged" on the leaderboard, "101 members, unchanged" in this list. Up and down carry their own noun ("places"); unchanged has to borrow one, so the asymmetry is deliberate.

*Also renamed:* iOS's `leaderboard_missing_rank_accessibility` / `leaderboard_missing_score_accessibility` become Android's modifier-last spelling `leaderboard_rank_missing_accessibility` / `leaderboard_score_missing_accessibility`, so one logical key has one name. iOS reaches these keys through plain string literals with no compile-time net, so the rename was verified by a repo-wide grep for the old names returning zero hits.

### D5b: Counted nouns were given plural forms, which retired the iOS string helper

*Found during implementation, fixed at the user's direction.* Both platforms announced "Up 1 places", "1 points" and "1 members", and `n = 1` is the **modal** movement on a board that updates every match. Because these are screen-reader strings, the only people who ever met the broken grammar were the users this work is for.

Four keys take plural forms — `leaderboard_points_accessibility`, `leaderboard_movement_up_accessibility`, `leaderboard_movement_down_accessibility`, and `gamblers_text`. `leaderboard_rank_accessibility` does not: "Rank 1" names a position, not a quantity. `points_text` ("%d pts.") does not either, because abbreviations conventionally do not inflect.

- **Android**: `<string>` becomes `<plurals>` with `one`/`other` items in all three values folders, and the call sites move to `pluralStringResource(id, count, count)`. `R.string.x` → `R.plurals.x` is a different namespace, so the compiler catches every missed site.
- **iOS**: the localization gains `variations.plural` per locale. This *forced* a design reversal. The `localizedFormat` helper introduced in D1 resolves the key with `String(localized:)` **first** and applies `String(format:)` after, so a plural-varied entry has no count at lookup time — it would return the unsubstituted `%#@…@` token and degrade silently under VoiceOver. The helper is therefore deleted and every call site moves to the generated string symbols (`String(localized: .leaderboardPointsAccessibility(score))`), which carry the count into the resource and let the catalog select the category.

*Plural categories per locale:* English defines `one` and `other`; Spanish also defines `many`, which CLDR reserves for millions. Every Spanish plural on both platforms carries all three. Because these values are formatted as plain integers (`%lld` / `%1$d`) rather than compact decimals, the `many` text is the same as `other` — "1000000 puntos" is correct Spanish, and the compact form that would need "de" ("un millón **de** puntos") is never produced. The category exists so each locale's rule set is complete: Android lint reports a `MissingQuantity` warning without it, and a future locale added by copying these entries inherits the full shape rather than a two-category one.

*Second benefit:* the symbols also retire every raw string-key literal in these two rows. That removes the one class of bug the D5a key rename had no net for — a mistyped or unrenamed key previously compiled cleanly and shipped the key name as spoken output. Android already had this safety through `R.string.*`; iOS now does too.

*Cost:* D1's "one shared helper" is gone. It was the right call while the strings were flat and the wrong one the moment they needed plurals, and it was still untracked, so retiring it cost four call sites and one deleted file rather than a migration.

*One visible consequence:* `gamblers_text` is visible copy, not an accessibility-only string. A one-member pool now reads "1 member" on screen where it read "1 members" before. This is the only pixel this change alters, and the delta spec's visible-design requirement was amended to permit exactly it.

*Contested:* the Spanish movement tense. The preterite ("Subió") describes a completed delta between two published standings and avoids the imperative reading of a sentence-initial "Sube 1 puesto"; the present tense is the more common live-standings idiom and is a defensible reversal — two values in two files, no test impact.

### D6: Platform mechanics

The two platforms split the work differently, because Android's wrapper already owns the click semantics and iOS's wrapper owns nothing.

| Concern | iOS (`PoolScoreItem`) | Android |
| --- | --- | --- |
| One element | `.accessibilityElement(children: .ignore)` + `.accessibilityLabel(...)` | `clearAndSetSemantics { contentDescription = ... }` on the row `Row` in `PoolScoreItem` |
| Button role | `.accessibilityAddTraits(.isButton)` | `role = Role.Button` on the wrapper's `clickable` in `PoolScoreList` |
| Open action | `.accessibilityAction { onOpen() }`, named through `.accessibilityHint` | `onClickLabel` on that same `clickable` |
| Invite action | `.accessibilityAction(named: Text(.poolScoreInviteAccessibilityAction)) { onJoin() }` | `customActions` in a `.semantics {}` on the wrapper in `PoolScoreList` |
| Placeholder | `.accessibilityHidden(isPlaceholder)` | the description is set only when not a placeholder, so `clearAndSetSemantics` leaves the filler with no semantics at all |

*Why the Android actions sit on the wrapper rather than in the row:* `Modifier.clickable` already merges descendants and sets its own role and `onClick`, and a parent's value wins over a child's for both. A role or click label declared inside `PoolScoreItem` would be shadowed by the wrapper's. Putting them on the `clickable` itself uses the supported API, keeps each semantic next to the callback it invokes (`onPoolOpen`, `onPoolJoin` both live in the list), and leaves `PoolScoreItem` exactly parallel to `GamblerScoreItem`, which also sets nothing but a description. The child's `contentDescription` still merges up into the wrapper's node — `PoolScoreListAccessibilityTest` asserts that the one merged node carries description, role, open label and invite action together.

*Consequence:* Android's `PoolScoreItem` takes no `onOpen`; iOS's does, because SwiftUI gives the wrapper's `onTapGesture` no way to reach an element declared with `children: .ignore`.

iOS names the open action through `.accessibilityHint`, since SwiftUI's default accessibility action cannot carry a label the way Android's `onClickLabel` can.

Both platforms keep their existing wrapper gesture for the sighted hit area (D3).

### D7: Test coverage mirrors the leaderboard's

- **iOS**: Swift Testing cases in `iOS/Pool/Tests/PoolTests/` asserting the string `PoolScoreItem` hands to `.accessibilityLabel` — for a complete row, a row with no rank and no score, a row with no previous rank, and a placeholder (empty label, `isPlaceholder` true). Following the precedent documented in `GamblerScoreItemRankPreviewTests`, these assert the composed property rather than the rendered modifier, because ViewInspector cannot read `accessibilityLabel` on current iOS.
- **Android**: instrumented Compose tests asserting `onNodeWithContentDescription` for the composed description, the presence of the invite custom action, and that a placeholder row exposes no pool description.

## Risks / Trade-offs

- **The composed description drifts from the visible row when the layout changes later** → the description is built from the model in the same file as the layout, and the tests assert the full composed string, so a field added to the row without a matching fragment shows up as a gap in review rather than silently.
- **Android's `clearAndSetSemantics` also clears the child `IconButton`, making the invite control unreachable by direct touch exploration** → this is why the invite is re-exposed as a custom action on the row (D6). The trade-off is deliberate: one focusable row with two named actions is a better TalkBack experience than a row plus a stray button, and it matches how `ManageGamblersList` already exposes its remove action on iOS.
- **iOS names the open action with `.accessibilityHint`, which is weaker than Android's `onClickLabel`** → SwiftUI's default accessibility action cannot carry a name, so "Open pool" rides on the hint. VoiceOver speaks hints by default but users can turn them off, and then iOS announces the row as an unnamed button while Android still says "double tap to open pool". Android's guarantee here is genuinely stronger; no iOS API closes the gap without turning the row into a real `Button`, which would change its visual press behavior.
- **iOS plural resolution depends on the generated symbols staying in use (D5b)** → if anyone reintroduces a `String(localized: "literal_key")` for a plural-varied entry, the lookup silently returns the raw `%#@…@` token instead of a category. There is no compile error and no failing test unless an assertion covers `n = 1`; the boundary cases added on both platforms are the guard.
- **iOS's shimmer moves inside the row component (D4)** → visually identical only because the row paints no background of its own; verify the placeholder list against the current build before and after, since a future background fill on the row would make the two differ.
- **New `.xcstrings` keys are skipped by symbol generation unless `extractionState` is `manual`** → the tasks call this out explicitly; a missed `extractionState` surfaces as a compile error on the generated symbol, not as a silent English fallback.
- **The three locales use different words for "pool"** (`pool` / `polla` / `quiniela`) → the new strings are written per locale rather than translated mechanically, following the existing entries in each table.
- **Spanish has no test coverage on either platform** → every assertion in every affected test file is an English literal, so the `es` and `es-ES` cells — where most of the D5a reconciliation lands — carry no test risk and no verification either. They need a native read or a per-locale device pass, not a green build.
- **Aligning the format specifier would crash Android** → `%lld` in an Android values file throws `UnknownFormatConversionException` while TalkBack builds the description, and `aapt2` does not reject it; this module has no lint configuration to catch it either. The specifier stays per-platform.

## Migration Plan

Not applicable — no persisted data, no API surface, and no stored user preference changes. The change ships with the app binary on both platforms and is inert for users who do not run a screen reader.
