## Context

Fortuna's Profile screen pushes a dedicated username route, but the reused `UsernameEditor` still has the structure of its former modal: title, explanatory sentence, one text field, and generic Save/Cancel buttons. It validates a trimmed non-empty value, clamps input to 100 graphemes, calls the existing account update use case, supports retry, and dismisses after success. Android models that save lifecycle with the shared ViewingState `SaveState<String>`, while iOS currently uses the load-oriented `LoadState<String>`.

Fortuna already has a distinctive representation of a gambler in pool standings: `GamblerScoreItem` in the Pool module on both platforms. It owns the rank rail, account avatar, username truncation, current-user marker, score, theme behavior, and accessibility description. Android exposes the composable to the app module. On iOS, the view and the memberwise initializer of `PoolGamblerScoreModel` are currently module-internal even though the model type and properties are public.

The subject is a football-pool identity editor for signed-in gamblers. Its single job is to let a gambler choose a recognizable public username with confidence about how it will appear in a pool.

## Goals / Non-Goals

**Goals:**

- Turn the username editor into a focused pushed screen that previews the draft in Fortuna's real pool-row presentation.
- Reuse `GamblerScoreItem` itself on both platforms so the preview cannot visually drift from the leaderboard.
- Make the 100-grapheme constraint and save eligibility visible.
- Give validation, saving, failure, retry, and success states clear behavior and accessible copy.
- Preserve the existing account update API, storage, and propagation behavior.

**Non-Goals:**

- Editing the avatar, email, rank, score, or any other profile field.
- Fetching a real pool, rank, or score for the preview.
- Changing username validation rules, uniqueness rules, the `PATCH /accounts` contract, or backend propagation.
- Redesigning `GamblerScoreItem` or the pool leaderboard.

## Decisions

### 1. The production gambler row is the screen's signature element

The screen places a live `GamblerScoreItem` beneath the explanatory copy and above the field. It is rendered as the current user and receives a presentation-only `PoolGamblerScoreModel`:

- `gamblerId`: signed-in account id, so `AccountAvatar` resolves the current avatar;
- `gamblerUsername`: trimmed live draft, or the localized "Your username" placeholder when the draft is empty;
- `position`: `1`;
- `beforePosition`: `2`, so the production rank calculation (`beforePosition - position`) renders an upward movement of one place;
- `score`: `18`;
- `poolId` / `poolName`: local preview constants;
- `gamblerCount`: `nil`;
- `isCurrentUser`: `true`.

The surrounding labels "Pool preview" and "Live" make the row illustrative. Rank, movement, and score are fixed examples, never loaded from a repository and never persisted. Only the account id and draft username are user data.

The complete illustrative rank rail is an invariant of the preview: the leading rank tile displays `1` and its movement indicator displays an upward change of `1` for every draft and save-lifecycle state, including an empty draft and a failed save. Both values are produced through the production component by setting `position = 1` and `beforePosition = 2`; no preview-specific rank UI is introduced. The rank rail remains visible at supported viewport widths and text sizes. When horizontal space is constrained, the username yields and truncates before the rail can be hidden or displaced. The row's combined accessibility description exposes the localized equivalents of "Rank 1" and "Up 1 place".

*Why*: the row answers the editor's promise—"This is how other players will see you in pools"—using the exact component that fulfills that promise in the product. It also tests long-name truncation honestly.

*Alternative considered*: build a lighter preview card containing only avatar and username. Rejected because it would duplicate production styling and could drift from the leaderboard.

*Alternative considered*: fetch the signed-in gambler's latest real score. Rejected because Profile has no selected-pool context, the extra network state does not help edit a username, and a failure would make an otherwise local editor less reliable.

### 2. Use a disciplined native screen around the row

Layout:

```text
┌──────────────────────────────────────┐
│ ‹ Profile           Username         │
├──────────────────────────────────────┤
│ This is how other players will see  │
│ you in pools.                        │
│                                      │
│ Pool preview                   Live  │
│ ┌──────────────────────────────────┐ │
│ │ [1]↑1 (avatar) Draft name You 18│ │
│ └──────────────────────────────────┘ │
│                                      │
│ Username                      10/100 │
│ [ Draft name                       ] │
│ Use the name your poolmates         │
│ recognize.                          │
│                                      │
│ [          Save username           ]│
└──────────────────────────────────────┘
```

The content remains a single scrollable column and uses existing spacing tokens. "Save username" appears immediately after the field's helper or validation message in that content flow. It is a bottom-of-content action, not a sticky footer, bottom app-bar action, safe-area overlay, or control fixed to the bottom edge of the viewport. It scrolls with the rest of the editor and may move below the fold when the keyboard, a small viewport, localization, or large accessibility text reduces the available space; the screen remains scrollable so the action can always be reached without covering content.

On each new presentation of the Username screen, both platforms populate the field before requesting focus and place a collapsed insertion caret immediately after the final character of the stored username. An empty username places the caret at position `0`. This initialization is one-shot for that screen presentation: recomposition, preview updates, validation, or `SaveState` transitions do not request focus again or move the caret. After initialization, the gambler owns the selection and may move the caret or select text without the editor resetting it.

There is no second Cancel button: this is a pushed screen, so the platform back action is the cancellation affordance.

Visual tokens are existing theme values, not new hard-coded component colors:

