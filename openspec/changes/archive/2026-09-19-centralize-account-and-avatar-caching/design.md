## Context

The signed-in account is represented on both platforms by `AccountBundle` (`accountId`, `externalAccountId`, `email`, and `username`). Android's `AccountStorageInKeyStore` also exposes a process-wide `StateFlow`, while iOS persists the same bundle in UserDefaults and requires each view model to retrieve and mirror it manually. Sign-in and a successful username mutation write the local bundle, but no authenticated read endpoint refreshes it afterward. A username changed on another device can therefore remain stale until the next sign-in.

Avatar objects use the deterministic public S3 key `avatars/<accountId>.jpg`. The objects require HTTP revalidation and clients request revalidation so a fixed URL does not remain stale indefinitely. Android already uses one Coil image loader with memory and disk keys, but on-device verification showed its request directive `Cache-Control: no-cache` makes Coil send a plain GET, so every memory miss re-downloads the unchanged JPEG (see Decision 4). iOS uses `URLSession.shared` but each `AccountAvatar` owns and decodes its own `UIImage`. After a local upload, Profile temporarily retains the rendered 512×512 image and bumps a process-local avatar version; other surfaces may still perform another URL load.

Uploaded avatars are 512×512 JPEGs. A full decoded RGBA avatar is approximately 1 MiB, while a 40 pt/dp row avatar needs only about 128–160 pixels on common device densities. Keeping every gambler at the upload resolution would make an unbounded shared cache expensive.

## Goals / Non-Goals

**Goals:**

- Make one observable current-account value the source of truth for authenticated UI on iOS and Android.
- Render the persisted account snapshot immediately and reconcile it with the server without blocking app startup.
- Prevent stale refresh responses from overwriting newer username mutations.
- Reuse avatar images and in-flight loads across Profile, navigation chrome, username preview, and leaderboard rows.
- Decode avatars near their rendered pixel size and keep decoded avatar memory bounded by cost.
- Preserve the existing S3 key, conditional revalidation, letter fallback, and upload failure behavior.
- Publish a successful local upload to every visible avatar surface without re-downloading it.

**Non-Goals:**

- Storing JPEG data or decoded images in `AccountBundle`, UserDefaults, KeyStore, or DynamoDB.
- Creating a generic cache for authentication tokens, pool data, or every remote resource.
- Adding server push or real-time multi-device account synchronization.
- Adding an avatar database field, avatar revision, upload-completion endpoint, CDN, or versioned S3 object key.
- Redesigning avatar UI, icons, model-backed loading placeholders, or fallback presentation.
- Changing the existing 512×512 upload format or 1 MiB upload limit.

## Decisions

### 1. Use one change with two separate runtime stores

The client SHALL have a durable `CurrentAccountStore` and a disposable `AvatarImageStore`. They are delivered together because account identity keys avatar requests and both changes remove cross-screen duplication, but they SHALL not share persistence or eviction rules.

`CurrentAccountStore` owns the observable current value and delegates durable encoding to the existing platform account-storage adapter. `AvatarImageStore` owns decoded image reuse and delegates compressed response caching to Coil/OkHttp on Android and URLCache on iOS.

Alternatives considered:

- Store image bytes inside the account bundle: rejected because every username write would copy/encode unrelated binary data, eviction would become impossible without mutating account state, and the bundle would be unsuitable for UserDefaults/KeyStore.
- Implement one generic application cache: rejected because account metadata, decoded images, authentication, and pool data have different authority, security, expiry, and failure semantics.

### 2. Add `GET /accounts/me` without changing the Account table

The Account HTTP API and Lambda entry point SHALL add an authenticated `GET /accounts/me`. The handler derives the account from the authenticated principal using the existing caller-resolution and Account repository paths; it does not accept a caller-provided account id. It returns the existing canonical account response containing account id, external account id, email, and username.

No DynamoDB attributes, indexes, keys, or migrations are required. Existing POST and PATCH contracts remain unchanged.

Alternative considered: call the existing account-link POST whenever the app starts. Rejected because linking is a mutation-oriented authentication operation and obscures the distinction between session establishment and ordinary refresh.

### 3. Put refresh and mutations behind an application coordinator

An application-scoped current-account coordinator SHALL serialize account refresh, username mutation, sign-in installation, and logout clearing. The coordinator is application orchestration, not a repository: remote repositories remain data-access-only and the storage adapter remains local persistence-only.

Android SHALL expose the current account as a `StateFlow<AccountBundle?>`; iOS SHALL expose an equivalent replaying observable stream suitable for SwiftUI view models. Consumers subscribe to this source instead of retaining manual copies or relying on success callbacks to propagate a username.

The persisted form becomes a cache envelope containing the account bundle and an optional last-successful-validation timestamp. Both platform decoders SHALL accept the legacy raw bundle and wrap it as stale, so existing signed-in users are migrated in place without being logged out.

