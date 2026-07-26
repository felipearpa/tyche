## ADDED Requirements

### Requirement: Photo selection and square crop
WHEN the user chooses "Change Photo", the app SHALL let the user choose an image source — the photo library or the device camera — and SHALL present the selected or captured image in a crop screen with a fixed square crop window, pan and pinch-zoom gestures, a circular mask preview inside the window, and a small preview of the final result. The image SHALL be constrained so the crop window is always fully covered, and image orientation (EXIF) SHALL be normalized before display.

#### Scenario: Cropping a picked photo
- **WHEN** the user picks a photo and pans/zooms it within the crop screen
- **THEN** the visible square region under the crop window is what the preview shows and what confirmation will produce, and the image can never be moved or zoomed to leave part of the window uncovered

#### Scenario: Taking a photo with the camera
- **WHEN** the user chooses the camera source and takes a photo
- **THEN** the captured photo enters the same crop screen as a library pick

#### Scenario: Cancelling the crop
- **WHEN** the user cancels from the crop screen
- **THEN** no upload occurs and the Profile screen's avatar is unchanged

### Requirement: Client-side image optimization
On confirmation, the app SHALL render the selected square region to a 512×512 JPEG (quality ≈ 0.8) on the device. The uploaded object MUST NOT exceed 1 MB.

#### Scenario: Large photo is optimized
- **WHEN** the user confirms a crop of an arbitrarily large source photo
- **THEN** the app produces a 512×512 JPEG under 1 MB before any network call

### Requirement: Presigned upload URL issuance
The backend SHALL expose an authenticated endpoint in the Account context that returns a presigned S3 PUT URL for the caller's avatar object. The endpoint SHALL reject callers requesting a URL for another account. The presigned URL SHALL fix the object key server-side to `avatars/<accountId>.jpg`, SHALL constrain content type to `image/jpeg` and content length to at most 1 MB, and SHALL expire within 5 minutes.

#### Scenario: Owner requests an upload URL
- **WHEN** an authenticated user requests an avatar upload URL for their own account
- **THEN** the endpoint returns a presigned PUT URL for `avatars/<accountId>.jpg` with the content-type, size, and expiry constraints applied

#### Scenario: Caller requests a URL for another account
- **WHEN** an authenticated user requests an avatar upload URL for a different account id
- **THEN** the endpoint rejects the request and no URL is issued

### Requirement: Avatar upload and storage contract
The app SHALL upload the optimized JPEG with an HTTP PUT to the presigned URL. Avatar objects SHALL live at the deterministic key `avatars/<accountId>.jpg`, one object per account, overwritten on each change. Upload failures SHALL be reported to the user with the option to retry, leaving the previous avatar intact.

#### Scenario: Successful upload replaces the avatar
- **WHEN** the PUT to the presigned URL succeeds
- **THEN** the object at `avatars/<accountId>.jpg` contains the new image and the Profile screen shows it

#### Scenario: Failed upload preserves the previous avatar
- **WHEN** the PUT fails or times out
- **THEN** the user is shown an error with a retry option and the previously stored avatar (or letter placeholder) remains in effect
