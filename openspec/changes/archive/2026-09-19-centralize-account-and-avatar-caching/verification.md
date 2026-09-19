# Verification record

Implementation session 2026-08-01. Automated portions of tasks 1.4, 2.13, 3.13, and 4.5.

Post-review fix round (same session): a 10-angle code review surfaced 15 findings (all fixed) —
notably the iOS URLCache fast path now rejects cached bytes whose ETag differs from the
validated one; Android's null-ETag handshake was replaced by treat-first-sight-as-changed plus
a generation fence on probe results; install/clear persistence is fail-loud and
cancellation-proof on both platforms; upload seeding moved from the Profile view models into
the upload use case behind an `InstallUploadedAvatar` port (design decision 6); cached
absences now expire on-screen. Suites after fixes: iOS Session 23, Account 16, Tyche unit
tests green; Android session 23, account 32, pool 2, app 54, `assembleProdDebug` builds.

## Backend

```bash
cd Amazon.Lambda
dotnet test Felipearpa.Tyche.slnx --nologo --verbosity minimal
```

All suites pass, 0 failures: Type 54, Core 2, Crypto, Data.DynamoDb 4, Account 19, Pool 51, PoolLayout 7, Function 1, AmazonLambda 71 (includes 4 new `GetCurrentAccountTest` cases). `Felipearpa.Tyche.HttpApi` has no test project; it was built explicitly (`dotnet build Felipearpa.Tyche.HttpApi/src/...` — 0 warnings, 0 errors). Confirmed the endpoint reuses `GetByEmailAsync` on the existing `GetByEmail-index`; no new Account-table attributes, indexes, keys, or migration.

## iOS

```bash
cd iOS/<Pkg> && xcodebuild test -scheme <Pkg> \
  -destination "platform=iOS Simulator,name=iPhone 17,OS=latest" \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO
# Pkg ∈ Session, Account, Pool, UI, Core, Bet, DataPool, DataBet

cd iOS && xcodebuild test -workspace Tyche.xcworkspace -scheme Tyche \
  -destination "platform=iOS Simulator,name=iPhone 17,OS=latest" \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO   # unit + UI tests
```

- Session: 23/23 pass (new `CurrentAccountCoordinator` 13, `CurrentAccountSnapshot` 5, rewritten `UploadAvatarUseCase` 4, plus existing).
- Account: 14/14 pass (new `AvatarImageStore` suite: memory hits, coalescing, size buckets, 16 MiB eviction, memory-pressure recovery via URLCache, cached absence, unchanged/changed/offline revalidation, upload seeding, absence bypass, seeded-ETag handshake).
- Tyche app: TycheTests pass (ProfileViewModel suite rewritten for the shared store); TycheUITests pass — `AvatarSanityPassUITests` self-skips without `AVATAR_SANITY_PASS=1` (uploads to the live bucket; run deliberately).
- UI 9, Core 13, Bet, DataPool, DataBet: pass.
- Pool: 2 pre-existing failures also present at clean HEAD (`EmptyView does not have 'accessibilityLabel' modifier` from ViewInspector on the current Xcode SDK) — verified via `git stash` baseline; tracked as a separate task, unrelated to this change.

## Android

```bash
cd Android
./gradlew :session:testDebugUnitTest :account:testDebugUnitTest :pool:testDebugUnitTest :app:testProdDebugUnitTest
./gradlew :app:assembleProdDebug
```

All pass: session 22 (coordinator 12 + snapshot 4 + existing), account 26 (store 11 + dedup registry 3 + existing), pool 2, app 54. `assembleProdDebug` builds.

## OpenSpec

```bash
openspec validate centralize-account-and-avatar-caching --strict   # → valid
```

## Re-verification 2026-09-18

Same commands as above, re-run against the current working tree (iOS destination
`id=4D209B45-123A-426F-8ABC-B882E7211E6B`, iPhone 17 / iOS 27.0). Counts are unchanged from the
post-review fix round.

- Backend (`dotnet test Felipearpa.Tyche.slnx`): 0 failures — Type 54, Core 2, Crypto 1, Data.DynamoDb 4, Account 19, Pool 51, PoolLayout 7, Function 1, AmazonLambda 71.
- iOS Session: 23 Swift Testing + 2 XCTest pass. iOS Account: 16 Swift Testing + 4 XCTest pass.
- iOS Tyche app (workspace, unit + UI): 53 total, 51 pass, 0 fail, 2 skipped (`AvatarSanityPassUITests`, opt-in via `AVATAR_SANITY_PASS=1`).
- Android: session 23, account 32, pool 2, app 54 — 0 failures; `:app:assembleProdDebug` builds.
- `openspec validate centralize-account-and-avatar-caching --strict` → valid.

Pre-deploy baseline of the live `TycheApi` stack (us-east-2, last updated 2026-07-26):
`GetCurrentAccount` is the only template function absent from the stack. Deployed account routes
are `POST /accounts`, `PATCH /accounts`, `POST /accounts/{accountId}/avatar-upload-url`.
Unauthenticated `GET /accounts/me` → 404 (control `GET /version` → 200). Expected after deploy: 401.

Not run this session:

- Android instrumented suites (`:session:connectedDebugAndroidTest`, `:pool:connectedDebugAndroidTest`): the attached SM-G955F was locked (keyguard showing); Compose UI tests need an unlocked screen.
- iOS request-logging/Instruments pass: the iPhone 17 simulator has no Fortuna install or signed-in session.

## Deploy smoke test and instrumented suites 2026-09-18 (afternoon)

### 4.5 — `GET /accounts/me` is live

The `TycheApi` stack reached `UPDATE_COMPLETE` at 2026-09-18T18:41:58Z. The update created
`GetCurrentAccountRole`, `GetCurrentAccount` (`TycheApi-GetCurrentAccount-4crkW8TemtyO`, dotnet10,
`Active`) and `GetCurrentAccountHttpApiPermission`; no other route changed.

Through the API (`https://wfykmbiop4.execute-api.us-east-2.amazonaws.com`):

```bash
curl -s -o /dev/null -w "%{http_code}\n" https://wfykmbiop4.execute-api.us-east-2.amazonaws.com/accounts/me
```