- Pitch green `#4CAF50`: primary focus and enabled action;
- Fortuna amber `#FFC107`: retained as the existing secondary brand token, not added decoratively;
- Light surface `#FFFFFF`;
- Light raised surface `#F5F5F5`;
- Dark surface `#121212`;
- Dark raised surface `#1E1E1E`.

Typography remains native for platform consistency and accessibility: the navigation/title role uses iOS title / Android `titleLarge`, the field and username use body roles, and preview labels/counter use caption / `bodySmall`. The production row continues to own its own typography.

The intentionally distinctive choice is component reuse, not ornament. An earlier generic-card direction was discarded because the same design could belong to any account form; the real standings row makes the editor specific to Fortuna.

### 3. Keep preview projection local and deterministic

Each platform adds a small pure mapper/factory that combines the account id and live draft with the fixed illustrative fields. It has no repository or view-model dependency and is unit-testable.

- Android calls the existing public `GamblerScoreItem` from the app module.
- iOS makes `GamblerScoreItem` and its initializer public and adds the minimum public model-construction surface needed by the app target. No second gambler-row view is introduced.

The app targets already depend on their Pool modules, so this creates no new module edge.

### 4. Model saving with the shared ViewingState `SaveState`

Both platforms use the state type that matches the operation's lifecycle instead of defining a feature-specific enum or treating a save as a load:

- Android retains `com.felipearpa.ui.state.SaveState<String>`;
- iOS migrates `UsernameEditorViewModel.saveState` from `ViewingState.LoadState<String>` to `ViewingState.SaveState<String>`.

The cases map directly across platforms:

| Lifecycle | Android | iOS |
|---|---|---|
| No attempt | `SaveState.Idle` | `.idle` |
| Request in flight | `SaveState.Saving(draft)` | `.saving(draft)` |
| Request succeeded | `SaveState.Saved(username)` | `.saved(username)` |
| Request failed | `SaveState.Failure(draft, error)` | `.failure(value: draft, error: error)` |

Views and view models SHALL use the package's predicates, value/error accessors, mapping, or folding helpers when they make the transition handling clearer. They SHALL NOT add a parallel username-specific async state.

*Why*: `SaveState` carries the attempted username through saving and failure, which is exactly what retry and draft preservation require. Using the same lifecycle abstraction keeps the iOS and Android implementations behaviorally aligned.

*Alternative considered*: keep iOS on `LoadState` because it already works. Rejected because `loaded`/`loading` describes retrieval rather than persistence and drops the attempted value from the failure case.

*Alternative considered*: use `MutationState`. Rejected because the screen keeps its local initial/draft values separately and does not need committed-versus-optimistic resource rendering; this operation is a focused save.

### 5. Save only the username

The primary button is labeled "Save username", not "Save changes", because the screen edits exactly one field. It is enabled only when the trimmed draft is non-empty, differs from the initial trimmed username, and no save is in progress.

On save:

- the trimmed value is submitted through the existing view model/use case;
- the field and navigation back action are disabled while the request is in flight;
- the button retains its label and shows progress, with the accessible state "Saving username";
- success returns to Profile and relies on the visible updated username as confirmation;
- failure preserves the draft, shows an inline cause-specific message, and changes the primary action to "Try again";
- dismissing the failure returns to the editable state without discarding the draft.

The existing retry operation continues to submit the same pending value.

### 6. Validation and copy stay close to the field

English source copy:

- Navigation title: "Username"
- Intro: "This is how other players will see you in pools."
- Preview label: "Pool preview"
- Dynamic preview marker: "Live"
- Field label: "Username"
- Helper: "Use the name your poolmates recognize."
- Empty error: "Enter a username."
- Primary action: "Save username"
- Retry action: "Try again"
- Network failure: "Username not saved. Check your connection and try again."
- Unknown failure: "Username not saved. Try again."

Spanish and Spanish (Spain) receive natural localized equivalents, preserving the repository's regional terminology for pools.

### 7. Accessibility follows the real component without excessive announcements

The production `GamblerScoreItem` keeps its combined accessibility description, including the localized illustrative rank "Rank 1", upward movement "Up 1 place", draft username, "You", and points. The preview heading makes its illustrative context discoverable. Draft changes update that description but SHALL NOT force a live announcement after every keystroke.

The text field exposes its label, helper or validation message, and grapheme count. Progress is announced as "Saving username". Dynamic Type/font scaling, TalkBack, VoiceOver, light mode, and dark mode use the existing platform/theme behavior.

## Risks / Trade-offs

- [Illustrative rank, movement, and score may be mistaken for current data] → Label the section "Pool preview" and "Live", never fetch data, and document the fixed values in tests.
- [Making the iOS row reusable widens the Pool module API] → Expose only `GamblerScoreItem` initialization and the minimum model construction needed; keep layout internals private.
- [The production row may change height or content later] → Host it in a scrollable column and deliberately inherit future row changes so the preview remains truthful.
- [Long localized copy or large accessibility text may push the action below the fold] → Keep normal document flow and scrolling; do not pin or overlay the button.
- [Preview accessibility could become noisy during typing] → Update semantics without using an assertive/live announcement.

## Migration Plan

No data or backend migration is required. Ship both native redesigns independently behind their existing Profile navigation. Rollback restores the previous editor layout without changing stored account data or API compatibility.

## Open Questions

None.
