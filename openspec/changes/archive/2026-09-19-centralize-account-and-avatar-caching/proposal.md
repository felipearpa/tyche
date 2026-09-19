## Why

Fortuna currently persists the signed-in account after authentication but has no server refresh path, so account metadata such as the username can remain stale after it changes on another device. Avatar surfaces also lack one consistent cross-platform byte-reuse contract: Android benefits from Coil's shared caches, while iOS reloads and decodes the same avatar independently in each view.

## What Changes

- Introduce a typed, observable, offline-first current-account repository that composes local persistence with the authenticated account API, serves a persisted snapshot immediately, and remains the single client-side source of truth for account metadata.
- Add an authenticated current-account read API so clients can reconcile locally persisted account data without signing in again.
- Route sign-in installation, account refresh, successful account mutations, and logout clearing through the repository, and prevent an older remote result from overwriting newer local state.
- Introduce a shared platform avatar image loader that reuses decoded images across profile, navigation, and leaderboard surfaces; coalesces duplicate requests; and relies on the existing HTTP/disk cache when memory entries are absent or stale.
- Account for avatar memory by decoded byte cost, decode images for their requested display size, configure a bounded memory target, and allow eviction under system memory pressure.
- Seed the shared avatar cache with the optimized local image after a successful upload so every visible surface updates without downloading the just-uploaded image.

## Capabilities

### New Capabilities

- `current-account`: Repository-owned persisted and observable signed-in account state, authenticated refresh, mutation ordering, offline fallback, and logout clearing.
- `avatar-loading`: Shared, size-aware avatar image loading with a configured memory budget, request coalescing, cache revalidation, and post-upload replacement.

### Modified Capabilities

None. The existing `profile`, `avatar-upload`, and `pool-leaderboard` requirements retain their visible behavior and will consume the new capabilities without changing their contracts.

## Impact

- Account backend and HTTP API: add an authenticated current-account read route using the existing Account domain and response model.
- iOS Session and Account packages: add an offline-first current-account repository over the existing local adapter and a shared avatar image loader over `NSCache` and URL loading.
- Android Session and Account modules: add the same repository boundary over the existing observable persistence and use the singleton Coil image loader without a second bitmap cache.
- Profile, drawer, toolbar, username editor, and leaderboard consumers on both platforms: observe repository-owned account state and request avatars through the platform image loader.
- Tests: account bootstrap/refresh/mutation ordering, offline behavior, avatar request coalescing, size-aware decoding, configured memory-budget behavior, upload replacement, and cross-surface propagation.
