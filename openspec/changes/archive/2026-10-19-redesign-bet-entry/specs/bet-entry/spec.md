# Spec Delta

## Purpose

Lets a gambler place and change score predictions for a pool's upcoming matches directly in the Bets tab, with the fewest taps and an unmistakable status for every match.

## ADDED Requirements

### Requirement: Bets follows the reference card layout

The Bets tab on iOS and Android SHALL present pending matches as the reference canvas shows (`reference/bets-list.png`, `reference/bet-row-states.png`): a scrolling "Bets" heading with the pool name above it, a filter row, and matches grouped under date headers as cards. Each card SHALL show the kickoff time, a status pill at the trailing edge, both teams' flags and names, and a score area with one score box per team. The tab SHALL keep the app's existing dark canvases, typography, flag assets, spacing, and the platform's native tab bar. Example teams, dates, scores, and device chrome in the references SHALL NOT become production behavior.

#### Scenario: Opening the Bets tab
- **GIVEN** a gambler opens a pool with upcoming matches
- **WHEN** the Bets tab loads
- **THEN** matches appear as cards under their kickoff date headers in kickoff order
- **AND** each card shows both flags, team names, the kickoff time, a status pill, and two score boxes

#### Scenario: Large text
- **GIVEN** the largest accessibility text size is active
- **WHEN** a card renders
- **THEN** team names wrap or truncate without overlapping the score area
- **AND** every control remains reachable by scrolling

### Requirement: Day headers read as relative days

Each date header SHALL name the kickoff day relative to the device's current date and time zone: "Today", "Tomorrow", or "Yesterday" for those days; the weekday name (for example "Wednesday") for other days within the next six days; and otherwise a short named date with weekday, day, and month (for example "Wed, Oct 21"), adding the year only when it differs from the current year. Headers SHALL use the platform's localized date formats and the device's locale, in English, Spanish, and Spain Spanish. Card kickoff times SHALL stay clock times in the device's 12- or 24-hour setting. Headers SHALL update when the day changes while the tab is shown, without a reload.

#### Scenario: Matches today and later this week
- **GIVEN** today is Sunday, October 18 and matches kick off today, tomorrow, on Wednesday, and on October 30
- **WHEN** the Bets tab renders
- **THEN** the headers read "Today", "Tomorrow", "Wednesday", and "Fri, Oct 30"
- **AND** in Spanish they read "Hoy", "Mañana", "Miércoles", and the localized short date

#### Scenario: A match in another year
- **GIVEN** a match kicks off in the next calendar year
- **WHEN** its header renders
- **THEN** the short named date includes the year

#### Scenario: Midnight passes
- **GIVEN** the tab is open with a "Tomorrow" header
- **WHEN** the device's date changes at midnight
- **THEN** that header reads "Today" without a refresh

### Requirement: Scores are entered directly without an edit mode

Each open match card SHALL expose its two score boxes, a `+` and `−` stepper for each team. There SHALL be no Edit, Save, or Cancel control. Tapping a score box SHALL open the numeric keyboard with its current value selected; typing SHALL accept digits only and store a non-negative whole number with leading zeros removed. After the first digit is typed into the home box, focus SHALL move to the away box; the gambler MAY return to either box to change it. A stepper SHALL change its team's score by one and SHALL NOT go below zero. On a card without a prediction, `+` SHALL set the tapped team to 1 and the other team to 0, and `−` SHALL set both teams to 0. On both platforms, focusing a score box SHALL bring the whole card, including both boxes and their steppers, into the visible area above the keyboard.

#### Scenario: Typing a prediction
- **GIVEN** a card with no prediction
- **WHEN** the gambler taps the home score box, types 2, and types 1
- **THEN** the home box shows 2, focus moved to the away box after the first digit, and the away box shows 1
- **AND** the prediction 2–1 is saved automatically

#### Scenario: Adjusting with steppers
- **GIVEN** a card with the saved prediction 2–1
- **WHEN** the gambler taps `+` beside the away team
- **THEN** the away box shows 2 and the prediction 2–2 is saved automatically
- **AND** tapping `−` beside a team showing 0 leaves it at 0