| Request | Before deploy | After deploy |
|---|---|---|
| `GET /version` (control) | 200 | 200 `{"version":"2.0.0"}` |
| `GET /accounts/me`, no token | 404 | 401 |
| `GET /accounts/me?accountId=<other ulid>`, no token | — | 401 |
| `GET /accounts/me`, `Authorization: Bearer not-a-real-token` | — | 401 |
| `POST /accounts`, `PATCH /accounts`, no token (existing routes still routed) | — | 401 |

The JWT authorizer rejects those before the handler runs (the function had no log group yet), so
the handler and its IAM policy were exercised directly with `aws lambda invoke` and a synthetic
HTTP API v2 event carrying `requestContext.authorizer.jwt.claims.email` — read-only:

| Synthetic authorizer context | Handler response |
|---|---|
| Email of an existing account, plus `queryStringParameters.accountId=<a different ulid>` | 200; body keys exactly `accountId`, `externalAccountId`, `email`, `username`; the returned `accountId` is the caller's, not the query parameter's |
| Email with no Account record | 401 |
| No `email` claim | 401 |

This confirms the `dynamodb:Query` grant on `Account` + `GetByEmail-index` is sufficient and that
request input cannot select another account. `openspec validate
centralize-account-and-avatar-caching --strict` → valid.

### 3.13 — Android instrumented suites

```bash
cd Android
ANDROID_SERIAL=<serial> ./gradlew :pool:connectedDebugAndroidTest :session:connectedDebugAndroidTest --continue
```

The first run (SM-G955F, Android 9, now unlocked) exposed a regression from this change: 6 of 10
`:pool` Compose tests crashed with `IllegalStateException: KoinApplication has not been started`.
`AccountAvatar` now resolves the shared `AvatarImageStore` with `koinInject` (it used Coil directly
before), and these tests render production leaderboard rows without starting Koin. Production is
unaffected — `TycheApplication` starts Koin with `avatarImageStoreModule`.

