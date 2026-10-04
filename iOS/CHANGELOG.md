# Changelog

## 2.0.0

- Add a profile picture: pick a photo or take one, crop it to fit, and it appears in the menu button wherever your account shows
- Redesigned pool leaderboard: every player shows their photo or a colored initial next to a clearer rank, trend, and score, and your own row is highlighted as You
- Redesigned History: your total points in the pool sit at the top, and each match shows its final score up front, with your bet, the points it earned, and the match date right in the row
- Redesigned Timeline: another player's photo and name sit at the top with their total points in the pool, and each match shows its score, their bet, the points it earned, and the date right in the row, with Points pending until a match is scored
- New Profile screen, reached from the drawer, where you can change your photo and edit your username in one place
- A live preview shows how you will appear on the leaderboard while you type a new username, with a character counter, and Save turns on only when the name actually changes
- A new photo shows up everywhere in the app right away, and a username changed on another device catches up on its own
- Your pool list now shows your points and how many members each pool has, so you can compare pools at a glance
- Your ranking trend now appears right under your position in the pool list
- Your pool list now reads as one clear line with VoiceOver, giving each pool's name, your rank, points, members, and trend together, and you can open the pool or send an invite without leaving the row
- Counts now read correctly when there is only one of something, so a pool with a single member says "1 member" and a one-place move says "Up 1 place"
- A refreshed look for Liquid Glass on iOS 26 and later: navigation bars, tabs, and sheets use the system glass, while text fields and the buttons inside forms and lists stay solid and easy to read
- Text on green buttons and highlights is easier to read, with a darker brand green in light mode and dark labels in dark mode
- Creating a pool is easier to find: a larger green button with a plus sign, announced by VoiceOver as Create pool
- Scores, Bets, and History each shrink their own title as you scroll, and their rows pass beneath the navigation and tab bars
- Brand-new app icon with a Liquid Glass look
- Refreshed splash screen logo
- Loading looks the same everywhere: pools, leaderboards, bets, history, match details, and the menu show the real rows with a gentle pulse while they load, which stays still when Reduce Motion is on
- Pulling to refresh or returning to a match keeps what you were reading on screen instead of flashing back to a loading state
- When a list can't load, a Retry button sits next to the message, alongside pull to refresh, and if a points total can't load, a small retry button beside it reloads just the total
- Redesigned side menu with a clearer account header, grouped pool details and actions, and a separate Sign out area
- Swipe horizontally on the pool list or pool home to open the menu; drag it closed or tap the dimmed screen to dismiss it
- Menu animations follow your finger smoothly, even when you change direction, and respect Reduce Motion
- The menu adapts to wide windows, landscape, large text, and right-to-left layouts, with scrolling to keep every action reachable
- VoiceOver focus stays inside the open menu and returns to the menu button when it closes, with an accessibility escape gesture to dismiss it
- Fix: navigation titles and toolbar buttons stay aligned as the menu moves and the device rotates

## 1.7.2

- The Save button on a pending bet stays disabled until both scores are valid, so you can't submit an incomplete prediction by mistake

## 1.7.1

- The score field you are editing keeps focus while the bet list refreshes in the background, so paging activity no longer kicks you out mid-entry
- The first score field is focused automatically when you start editing a pending bet

## 1.7.0

- Pool owners can now manage their gamblers: open the new Gamblers screen from the drawer to see everyone in the pool
- Swipe a gambler to remove them, with a confirmation prompt before they leave the pool
- The pool owner is protected and can't be removed
- Empty state when no one else has joined yet, with a quick action to invite gamblers
- Friendlier error handling when a removal fails

## 1.6

- Edit your account username from the drawer via a modal sheet
- Drawer redesigned with a sliding push animation, swipe-to-dismiss gestures, and an email-initials avatar in the header
- Liquid Glass styling extended to text fields, dialogs, and the drawer, including disabled states
- Pending bet items animate smoothly between idle, editing, saving, and saved states, with matched-geometry transitions on the score field
- Score column width stays steady so the row no longer jitters as you type
- Pool, gambler-score, and bet-timeline lists retry inline on load failures instead of reloading the whole screen
- Larger page size for smoother scrolling through long lists

## 1.5

- Delete a pool you created from the home drawer, with a confirmation prompt before every gambler and bet is removed

## 1.4

- Team flags shown next to every team name across match headers and bet items
- Pool joiner flow redesigned
- Liquid Glass design refresh
- Empty state placeholder when you have no pending or finished bets yet

## 1.3

- New Bet Timeline: tap a gambler from a match to see their full prediction history, with live, finished, and pending bets grouped by date
- Drawer redesigned with an account header and pool status; pool selection moved to the top navigation bar
- Quick "Home" action in the Bet Timeline and Match Bet views
- Email link sign-in screen redesigned with a verified email pill, clearer success messaging, and an edge-to-edge layout
- Predictions from other gamblers stay hidden until a match locks
- Pool tab selection is preserved when rotating the device
- Friendlier handling when access to a pool has been revoked

## 1.2

- Tap a gambler in the score view to see their bets
- Tap a match in the pool view to see how everyone bet on it
- Pending bets that have already been locked can no longer be edited

## 1.1

First public release.

- Create and join prediction pools with friends and family
- Predict football match results and track standings live
- Tournament templates including World Cup 2026
- Email link sign-in via Firebase Authentication
- Spanish and English localization