#### Scenario: First tap on an empty card
- **GIVEN** a card with no prediction
- **WHEN** the gambler taps `+` beside the away team
- **THEN** the card shows 0–1 and that prediction is saved automatically

### Requirement: Predictions save automatically

Whenever both scores of a card hold a value that differs from the last saved prediction, the client SHALL submit the prediction after a short debounce of about 0.7 seconds without a confirmation control. Changes made during the debounce SHALL coalesce into one submission of the latest values. While a submission is in flight, the status pill SHALL be replaced by the shared ball spinner; on success the pill SHALL read Saved with a check icon and the boxes SHALL show the saved values. A submission in flight SHALL NOT block further edits; a later edit SHALL queue a submission of the newest values after the current one completes.

#### Scenario: Rapid stepper taps
- **GIVEN** a card with the saved prediction 0–0
- **WHEN** the gambler taps `+` beside the home team three times within half a second
- **THEN** one submission is made with 3–0
- **AND** the card shows the spinner once and then Saved

#### Scenario: Edit while saving
- **GIVEN** a submission of 2–1 is in flight
- **WHEN** the gambler changes the away score to 2
- **THEN** 2–2 is submitted after the first submission completes
- **AND** the card ends with Saved showing 2–2

### Requirement: Empty and incomplete predictions are explicit

A card with no saved prediction and no draft SHALL show a dashed box with a question mark in each score position and a No bet pill. A card where exactly one score has a value SHALL show that value in its box, an amber dashed box on the missing side and an amber Incomplete pill, with no hint line below the scores; the missing box SHALL tell assistive technology that the named team's score is needed to save. An incomplete draft SHALL NOT be submitted and the missing score SHALL NOT be filled in with zero on the gambler's behalf. The draft SHALL be retained while the tab is shown, including across paging updates and keyboard dismissal, and SHALL be discarded when the gambler leaves the pool. Clearing both scores of a card that has a saved prediction SHALL NOT submit anything; when focus leaves the card, its boxes SHALL show the saved prediction again. Clearing one score leaves the card Incomplete.

#### Scenario: One side typed
- **GIVEN** a card with no prediction
- **WHEN** the gambler types 2 in the home box and dismisses the keyboard
- **THEN** the card shows 2, an amber dashed away box, and the Incomplete pill, and its height does not change
- **AND** the away box is announced as needing [away team]'s score to save
- **AND** no submission is made

#### Scenario: Completing the draft later
- **GIVEN** a card in the Incomplete state
- **WHEN** the gambler scrolls away, more pages load, and the gambler returns and taps `+` beside the away team
- **THEN** the card shows 2–1 and submits that prediction

#### Scenario: Clearing a saved prediction
- **GIVEN** a card with the saved prediction 2–1
- **WHEN** the gambler clears both boxes and dismisses the keyboard
- **THEN** the boxes show 2–1 again and the pill reads Saved
- **AND** no submission is made

### Requirement: A failed save keeps the draft and offers Retry

When a submission fails for a reason other than the lock, the card SHALL keep the attempted scores in its boxes, show a Not saved pill with an error icon, and show an inline banner reading "Couldn't save. Your bet is kept here." with a Retry control. Retry SHALL resubmit the same scores. Editing a score on a failed card SHALL clear the banner and start a new automatic save. When the submission is rejected because predictions are locked, the card SHALL become locked as it does today.

#### Scenario: Network failure
- **GIVEN** the device is offline
- **WHEN** a prediction submission fails
- **THEN** the card shows Not saved, the banner, and Retry, with the attempted scores still in the boxes
- **AND** Retry submits the same scores and the card returns to the spinner and then Saved when the request succeeds

#### Scenario: Forbidden save
- **GIVEN** the match locked while the card was still open
- **WHEN** the submission is rejected as forbidden
- **THEN** the card becomes locked and shows the last saved prediction, or a muted dash in each box if none was saved

### Requirement: Locked matches are read-only and approaching locks are announced

