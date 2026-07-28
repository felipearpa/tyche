## Context

Fortuna has no profile surface today. Account UI is exactly: a drawer header (`AccountHeaderDrawer` — letter avatar + username + email), a sign-out button, and a one-field username editor modal reachable from two drawers (pool home and pool score list). Presentation is inconsistent on Android (`ModalBottomSheet` from the pool-home drawer, `MinimalDialog` from the pool-score drawer; iOS uses `minimalDialog` in both). The letter avatar (`EmailAvatar`, defined once per platform in the `Account` package/module) derives a letter and color from the email and is used in drawer headers, toolbars, and manage-gamblers rows.

Backend username editing is `PATCH /accounts` → `UpdateUsername` use case → Account table update + synchronous in-process fan-out to Pool items (`PropagateGamblerUsernameAsync`). There is no DynamoDB stream on the Account table.

Reference design: `assets/Profile-selection.png` — Profile screen with a large circular avatar, green camera badge, "Change Photo" action, a chevroned Username row, and a footer note under the username row. The mock's "‹ Settings" back label is aspirational; no Settings screen exists and none is being built.

## Goals / Non-Goals

**Goals:**

- A Profile screen on both platforms that is the single home for identity: avatar + username editing.
- Avatar upload with the familiar big-platform UX: pick → square crop (pan/zoom) → preview → optimized result.
- Client-side-only image processing; the backend never touches image bytes.
- A storage contract (`avatars/<accountId>.jpg`) that the upcoming "avatars everywhere" proposal can consume with zero backend changes.

**Non-Goals:**

- Rendering avatars for **other** gamblers (leaderboard rows, manage-gamblers rows) — follow-up proposal. The signed-in gambler's own avatar in the drawer header and toolbar **is** in scope here.
- Cache-invalidation strategy for other-user avatars (`avatarVersion` vs. CDN TTL) — decided in that follow-up. Freshness of the signed-in gambler's own avatar is decided here (Decision 10).
- Settings hub, avatar delete/reset, moderation, animated avatars.

## Decisions

### 1. Client-side crop + resize; backend issues presigned URLs only

The client produces the final 512×512 JPEG (quality ~0.8) before upload; the backend's only new surface is an endpoint returning a presigned S3 PUT URL.

- *Why*: avoids an image-processing Lambda entirely (no libvips/ImageSharp layer, no memory tuning, no second copy of the image), avoids pushing image bytes through API Gateway (payload limits), and both platforms have first-class native APIs for crop/downscale (`UIGraphicsImageRenderer` / `Bitmap`).
- *Alternative considered*: upload original + server-side resize pipeline (S3 trigger → Lambda). Rejected: more moving parts, more cost, no benefit at a 512px target.
- *Constraint*: the presigned URL must constrain `Content-Type: image/jpeg` and enforce a size cap (1 MB) so the client contract is the only path to valid objects.
- *Refinement (settled at apply time)*: SigV4 presigned PUTs can only sign an **exact** `Content-Length` (length *ranges* exist only in POST policies), so the client declares its byte count in the presign request body (`{ contentLength }`); the server rejects > 1 MB and signs that exact length — any deviation in size or content type fails with `SignatureDoesNotMatch` (verified against S3).

### 2. Deterministic S3 key: `avatars/<accountId>.jpg`

One object per account, overwritten on change. No pointer/URL attribute is written to DynamoDB in v1.

- *Why*: the URL becomes a pure function of identity. Every screen that has a `gamblerId` (leaderboard rows are keyed `GAMBLER#<id>`, which is the accountId) can derive the avatar URL with no API or schema change — exactly what the follow-up proposal needs.
- *Alternative considered*: versioned keys (`avatars/<accountId>/<ulid>.jpg`) + `avatarUrl` attribute on the Account item. Rejected for v1: requires a DynamoDB write, a read path, and pushes the denormalization question (how do Pool items learn the URL?) into this change. Versioning is precisely the cache-invalidation question deferred to the follow-up.
- *Trade-off accepted*: other-viewer freshness is out of scope by definition of the non-goals. Rendering the just-uploaded bytes locally covers the uploading client **within that session only** — it does not cover a second device or a later launch, which is what Decision 10 addresses.

### 3. Existence check, not metadata

The Profile screen determines photo-vs-letter by attempting to load `avatars/<accountId>.jpg` (or a locally cached copy after upload) and falling back to `EmailAvatar` on 404/403.

- *Why*: keeps v1 free of any Account-table change; `docs/database-model.md` stays untouched.
- *Alternative considered*: `hasAvatar` flag on the Account item. Rejected: a second source of truth that can drift from S3, for no v1 benefit.

### 4. Presigned-URL endpoint lives in the Account bounded context

`POST /accounts/{accountId}/avatar-upload-url` (Lambda function + HttpApi route, mirroring the `UpdateUsername` dual-host pattern). Caller-gated the same way: resolve caller from JWT, reject when `callerAccountId <> accountId`.

