## Why

The live-preview username editor is implemented on iOS and Android, but Android still lacks a visible back button for leaving without saving. The Profile specification now defines that behavior. Reopening this change lets the existing username-editor work track the remaining Android implementation and verification.

The original redesign replaced the generic one-field form with a preview of how the username appears in Fortuna while preserving the account-update behavior. Those completed tasks remain checked off.

## What Changes

The original redesign includes:

- Redesign the pushed Username screen on iOS and Android around a live "Pool preview", a focused username field, a visible 100-grapheme counter, and a specific "Save username" action.
- Render the preview with each platform's existing production `GamblerScoreItem`, populated with the signed-in account identity, the live username draft, and fixed illustrative score-row values. The preview does not fetch or persist pool data.
- Make validation, unchanged, saving, failure, retry, and success states explicit and accessible while preserving the existing `PATCH /accounts` flow and username propagation.
- Model the username-save lifecycle with the existing shared ViewingState `SaveState<String>` on both platforms; migrate the iOS editor from `LoadState<String>` instead of introducing a custom state.
- Add or update localized interface copy in English, Spanish, and Spanish (Spain).

The remaining scope adds a visible, accessible back button to the Android username editor's top app bar. It returns to Profile and discards unsaved edits without submitting an update or retry, stays visible but disabled while saving, and becomes available again after failure. It remains visible with the keyboard open and while the content scrolls. The app-rendered icon follows the repository's Material Symbols and committed canonical-vector rules; iOS retains its existing system-rendered navigation control.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `profile`: Retain the implemented live-preview editor and explicitly provide visible Android back navigation with cancellation and save-lifecycle behavior.

## Impact

- **iOS**: `UsernameEditor`, `UsernameEditorViewModel`, `UsernameEditorScreen`, Pool module visibility for `GamblerScoreItem` and its model construction, ViewingState migration, localized strings, and UI/view-model tests.
- **Android**: `UsernameEditor`, `UsernameEditorViewModel`, Profile navigation host, Pool `GamblerScoreItem` integration, existing shared ViewingState use, localized strings, and Compose/view-model tests.
- **Backend**: no API, persistence, or propagation changes.
- **Dependencies**: no new dependencies; the app targets already depend on their Pool modules and shared ViewingState packages.

The reopened implementation work is limited to the Android username route, its top app bar and back icon, shared save-state observation, and navigation regression checks. The existing account and avatar integrations remain in use.
