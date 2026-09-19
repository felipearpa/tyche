# avatar-loading Specification

## Purpose
Load every avatar image on iOS and Android through one shared, application-scoped store: reuse a decoded image across Profile, navigation chrome, the username preview, and leaderboard rows; coalesce duplicate loads for the same account, generation, and target size; decode for the requested display size within a bounded memory budget; revalidate stale entries once against the existing HTTP or disk cache; and replace the shared image from local memory after a successful upload.

## Requirements

### Requirement: Avatar images load through one shared platform store
Every iOS and Android `AccountAvatar` surface SHALL request avatar images through one application-scoped platform store. The store SHALL reuse a decoded image across Profile, navigation chrome, username preview, and leaderboard consumers and SHALL coalesce overlapping loads for the same account, generation, and target size into one in-flight operation.

Individual avatar views MUST NOT maintain separate network-loading implementations. A missing or failed image SHALL continue to render the existing account-appropriate letter or person fallback and MUST NOT expose fallback or loading values as real gambler content.

#### Scenario: Multiple surfaces request the same avatar
- **GIVEN** Profile, a drawer, or a toolbar request the same account avatar generation at a compatible target size
- **WHEN** one request has already produced a decoded image
- **THEN** the other surfaces receive the shared decoded image
- **AND** they do not start additional URL loads for that fresh entry

#### Scenario: Concurrent consumers miss the cache
- **GIVEN** no memory entry exists for an avatar cache key
- **WHEN** multiple visible consumers request that key concurrently
- **THEN** the store performs one underlying load
- **AND** every consumer receives its result

#### Scenario: Avatar cannot be loaded
- **GIVEN** the avatar object is absent or its load fails
- **WHEN** `AccountAvatar` renders
- **THEN** the production avatar component shows its existing letter or person fallback
- **AND** no separate loading-only avatar layout is introduced

### Requirement: Decoded avatar memory is size-aware and bounded
The avatar store SHALL derive a target pixel size from the avatar's rendered bounds and display density, SHALL decode no larger than the smallest supported size bucket that covers that target, and SHALL cap decoding at the 512-pixel source size. A cached larger compatible variant MAY satisfy a smaller request.

The combined accounted cost of retained decoded avatar entries MUST NOT exceed 16 MiB per process. The store SHALL calculate cost from decoded pixel memory rather than compressed JPEG length, SHALL evict entries when admitting another image would exceed the limit, and SHALL allow earlier eviction under platform memory pressure. Compressed response data SHALL remain the responsibility of the existing HTTP or disk cache rather than being retained a second time by the decoded-image store.

#### Scenario: Leaderboard row requests an avatar
- **GIVEN** a leaderboard avatar renders substantially smaller than the 512-pixel upload
- **WHEN** the store decodes its image
- **THEN** the decoded variant covers the row's target pixel size
- **AND** it is not retained as an unnecessary full-resolution bitmap

#### Scenario: Profile requests a larger compatible variant
- **GIVEN** a smaller cached variant cannot cover the Profile avatar's target pixel size
- **WHEN** Profile requests the same account avatar
- **THEN** the store decodes or retrieves a larger bounded variant
- **AND** the cache keys prevent the smaller image from being presented as if it met the larger target

#### Scenario: Memory admission exceeds the budget
- **GIVEN** retained decoded avatar entries are near the 16 MiB limit
- **WHEN** admitting another decoded image would exceed that limit
- **THEN** the store evicts eligible least-recently-used entries until the accounted cost is within the limit
- **AND** visible image ownership remains valid for the surfaces already rendering it

#### Scenario: A decoded variant is rebuilt while the compressed image is cached
- **GIVEN** the avatar's compressed response is held by the HTTP or disk cache
- **AND** no decoded variant in memory covers the requested target size
- **WHEN** the store loads that avatar again and the remote object is unchanged
- **THEN** it rebuilds the decoded variant from the cached bytes, revalidating conditionally when the cache requires it
- **AND** the unchanged JPEG body is not transferred again, including when several target sizes load concurrently

