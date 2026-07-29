## Why

Fortuna's pool leaderboard currently presents rank, username, score, and movement as a generic list without the identity treatment or visual hierarchy shown in the approved reference. Redesigning it now also completes the profile-avatar work by making gamblers recognizable in the competitive screen where identity matters most.

## What Changes

- Redesign only the pool leaderboard list and its rows on iOS and Android to match the supplied phone reference: rank tiles, gambler avatars, stronger score typography, row separators, and rank movement beneath each position.
- Preserve the existing screen title, subtitle/header content, top navigation bar, and bottom tab navigation without visual, structural, copy, or behavior changes.
- Identify the signed-in gambler with a full-row highlight, a localized "You" label, and a dedicated theme-aware `currentUser` color family applied consistently to rank, fallback avatar, username, and score without relying on color alone.
- Reuse the existing `AccountAvatar` photo-loading path for each gambler and extract the initial/fallback rendering already owned by `EmailAvatar` into a shared `InitialAvatar` primitive; leaderboard rows derive the fallback from username while existing email-avatar callers keep their current API and appearance.
- Reuse the existing shared position and trend/rank-movement indicators on both platforms; extend their size or style APIs only when the reference treatment requires it, and do not create leaderboard-specific duplicates.
- Preserve the existing ordered paging list, pull-to-refresh, retry, and gambler-detail navigation behavior.
- Make the leaderboard list inherit its base background from its container—exactly as the app's other paged lists (the Bets and History tabs) already do—by not painting its own canvas color; this yields the platform's native base background on each client (SwiftUI's system background on iOS, the Material container background on Android), so the leaderboard matches the surrounding screens. Keep surrounding screen and navigation surfaces unchanged.
- Give actionable leaderboard rows immediate full-row press feedback through each platform's native interaction convention—Material ripple on Android and semantic button pressed/highlight state on iOS—using a reusable treatment that can support later list redesigns; the signed-in gambler row remains non-actionable.
- Bring loading, missing-rank, missing-score, long-name, accessibility, and dynamic-type behavior into the same responsive visual system.

## Capabilities

### New Capabilities

- `pool-leaderboard`: The cross-platform pool leaderboard list's content, gambler identity treatment, current-user treatment, states, interactions, and accessibility.

### Modified Capabilities

<!-- None. Existing profile and avatar-upload requirements remain unchanged; this capability consumes their avatar storage contract. -->

## Impact

- **iOS:** `GamblerScoreListView`, `GamblerScoreList`, `GamblerScoreItem`, the existing `AccountAvatar` and `EmailAvatar`, the extracted shared `InitialAvatar`, the existing `PostionIndicator` and shared `TrendIndicator`, custom color assets, list-specific localized strings, previews, and tests. `GamblerScoreList` stops overriding its canvas and inherits the base background from its container like the Bets/History lists (SwiftUI's system background); `PoolHomeView` and its surrounding chrome are not changed.
- **Android:** the `pool/gamblerscore` composables, the existing `AccountAvatar` and `EmailAvatar`, the extracted shared `InitialAvatar`, the existing `PositionIndicator` and shared `TrendIndicator`, `ExtendedColorScheme`, list-specific string resources, previews, and tests. `GamblerScoreList` stops overriding its canvas and inherits the base background from its container like the Bets/History lists (the app's dark `#121212`); `PoolHomeView` and its surrounding chrome are not changed.
- **Data/API:** no endpoint or persistence change; the existing pool-gambler-score response already supplies positions, score, gambler id, and username. Avatar photos use the existing public `avatars/<accountId>.jpg` contract and cache revalidation behavior.
- **Dependencies:** no new third-party dependencies.