Refresh follows stale-while-revalidate:

1. Hydrate and publish the persisted snapshot immediately.
2. Refresh once after authenticated cold start.
3. Refresh when Profile opens or the app returns to the foreground if the last successful validation is at least five minutes old.
4. Coalesce concurrent refresh requests.
5. On success, atomically persist and publish the canonical response and validation time.
6. On failure, retain the snapshot and leave it stale so a later trigger can retry.

All coordinator operations are serialized with a Kotlin mutex and a Swift actor (or platform-equivalent primitives). A refresh result applies only while its operation is current, so a request begun before a successful username mutation cannot overwrite the submitted username afterward. A successful PATCH updates and publishes the stored bundle with the submitted, already-trimmed username; a failed PATCH leaves the account snapshot unchanged.

Alternative considered: add a server account revision solely for client ordering. Rejected for this change because client serialization solves same-process races and avoids a persistence migration; cross-device edits remain server-authoritative and are reconciled by the next refresh.

### 4. Make avatar loading shared, size-aware, and request-coalesced

Every `AccountAvatar` SHALL use the platform `AvatarImageStore`; individual views SHALL not own network loading. A cache key consists of account id, process-local avatar generation, and requested target pixel side. The component derives the target from its rendered bounds and display density, rounds upward to a stable 32-pixel bucket, and caps it at the 512-pixel source size. A cached larger variant may satisfy a smaller request.

Android SHALL build on the existing singleton Coil loader and its memory/disk caches rather than introduce a second bitmap cache. Requests SHALL provide a concrete target size and revalidate conditionally: the request directive is `Cache-Control: max-age=0`, never `no-cache`, because Coil's `CacheControlCacheStrategy` answers `no-cache` with a plain GET before it reaches its `If-None-Match` branch. Fetches of one URL at different target sizes SHALL run one after another (`SerialConcurrentRequestStrategy`, a per-key mutex); raced, all but one cannot open the disk entry's editor, receive an empty 304 body, and Coil falls back to a second, full download. Coil's own `DeDupeConcurrentRequestStrategy` serializes only the first caller and releases the rest together, which is enough for two sizes at cold start but not for the several surfaces a replaced photo re-keys at once. iOS SHALL add a shared actor-backed loader using `NSCache` for decoded images, a map of in-flight tasks for coalescing, Image I/O thumbnail decoding, and `URLSession`/URLCache for compressed responses.

The decoded avatar cache has a maximum accounting budget of 16 MiB per process and MAY evict earlier under platform memory pressure. Entry cost is calculated from decoded pixel bytes, not JPEG length. Compressed response data is released after decoding and remains the responsibility of the HTTP/disk cache. On Android the budget is the singleton Coil memory cache's total size, which the bundled match-flag images (`FlagImage`) share, so decoded avatars can fall short of the budget but never exceed it. Missing or failed avatars retain the existing letter/person fallback and do not become user content.

Alternatives considered:

- Cache every avatar at 512×512: rejected because each decoded image is about 1 MiB and a 16 MiB cache would hold only sixteen gamblers.
- Cache only compressed JPEG bytes in memory: rejected because every surface would still pay repeated decode cost and briefly allocate the full decoded image.
- Add a separate skeleton avatar while loading: rejected because the existing letter/person fallback already preserves layout and this change does not introduce a model-backed loading placeholder.

### 5. Serve memory immediately and revalidate stale entries once

An in-memory avatar entry records its last successful validation. A fresh entry is returned without URL access. An entry at least five minutes old is returned immediately while one background conditional request is coalesced for that cache key. An unchanged response refreshes the validation time without replacing decoded pixels; a changed response replaces the image and notifies active consumers. Offline or failed revalidation retains the stale image.

A missing-avatar result is shared across concurrent consumers and cached for the same five-minute interval to avoid repeated 404 requests. A successful local upload bypasses that negative entry.

This policy preserves cross-device freshness while avoiding the current iOS behavior of asking URLSession independently from each view. The S3 `ETag` and existing `Cache-Control: max-age=0, must-revalidate` contract prevent retransferring unchanged JPEG bytes only while the client actually sends a conditional request; Android did not until Decision 4's directive change.

Both platforms decide "unchanged or replaced" by comparing the `ETag` of an origin answer with the `ETag` of the photo the current generation was decoded from. iOS reads it from its own `URLSession` responses. On Android Coil owns the fetch and does not hand response headers to the app, so the singleton loader's `NetworkClient` is decorated: it reports every avatar answer, with its `ETag`, to `AvatarImageStore`. The same `ETag` only refreshes the validation time; a different one advances the account's generation, and every consumer reloads from the refreshed disk entry while it keeps showing what it already shows. This covers a change met by a surface's own load (a new size inside the freshness window), not only a revalidation, and the first answer of a process, when no decoded variant exists to supersede, never advances. Because memory hits never reach the origin, a surface served from memory whose validation is older than the interval sends one coalesced load past the memory cache at the smallest size in use; its decode is discarded, which is the one cost accepted over a separate probe.