Fix (test harness only): `pool/src/androidTest/.../AvatarImageStoreKoinRule.kt` starts Koin with the
**production** `avatarImageStoreModule` and an `HttpClient` on a Ktor `MockEngine` (the module needs
one for the `HEAD` probe, which never runs in these tests; Coil's image requests are not intercepted), declared at `order = 0` so Koin outlives the composition; the three
test classes use it, and `:pool` gained `androidTestImplementation` of `koin-android` and
`ktor-client-mock`.

Results after the fix:

- `:pool`, Pixel_10_Pro_XL AVD (Android 16, `en-US`): **10/10 pass**.
- `:pool`, SM-G955F (Android 9, `es-CO`): 5 pass, 5 fail. All five are assertions on English
  content descriptions (`"Rank 1"`, `"Up 1 places"`, `"Rank unavailable, …"`) evaluated against the
  `values-es` strings; no Koin failure remains. Locale-dependent assertions from the leaderboard
  redesign, not from this change.
- `:session`, both devices: the module's only instrumented test,
  `ExampleInstrumentedTest.useAppContext`, fails — a stale Android Studio template asserting the
  package `com.felipearpa.data.user.test` (actual `com.felipearpa.tyche.session.test`), untouched
  since the folder-rename commit `8f70b37a`. `:data:bet` and `:data:pool` carry the same stale stub.
  Unrelated to this change.
- `:account` and `:app` have no `androidTest` source set.

## On-device verification, Android, 2026-09-18 (signed-in phone)

Samsung SM-G955F, Android 9, locale `es-CO`, debuggable `com.felipearpa.fortuna` 2.0.0 (12), signed in as the owner's real
account. The installed build was confirmed to be this change (dex contains `CurrentAccountCoordinator`, `AvatarImageStore`,
`accounts/me`; the deleted `AvatarVersion` is absent; no Android main source had changed since 2026-08-02). iOS was **not**
exercised: no simulator has a signed-in install and a physical iPhone cannot be driven from here.

### How things were observed (no code changes, no profiler GUI)

- **Ktor logcat** (`Ktor Client`, `LogLevel.ALL`): every API call plus the store's avatar `HEAD` probe. Captured through a
  `sed` pipe that redacts `Authorization`, JWTs and presigned-URL signatures before anything reaches disk.
- **Coil's disk cache via `run-as`** (`cache/coil3_disk_cache/<sha256(url)>.0`): Coil's avatar GETs are *not* logged, but this
  file stores the last exchange's **status line**, `date:` and `etag:`; the `.1` body file's mtime shows when a body was last
  persisted. Journal `READ` lines are not flushed promptly, so only `DIRTY`/`CLEAN` were used.
- **Per-UID received bytes** (`/proc/net/xt_qtaguid/stats`, uid of the app) around one isolated UI action, after a 3 s idle
  check of 0 bytes. The avatar JPEG is 53,391 bytes; a TLS handshake alone is ~5.5 KB.
- `uiautomator dump` + screenshots for what is on screen; the foreground activity is asserted before every tap (after one
  early operator error: BACK on the pool screen exits the app, and two taps landed on empty launcher space — no effect).

### Defects found on the device and fixed (task 3.14)

**1. An unchanged avatar was re-downloaded on every memory miss.** Opening Profile two minutes after a successful validation
received **60,969 bytes** and Coil stored a fresh `200`. Cause: the request sent `Cache-Control: no-cache` (pre-existing since
profile pictures shipped), and Coil 3.4.0's `CacheControlCacheStrategy.compute()` returns the plain request for `no-cache`
*before* its `If-None-Match` branch. Fix: send `max-age=0`. Same action afterwards: **442 bytes** on a warm connection,
**5,961 bytes** on a fresh one, entry status line **`304`**, body file untouched.

**2. Concurrent loads of one URL at different sizes still downloaded it.** With fix 1 only, a cold start received **82,085
bytes** although its three API payloads total under 1 KB: toolbar and drawer load concurrently, the loser cannot open the disk
entry's editor, gets an empty 304 body, and `NetworkFetcher` falls back to an unconditional, unpersisted GET. Fix:
`DeDupeConcurrentRequestStrategy` (keyed by disk-cache key). Cold start afterwards: **20,700 and 20,385 bytes** (two runs).

Also corrected: validation is recorded only for `DataSource.NETWORK` results (was `!= MEMORY_CACHE`, which would count an
unvalidated disk hit); `TycheApplication`'s comment claimed avatars are the only Coil-loaded images, but `FlagImage` shares
the loader and its 16 MiB cache (the bound on avatars still holds).

Tests: two strategy-level tests in `AccountAvatarTest` run the app's request headers through Coil's real
`CacheControlCacheStrategy`; one fails with `no-cache`, the other fails with no request directive at all. They do not prove the
header reaches `NetworkFetcher` unchanged — that is what the byte measurements above show. Suites after the fixes: account 34,
app 54, session 23, pool 2, 0 failures. Each fixed build was installed **in place** (`adb install -r`, same key and
versionCode) and the gambler stayed signed in; that is a reinstall, not an upgrade test, and says nothing about 4.1.

An independent three-lens review (fix correctness, evidence vs. spec, spec/parity) was run before this was written; it found
defect 2, the `NETWORK` condition, an invalid test comment, and the first-sight gap below, and its narrower wording is used here.

### Current account (4.2, Android)

| Scenario | Observation | Result |
|---|---|---|
| Launching with a persisted account | Cold start: `GET /gamblers/<persisted id>/pools` left 49 ms after the `/accounts/me` request and ~2 s before its 200; the signed-in home rendered. Only this consumer was timestamped. | holds |
| Cold start refreshes once | Exactly one `/accounts/me` → 200 on each of six cold starts; the encrypted `account.xml` is rewritten each time (mtime; content not readable). | holds |
| Still fresh → no request | Foreground 8 s after a validation: zero requests. Profile opened 2m14s after one: zero `/accounts/me`. | holds |
| Stale foreground trigger | Same process, 5m29s after validation: exactly one `/accounts/me` → 200. | holds |
| Stale Profile trigger | Profile opened 5m20s after validation: exactly one `/accounts/me` → 200. | holds |
| Username changed by another client | Backend username changed with one conditional `UpdateItem` on the Account item only (pool rows untouched, so invisible to other gamblers; 63 s window, reverted with the inverse conditional update). Cold start: response carried the new name; drawer and Profile showed it; `account.xml` grew by exactly the 4 added characters. | holds |
| Refresh fails while a snapshot exists | Airplane mode on (toggled by the owner): stale foreground → one `/accounts/me` → `UnknownHostException`; no crash, still signed in, cached name still shown, `account.xml` untouched. | holds |
| A later trigger retries | Airplane mode off, next foreground in the same process: one `/accounts/me` → 200 with the real name; drawer updated; `account.xml` back to its original size. | holds |
| Username save succeeds/fails, refresh-vs-save ordering, logout races | Not exercised on device (the owner declined an in-app save; races are not reproducible by hand). | unit tests only |

### Avatars (4.3, Android)

| Scenario | Observation | Result |
|---|---|---|
| Same-size surface reuses the decoded image | Leaderboard own row after the editor's preview row: no cache write, entry `date:` unchanged. Profile re-opened: **0 bytes**. | holds (exact 32-px bucket only; a larger cached variant is not reused for a smaller request, which the spec allows) |
| A different size while the entry is fresh | Does perform a URL load; since the fix it is a 304, not a download. | holds after 3.14 |
| Missing avatars | The other 12 gamblers rendered letter fallbacks; their HTTP status was not captured. | fallback only |
| Stale, unchanged remotely | Known ETag, 5m46s stale: exactly one `HEAD` → 200 same ETag, no Coil exchange, 6,060 bytes total, photo stayed on screen. Number of simultaneous consumers not recorded, so cross-surface coalescing is not shown. | holds |
| First stale probe per process | One `HEAD`, then every composed size reloads as a 304 (18,217 bytes in total on the fixed build, body file untouched). Evicting and re-decoding an unchanged photo deviates from the spec and from iOS — design.md "Known gap", task 3.15. | **gap, open** |
| Stale, changed on another device | The S3 object was overwritten with a mirrored copy of the same photo (backup verified against the live ETag first; public for 32 s; original bytes restored, identical ETag). Next stale composition: one `HEAD` → new ETag; toolbar and drawer both switched to the mirrored photo (toolbar crop vs. pixel-mirror of the original: mean difference 0.2, vs. 23.3 unmirrored); disk entry replaced. | holds |
| Changed photo, cost | Replacing two visible consumers while navigating back received 188,522 bytes for a 52,855-byte object (~3 bodies). Detecting the restore five minutes later from Profile received 72,935 bytes (~1 body plus a `HEAD`, a 304 and an `/accounts/me`), and Profile ended pixel-identical to the original. Correct result both times; the first transfer is unexplained. | **open question** |
| Revalidation fails offline | Airplane mode on, stale avatar recomposed: one `HEAD` → `UnknownHostException`; the photo stayed on toolbar and drawer. Memory-only by design: after a memory miss an offline device shows the letter fallback although the JPEG is on disk (`must-revalidate`). | holds, as scoped |
| Memory pressure | App backgrounded, `am send-trim-memory … COMPLETE`, same process: the next Profile open revalidated (5,961 bytes vs 0 in the control) and rebuilt from the cached body. Real pressure was not induced, and Coil's own callback clears at the same level, so this is not attributable to `TycheApplication.onTrimMemory`. | holds |
| 16 MiB bound, LRU eviction | **Not verified on device.** One gambler in the account's pools has a photo; ~256 are needed at this density. `dumpsys meminfo` cannot isolate Coil's cache. The bound is Coil's memory cache sized by `AVATAR_MEMORY_BUDGET_BYTES`; this row used to say "unit tests only (3.12)", but no test of ours covered it until `AvatarMemoryBudgetTest` (2026-09-19, below). | not verified |

### 4.1 and 4.4

- **4.1 upgrade from the previous release:** not verified. The legacy-bundle shape could only have been shown before the first
  successful refresh, and the blob's size was not captured before the first launch of the session; that refresh rewrote it as
  the envelope. Unit tests cover the decoder at the JSON level only; nothing exercises `AccountStorageInKeyStore` with a
  legacy value. Reproducing it needs a device still signed in on 1.7.2 or a pre-deploy build.
- **4.4 local upload:** not exercised — the owner declined an in-app upload (it requires browsing the phone's gallery).

### `GET /accounts/me` end to end

Every refresh above is a real authenticated call through API Gateway's JWT authorizer → 200, and each success rewrote the local
store, so the body parsed. Field-level checks and "request input cannot select another account" remain verified by the
direct-invoke smoke test only.

## Addendum 2026-09-18 (evening) — task 3.15, Android avatar store reworked

Everything above this line that mentions a `HEAD` probe, an app-learned `ETag` with a first-sight rule, `markValidated`,
`DeDupeConcurrentRequestStrategy`, or a `MockEngine` in the pool Koin rule describes builds A–C and is **no longer the code**.
The "First stale probe per process — gap, open" and "Changed photo, cost — open question" rows are closed by this addendum.

### What changed and why

The owner asked whether the store's own `ETag` was needed at all. The `HEAD` probe existed only because Coil does not hand
response headers to the app, so the store could not know the `ETag` of the image it had decoded. Decorating the singleton
loader's `NetworkClient` removes that limitation: every avatar answer, with its `ETag`, is reported to `AvatarImageStore` by the
exchange that produced the image — the same model iOS uses. Deleted: `AvatarKtorRemoteProbe`, the store's Ktor `HttpClient`
(and `ktor-client-core` from `:account`, `ktor-client-mock` from `:pool`), the first-sight rule, `markValidated` in the
composable. Kept: the quoted-MD5 `ETag` seeded on upload, now for a principled reason (it is the uploaded body's identity).

A first attempt dropped the `ETag` entirely and inferred change from `200` vs `304`. An independent three-lens review found a
**blocker** before it was exercised: Coil 3.4.0's `NetworkFetcher.writeToDiskCache` returns without closing the disk snapshot
when the cache strategy refuses a response (`val modifiedNetworkResponse = writeResult.response ?: return null`), and the caller
has already overwritten its only reference. Any non-cacheable answer for an existing entry (Coil's synthetic 504 while it
believes the device is offline, a 403, a 5xx) therefore locks that entry for the process: each 304 becomes a full download,
and a replaced photo answers `200` on every reload — an endless re-key-and-download loop under a status-code rule. The final
design compares identities, which is idempotent, and removes the trigger: the decorator fails such answers before Coil sees
them, and Coil's connectivity check is bypassed so offline fails inside OkHttp.

The same review explained the earlier "188 KB for a replaced photo": Coil's de-duplicating strategy serializes only the first
caller and releases the rest together, so three surfaces re-keyed at once still raced for the disk editor.
`SerialConcurrentRequestStrategy` (a per-key mutex) replaces it. Also fixed from that review: the requested-bucket table now
drops the least recently rendered account (it used to drop the oldest inserted — normally the signed-in gambler, the only one
who uploads); revalidation is asked only by a surface served from memory, so a load that reaches the origin no longer doubles
it; a re-keyed surface keeps what it shows (`useExistingImageAsPlaceholder`, plus the previous generation's variant for
surfaces composed meanwhile) inside `key(accountId)`.

Suites (before the second review's fixes): account 48 (`AccountAvatarTest` 7, `AvatarImageStoreTest` 22, `AvatarOriginNetworkClientTest` 5, `InFlightRequestRegistryTest` 3, `SerialConcurrentRequestStrategyTest` 3, `PasswordWithInvalidValuesTest` 5, `PasswordWithValidValuesTest` 3), app 54,
session 23, pool 2 — 0 failures; `:pool` instrumented sources compile; `assembleProdDebug` builds.

### Device results (build E, installed in place; same methods as above)

| Check | Observation |
|---|---|
| Cold start | Signed in after the in-place update; 20,324 bytes; two 304s; no crash. |
| `HEAD` probe | None in the whole session (0 Ktor lines to the avatar host). |
| First stale check of a process | Profile re-opened from memory 5m14s after the last answer: **exactly one** exchange, a 304; no re-key (a bump would add one exchange per composed size). Build C did a `HEAD` plus a reload of every size here. |
| Fallback flash, unchanged | Screen recording, frames matched against the real photo (a letter avatar scores 62, the mirrored photo 38, threshold 12): once the avatar appears it is the photo in every frame; the contact sheet shows it cross-fading in as the photo. |
| Photo replaced remotely | S3 object overwritten with the mirrored copy for 39 s, then the identical original restored (ETag and public GET verified). Stale, memory-served toolbar: **61,949 bytes — one body — and 3 exchanges** (the revalidation's 200, then a 304 each for toolbar and drawer, one after another), against 188,522 bytes before. |
| Fallback flash, replaced | 20 fps recording of the toolbar avatar: original for 14 frames, then mirrored; **no frame that is neither**, i.e. no letter avatar and no gap. |
| Change met by an ordinary load | 80 s after that validation — still fresh — the leaderboard opened; the own row is a size not yet decoded in that generation, so its load fetched the restored photo, the identity differed, every surface re-keyed, and the toolbar was the original again (difference 0.2 vs 23.1 to the mirror). The probe design would have shown mixed photos until its next probe. |

A correction to this session's own evidence: the first frame classifier judged "textured = photo" and "flat = letter". A
positive control on real letter avatars failed (the glyph makes them textured), so the two unchanged-case recordings were
re-judged by matching against the actual photo; the results above are from the valid method.

A second two-lens review of the final code found no blocker or major defect and could not rebuild the loop (leaked entry,
Coil's fallback GET, stripped or alternating `ETag`s, MD5 mismatch, upload races). Fixed from it: an answer to a request sent
before a local upload is ignored (it used to displace the seeded `ETag` and cause two needless re-keys); no advance when there
is no known `ETag` to compare; the requested-bucket bound raised from 64 to 1024 accounts with an honest KDoc (a toolbar that
stays composed is not re-noted); a dead store method and a redundant `suspend`; two weak test assertions. Account suite: 50.
Left as follow-ups, none introduced by 3.15: the August request-coalescing interceptor hops to `Dispatchers.Default`, so a fresh
memory hit can render the letter for one frame; a surface whose reload *fails* after a re-key drops to the letter although the
previous photo is still in memory; the serial gate releases before the previous fetch's decode closes its snapshot, so the
"empty 304 → full download" fallback is narrowed (188 KB → 62 KB measured), not provably eliminated.

### Snapshot-leak guard on the device (2026-09-18, 22:12–22:20; owner toggled airplane mode)

Final build, one process (pid unchanged throughout), avatar disk entry present.

| Step | Observation |
|---|---|
| Airplane mode **on**, open the username editor (its preview-row size was never decoded in this process → memory miss) | Fetch fails; the row shows the letter avatar — the accepted "offline retention is memory-only" limitation, now seen on a device. No crash; disk entry untouched. |
| Airplane mode **off**, back to Profile (served from memory, validation 23 min old → revalidation) | **5,961 bytes** (fresh TLS + a 304); entry `date:` 02:56:59 → 03:19:52 GMT, status `304`, one `DIRTY/CLEAN` pair — the entry was **rewritten**, so it was not locked. |
| Re-open the editor (memory miss again) | **442 bytes**, a 304 on the warm connection; entry rewritten again; the photo renders. |

With the leak, each 304 could not reopen the entry's editor: it would cost a full ≥53 KB fallback download and leave `date:` unchanged.
**Positive control (2026-09-19, 07:45–07:47).** The same steps on a temporary build with both guards removed (no
`connectivityChecker = ConnectivityChecker.ONLINE`, no fail-fast in the decorator), same phone, fresh process: after the one offline
fetch, re-opening the editor online received **61,408 bytes** — the whole JPEG again — and the entry was **not** rewritten (`date:`
unchanged, no `DIRTY/CLEAN`). Guarded: 442 bytes and the entry rewritten. So Coil 3.4.0's snapshot leak is real on this device,
this test detects it, and the guards prevent it. Afterwards the two source files were restored from a backup and verified by
SHA-256, the APK was rebuilt from scratch, and that build was reinstalled (the phone's APK hash equals the local one; its dex
contains the final classes and no `AvatarKtorRemoteProbe`).

Harness lesson: `uiautomator dump` fails on the editor screen and leaves the previous dump file in place, so a screen check that
only greps the file reads a stale screen. The first attempt at the online half never left the editor for that reason and
measured 0 bytes; the helper now deletes the file first and treats a failed dump as "unknown".

Not verified on device: an in-app upload followed by its deferred transfer.

## On-device verification, iOS, 2026-09-19 (owner's iPhone 13 Pro, iOS 27.0)

The phone was driven with `xcrun devicectl` only — install, launch/terminate, and read-only copies from the app's data
container (possible because the installed Fortuna was already a development build). **No UI automation:** Sign out in both
drawers is a single tap with no confirmation, and the owner chose not to risk the session. On-screen facts were reported by
the owner. Package tests cannot run on a physical device ("tool-hosted testing is unavailable on device destinations"), so
wire-level evidence comes from the simulator's real `URLSession`/`URLCache` against the live object.

### How things were observed

- **The account blob.** The snapshot is AES-GCM ciphertext in `Library/Preferences/account.plist` (key = SHA-256 of "account";
  the AES key stays in the keychain), so its content is never read. Its **length** is: plaintext = blob − 28 (12-byte nonce +
  16-byte tag); a legacy raw bundle is 64 + the four field lengths; the envelope adds 27 + the digits of `validatedAt`. Its
  **nonce** is fresh on every successful store, so "bytes changed or not" says whether a refresh succeeded.
- **The server.** CloudWatch `REPORT` lines of `GetCurrentAccount` give the time of every authenticated refresh.
- **URLCache bodies.** `Library/Caches/<bundle id>/fsCachedData` holds one file per downloaded body: a re-download adds a
  ~52 KB file dated today, a 304 adds nothing. No secrets involved.
- **Foreground without a tap.** Launching another app (Calculator) backgrounds Fortuna; launching Fortuna again without
  `--terminate-existing` brings the same process back (`scenePhase` → active).

### 4.1 — upgrade from data written before this change (iOS: verified)

`account.plist` was dated **27 July**, before this change existed, and the app had last been used on 18 Sept at 11:20 — before
`/accounts/me` was deployed — so no refresh had ever succeeded and the blob had never been rewritten. It was captured before
anything launched (the step missed on Android).

| Moment | Blob | Plaintext | File dated |
|---|---|---|---|
| Before | 178 B | **150 B** = 64 + 26 + 28 + 22 + 10 → legacy raw bundle | 27 Jul |
| After the in-place install, before launch | byte-identical | 150 B | 27 Jul |
| After the first launch of the new build | 221 B | **193 B = 150 + 43** → envelope with `validatedAt` | 19 Sep 08:21 |

The process stayed alive and the server logged an authenticated `GET /accounts/me` five seconds after launch; a signed-out app
cannot make that call, so the legacy blob was decrypted, hydrated and used. It grew by exactly the envelope overhead, so the
four account fields are unchanged. The storage code is byte-identical at `ios/1.6`, `ios/1.7.2` and HEAD, so this is the
format every released build wrote. One-way door, as designed: a build without this change would now show the sign-in screen.

### 4.2 — current account (iOS: verified, same process ids noted)

| Scenario | Observation |
|---|---|
| Fresh foreground (1m20s after validation), same pid | Nonce unchanged, zero server calls. |
| Stale foreground (5m22s), same pid | Blob re-encrypted; exactly one server call (08:26:47). |
| Cold start 30 s later, while fresh | New pid; refreshed anyway (08:27:04) — cold start ignores the window. |
| Username changed by another client | Conditional `UpdateItem` on the Account item only, `felipearpa` → `felipearpa-tmp-ios` (+8), live 21 s. After a cold start the plaintext went **192 → 201 B** (+8, +1 timestamp digit); after the revert and another cold start, **193 B**. Backend verified restored. |
| Refresh fails offline | First attempt was not offline (iOS keeps Wi-Fi on in airplane mode if it was ever re-enabled; the blob changed and the server saw the call). With Wi-Fi off too: cold start, new pid alive after 20 s, **same nonce, zero server calls**; the owner confirmed the app showed the signed-in UI, not the sign-in screen. |
| A later trigger recovers | Back online, cold start: one server call (08:35:31), blob re-encrypted. |
| Save ordering and logout races | Not reproducible by hand; coordinator unit tests (2.7, 3.7). |

### Avatars on iOS

**Wire probe** (`AvatarWireProbeTests`, opt-in via `TEST_RUNNER_AVATAR_WIRE_PROBE_ACCOUNT_ID`, GET-only, its own test runner):
the production request (`.reloadRevalidatingCacheData`) measured with `URLSessionTaskMetrics` against the live object —
first request `200`, 53,391 B body; every later one a **`304` with `If-None-Match` sent, 361 B of headers, 0 B of body**, while
the caller still receives 200 and the full JPEG. So iOS never had Android's `no-cache` re-download. The real store against the
live object: a size not decoded yet while fresh → no network (URLCache fast path); a stale memory hit → one revalidation, no
replacement; a stale memory miss → reload via 304, delivered usable.

**On the phone:** `fsCachedData` was listed after the first launch, after the offline cold start and after the final reinstall,
and always held only its three pre-existing files (dated 29 Jul, 1 Aug, 4 Aug) — across eight launches the toolbar and drawer
avatars were never re-downloaded, since a download would have left a new file behind.

**Defect found and fixed (task 2.14).** An *ordinary* load that met a replaced photo (a size not decoded yet, entry stale)
adopted the new `ETag` without advancing the generation or notifying, so the other sizes kept the old photo — and every later
revalidation compared against the new `ETag`, called it unchanged, and pinned them. The same class as the Android finding,
worse here because it never healed. `performLoad` now advances the generation, purges the older variants and notifies, as the
revalidation path already did. A new store test fails without it (no notification; the toolbar still served the old image).
Account package: 19 Swift Testing (17 store + 2 live probe) + 4 XCTest, 0 failures. The fixed build was reinstalled in place.

Not verified on the phone: a remotely replaced photo and cross-surface reuse on screen (both need UI driving — done on a
simulator instead, next section); the 16 MiB bound; an in-app upload (4.4). Known and left as follow-ups: loads of one avatar
at different sizes are coalesced per size, so a cold URLCache downloads it twice; a newly composed avatar shows the letter
for a frame; offline with a stale validation shows the letter although URLCache holds the bytes (the same accepted
limitation as Android).

## On-screen avatar checks, iOS simulator, 2026-09-19 (4.3)

The phone cannot be tapped from here and the owner ruled out UI automation on it, so the on-screen halves of 4.3 ran on an
iPhone 18 Pro simulator (iOS 27.0) with the working-tree Debug build (scheme `Tyche`, production backend). The owner signed
in; every tap after that was mine, by coordinates from screenshots, never near Sign out.

**Instrument.** The app was launched with `CFNETWORK_DIAGNOSTICS=3` (`SIMCTL_CHILD_…`). CFNetwork then logs, per request, the
loader decision with the host and a task summary with status, request/response bytes and `cache_hit`. Avatar requests are
the `http/1.1` connections to the S3 host (API and Firebase are `h2`; requests on reused connections are mapped back through
the connection number). Only derived lines were printed — raw lines carry the bearer token. Positive control: the sign-in
cold start shows two `200`s of 53,851 B and 53,831 B, which also measures the known double download (the toolbar's 96 px
and the always-composed drawer header's 160 px load concurrently on a cold URLCache). `fsCachedData` and S3 `head-object`
were the second and third witnesses.

| Step (local time) | On screen | Avatar-host traffic for the own account |
|---|---|---|
| 09:25 open the drawer, validation 5 min old | Header and toolbar show the photo | None — the drawer content is always in the hierarchy, so opening it restarts no load |
| 09:26:12 open Profile, validation 6m12s old, size not decoded yet | Photo | **One conditional request: `304`**, request 372 B (271 B when unconditional), response 341 B, `cache_hit=true`; no new body file |
| 09:26–09:31, validation fresh: username editor preview → Profile → pool list → pool toolbar → leaderboard own row | Photo on every surface | **None.** The only S3 traffic was 11 × `403` for the 11 other gamblers on screen, who have no photo |
| 09:31:23 S3 overwritten with a mirrored copy; 09:31:36 Bets → Scores restarts the row and toolbar loads (stale memory hit) | Toolbar and own row switch to the mirrored photo | **One conditional request answered `200`**, 53,315 B |
| 09:31:57 original restored — public for **34 s**; ETag, size, headers and public body MD5 verified | | |
| 09:32–09:34: pool drawer header (always composed), Profile (new surface, fresh), pool-list toolbar and its drawer header (alive under the stack during the swap) | Mirrored on all four | None (8 × `403` when the other gamblers' cached absences lapsed after 5 min — the designed retry) |
| 09:35 decoded images evicted (see below); 09:37:07 open Profile, validation 5m31s old | Profile: letter for 0.36 s, then the **original** | **One conditional request answered `200`**, 53,831 B (S3 held the original again; the known ETag was the mirrored one) |
| 09:37:27 back; open the drawer | Pool-list toolbar and drawer header show the **original** | None |

- **No fallback flash on replacement.** The simulator recording stores every changed frame. Frame by frame against the two
  photos (control: original vs mirrored differ by 78–79, a match is under 8): toolbar and own-row avatars went original →
  mirrored **in the same frame**, 0.50 s after the leaderboard reappeared, and no frame showed a letter or a blank on either.
- **Both store paths met a replaced photo.** The first swap went through a stale memory hit (background revalidation). For
  the second, `UIApplication.didReceiveMemoryWarningNotification` was posted inside the process through `lldb` (the
  simulator's `UISimulatedMemoryWarningNotification` Darwin notification had no effect on this runtime), so Profile's load was
  a miss. The recording confirms it: Profile showed the letter and then the original, **never the mirrored photo** that a
  memory hit would have served first. That is the ordinary-load path fixed in task 2.14 — the toolbar and drawer header, alive
  but hidden under Profile and never reloaded by themselves, came back showing the original with no further request, so the
  replacement notification reached them. No device-level negative control was run (it needs a second S3 overwrite); the
  store test that fails without the fix is the control.
- **Seen again, already listed as a follow-up:** a new surface whose decoded variant is gone and whose validation is stale
  waits for the network (letter for 0.36 s) although URLCache holds the bytes.
- **Not possible on a simulator:** offline retention — it shares the Mac's network, and taking the Mac offline takes this
  session offline too. The owner accepted the store test `offlineRevalidationRetainsStaleImage` for that clause on iOS: after
  a `.notConnectedToInternet` failure of a stale revalidation the same decoded image stays available, a later access retries,
  and no replacement is notified. Re-run on 2026-09-19 with the whole Account package (19 Swift Testing + 4 XCTest, 0
  failures). It was observed on screen on Android only.

Production unchanged: the avatar object is byte-identical to before (`"5b660e06…"`, 53,391 B, same headers). Scratch copies
of the photo, the screenshots and the recordings were deleted.

## Closing 2.13 and 3.13 on unit tests, 2026-09-19

Both tasks ask for the decoded-avatar bound and same-size coalescing to be watched on a running app (Instruments; Android
Studio's profilers). Neither can be staged with real data. A decoded avatar costs width × height × 4 bytes — 64 KiB at the
leaderboard's 128 px, 324 KiB at Profile's 288 px — so 16 MiB means about 256 photo avatars in memory at once, and the largest
pool has 13 gamblers of whom one has a photo. Every surface asks for a different size (toolbar 96, leaderboard 128, drawer
160, Profile 288 px), so two loads of one avatar at one size do not overlap in practice. The owner accepted unit tests.

| Clause | iOS | Android |
|---|---|---|
| Decoded memory stays within 16 MiB, least recently used evicted | `decodedCostStaysWithinBudget`: 20 accounts at 512 px ask for 20 MiB; accounted cost ≤ 16 MiB, the first account loads again, the last does not | `AvatarMemoryBudgetTest` (**added today** — task 3.12 listed a "16 MiB eviction" test that did not exist): a Coil `MemoryCache` built from `AVATAR_MEMORY_BUDGET_BYTES`, as `TycheApplication` builds it, reports `maxSize` 16 MiB and keeps `size` within it after 20 × 1 MiB; with weak references off, the first entry is gone and the last remains |
| Simultaneous consumers share one load | `concurrentConsumersCoalesceIntoOneLoad` | `InFlightRequestRegistryTest` (3), the store's "exactly one [revalidation] runs at the smallest bucket in use", `SerialConcurrentRequestStrategyTest` (3) |
| Seen on a running app | Simulator: one revalidation request for two simultaneous consumers; a miss after decoded images were evicted recovered | Phone: `am send-trim-memory` then a rebuild from the cached body; one request per stale composition |

Limits of this basis: `TycheApplication`'s wiring of the cache is not under test (the test builds the same three-line
configuration, it does not read the application's), and Coil's weak references mean an evicted bitmap stays reachable while a
composed surface still holds it — by design, and outside the budget only for images that are on screen anyway.

Commands and results: `cd Android && ./gradlew :account:testDebugUnitTest` → 52 tests, 0 failures (was 50; +2
`AvatarMemoryBudgetTest`). `cd iOS/Account && xcodebuild test -scheme Account -destination "platform=iOS Simulator,…"` →
19 Swift Testing + 4 XCTest, 0 failures. No production code changed.

## In-app upload on devices (4.4), 2026-09-19

The owner approved two real uploads (they replace the public avatar) and a final S3 restore, and chose unit tests for the
failed-upload half. Android ran on the signed-in SM-G955F, driven with `uiautomator` text dumps and `input tap`; iOS on the
signed-in iPhone 18 Pro simulator. Test images were copies of the owner's own avatar: a mirrored copy for Android, the
original for iOS, so every surface visibly changes each time. On the phone the app opened Google's photo picker, which shows
no file names; the test image was chosen by its unique timestamp from the text dump, and no screenshot was taken while the
picker was open.

**Android (phone).** Instruments: redacted Ktor log, per-UID byte counters, Coil's disk-cache journal and metadata.

| Moment | Observation |
|---|---|
| Upload, 10:48:43–47 | `POST …/avatar-upload-url` → 200, then `PUT` to S3 → 200. Object afterwards: 32,765 B, new ETag, same `Content-Type` and `Cache-Control`. The app received 8,912 B and sent 37,498 B — no avatar download. Coil's disk entry for the avatar untouched (journal 76/76, the old 53,391 B body). |
| Active consumers | Profile showed the mirrored photo when the crop screen closed; the pool-list toolbar and drawer header, composed underneath, showed it on return. 135 B received; disk entry still untouched — served from memory. |
| A size never requested in that process (leaderboard row) | One exchange (journal 77/77): `200` with the **uploaded** ETag, body 32,765 B — the single transfer the spec allows a platform whose HTTP cache still holds the replaced object. It must not re-key or show the fallback: opening the pool drawer and returning to the list made no further exchange (still 77/77) and every surface kept the mirrored photo. |

**iOS (simulator).** Instrument: the CFNetwork diagnostics log (as in the 4.3 section).

| Moment | Observation |
|---|---|
| Cold start after the phone's upload | Two conditional requests answered `200` (33,205 B and 33,225 B): the photo uploaded from the phone reached iOS, again as the known cold double download (toolbar and drawer header sizes). |
| Upload, 10:54:02–03 | One API call (presign) and one S3 task: the `PUT`, 55,511 B sent, 393 B back, `200`. No avatar GET. Object afterwards: 53,470 B, new ETag. |
| Every surface | Profile, the username editor's preview, pool-list toolbar, drawer header, pool toolbar and the leaderboard's own row all showed the uploaded photo. S3 traffic afterwards: 12 × `403` for the other gamblers, **none** for the own avatar — the seeded 512 px image serves every smaller size. |

**Failed upload — unit tests, by the owner's decision; not observed on a device.** Android (re-run today, session 23 and
app 54 tests, 0 failures): `UploadAvatarTest` "presign call fails … nothing is uploaded nor seeded" and "upload PUT fails …
nothing is seeded"; `ProfileViewModelTest` "the upload fails then the previous avatar is preserved and retry can succeed" and
"… the error is dismissed then the pending photo is discarded". iOS: the Session package's `UploadAvatarUseCase` cases for a
failed presign and a failed PUT (re-run today), and the app's `ProfileViewModel` cases with the same two names (last run
2026-09-18 with the workspace suite).

**The public object.** Mirrored photo 10:48:44–10:54:04 (5 min 20 s), then the iOS re-encode of the original, then at
10:57:08 the verified backup was put back: ETag `"5b660e06…"`, 53,391 B, same headers, public body MD5 equal to the backup —
byte-identical to before the test. Left behind on purpose: the test image on the phone at
`/sdcard/Pictures/fortuna-test-mirrored.jpg` (the owner's to delete) and the original photo in the simulator's library.

## Upgrade from the previous release, Android (4.1), 2026-09-19

The owner's phone could not show this any more: its stored account had already been rewritten in the new format, and the
previous release cannot safely be put back on it. Judging by its code (not tried), `android/1.7.2` decodes the stored
account with strict JSON in a field initializer, so it would throw on the envelope at startup, and the only way out would be
clearing the app's data. The upgrade was therefore staged on an emulator (Pixel 10 Pro XL image, API 36) and the phone was
left alone.

1. `android/1.7.2` (versionCode 11) was built from the tag in a scratch copy of the sources (`:app:assembleDebug`; that tag
   has no flavors; production backend) and installed. **The owner signed in on it** — the agent never handles credentials.
2. What 1.7.2 persisted: `shared_prefs/account.xml`, 1,462 B. It is an `EncryptedSharedPreferences` file, so nothing in it is
   readable, but its one data entry is 191 B of ciphertext = 150 B of plaintext (Tink prefix 5 + IV 12 + tag 16 + type and
   length 8 = 41 B of overhead). 150 B is exactly the legacy raw `AccountBundle` JSON for this account (the JSON punctuation
   and key names, 64 B, plus the four field lengths 26 + 28 + 22 + 10) — the same number the iPhone showed.
3. The current build (versionCode 12, SHA-256 equal to the APK verified on the phone) was installed over it with
   `adb install -r`: `firstInstallTime` kept, `account.xml` byte-identical after the install and before any launch.
4. First launch of the new build: it opened on "My pools", not the sign-in screen, with the same username and email in the
   drawer as before the upgrade, and the photo avatar — new in this release — loaded from the account id in the legacy
   bundle. Its first request was `GET /accounts/me` → `200`, which a signed-out app cannot make; the pools request that
   followed carried the same gambler id.
5. The stored entry was rewritten at that moment under the same encrypted key name: 242 B = **201 B of plaintext = 150 + 51**,
   and 51 is exactly the envelope (`{"account":` 11 + `,"validatedAtEpochMillis":` 26 + 13 digits + `}` 1). The four account
   fields are therefore unchanged, and the legacy bundle migrated as designed: hydrated as never validated, reconciled once.

Two further cold starts showed the pools request and `/accounts/me` leaving within a quarter of a second of each other, the
pools one carrying the hydrated account's id before the refresh answered — the first screen does not wait for
reconciliation. (On the very first launch after the upgrade the emulator took ~20 s to compose the home screen, so there the
refresh happened to answer first.)

One-way door, as on iOS: by the same reading of its code, a 1.7.2 build put back over this data would fail at startup
rather than show the sign-in screen. That only concerns downgrades, which the stores do not offer.

## Final regression, 2026-09-19 (afternoon, working tree as it stands)

| Suite | Result |
|---|---|
| Backend — `dotnet test Felipearpa.Tyche.slnx` | 210 tests, 0 failures (Crypto 1, Type 54, Core 2, Data.DynamoDb 4, Pool 51, PoolLayout 7, Function 1, Account 19, AmazonLambda 71) |
| Android — `:session :account :pool :app` unit tests, `:app:assembleProdDebug` | session 23, account 52, pool 2, app 54 — 0 failures. The rebuilt APK has the same SHA-256 as the one verified on the phone: production code has not moved since. |
| iOS app — `xcodebuild test -workspace Tyche.xcworkspace -scheme Tyche`, iPhone 17 simulator, `-parallel-testing-enabled NO` | 53 tests: 51 passed, 0 failed, 2 skipped (`AvatarSanityPassUITests`, opt-in) |
| iOS packages | Session 23 + 2, Account 19 + 4, Core 2 + 13, UI 9, DataPool 3, DataBet 3, Bet (no tests): all pass |
| iOS Pool package | 1 of 2 tests fails: `placeholderSuppressesAvatarRequestAndVoiceOver` — "EmptyView does not have 'accessibilityLabel' modifier" (ViewInspector on the current SDK). **Not from this change:** a scratch copy of the last commit (`git archive HEAD iOS`), without any working-tree change, fails identically. |
| `openspec validate centralize-account-and-avatar-caching --strict` | valid, 38/38 tasks |

A first attempt at the iOS app suite ran on the signed-in iPhone 18 Pro simulator and failed for a reason unrelated to the
tests: the suite is built unsigned (`CODE_SIGNING_ALLOWED=NO`), an unsigned host app cannot read the keychain item a signed
build created, and `StorageInUserDefaults.symmetricKey()` force-unwraps the key, so the host app trapped at launch (the clean
simulator has no stored account, so `retrieve()` returns before touching the keychain). Parallel testing had cloned the
simulator; the clones vanished and the original was shut down, its data intact. The force unwrap predates this change and is
filed as a follow-up: in production it would fire only if the keychain were unreadable at launch.

## Outstanding (manual)

- 2.13 and 3.13: closed on 2026-09-19 on unit tests, by the owner's decision (section above). Neither the 16 MiB bound nor same-size coalescing was observed on a device or under a profiler.
- 4.1: closed on 2026-09-19. iOS on the owner's iPhone; Android on an emulator running the previous release, signed in by the owner, then upgraded in place (section above).
- 4.3: closed on 2026-09-19. Android on the phone; iOS on the simulator plus the phone's no-re-download evidence. One clause is not an on-screen observation on iOS: offline revalidation retaining the stale image rests on the store test, accepted by the owner. To see it on the phone: open Profile, go offline (airplane mode and Wi-Fi off), wait 5 min, re-open Profile — the photo must stay.
- 4.4: closed on 2026-09-19 (section above). The successful upload was observed on an Android phone and an iOS simulator; the failed upload rests on unit tests by the owner's decision.
