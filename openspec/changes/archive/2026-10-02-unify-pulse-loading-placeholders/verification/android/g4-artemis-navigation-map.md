# Fortuna Navigation Map & Exploration Log

## 1. Screen: My Pools (Home)
- **Route / How to reach**: Initial screen upon app launch / signed in.
- **Header Controls**:
  - Menu icon / User avatar: `Open menu` (opens side drawer)
  - Title: `My pools`
  - Action button / FAB: `Create pool` (green '+' icon, top right)
- **Pool List Items**:
  - Each item shows: Rank badge (e.g. 8, 1, 3), Pool Name, Points, Member count, Rank change status, and an Invite button (person-plus icon on the right).
  - Row 1: `Copa Mundial de la FIFA 2026, Rank 8, 589 points, 13 members, Rank unchanged` (Invite icon at right)
  - Row 2: `Copa Mundial de la FIFA prur, Rank 1, 41 points, 1 member, Rank unchanged`
  - Row 3: `Prueba, Rank 3, 4 points, 3 members, Rank unchanged`
### Invite Trigger Behavior:
- Tapping the invite icon (person-plus) on a pool row opens the system share sheet (`com.android.intentresolver`).
- Headline: "Sharing link"
- Content: Deep link / join URL for the pool, e.g., `https://tyche-588ce.web.app/pools/01KTPAWF326C43QE3VDK8FC2CS/join`
- Controls: "Copy link" button, suggested app share targets (Quick Share, Chrome, Drive, Messages).
- Action taken: System BACK pressed to return to 'My pools' without sending or selecting any target.

## 2. Screen: Pool Main Screen (Scores / Leaderboard)
- **Route / How to reach**: Tap any pool row from "My pools" (e.g., 'Copa Mundial de la FIFA 2026').
- **Top Bar Controls**:
  - `Open menu` (avatar button, top left, bounds [12,85][138,211]): Opens navigation drawer.
  - Title: `Scores`
  - Swap Pool button (two opposing arrows icon, top right, bounds [944,85][1070,211]): Navigates back / swaps pool.
- **Bottom Navigation Tabs**:
  - Tab 1: `Scores` (selected) - Leaderboard listing gamblers with ranks, names/emails, points, rank changes.
  - Tab 2: `Bets` (clickable) - Bet management tabs.
  - Tab 3: `History` (clickable) - Pool bet/event history.
- **Scores Content**:
  - Gambler leaderboard rows (e.g. Rank 1 El mono 676 pts ... Rank 8 felipearpa (You) 589 pts).

## 3. Component: Navigation Drawer (Pool Screen)
- **Route / How to reach**: Tap `Open menu` (avatar icon) from the pool screen.
- **Drawer Header / User Info**:
  - Avatar image
  - Username: `felipearpa`
  - Email: `felipearcila@gmail.com`
- **Drawer Items**:
  - Item 1: `Profile`
  - Pool Summary Item: `Playing now` badge, Pool Name: `Copa Mundial de la FIFA 2026`, Rank & Points: `8° · 589 points` (content-desc: `Playing now, Copa Mundial de la FIFA 2026, Rank 8, 589 points`)
  - Section Header: `POOL`
  - Item 2: `Invite` (person-plus icon)
  - Item 3 (Bottom): `Log out` (DO NOT TAP)
  - Note on "Manage gamblers": No "Manage gamblers" entry is present in this drawer for this pool.
- **Close Action**: Tap outside / scrim (`Close menu`, bounds [918,0][1080,2400]) or press BACK.

## 4. Screen: Pool Bets & History Tabs
- **Bets Tab**:
  - Route: Tap `Bets` in bottom navigation.
  - State: Empty state displaying an icon and text `Nothing to show`. No sub-tab selector visible when empty.
  - Rows: 0 rows (empty).
- **History Tab (Finished Bets)**:
  - Route: Tap `History` in bottom navigation.
  - State: Displays list of finished match bets grouped by date (e.g. 7/19/26, 7/18/26, 7/15/26, 7/14/26).
  - Rows present: Yes, multiple finished bet rows (e.g., España 0 vs Argentina 0, Francia 4 vs Inglaterra 6). Each row displays match time, teams, actual score, user's bet prediction, and points earned (+0, +2, etc.). Rows are clickable.

## 5. Screen: Match Screen (Score / Match Details)
- **Route / How to reach**: Tap any finished match bet row from the `History` tab.
- **Top Bar**:
  - In-app back arrow: `←` (bounds [12,85][138,211])
  - Screen title: `Score`
  - Pool icon (right)
- **Match Header**:
  - Teams: Left flag + `España`, score `0 - 0`, `Argentina` + right flag
  - Date & Time: `7/19/26, 2:00 PM`
- **Gamblers' Bets List**:
  - Displays each gambler's username/email, their predicted score (e.g., `3 - 2`, `5 - 0`), and points earned (e.g. `+0`, `+4`).
  - Rows are clickable (each row has `clickable=true`).

## 6. Screen: Gambler Timeline
- **Route / How to reach**: On the Match Screen (Score), tap any gambler row (e.g. `Santiago Arcila`).
- **Top Bar**:
  - In-app back arrow: `←` (bounds [12,85][138,211])
  - Screen title: `Timeline`
  - Pool icon (right)
- **Content**:
  - Gambler name header: `Santiago Arcila`
  - Timeline list: Chronological history of matches with match date/time, teams, actual scores, user's bet prediction, and points earned (e.g., +4, +10, +0).

- **Navigation verification**:
  - Tapping gambler row `Santiago Arcila` opened the `Timeline` screen for Santiago Arcila.
  - Tapping the in-app back arrow `←` on the `Timeline` screen successfully returned to the `Score` (Match Screen) with all gambler bets and match header intact.

## 7. Manage Gamblers Screen Check
- **Drawer check result**: Checked the navigation drawer thoroughly. The entries under `POOL` are only `Invite` (and `Log out` at bottom; `Profile` and pool card at top). There is no "Manage gamblers" entry present in the navigation drawer for this pool.

## 8. Screen: Create Pool from Layout (`Pool from layout`)
- **Route to reach**: From `My pools`, tap the green `+` FAB (`Create pool`) in the top right.
- **Top bar controls**:
  - In-app back arrow `←` (Button, bounds `[12,85][138,211]`)
  - Screen title: `Pool from layout` (`TextView`)
- **Main content / instructions**:
  - Section title: `Choose a template to create your pool`
- **Template / layout list**:
  - Template card 1:
    - Title: `Copa Mundial de la FIFA 2026`
    - Subtitle: `Starting 6/11/26`
    - Chevron `>` indicating selectable template
- **Safety check**: Per instruction, no template was tapped or submitted.

