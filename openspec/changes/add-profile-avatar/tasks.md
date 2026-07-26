## 1. Backend

- [ ] 1.1 Add the `tyche-avatars` S3 bucket to `serverless.template.yaml` per design.md Decision 8: public-read bucket policy on `avatars/*` (Block Public Access relaxed for policy only), plus `s3:PutObject` on `avatars/*` in the presign function's IAM policy
- [ ] 1.2 Define the domain interface for issuing an avatar upload URL in `Felipearpa.Tyche.Account` (data access only) and its S3-backed adapter: key fixed to `avatars/<accountId>.jpg`, `image/jpeg`, ≤ 1 MB, ≤ 5 min expiry
- [ ] 1.3 Add the use case performing the caller check (`callerAccountId <> accountId` → rejection), following the `UpdateUsername` pattern
- [ ] 1.4 Expose the endpoint on both hosts: Lambda handler in `AccountFunction.fs` + HttpApi route in `AccountRouter.fs`, request/response DTOs, DI registration
- [ ] 1.5 Add the SAM function entry (route, handler, IAM) and unit tests for the use case (owner OK, non-owner rejected) and the presign request builder
- [ ] 1.6 Deploy and verify with a manual presigned PUT (curl) that constraints reject wrong content-type and oversize bodies

## 2. iOS

- [ ] 2.1 Add the upload-URL client call in the `Session` package (Alamofire data source + repository + use case, mirroring `updateUsername`) and the S3 PUT upload call
- [ ] 2.2 Load the `frontend-design` skill, then build the Profile screen per `assets/Profile-selection.png`: circular avatar (photo or `EmailAvatar` fallback) with camera badge, "Change Photo" action, Username row with chevron, footer text
- [ ] 2.3 Rework both drawers: replace the "edit username" row with a "Profile" row pushing the new screen; remove the `minimalDialog` username presentation from `PoolHomeRouter` / `PoolScoreListRouter`
- [ ] 2.4 Wire the Username row to the existing `UsernameEditor`/`UsernameEditorViewModel` as sub-navigation; verify drawer header still refreshes via `applyUsername`
- [ ] 2.5 Load the `frontend-design` skill, then build the source chooser (photo library / camera via `UIImagePickerController`) and crop screen per design.md's visual direction: pan + pinch-zoom with cover-clamp and zoom bounds, circular mask on dark surface, dual-scale live preview strip, clamp haptics, EXIF orientation normalization
- [ ] 2.6 Render the confirmed crop to 512×512 JPEG (quality ~0.8) and wire pick → crop → upload → Profile refresh (local image, no re-fetch), with error + retry state and the confirm morph animation (`matchedGeometryEffect`, crossfade under reduced motion)
- [ ] 2.7 Add new strings to the xcstrings catalogs (remember `extractionState=manual` for symbol generation), add `NSCameraUsageDescription` to Info.plist (join `membershipExceptions` in pbxproj), and build the Tyche scheme
- [ ] 2.8 Unit tests with **Swift Testing** (`import Testing`; existing XCTest suites stay as they are): crop math (cover min-zoom for portrait/landscape/square sources, max-zoom cap with the `max ≥ min` invariant on tiny sources, pan clamp at all four edges, zoom-anchor re-clamp, crop-rect mapping identity case), Profile view model (photo-vs-letter fallback, post-upload local render without re-fetch), upload flow (state transitions, failure preserves previous avatar, presign/PUT error propagation)

## 3. Android

- [ ] 3.1 Add the upload-URL client call in the `session` module (Ktor data source + repository + use case, mirroring `UpdateUsername`) and the S3 PUT upload call
- [ ] 3.2 Load the `frontend-design` skill, then build the Profile composable per the mock: circular avatar (photo or `EmailAvatar` fallback) with camera badge, "Change Photo" action, Username row, footer text
- [ ] 3.3 Rework both `DrawerView`s to a single "Profile" entry that navigates to the new screen; delete the `ModalBottomSheet`/`MinimalDialog` username presentations
- [ ] 3.4 Wire the Username row to the existing `UsernameEditor`/`UsernameEditorViewModel` as sub-navigation; verify both drawer view models still refresh via `applyUsername`
- [ ] 3.5 Load the `frontend-design` skill, then build the source chooser (photo library / camera capture intent) and crop composable per design.md's visual direction: `pointerInput` pan/zoom with cover-clamp and zoom bounds, circular mask on dark surface, dual-scale live preview strip, clamp haptics, EXIF orientation normalization
- [ ] 3.6 Render the confirmed crop to a 512×512 JPEG (quality ~80) and wire pick → crop → upload → Profile refresh, with error + retry state and the confirm morph animation (shared-element transition, crossfade under reduced motion)
- [ ] 3.7 Add string resources and run `:app:compileDebugKotlin` + a device sanity pass of the full flow
- [ ] 3.8 Unit tests with **JUnit 5 + Kotest assertions** (plain `src/test`, no instrumentation; both already in `libs.versions.toml`): same crop-math suite as iOS (cover min-zoom, max-zoom cap with `max ≥ min` invariant, pan clamp, zoom-anchor re-clamp, crop-rect mapping), Profile view model (photo-vs-letter fallback, post-upload local render), upload flow (state transitions, failure preserves previous avatar, presign/PUT error propagation)
