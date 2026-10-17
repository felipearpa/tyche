# Verification

## Android — tasks 1.1–3.8, accepted gaps (2026-10-17)

- 2.3: paging past 50 not verifiable (members endpoint returns all members in one response, no Limit); a removing row surviving scroll-away was not observed in flight (requests finished ~2 s, before the scroll), only its outcome. Accepted without these checks by the user on 2026-10-17.
- 3.3: TalkBack pass not driven (emulator console touches don't move TalkBack focus); owner row has no swipe or Remove action in code and visually. Accepted without the TalkBack check by the user on 2026-10-17.
- 3.8: fast flick shorter than 56 dp and TalkBack not checked. Accepted without these checks by the user on 2026-10-17.

## Android — task 4.1 (2026-10-17)

Setup: emulator-5554 (AVD Pixel_10_Pro_XL, API 37, light mode), Fortuna 2.0.0 prodDebug, installed 11:33, newer than every changed source file, signed in as felipearpa against prod. Pools: "QA gamblers" (`01M55B9QZDA7725BDEB0PWA1EP`) and "QA solo" (`01M55EBNFB5DZNX85NSA4BAE23`, created on the emulator from the layout "Fortuna QA — kickoff groups", owner only). Locales were switched with `cmd locale set-device-locale`; failures were staged with airplane mode. Screenshots are in `verification/`.

Gamblers removed from QA gamblers: btester05, btester06, btester07 (57 → 54 members).

### Build checks

- `./gradlew :app:testProdDebugUnitTest :app:lintProdDebug` (from `Android/`): BUILD SUCCESSFUL. 60 tests, 0 failures. Lint: 0 errors, 45 warnings, none in the `pool` or `ui` modules or in changed files.

### Scenarios

| Scenario | Language | Result | Screenshots |
|---|---|---|---|
| Owner opens Gamblers from the drawer: "Gamblers" heading and count line above the rows, top bar shows only Back | en | Pass | `en-01-drawer.png`, `en-02-gamblers-opened.png` |
| Same | es-CO ("Jugadores", "54 jugadores en esta polla") | Pass | `es-CO-01-drawer.png`, `es-CO-03-gamblers-loaded.png` |
| Same | es-ES ("54 jugadores en esta quiniela") | Pass | `es-ES-02-drawer.png`, `es-ES-03-gamblers-opened.png` |
| Back returns to the pool home | en | Pass | `en-14-back-to-pool-home.png` |
| Heading scrolls with the rows | en | Pass (heading scrolled off while the owner row was in view) | `en-13-owner-row-swipe-noop.png` |
| Swipe past the threshold: person-remove glyph and "Remove" on the error background, dialog opens, row returns to rest | en | Pass | `en-03-swipe-past-threshold.png`, `en-04-confirm-dialog.png` |
| Same, with "Eliminar" label and "¿Eliminar a @btester08 de esta polla?" | es-CO | Pass | `es-CO-05-swipe-background.png`, `es-CO-04-confirm-dialog.png` |
| Same, with "¿Eliminar a @btester08 de esta quiniela?" | es-ES | Pass | `es-ES-04-confirm-dialog.png` |
| Swipe released early (about 33 dp): row settles, no dialog | en | Pass (UI dump showed no dialog) | none |
| Owner cancels: Cancel leaves the row unchanged | en, es-CO | Pass | `en-05-after-cancel.png` |
| Owner dismisses the dialog by tapping outside it: row unchanged | es-ES | Pass (UI dump) | none |
| Owner confirms: dialog closes, row dims with the ball spinner in place of the avatar | en (btester05) | Pass | `en-06-removing.png` |
| Removal succeeds: row leaves, count 57 → 56 | en (btester05) | Pass | `en-07-removed.png` |
| Removed gamblers stay gone after pull-to-refresh, count 54 from the server | en | Pass | `en-12-after-refresh.png` |
| Two failures: each row shows its own "Couldn't remove" with Retry and ✕, count unchanged, no banner | en (btester06, btester07) | Pass | `en-08-two-failures.png` |
| Same ("No se pudo eliminar", "Reintentar") | es-CO (btester08, btester09) | Pass | `es-CO-06-two-failures.png` |
| Single failure | es-ES (btester08) | Pass | `es-ES-05-failure.png` |
| Retry from the row: no confirmation dialog, row enters the removing state | en (btester06, btester07) | Pass | `en-10-two-concurrent.png` |
| Two removals at once (Retry tapped on both rows back to back, online) | en (btester06, btester07) | Partly observed: the captured frame shows btester06 removing and btester07 with its failure line and Retry gone but not yet dimmed, most likely the first frame of its animated transition. Both requests completed and the count went 56 → 54. | `en-10-two-concurrent.png`, `en-11-both-removed.png` |
| Dismiss a failure: row returns to idle with its email, count unchanged | es-CO, es-ES | Pass (offline, so no request could be sent; the count stayed 54) | `es-CO-07-dismissed.png`, `es-ES-06-dismissed.png` |
| Swipe a failed row: the confirmation dialog opens (cancelled, so the failure stayed) | es-CO (btester09) | Pass | `es-CO-08-swipe-failed-row-dialog.png` |
| Pull to refresh with a failed row: row returns to idle | es-CO (btester09) | Pass | `es-CO-09-refresh-clears-failure.png` |
| Owner's row: current-user highlight, "You" beside the username, "Owner" pill, natural position, swipe does nothing | en | Pass | `en-13-owner-row-swipe-noop.png` |
| Same ("Tú", "Dueño") | es-CO | Pass | `es-CO-10-owner-row.png` |
| Only the owner: owner row followed by "No other gamblers yet", singular count line | en ("1 gambler in this pool") | Pass | `en-17-only-owner-empty.png` |
| Same ("Aún no hay otros jugadores", "1 jugador en esta polla") | es-CO | Pass | `es-CO-11-only-owner-empty.png` |
| Same ("1 jugador en esta quiniela") | es-ES | Pass | `es-ES-01-only-owner-empty.png` |
| First load: eight placeholder rows | en | Pass | `en-15-first-load-placeholders.png` |
| First load: eight placeholder rows and a placeholder count line | es-CO | Pass | `es-CO-02-first-load-placeholders.png` |
| First load placeholders | es-ES | Not caught: the list had loaded before the first capture. The placeholders show no text, so this adds nothing language-specific beyond en and es-CO. | none |
| Load error: error with Retry, no empty state, count line omitted | en (opened in airplane mode) | Pass | `en-16-offline-open.png` |
| Count request fails while the list loads: heading without a count line, list usable | en | Pass (UI dump after Retry with the network back; the count is re-requested only on pull-to-refresh, which then showed "54 gamblers in this pool") | none |

### Not verified

- Two removals visibly in the removing state in the same frame: requests finish in about 2 s, and the only capture caught the second row at the start of its transition. Both outcomes were observed.
- Error haptic and the TalkBack announcement on failure: no haptic feedback on the emulator, and TalkBack was not driven (see 3.3 above). The rows expose "Couldn't remove @username" as their content description (UI dump).
- First-load placeholders in es-ES: see the table.

### Observations outside this change

- After a paging Retry from the load error, the pull-to-refresh indicator shows instead of placeholder rows. This is existing paging behavior.
- When the walk started, the drawer badge in `en-01-drawer.png` read "1" for QA gamblers. That pool home had been opened before the fake gamblers were seeded; after the pool was reopened, the badge read 54 (`es-ES-02-drawer.png`).
