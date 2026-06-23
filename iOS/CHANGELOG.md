# Changelog

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