#### Scenario: System reports memory pressure
- **GIVEN** decoded avatars are retained in memory
- **WHEN** the operating system reports memory pressure
- **THEN** the avatar store permits cached entries to be discarded
- **AND** a later request can recover through the HTTP or disk cache

### Requirement: Fresh avatars avoid URL access and stale avatars revalidate once
A fresh decoded avatar entry SHALL be returned without URL access. When an entry or cached absence is outside the configured staleness interval, the store SHALL return any retained image immediately and SHALL coalesce at most one background conditional revalidation per account, whatever sizes are retained. A load for a size that has no decoded variant is not a revalidation: it MAY ask the origin conditionally on its own, including while a revalidation is in flight, and what it learns about the photo SHALL update the same shared state.

An unchanged response SHALL refresh validation without transferring the full image again and without replacing the decoded image that consumers are showing; a platform whose image pipeline performs the revalidation MAY decode the cached bytes again and discard the result. A changed response SHALL replace the decoded entry and notify active consumers, which SHALL keep showing the previous photo, not the fallback, until the replacement is ready. A failed revalidation SHALL retain the stale image when one exists. An absent avatar result SHALL be shared across concurrent consumers for the bounded staleness interval so separate surfaces do not repeat the same not-found request.

#### Scenario: Fresh avatar is reused
- **GIVEN** a decoded avatar was successfully validated inside the staleness interval
- **WHEN** another compatible surface requests it
- **THEN** the surface receives the memory entry
- **AND** no URL request is made

#### Scenario: Stale avatar is unchanged remotely
- **GIVEN** a retained avatar is outside the staleness interval
- **WHEN** its conditional revalidation reports that the object is unchanged
- **THEN** the retained decoded image remains active
- **AND** the JPEG body is not transferred again

#### Scenario: Stale avatar changed on another device
- **GIVEN** a retained avatar is outside the staleness interval
- **AND** the remote object has changed
- **WHEN** conditional revalidation succeeds
- **THEN** the store replaces the decoded entry with the current photo
- **AND** active consumers receive the replacement without showing the fallback in between

#### Scenario: Revalidation fails offline
- **GIVEN** a stale decoded avatar remains in memory
- **WHEN** conditional revalidation fails because the network is unavailable
- **THEN** consumers continue to receive the stale image
- **AND** a later stale access remains eligible to retry

#### Scenario: Multiple surfaces request a missing avatar
- **GIVEN** the store has a fresh cached absence for an account
- **WHEN** multiple surfaces request that account's avatar
- **THEN** each surface displays its existing fallback
- **AND** no additional not-found request occurs inside the staleness interval

### Requirement: Successful upload replaces the shared avatar from local memory
After the avatar PUT succeeds, the client SHALL advance the signed-in account's process-local avatar generation and seed the shared decoded-image store with the optimized local image before reporting upload success. Every active compatible avatar consumer SHALL receive the replacement from local memory, without an avatar GET, before upload success is reported. A platform whose HTTP cache still holds the replaced object MAY transfer the uploaded image once afterwards, when that cache is next revalidated, and MUST NOT re-key consumers or show the fallback while doing so.

If the PUT fails, the client SHALL retain the previous generation and cached image. The pending local image SHALL remain scoped to the existing retry flow and MUST NOT replace shared saved-avatar state until an upload succeeds.

#### Scenario: Successful upload updates all visible surfaces
- **GIVEN** the crop flow produced an optimized local avatar image
- **WHEN** its S3 PUT succeeds
- **THEN** the shared store publishes that local image as the new signed-in avatar generation
- **AND** Profile, drawers, toolbars, username preview, and visible current-gambler rows update without an avatar GET

#### Scenario: Upload fails
- **GIVEN** a previous avatar or fallback is active
- **WHEN** the replacement avatar PUT fails
- **THEN** the shared avatar generation and cache entry remain unchanged
- **AND** other avatar surfaces do not display the failed replacement as saved state