A locked match card SHALL show a Locked pill with a lock icon and the saved prediction in muted read-only boxes, or a muted dash (–) in each box when none exists, with no steppers, chips, keyboard access, or banner. While an open match is within sixty minutes of its lock time, the card SHALL show an amber note "Locks in N min" beside the kickoff time, updated at least once a minute while visible. A card whose lock time passes while the tab is shown SHALL become locked on the next refresh or on the next rejected submission.

#### Scenario: Locked card
- **GIVEN** a match locked ten minutes before kickoff
- **WHEN** its card renders
- **THEN** it shows Locked and the saved scores in muted boxes
- **AND** tapping the boxes does nothing

#### Scenario: Locked card without a prediction
- **GIVEN** a match locked before the gambler saved a prediction
- **WHEN** its card renders
- **THEN** it shows Locked and a muted dash in each score box, not the dashed question-mark boxes of an open card

#### Scenario: Lock approaching
- **GIVEN** an open match whose lock time is eight minutes away
- **WHEN** its card renders
- **THEN** it shows "Locks in 8 min" in amber beside the kickoff time
- **AND** the number decreases as minutes pass

### Requirement: The filter row narrows the list

Above the date headers the tab SHALL show three filter chips: All, No bet, and Placed, each with a count. All SHALL show every loaded match; No bet SHALL show open matches without a saved prediction, including Incomplete ones; Placed SHALL show matches with a saved prediction, including locked ones. Counts SHALL reflect the matches loaded so far and update as pages load and predictions change. While further pages remain to load, each count SHALL carry a trailing plus sign (for example "No bet 2+"), and assistive technology SHALL hear it as "at least" that number; the plus sign SHALL disappear once the last page has loaded. Filtering SHALL NOT discard drafts or in-flight submissions of hidden cards. Selecting a filter that matches no loaded match SHALL show a short message for that filter instead of a blank list: No bet SHALL say there is nothing left to bet on, and Placed SHALL say no prediction has been placed yet. That message SHALL appear only once no further page remains to load; while more pages exist or one is loading, the tab SHALL keep loading pages and show the loading placeholder instead. Hiding a card through a filter SHALL NOT stop further pages from loading.

#### Scenario: Jumping to missing bets
- **GIVEN** six loaded matches of which two have no prediction
- **WHEN** the gambler taps No bet
- **THEN** only those two cards and their date headers remain, and the chip reads "No bet 2"
- **AND** saving one of them drops it from the list and the count reads 1

#### Scenario: Filter with nothing left
- **GIVEN** every loaded match has a prediction
- **WHEN** the gambler taps No bet
- **THEN** a short message explains there is nothing left to bet on

#### Scenario: Missing bets only on a later page
- **GIVEN** every match on the first loaded page has a prediction and a later page holds a match without one
- **WHEN** the gambler taps No bet
- **THEN** the tab loads the next page instead of showing the empty message
- **AND** the match without a prediction appears and the chip's count includes it

#### Scenario: Counts while more pages remain
- **GIVEN** the first page is loaded and more pages remain
- **WHEN** the filter row renders
- **THEN** each chip shows its loaded count followed by a plus sign, such as "No bet 2+"
- **AND** once the last page has loaded the chips show plain counts

#### Scenario: Placed filter with nothing placed
- **GIVEN** no loaded match has a saved prediction
- **WHEN** the gambler taps Placed
- **THEN** a short message says no bets have been placed yet

### Requirement: State changes animate without layout shifts

Every change of a card's state (No bet, Incomplete, Saving, Saved, Not saved, Locked), of a score box's appearance, and of the presence of the failure banner or the lock note SHALL be animated with the platform's standard short transitions rather than switching instantly. The status slot at the card's trailing edge SHALL keep one fixed height in every state. The status SHALL be one pill anchored to the trailing edge: No bet and Incomplete show text only, Saved, Not saved, and Locked show their check, error, or lock icon with text, and Saving shows the ball spinner. When the state changes, the pill's content SHALL crossfade while its width animates from the old size to the new one, with its trailing edge fixed, as a single change. The status slot SHALL keep its height, and the score boxes and stepper columns SHALL keep their size across states, so a state change SHALL NOT move the card's teams, scores, or neighbouring cards. A card's height SHALL NOT change when its state changes, with two exceptions that animate the card's height instead of jumping: the match becoming locked, which removes the steppers, and the failure banner appearing or leaving. When the system Reduce Motion setting is on, transitions SHALL reduce to fades or instant changes while the fixed sizes are kept; the status pill SHALL then crossfade between states with its width changing instantly.

