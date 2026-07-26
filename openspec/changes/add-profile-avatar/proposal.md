## Why

Gamblers have no visual identity in Fortuna — the only avatar is a generated letter derived from the email, and the only editable profile attribute (username) is buried in a one-field modal reachable from two different drawers, presented inconsistently on Android (bottom sheet in one drawer, dialog in the other). A proper Profile screen with a photo avatar gives users ownership of their identity and creates the foundation for showing avatars across the app in an upcoming follow-up proposal.

## What Changes

- New **Profile screen** on iOS (SwiftUI) and Android (Kotlin/Compose), matching the reference design (`assets/Profile-selection.png`): circular avatar with a camera badge, "Change Photo" action, and a Username row navigating to the username editor.
- The existing drawer "edit username" entry becomes a **"Profile" row** that pushes the new screen. Username editing moves behind the Profile screen as sub-navigation, reusing the existing editor views/view models and the `PATCH /accounts` flow. This removes the Android bottom-sheet/dialog inconsistency.
- New **avatar upload pipeline**, fully client-side image processing: photo pick (library or camera capture) → square crop UI (pan + pinch-zoom, circular mask preview) → final-result preview → on-device downscale/compress to 512×512 JPEG → upload to S3 via presigned PUT.
- New **backend endpoint** in the Account bounded context that issues a presigned S3 PUT URL (caller-gated, content-type and size constrained). No image-processing Lambda — the client produces the final image.
- Avatars are stored at the **deterministic S3 key `avatars/<accountId>.jpg`**, so future consumers can derive the URL from a gambler id with no backend changes.
- The existing letter avatar (`EmailAvatar`) remains the empty state; the Profile screen renders the photo when one exists.

### Non-goals

- Avatars on screens that show **other** gamblers (leaderboard, manage gamblers, drawer header, toolbars) — coming in a separate follow-up proposal.
- Cache-invalidation strategy for other-user avatars (e.g. `avatarVersion` denormalization vs. CDN TTL) — deferred to that follow-up.
- A Settings hub screen (the reference design's "Settings" back label is aspirational; no Settings screen exists today).
- Avatar deletion/reset, moderation, and animated avatars.

## Capabilities

### New Capabilities

- `profile`: the Profile screen — entry point from the drawer, avatar display (photo or letter fallback), "Change Photo" action, and username row navigating to the username editor.
- `avatar-upload`: the end-to-end avatar pipeline — photo selection, square crop with preview, client-side optimization, presigned-URL issuance, and S3 upload/storage contract.

### Modified Capabilities

<!-- None — username editing behavior is unchanged; only its navigation entry point moves, which is covered by `profile`. No main specs exist yet in openspec/specs/. -->

## Impact

- **Backend (F#)**: new presigned-URL endpoint in `Felipearpa.Tyche.Account` (Lambda function + HttpApi route), new S3 bucket (or bucket path) for avatars with IAM policy for presigned PUTs, SAM template additions. No DynamoDB schema change in v1.
- **iOS**: new Profile screen + crop/preview screen and view models; drawer entry rework in `PoolHomeDrawerView` / `PoolScoreListDrawerView`; upload client in the `Session`/`Account` package.
- **Android**: same surface — new Profile + crop/preview composables and view models; drawer rework in both `DrawerView`s; upload client in the `session` module.
- **Dependencies**: none added — crop component is hand-rolled natively on both platforms; upload uses existing HTTP stacks (Alamofire / Ktor) plus a plain S3 PUT.
- **Unaffected**: leaderboard and all other-gambler surfaces, `Pool`/`PoolLayout`/`MatchScoreIngestion` contexts, username propagation fan-out.
