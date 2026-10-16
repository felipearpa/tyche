# Refresh the Firebase ID token on 401

## Goal
The apps sometimes show the error screen because an API request gets a 401 after the Firebase ID token (1-hour lifetime) expires. Tapping retry works, because only then is a new token fetched.

When done, both apps behave the same way:
- Every authenticated request carries Firebase's current ID token, which Firebase refreshes on its own before it expires (no forced refresh on ordinary requests).
- When a request still gets a 401, the app forces one token refresh and retries that request **once** with the new token. The user never sees the first 401.
- A second 401 on the retried request, or a failed forced refresh, surfaces as an error exactly as today (existing error screen). No retry loops.
- When a user is signed in but fetching the token fails, the request fails with that error. It is never silently sent without an `Authorization` header. When there is no signed-in user, the request goes out without the header, as today.

## Decisions
- Android uses Ktor's built-in bearer refresh: `loadTokens` returns `getIdToken(false)` with `cacheTokens = false`, and `refreshTokens` returns `getIdToken(true)`. Ktor's `Auth` plugin then resends the request once (its `AuthCircuitBreaker` stops loops) and coalesces concurrent refreshes. (User, in the explore discussion.)
- iOS: `AuthInterceptor` also implements Alamofire's `retry`. On a 401 for a request not yet retried, it forces a token refresh and returns `.retry`; `adapt` then attaches the new token. Otherwise it does not retry. (User, in the explore discussion.)
- iOS coalesces concurrent forced refreshes, so N simultaneous 401s trigger one forced refresh, not N. Android already gets this from Ktor. (This session: parity with Android and avoids bursts of token-endpoint calls.)
- `AuthTokenRetriever` gains a force-refresh option on both platforms, so the Firebase call stays behind the existing abstraction and the interceptor or Ktor config can be tested with a fake. (This session.)
- When the forced refresh throws or yields no token, the request is not retried and the original 401 surfaces, not the refresh error, on both platforms. A normal (non-forced) token fetch that throws still fails the request with that error. (This session, after the Android group: keeps the existing error mapping the same on both platforms.)
- A truly dead Firebase session (forced refresh fails) still shows the existing error screen. Routing to sign-in is a separate follow-up. (User.)
- The Lambda keeps returning 401 for repository errors in `resolveCallerAccountAsync`; no backend change in this work. (User: mobile only.)

## Context
- Read first:
  - AGENTS.md
  - Android/session/src/main/java/com/felipearpa/tyche/session/di/AccountProvider.kt
  - Android/session/src/main/java/com/felipearpa/tyche/session/AuthTokenRetriever.kt
  - Android/session/src/main/java/com/felipearpa/tyche/session/AuthTokenFirebaseRetriever.kt
  - Android/session/build.gradle.kts (note: `ktor-client-mock` exists in gradle/libs.versions.toml and is not yet a test dependency here)
  - iOS/Session/Sources/Session/AuthTokenRetriever.swift
  - iOS/Session/Sources/Session/AuthTokenFirebaseRetriever.swift
  - iOS/Session/Sources/Session/Network/AuthInterceptor.swift
  - iOS/Session/Sources/Session/Network/AuthenticatedSession.swift
  - iOS/Session/Package.swift
- Library facts verified in Ktor 3.4.3 source: `BearerAuthConfig.cacheTokens` defaults to true; `refreshTokens` defaults to `{ null }`; with one provider, a 401 with no `WWW-Authenticate` header still reaches that provider; with `cacheTokens = false`, the retried request's header comes from calling `loadTokens` again, not from the value `refreshTokens` returned.
- Build/Test Android: from `Android/`, `./gradlew :session:testDebugUnitTest` and `./gradlew :app:assembleProdDebug`
- Build/Test iOS: Session package tests on an iOS simulator through `iOS/Tyche.xcworkspace` (not the .xcodeproj); build the Tyche app scheme too. Running package tests can rewrite Package.resolved; revert unintended changes and report them.
- Conventions: new iOS unit tests use Swift Testing; Android non-instrumented tests use JUnit 5 + Kotest assertions. No mutable collections and no NoOp stubs in product code. Never log or record token values.

## Out of scope
- Routing to sign-in or logging out when the session is really gone.
- Any backend change (Amazon.Lambda/**), including the 401-for-repository-errors mapping.
- Avatar upload to S3 (presigned PUT, no Authorization header).
- Other HTTP clients or retry policies unrelated to authentication.