Comparing identities rather than status codes is what makes the advance idempotent. Coil 3.4.0 leaks its disk snapshot when a response it will not cache (its own synthetic 504 while it believes the device is offline, a 403, a 5xx) reaches the fetcher for an entry that exists; that entry then refuses every write for the rest of the process, so each 304 becomes a full download and a replaced photo is answered with `200` again on every reload. A rule of "conditional `200` means replaced" would re-key the surfaces forever. The decorator also fails such responses before Coil sees them, and Coil's connectivity check is bypassed, so the leak is not triggered in the first place.

Rejected: a Ktor `HEAD` probe comparing against an `ETag` the store could only learn from that probe (the first implementation). The first probe of every process had to be treated as a change, which evicted and re-decoded an unchanged photo and flashed the fallback; it also needed a second HTTP client. Also rejected: dropping the `ETag` altogether and inferring change from `200` versus `304` (the second attempt), for the reason above.

### 6. Seed the shared memory cache after upload success

The existing crop flow already holds the optimized 512×512 image and its JPEG data. After the S3 PUT succeeds, the upload application flow SHALL advance the signed-in account's process-local avatar generation and insert the decoded local image into `AvatarImageStore` before reporting success. The store SHALL publish the replacement so Profile, drawers, toolbars, username preview, and any visible leaderboard row update from memory without a GET.

The seeded entry records the upload's `ETag` (S3 reports the quoted MD5 of a simple PUT), so both platforms recognize it later. On Android, Coil's disk entry still holds the replaced photo after an upload, so the next load that misses memory, or the first stale revalidation, transfers the upload once; its `ETag` matches the seeded one, so no surface is re-keyed. It is the transfer a cold start would otherwise have made.

If the PUT fails, the previous generation and cached image remain active. The selected image remains only in the existing retry state until retry or dismissal. Disk-cache injection is not required; after process death the existing conditional HTTP path repopulates from S3.

## Risks / Trade-offs

- [Five-minute freshness window] Account or avatar changes made on another device may remain visible for up to the staleness interval in a continuously active process. → Refresh on authenticated cold start, eligible foreground/Profile triggers, and stale avatar access; keep the interval in one configuration point.
- [Memory pressure] Decoded images can compete with screen and crop buffers. → Downsample to rendered size, account by decoded cost, cap at 16 MiB, and allow immediate platform eviction.
- [Duplicate platform behavior] Coil and the custom iOS loader could drift. → Share cache-key, freshness, upload-replacement, and acceptance-test semantics in the capability spec while using native implementations.
- [Migration regression] Changing account persistence could sign out existing users if legacy JSON is not recognized. → Add explicit legacy-bundle migration tests on both platforms before consumers move to the coordinator.
- [Operation race] A late refresh could reintroduce an old username. → Serialize coordinator operations and test refresh-before-save, save-before-refresh, and failure ordering.
- [Request-directive regressions] A cache directive that looks stricter can silently disable conditional revalidation, and only byte counts reveal it. → Pin the app's request headers against Coil's real cache strategy in unit tests, and verify on a device that an unchanged avatar costs a 304, not a download.
- [Offline after a memory miss] Android's offline retention is memory-only: `must-revalidate` forbids serving the disk copy unvalidated, so after process death, a background trim, or a new size, an offline device shows the letter fallback although the JPEG is on disk. → Accepted; the spec's offline scenario is scoped to a decoded image that is still in memory.
- [Stale negative cache] A photo uploaded on another device may show the fallback until the short absence entry becomes stale. → Use the same bounded interval and bypass it after local upload.

## Migration Plan

1. Add and deploy `GET /accounts/me` with authorization and response tests; no database migration is needed.
2. Add the current-account coordinator and legacy local-cache migration behind existing account-storage interfaces.
3. Move account consumers to the observable store, retaining existing sign-in/logout routes during the transition.
4. Introduce `AvatarImageStore` behind the existing `AccountAvatar` APIs so call sites keep their UI contract.
5. Seed the avatar store after upload, then remove view-local propagation workarounds that are no longer needed.
6. Verify memory, request coalescing, conditional revalidation, offline fallback, and multi-surface updates on both platforms.

Rollback is safe in reverse order. The backend route is additive, the Account table and S3 key are unchanged, and legacy clients continue using their existing local bundle and avatar-loading behavior.

## Open Questions

None. The five-minute staleness interval and 16 MiB decoded-avatar ceiling are initial cross-platform policy values and can be tuned later through their centralized configuration points without changing the capability contracts.
