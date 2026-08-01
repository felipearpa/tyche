## Why

The username editor is a generic one-field form that tells gamblers their username is public but does not show how it will appear in Fortuna. A focused redesign can make the consequence of the edit visible before saving while keeping the proven account-update behavior unchanged.

## What Changes

- Redesign the pushed Username screen on iOS and Android around a live "Pool preview", a focused username field, a visible 100-grapheme counter, and a specific "Save username" action.
- Render the preview with each platform's existing production `GamblerScoreItem`, populated with the signed-in account identity, the live username draft, and fixed illustrative score-row values. The preview does not fetch or persist pool data.
- Make validation, unchanged, saving, failure, retry, and success states explicit and accessible while preserving the existing `PATCH /accounts` flow and username propagation.
- Model the username-save lifecycle with the existing shared ViewingState `SaveState<String>` on both platforms; migrate the iOS editor from `LoadState<String>` instead of introducing a custom state.
- Add or update localized interface copy in English, Spanish, and Spanish (Spain).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `profile`: Replace the existing username editor presentation with the live-preview editor while preserving its navigation and account-update contract.

## Impact

- **iOS**: `UsernameEditor`, `UsernameEditorViewModel`, `UsernameEditorScreen`, Pool module visibility for `GamblerScoreItem` and its model construction, ViewingState migration, localized strings, and UI/view-model tests.
- **Android**: `UsernameEditor`, `UsernameEditorViewModel`, Profile navigation host, Pool `GamblerScoreItem` integration, existing shared ViewingState use, localized strings, and Compose/view-model tests.
- **Backend**: no API, persistence, or propagation changes.
- **Dependencies**: no new dependencies; the app targets already depend on their Pool modules and shared ViewingState packages.
