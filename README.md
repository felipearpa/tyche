# Fortuna

### Predict. Compete. Win the bragging rights.

**Fortuna turns every match into a competition between friends.** Call the scores, climb the leaderboard, and prove once and for all who actually knows football. The classic *polla* (Colombia) and *quiniela* (Spain) experience — now in your pocket, beautifully native on iOS and Android.

> *This repository is the home of Fortuna, developed under the codename **Tyche** — the Greek goddess of fortune, twin to her Roman counterpart.*

---

## Get Fortuna

Scan a code with your phone camera, or tap it to open the store.

<table>
  <tr>
    <td align="center" valign="middle" width="50%">
      <a href="https://apps.apple.com/app/fortuna/id6762291676">
        <img src="ios-store/app-store-qr.svg" width="180" height="180" alt="App Store QR" />
      </a>
    </td>
    <td align="center" valign="middle" width="50%">
      <a href="https://play.google.com/store/apps/details?id=com.felipearpa.fortuna">
        <img src="android-store/play-store-qr.svg" width="180" height="180" alt="Google Play QR" />
      </a>
    </td>
  </tr>
  <tr>
    <td align="center" valign="middle"><strong>iOS</strong> · iPhone &amp; iPad</td>
    <td align="center" valign="middle"><strong>Android</strong> · Phone &amp; Tablet</td>
  </tr>
</table>

Or visit the landing page at **[fortuna.felipearpa.com](https://fortuna.felipearpa.com)**.

---

## Why Fortuna

- **Built for friends, not strangers.** Spin up a private pool in seconds, invite your group, and let the trash talk begin.
- **Live standings, real stakes (the social kind).** Track points the moment matches end. No spreadsheets. No "wait, who's winning?"
- **Tournament-ready out of the box.** Templates for the biggest events — including **World Cup 2026** — so you're playing in under a minute.
- **Fluent in your language.** Full English and Spanish localization, including regional Spanish variants.
- **Truly native.** Swift on iOS, Kotlin on Android. Fast, polished, and at home on every device.

## How a round works

1. **Create a pool** — pick a tournament template or build your own.
2. **Invite your crew** — a single link, no friction.
3. **Lock in your predictions** — call the scores before kickoff.
4. **Watch the leaderboard heat up** — points settle automatically as matches finish.
5. **Take the crown.** (Or buy the next round. Loser's choice.)

## Under the hood

Fortuna is engineered for speed and built to scale.

- **Backend** — F# on AWS Lambda with single-table DynamoDB and SQS FIFO for ordered score processing
- **iOS** — Swift / SwiftUI
- **Android** — Kotlin / Jetpack Compose
- **Hosting** — Firebase Hosting (privacy policy)

## Repository layout

| Path | Description |
|---|---|
| `Amazon.Lambda/` | F# backend services |
| `iOS/` | Swift iOS app |
| `Android/` | Kotlin Android app |
| `Firebase.Hosting/` | Static site (privacy policy) |
| `Scripting/` | Operational scripts |
| `android-store/` · `ios-store/` | Store listing assets |

## Release notes

- iOS — see [`iOS/CHANGELOG.md`](iOS/CHANGELOG.md)
- Android — see [`Android/CHANGELOG.md`](Android/CHANGELOG.md)
- Backend — see [`Amazon.Lambda/CHANGELOG.md`](Amazon.Lambda/CHANGELOG.md)
- Hosting — see [`Firebase.Hosting/CHANGELOG.md`](Firebase.Hosting/CHANGELOG.md)

---

*Fortuna — bring your luck, but trust your call.*