- *Why Account*: the avatar is an account attribute; Pool knows nothing about it in v1.
- *Repository shape*: an S3-backed adapter behind a domain interface (data access only — presign is a data operation, no orchestration in the repository), injected into a small use case that performs the caller check. Follows the existing `IAccountRepository` + use-case pattern.
- *Alternative considered*: client-side AWS SDK with Cognito-style credentials. Rejected: the app authenticates with Firebase JWTs, not AWS credentials; presigning server-side keeps IAM entirely backend-owned.

### 5. Hand-rolled crop component on both platforms

A native pan + pinch-zoom crop view with a fixed square window, circular mask preview inside it, and a small rendered result preview — built twice (SwiftUI gestures / Compose `pointerInput` + `graphicsLayer`), sharing the same spec: clamp so the image always covers the crop window; output = visible square region rendered at 512×512.

- *Why*: matches the `SwipeToDismissBox` precedent (same component name and behavior per platform, native implementation each). Android's uCrop was considered and rejected — its UI wouldn't match the mock, it adds a dependency for ~200 lines of gesture math, and iOS would still need a hand-rolled version.

**Zoom bounds**: minimum scale = the image exactly covers the crop window (aspect-fill; never less). Maximum scale = source-pixels-per-window-side ÷ 512, capped at 5× — the visible region can never contain fewer source pixels than the 512×512 output; when a tiny source makes the computed max fall below the cover minimum, the minimum wins (`max ≥ min` invariant). Pinch zooms around the pinch midpoint, then the pan offset is re-clamped so the window stays covered.

**Testability**: the crop math (min/max scale, pan clamp, anchor re-clamp, crop-rect mapping) lives in a pure per-platform function, unit-tested with the same case list on both platforms — Swift Testing on iOS (new tests; existing XCTest suites untouched), JUnit 5 + Kotest assertions on Android (plain `src/test`, both already in the version catalog).

**Visual direction** (worked out with the `frontend-design` skill; there is no mock for this screen):

- Near-black surface in both light and dark themes — photo-editing convention with a reason: a neutral dark surround keeps the photo's colors readable while framing. Scrim outside the crop circle at ~62% black with a 1px white-24% hairline on the circle. No square chrome and no rule-of-thirds grid: the circle is the only shape the avatar is ever displayed in, and a grid is a false affordance for face-framing.
- System typefaces and the app's existing green accent for the confirm action — no new faces or colors on a utility screen inside a native app; restraint is the deliberate choice here.
- Signature element: a **dual-scale live preview strip** below the crop window — the crop rendered at Profile size (~96pt) and at the 32pt `navigationEmailAvatar` row size beside the user's username, updating live during pan/zoom. It answers "will my face read when it's tiny?" in the app's own vernacular, replacing the generic single small preview circle.
- Motion: one moment only — on confirm, the circle morphs from the crop window into the Profile screen's avatar position (`matchedGeometryEffect` on iOS, Compose shared-element transition on Android); crossfade when reduced motion is enabled. A haptic tick fires when pan/zoom hits its clamp bounds.
- *Apply-time note (Android)*: Compose's shared-element transition (BOM 2026.05) left the crop screen's pointer input consumed by its transition overlay on device, so Android ships the crossfade path unconditionally; the full morph runs on iOS only.
- Copy: verb-first and consistent through the flow — entry "Change Photo" → confirm "Use Photo" (not "Done"), plus "Cancel". No success toast; the avatar visibly updating is the confirmation.

### 6. Profile is a pushed screen; the modal editor pattern is retired from the drawers

The drawer "edit username" row becomes "Profile" and pushes the Profile screen; the Username row inside it presents the existing editor (reusing `UsernameEditor` + its view model unchanged). Both Android drawers converge on the same presentation, removing the bottom-sheet/dialog asymmetry.

- *Why push, not modal*: Profile is a destination with sub-navigation (username, future rows), not a one-shot input.

### 7. Camera capture via system UIs, not an in-app camera

"Change Photo" offers two sources: photo library and device camera. iOS uses `UIImagePickerController` with the `.camera` source, which requires `NSCameraUsageDescription` in Info.plist (the Info.plist edit must join `membershipExceptions` in the pbxproj, per repo convention). Android uses the system capture intent (`ACTION_IMAGE_CAPTURE`) — no runtime permission is needed because the app does not declare `CAMERA` in its manifest.

- *Alternative considered*: CameraX / a custom in-app camera. Rejected: a full camera UI for a single square shot is over-engineering; both sources converge on the same crop screen anyway, which is where framing happens.

## Risks / Trade-offs

