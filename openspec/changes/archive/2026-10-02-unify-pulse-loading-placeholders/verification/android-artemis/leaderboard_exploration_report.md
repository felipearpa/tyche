# Leaderboard Exploration Report

## 1. Placeholder Rows (Loading State)
- **Visible Duration**: Approximately 1.0 second (visible from ~53.0s to 54.0s during screen recording transition).
- **Visual Appearance**: Stacked horizontal skeleton placeholder rows separated by subtle horizontal divider lines. Each placeholder row mimics the final layout with light gray rounded placeholder blocks:
  - Leftmost rounded rectangular badge block with a smaller tag block below it (rank indicator placeholder).
  - Circular avatar placeholder.
  - Elongated horizontal rectangular bar (username/identifier placeholder).
  - Right-aligned rectangular score placeholder block.
  - Continuous animated horizontal shimmer gradient sweep across all placeholder shapes until data loads.
- **UI Hierarchy Attributes**:
  - The placeholder state is transient (~1 second shimmer animation in Jetpack Compose) and transitions to live content before static accessibility inspection. In the UI hierarchy, Compose loading skeletons do not expose interactive accessibility nodes (clickable="false", focusable="false").

## 2. Loaded Rows (UI Hierarchy Attributes)
- **Another Gambler's Row (e.g., Rank 1, El mono)**:
  - Container Node:
    - class: `android.view.View`
    - clickable: `true`
    - focusable: `true`
    - text: `""`
    - content-desc: `""`
  - Child Content Node:
    - class: `android.view.View`
    - clickable: `false`
    - focusable: `false`
    - text: `""`
    - content-desc: `"Rank 1, El mono, 676 points, Rank unchanged"`
- **Signed-in User's Row (Rank 8, felipearpa, marked "You")**:
  - Container Node:
    - class: `android.view.View`
    - clickable: `false`
    - focusable: `false`
    - text: `""`
    - content-desc: `""`
  - Child Content Node:
    - class: `android.view.View`
    - clickable: `false`
    - focusable: `false`
    - text: `""`
    - content-desc: `"Rank 8, felipearpa, You, 589 points, Rank unchanged"`

## 3. Interaction Test Results
- **Step 5 (Tapping Another Gambler's Row)**:
  - **Result**: Tapping the row for "El mono" successfully opened that gambler's bet timeline screen ("Timeline" header, showing match bets such as España vs Argentina, Francia vs Inglaterra with predicted scores and points).
  - Returning via the in-app back arrow button at the top-left cleanly returned to the Scores leaderboard screen without exiting the app.
- **Step 6 (Tapping Signed-in User's Own Row)**:
  - **Result**: Tapping the row for "felipearpa, You" triggered no navigation or UI change; the screen remained on the Scores leaderboard screen as expected, confirming the row container is non-clickable (`clickable="false"`).