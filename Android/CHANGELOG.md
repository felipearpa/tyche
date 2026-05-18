# Changelog

## 1.6

- Edit your account username from the drawer via a bottom sheet
- Drawer redesigned with a sliding push animation, swipe-to-dismiss gestures, and an email-initials avatar in the header
- Dark mode support across the app, including the splash screen
- Edge-to-edge layout with transparent status and navigation bars
- Pending bet items animate smoothly between idle, editing, saving, and saved states
- Score column width stays steady so the row no longer jitters as you type
- Pool, gambler-score, and bet-timeline lists retry inline on load failures instead of reloading the whole screen
- Larger page size for smoother scrolling through long lists

## 1.5

- Delete a pool you created from the home drawer, with a confirmation prompt before every gambler and bet is removed
- Fix: corrected error message shown when pool creation fails

## 1.4

- Team flags shown next to every team name across match headers and bet items
- Pool joiner flow redesigned
- Loading screens refreshed
- Empty state placeholder when you have no pending or finished bets yet
- Friendlier error state when the pending bet list fails to load

## 1.3

- New Bet Timeline: tap a gambler from a match to see their full prediction history, with live, finished, and pending bets grouped by date
- Drawer redesigned with an account header and pool status; pool selection moved to the top navigation bar
- Quick "Home" action in the Bet Timeline and Match Bet views
- Email link sign-in screen redesigned with a verified email pill, clearer success messaging, and an edge-to-edge layout
- Predictions from other gamblers stay hidden until a match locks
- Pool tab selection is preserved across configuration changes
- Pull-to-refresh added to the match bet list
- Friendlier handling when access to a pool has been revoked

## 1.2

- Joining a pool now requires sign-in, with a clearer prompt when you are not signed in
- Tap a gambler in the score view to see their bets
- Tap a match in the pool view to see how everyone bet on it
- Pending bets that have already been locked can no longer be edited
- Fix: match dates are now correctly converted from UTC

## 1.1

First public release.

- Create and join prediction pools with friends and family
- Predict football match results and track standings live
- Tournament templates including World Cup 2026
- Email link sign-in via Firebase Authentication
- Spanish and English localization
