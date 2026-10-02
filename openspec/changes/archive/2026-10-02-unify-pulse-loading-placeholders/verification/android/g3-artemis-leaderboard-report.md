# Fortuna Leaderboard Exploration Report

## 1. Pool Launch & Initial State
- Launch Timestamp: T+01:53 (6:45) 
- Pool Selected: Copa Mundial de la FIFA 2026
- Error with Retry Encountered: None

## 2. Placeholder Rows Analysis (Loading State)
- Number of Placeholder Rows Visible: 9
- Hierarchy Attributes of Placeholder Rows:
  - Class: android.view.View
  - Clickable: false
  - Focusable: false
  - Long-clickable: false
  - Text: "" (empty)
  - Content-desc: "" (empty)
  - Any Child Node with Text/Content-desc: No (all child nodes have class android.view.View, text="", content-desc="") 
- Placeholder Row Tap Test:
  - Target: Middle of first placeholder row ([500, 150])
  - Pre-tap Evidence: Screencap saved to /sdcard/before_placeholder_tap.png
  - Post-tap Evidence: Screencap saved to /sdcard/after_placeholder_tap.png, screen remained unchanged
  - Observed Effect (Navigation / Ripple / New Screen / None): None (no navigation, no pressed ripple, no new screen; row is non-clickable)

## 3. Loading Duration & Polling Log
- Pool Tap Timestamp: T+02:02 (6:45)
- Real Rows Appearance Timestamp: T+06:51 (6:50)
- Approximate Total Loading Duration: ~4 minutes 49 seconds (~289 seconds)
- Polling Records (Interval ~30s):
  - Check 1 (T+02:49, ~47s elapsed): Placeholders still visible, no names yet.
  - Check 2 (T+03:39, ~1m37s elapsed): Placeholders still visible, no names yet.
  - Check 3 (T+04:17, ~2m15s elapsed): Placeholders still visible, no names yet.
  - Check 4 (T+04:58, ~2m56s elapsed): Placeholders still visible, no names yet.
  - Check 5 (T+06:51, ~4m49s elapsed): Real gambler rows successfully loaded and displayed.

## 4. Loaded Leaderboard Hierarchy Analysis
- Other Gambler Row (Rank 1, El mono):
  - Container Clickable: true
  - Container Focusable: true
  - Child Content-desc: "Rank 1, El mono, 676 points, Rank unchanged"
- Signed-in User Row ("You", Rank 8, felipearpa):
  - Container Clickable: false
  - Container Focusable: false
  - Child Content-desc: "Rank 8, felipearpa, You, 589 points, Rank unchanged" 

## 5. Row Interaction & Navigation Verification
- Other Gambler Row Tap:
  - Gambler Name/Row Selected: El mono (Rank 1)
  - Timeline Screen Opened: Verified (Title "Timeline", Gambler "El mono", match scores and point history displayed)
  - In-App Back Arrow Used: Verified (tapped top-left in-app back arrow, candidate 51)
  - Successfully Returned to Scores Tab: Verified (Scores screen active with leaderboard 1-9) 
- Signed-in User Row ("You") Tap:
  - Observed Effect (Expected: Nothing happens): Verified. Tapping the signed-in user's own row (Rank 8, felipearpa, You) resulted in no navigation, no ripple, and no screen change. The Scores tab remained unchanged, matching the hierarchy attributes where container clickable=false and focusable=false.