- [Stale avatar on a second device or later launch] **Observed on device, not hypothetical**: the S3 object carries no `Cache-Control`, so the URL-keyed image caches (Coil's disk cache on Android, `URLCache` behind `AsyncImage` on iOS) reuse whatever they hold without revalidating. A phone kept showing a photo cached at 16:04 while the stored object had changed hours earlier; deleting Coil's cache directory made the current photo appear immediately. The deterministic key gives no cache-busting signal by design, so this cannot resolve itself. → Mitigated per Decision 10.
- [Presigned PUT misuse window] A leaked URL allows overwriting that one account's avatar until expiry. → Short expiry (~5 min), content-type + content-length conditions, key is fixed by the server (client never chooses the key).
- [Non-JPEG or oversized uploads bypassing the client] → S3 policy conditions on the presigned URL are the enforcement point, not client goodwill.
- [Crop math divergence between platforms] Two implementations of the same gesture/clamp spec can drift. → Specify the clamp rules and output contract in the spec scenarios; keep both implementations parameter-compatible (window size, min/max zoom).
- [EXIF orientation] Photos arrive rotated on some Android devices — camera captures included. → The crop pipeline must normalize orientation before rendering, regardless of source (both platforms decode via APIs that apply orientation when asked; called out as an explicit task).

## Migration Plan

No data migration. Deploy order:

1. Backend: S3 bucket + SAM function/route for the presign endpoint (inert until clients call it).
2. Clients: Profile screen + upload flow behind the new drawer row.

Rollback = remove the drawer entry point (screen unreachable); S3 objects are harmless residue.

### 8. Storage and read model: dedicated `tyche-avatars` bucket, public GET (settled at apply time)

The SAM template has no S3 resources today and the account's only bucket is the deploy bucket, so a dedicated bucket following the `tyche-*` convention is the topology. Reads are public: a bucket policy allows `s3:GetObject` on `avatars/*` only; clients construct the stable URL (`https://tyche-avatars.s3.<region>.amazonaws.com/avatars/<accountId>.jpg`) and load it directly, so URL-keyed image caches (AsyncImage/Coil) work without custom keys and displaying an avatar costs zero backend calls. Writes remain owner-gated via presigned PUT — the public surface is read-only.

- *Alternatives considered*: CloudFront from day one (edge caching + TTL control; deliberately deferred to the follow-up proposal, whose cache-invalidation question is exactly what TTL control answers — migrating is a one-host-constant change per client because the `avatars/<id>.jpg` path survives). Private bucket + presigned GETs (rejected: one backend call per display, presigned URLs defeat URL-keyed caches, and the future leaderboard would pay N calls per screen). API proxy (rejected: Lambda time per image, payload limits).
- *Privacy model accepted*: anyone holding an accountId (a ULID, visible to pool-mates in API responses) can view that avatar — the GitHub/Gravatar model.

### 9. Self-avatar rendering goes through the existing auto-avatar component

`AutoEmailAvatar` (iOS `Account` package / Android `account` module) already self-resolves the signed-in account from `AccountStorage`, so it becomes the single place that prefers the photo and falls back to `EmailAvatar`. That covers every toolbar call site for free — iOS `PoolHomeView.swift:88` and `PoolScoreListRouter.swift:155`, Android `PoolHomeView.kt:352` and `PoolScoreListView.kt:163` — because all four already go through it.

- `AccountHeaderDrawer` is the one caller that must change shape: it takes a bare `email` today and calls `EmailAvatar(email:)` directly, so it needs the account id (both drawer view models already read the bundle) or should switch to the auto variant.
- `ManageGamblerItem` (both platforms) is deliberately left on the letter avatar — it renders *other* gamblers from their email, and those surfaces belong to the follow-up.
- *Why one component*: the alternative — a photo-aware avatar at each call site — is four copies per platform of the same fallback and freshness logic, and guarantees drift.

### 10. Own-avatar freshness: revalidate, at both the object and the request

Two complementary mitigations, because they fix different halves of the problem:

- **Object level**: the presign request signs `Cache-Control: max-age=0, must-revalidate`, so every stored object tells any cache — Coil, `URLCache`, a browser, a future CloudFront — to check before reuse. With the `ETag` already present, the common case is a `304 Not Modified` with no image bytes.
- **Request level**: the avatar surfaces additionally force revalidation client-side (a `Cache-Control: no-cache` request header on Android's Coil request; `URLSession` with `.reloadRevalidatingCacheData` on iOS, since `AsyncImage` accepts no `URLRequest`). This fixes clients holding objects uploaded before the header existed.
- *Apply-time note (Android)*: Coil 3 ignores HTTP cache headers entirely by default — both directives above are no-ops until the app's `ImageLoader` installs `CacheControlCacheStrategy` (the separate `coil-network-cache-control` artifact), wired via `SingletonImageLoader.Factory` on `TycheApplication`. Verified on device: without it the drawer kept serving a stale disk entry; with it the replaced photo appeared on next launch.
- *Cost accepted*: signing the header couples the release — backend and both apps must ship together, or the PUT fails with `SignatureDoesNotMatch`. Objects uploaded before this change keep no header until re-uploaded (a one-off `aws s3 cp --metadata-directive REPLACE` can backfill).
- *Alternative considered*: a versioned display URL (`?v=<avatarVersion>`). Rejected here — it needs a version signal per account, which is exactly the denormalization question the follow-up owns.

## Open Questions

- Whether the footer copy ("how other gamblers see you on the leaderboard") ships as-is before the follow-up proposal makes it fully true, or gets softened in v1 strings.
