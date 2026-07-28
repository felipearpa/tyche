## 1. Backend

- [x] 1.1 Add the `tyche-avatars` S3 bucket to `serverless.template.yaml` per design.md Decision 8: public-read bucket policy on `avatars/*` (Block Public Access relaxed for policy only), plus `s3:PutObject` on `avatars/*` in the presign function's IAM policy
- [x] 1.2 Define the domain interface for issuing an avatar upload URL in `Felipearpa.Tyche.Account` (data access only) and its S3-backed adapter: key fixed to `avatars/<accountId>.jpg`, `image/jpeg`, ≤ 1 MB, ≤ 5 min expiry
- [x] 1.3 Add the use case performing the caller check (`callerAccountId <> accountId` → rejection), following the `UpdateUsername` pattern
- [x] 1.4 Expose the endpoint on both hosts: Lambda handler in `AccountFunction.fs` + HttpApi route in `AccountRouter.fs`, request/response DTOs, DI registration
- [x] 1.5 Add the SAM function entry (route, handler, IAM) and unit tests for the use case (owner OK, non-owner rejected) and the presign request builder
- [x] 1.6 Deploy and verify with a manual presigned PUT (curl) that constraints reject wrong content-type and oversize bodies
- [x] 1.7 Sign `Cache-Control: max-age=0, must-revalidate` into the presign request per design.md Decision 10, update the request-builder tests, redeploy, and verify with curl that a conditional GET returns `304`; backfill the header on existing objects with a one-off `aws s3 cp --metadata-directive REPLACE`

## 2. iOS

- [x] 2.1 Add the upload-URL client call in the `Session` package (Alamofire data source + repository + use case, mirroring `updateUsername`) and the S3 PUT upload call
- [x] 2.2 Load the `frontend-design` skill, then build the Profile screen per `assets/Profile-selection.png`: circular avatar (photo or `EmailAvatar` fallback) with camera badge, "Change Photo" action, Username row with chevron, footer text
- [x] 2.3 Rework both drawers: replace the "edit username" row with a "Profile" row pushing the new screen; remove the `minimalDialog` username presentation from `PoolHomeRouter` / `PoolScoreListRouter`
- [x] 2.4 Wire the Username row to the existing `UsernameEditor`/`UsernameEditorViewModel` as sub-navigation; verify drawer header still refreshes via `applyUsername`
- [x] 2.5 Load the `frontend-design` skill, then build the source chooser (photo library / camera via `UIImagePickerController`) and crop screen per design.md's visual direction: pan + pinch-zoom with cover-clamp and zoom bounds, circular mask on dark surface, dual-scale live preview strip, clamp haptics, EXIF orientation normalization
- [x] 2.6 Render the confirmed crop to 512×512 JPEG (quality ~0.8) and wire pick → crop → upload → Profile refresh (local image, no re-fetch), with error + retry state and the confirm morph animation (`matchedGeometryEffect`, crossfade under reduced motion)
- [x] 2.7 Add new strings to the xcstrings catalogs (remember `extractionState=manual` for symbol generation), add `NSCameraUsageDescription` to Info.plist (join `membershipExceptions` in pbxproj), and build the Tyche scheme
- [x] 2.8 Unit tests with **Swift Testing** (`import Testing`; existing XCTest suites stay as they are): crop math (cover min-zoom for portrait/landscape/square sources, max-zoom cap with the `max ≥ min` invariant on tiny sources, pan clamp at all four edges, zoom-anchor re-clamp, crop-rect mapping identity case), Profile view model (photo-vs-letter fallback, post-upload local render without re-fetch), upload flow (state transitions, failure preserves previous avatar, presign/PUT error propagation)
- [x] 2.9 Simulator/device sanity pass of the full flow, mirroring Android's: drawer → Profile → source chooser (the camera row must track `isSourceTypeAvailable` — note newer Xcode simulators expose a simulated camera) → photo library pick → crop (clamp + dual-scale previews + confirm morph) → upload → Profile refresh, then remote re-fetch on re-entry
- [x] 2.10 Teach `AutoEmailAvatar` (Account package) to prefer the account's avatar photo with `EmailAvatar` fallback per design.md Decision 9, and give `AccountHeaderDrawer` the account id so the drawer header uses it; leave `ManageGamblerItem` on the letter avatar
- [x] 2.11 Force revalidation on the avatar surfaces (`URLSession` with `.reloadRevalidatingCacheData`, since `AsyncImage` takes no `URLRequest`) and add Swift Testing coverage for photo-vs-letter selection and the post-upload refresh of the chrome
- [x] 2.12 Verify on the simulator that the drawer header and toolbar show the photo, and that replacing the stored object is picked up on next load rather than served from cache

## 3. Android

- [x] 3.1 Add the upload-URL client call in the `session` module (Ktor data source + repository + use case, mirroring `UpdateUsername`) and the S3 PUT upload call
- [x] 3.2 Load the `frontend-design` skill, then build the Profile composable per the mock: circular avatar (photo or `EmailAvatar` fallback) with camera badge, "Change Photo" action, Username row, footer text
- [x] 3.3 Rework both `DrawerView`s to a single "Profile" entry that navigates to the new screen; delete the `ModalBottomSheet`/`MinimalDialog` username presentations
- [x] 3.4 Wire the Username row to the existing `UsernameEditor`/`UsernameEditorViewModel` as sub-navigation; verify both drawer view models still refresh via `applyUsername`
- [x] 3.5 Load the `frontend-design` skill, then build the source chooser (photo library / camera capture intent) and crop composable per design.md's visual direction: `pointerInput` pan/zoom with cover-clamp and zoom bounds, circular mask on dark surface, dual-scale live preview strip, clamp haptics, EXIF orientation normalization
- [x] 3.6 Render the confirmed crop to a 512×512 JPEG (quality ~80) and wire pick → crop → upload → Profile refresh, with error + retry state and the confirm morph animation (shared-element transition, crossfade under reduced motion)
- [x] 3.7 Add string resources and run `:app:compileDebugKotlin` + a device sanity pass of the full flow
- [x] 3.8 Unit tests with **JUnit 5 + Kotest assertions** (plain `src/test`, no instrumentation; both already in `libs.versions.toml`): same crop-math suite as iOS (cover min-zoom, max-zoom cap with `max ≥ min` invariant, pan clamp, zoom-anchor re-clamp, crop-rect mapping), Profile view model (photo-vs-letter fallback, post-upload local render), upload flow (state transitions, failure preserves previous avatar, presign/PUT error propagation)
- [x] 3.9 Teach `AutoEmailAvatar` (`account` module) to prefer the account's avatar photo with `EmailAvatar` fallback per design.md Decision 9, and give `AccountHeaderDrawer` the account id so the drawer header uses it; leave `ManageGamblerItem` on the letter avatar
- [x] 3.10 Force revalidation on the avatar surfaces (`Cache-Control: no-cache` request header on the Coil request) and add JUnit 5 + Kotest coverage for photo-vs-letter selection and the post-upload refresh of the chrome
- [x] 3.11 Device pass: drawer header and toolbar show the photo, and replacing the stored object is picked up on next load instead of being served from Coil's disk cache
