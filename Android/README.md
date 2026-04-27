# Fortuna for Android

### Predict. Compete. Win the bragging rights.

The Android client for **Fortuna** — the prediction pool app that turns every match into a competition between friends. Built native with Kotlin and Jetpack Compose.

> *Developed under the codename **Tyche**. The consumer-facing app is **Fortuna**.*

---

## Get Fortuna on Android

<table>
  <tr>
    <td align="center" valign="middle" width="50%">
      <a href="https://play.google.com/store/apps/details?id=com.felipearpa.fortuna">
        <img src="../android-store/play-store-qr.svg" width="180" height="180" alt="Google Play QR" />
      </a>
    </td>
    <td align="center" valign="middle" width="50%">
      <a href="https://play.google.com/store/apps/details?id=com.felipearpa.fortuna">
        <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="84" alt="Get it on Google Play" />
      </a>
    </td>
  </tr>
</table>

Currently in **closed beta** on Google Play.

## Screenshots

<p align="center">
  <img src="../android-store/Google%20Play%20Phone%20Screenshot%201.png" width="200" alt="Android screenshot 1" />
  <img src="../android-store/Google%20Play%20Phone%20Screenshot%202.png" width="200" alt="Android screenshot 2" />
  <img src="../android-store/Google%20Play%20Phone%20Screenshot%203.png" width="200" alt="Android screenshot 3" />
  <img src="../android-store/Google%20Play%20Phone%20Screenshot%204.png" width="200" alt="Android screenshot 4" />
</p>

## Tech stack

- **Language** — Kotlin
- **UI** — Jetpack Compose, Material 3
- **Navigation** — Navigation Compose
- **DI** — Koin
- **Networking** — Ktor client
- **Auth** — Firebase Authentication (email link sign-in)
- **Serialization** — kotlinx.serialization
- **Async** — Kotlin Coroutines
- **Min SDK** — see `gradle.properties`
- **JDK** — 17

## Module layout

The app is organised as a multi-module Gradle project. Each feature lives behind a stable contract so modules can evolve independently.

| Module | Responsibility |
|---|---|
| `:app` | Application entry point, navigation graph, DI wiring |
| `:account` | Sign-in, account creation, session bootstrap |
| `:pool` | Pool browsing, joining, leaderboard |
| `:bet` | Match predictions and scoring |
| `:session` | Session storage and lifecycle |
| `:core` | Cross-cutting domain primitives |
| `:ui` | Shared composables, theming, design tokens |
| `:data:pool` · `:data:bet` | Repositories and remote data sources per feature |
| `:network:core` · `:network:ktor` | HTTP client abstractions and Ktor implementation |

## Building locally

```bash
./gradlew :app:assembleDebug
```

You'll need a `google-services.json` for Firebase and a `local.properties` with the SDK location. Build-time configuration (`urlBasePath`, `signInLinkUrlTemplate`, `joinPoolUrlTemplate`, `iosBundleId`) is read from `gradle.properties`.

## Release notes

See [`CHANGELOG.md`](CHANGELOG.md).

---

*Part of the [Fortuna](../README.md) project.*