#### Scenario: Pill becomes spinner and back
- **GIVEN** a Saved card
- **WHEN** the gambler taps `+` and the save runs and completes
- **THEN** the Saved pill crossfades into the spinning ball while its width eases to the ball's size, and then crossfades into the Saved pill while its width eases back
- **AND** the pill's trailing edge and the status slot's height stay fixed
- **AND** the time, flags, teams, and scores do not move at any point

#### Scenario: A first bet keeps the card's size
- **GIVEN** a No bet card
- **WHEN** the gambler taps `+`, the card becomes Incomplete or complete, and the save runs and completes
- **THEN** the card's height stays the same throughout
- **AND** the cards above and below do not move

#### Scenario: Save rejected because the match locked
- **GIVEN** an open card whose match has locked on the server, with the saved prediction 1–0 or none
- **WHEN** the gambler changes it to 2–1 and the submission is rejected as forbidden
- **THEN** the boxes animate from 2–1 back to 1–0, or to muted dashes when none was saved
- **AND** the spinner crossfades into the Locked pill while its width eases to the new size and the steppers and any failure banner animate out while the card's height eases to the locked size
- **AND** the change to Locked is announced to assistive technology
- **AND** with Reduce Motion on, the same changes happen as fades only

#### Scenario: Reduce Motion is on
- **GIVEN** Reduce Motion or a zero animator scale is set on the device
- **WHEN** a card changes state
- **THEN** the change happens without movement animations
- **AND** the status slot, boxes, and steppers keep the same sizes as with animations on

### Requirement: Entry is accessible and localized

Each score box SHALL carry an accessibility label naming its team ("[team] goals"), each stepper SHALL be labelled "One more goal for [team]" or "One less goal for [team]", and a box whose score is missing on an Incomplete card SHALL say that team's score is needed to save. The status pill text, including the spinner's Saving state, SHALL be exposed to assistive technology, and a save completing or failing SHALL be announced. Steppers, score boxes, Retry, and filter chips SHALL have touch targets of at least 44 points on iOS and 48 dp on Android. All copy SHALL be provided in English, Spanish, and Spain Spanish using the existing resource mechanisms.

#### Scenario: Screen reader places a bet
- **GIVEN** VoiceOver or TalkBack is active
- **WHEN** the gambler focuses the home stepper and double-taps it on an empty card
- **THEN** the control announces "One more goal for [home team]"
- **AND** the card's status change to Saved is announced after the save completes

### Requirement: Loading cards reuse the production card

While the first page or a subsequent page is loading, the tab SHALL render the production match card populated with a placeholder model and the shared pulse loading-placeholder treatment selected through the card's `isPlaceholder` parameter. Placeholder cards SHALL expose no steppers, chips, keyboard, or accessibility content as real data.

#### Scenario: First page loading
- **GIVEN** the Bets tab opens before the first page arrives
- **WHEN** placeholders are displayed
- **THEN** they use the same card layout with pulsing shapes in place of flags, names, time, pill, and scores
- **AND** none of them is actionable or announced as a match

### Requirement: App-rendered icons derive from one canonical source

The check, lock, error, add, and remove glyphs used by the Bets tab SHALL be Material Symbols, and iOS and Android SHALL render them from assets derived from the same committed canonical vector source. The saving indicator SHALL be the app's existing shared ball spinner. OS-rendered controls, including the native tab bar and keyboard, are excluded.

#### Scenario: Status glyphs on both platforms
- **GIVEN** a Saved card and a Locked card
- **WHEN** they render on iOS and on Android
- **THEN** both platforms draw the same check and lock geometry
- **AND** the saving state spins the shared ball on both
