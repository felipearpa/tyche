# Match bets specification delta

## Purpose

Let Fortuna participants inspect predictions and match awards for a selected match, with the current pool leader first, while preserving prediction privacy and a consistent accessible experience on iOS and Android.

## ADDED Requirements

### Requirement: Match bets follow the shared screen hierarchy

The match-bets destination on iOS and Android SHALL use the visual hierarchy of the change's hidden, revealed, and finished references: existing localized Score title and Back/Home actions, a match heading, and either a hidden-bets explanation or a flat participant list. It SHALL use the app's existing platform canvases, typography, avatars, point treatments, and thin separators. The header SHALL be part of the scrolling content. Example values, device frames, missing-icon blocks, and reference labels superseded by requirements below SHALL NOT become production behavior.

Existing match-detail entry actions for loaded upcoming matches SHALL remain enabled before kickoff and while predictions are open. Activating an entry SHALL open this destination with its hidden-bets presentation when predictions are open. The prediction-visibility gate SHALL restrict other participants' predictions, not navigation to the match screen. This requirement SHALL NOT repurpose prediction-editing controls or make loading placeholders actionable.

#### Scenario: Viewing the match from History or Timeline
- **GIVEN** the viewer opens a match from an existing entry point
- **WHEN** match data is available
- **THEN** the same redesigned match destination shows the correct teams and match state
- **AND** Back and Home preserve their existing destinations

#### Scenario: Opening an upcoming match while bets are hidden
- **GIVEN** an existing match-detail entry represents a loaded upcoming match whose predictions remain open
- **WHEN** the viewer activates that entry by touch or a supported keyboard or assistive-technology action
- **THEN** the match screen opens with the matchup, start time, hidden-bets explanation, and viewer's own prediction region
- **AND** the entry is not disabled because the match has not started or predictions remain open
- **AND** other participants' predictions remain hidden

#### Scenario: Large header in a short window
- **GIVEN** large text or a short window makes the header consume most of the visible space
- **WHEN** the viewer scrolls
- **THEN** the header scrolls with the content and the body and recovery actions remain reachable

### Requirement: Prediction visibility keeps the existing lock policy

Other participants' predictions SHALL remain hidden while the existing server policy reports predictions open. The ten-minute pre-kickoff lock and existing computation precedence SHALL remain unchanged. The ranked endpoint and client presentation SHALL both preserve this boundary. Unknown/loading gate state SHALL NOT expose cached other-participant predictions or their accessibility text. An ordinary refresh SHALL recheck the gate; the passage of the client clock alone SHALL NOT reveal predictions. There SHALL be no bypass or reveal-anyway action.

#### Scenario: Before the lock boundary
- **GIVEN** the match is not computed and scheduled kickoff is more than ten minutes away under the existing server policy
- **WHEN** the match screen or ranked match-bets endpoint is requested
- **THEN** other participants' predictions are not exposed
- **AND** the screen shows the hidden-bets presentation

#### Scenario: Betting has closed before kickoff
- **GIVEN** the server reports the match locked within the ten-minute pre-kickoff window
- **WHEN** a normal load or refresh confirms this state
- **THEN** participant predictions can appear under the existing visibility rule
- **AND** the interface does not claim that play is in progress

#### Scenario: A corrected schedule opens predictions again
- **GIVEN** participant predictions were previously visible
- **WHEN** a fresh server response reports predictions open and the match not computed
- **THEN** participant predictions and their accessibility nodes are removed
- **AND** the hidden-bets presentation replaces them

### Requirement: Hidden bets retain match context and show only the viewer's own prediction

The hidden state SHALL show a centered neutral pill with localized Starts date/time, the two team identities around VS, a neutral closed-lock illustration, the localized heading Bets are hidden, and an explanation that everyone's bets are revealed when betting closes for this match. It SHALL follow the open spacing of the latest hidden-state reference and center the explanatory group in remaining viewport space when it fits. Below the explanation it SHALL show the authenticated viewer's own prediction labeled Your bet, or a localized No bet after confirmed absence. It SHALL NOT use another participant's routed identity as the owner. Loading, failed retrieval, and confirmed absence SHALL remain distinct, with a scoped retry for failure. This own-prediction display SHALL be informational.

#### Scenario: Viewer already predicted the match
- **GIVEN** the signed-in viewer predicted 3–2 and predictions remain open
- **WHEN** the hidden state renders
- **THEN** Your bet 3–2 appears beneath the explanation
- **AND** no other participant's prediction appears

#### Scenario: Entry route belongs to another participant
- **GIVEN** the route identifies another participant whose prediction is 1–0 and the viewer's own prediction is 3–2
- **WHEN** the hidden state renders
- **THEN** Your bet identifies the viewer's 3–2 prediction
- **AND** the other participant's prediction is not shown or described as the viewer's

#### Scenario: Own prediction is missing or fails to load
- **GIVEN** the viewer's own prediction request is loading, fails, or confirms no prediction
- **WHEN** the private prediction region renders
- **THEN** it respectively shows the production placeholder, a localized recoverable failure, or No bet
- **AND** neither loading nor failure is presented as confirmed absence

### Requirement: Match status and results reflect available evidence

A locked, uncomputed match SHALL show a neutral localized Bets closed status, participant predictions, and no numeric awarded-point pills. Its center SHALL show VS when no match score exists, or the available score described as a match score. A computed match SHALL show the localized date/time and a prominent final score, with neutral dashes if the result is unavailable. The interface SHALL NOT infer live play or finality from lock status, scheduled time, an uncomputed score, or missing data. This redesign SHALL NOT introduce polling or a new live-status source.

#### Scenario: Locked match without a score
- **GIVEN** betting is locked and points are not computed, with no match score
- **WHEN** the screen renders
- **THEN** Bets closed and VS appear with the participant list
- **AND** no live-play claim or zero-point award is synthesized

#### Scenario: Score is available before computation
- **GIVEN** an uncomputed match contains a score of 1–0
- **WHEN** the heading is displayed and announced
- **THEN** it identifies 1–0 as the available match score
- **AND** it does not identify it as final or claim settled awards

#### Scenario: Computed result or unavailable result
- **GIVEN** computation is complete and the match result is either 0–0 or unavailable
- **WHEN** the heading renders
- **THEN** the real 0–0 is shown as a final score and unavailable data uses neutral dashes
- **AND** both states have distinct meaningful announcements

### Requirement: Participants are ordered by total pool points descending

Every revealed or computed participant feed SHALL order participants by the latest propagated copies of their total points in the selected pool, greatest first. After propagation and index updates converge, these copies SHALL match the authoritative current membership totals and eligibility, including when opening an older match. Temporary score and participant-projection lag during propagation SHALL be allowed; the feed SHALL NOT claim an instantaneous or historically frozen leaderboard. The order SHALL apply to the complete paginated result for unchanged projections, not independently to loaded pages. It SHALL NOT use this match's award, prediction, client-summed awards, or the viewer's identity as the primary ordering. Equal totals SHALL use gambler ID ascending as a stable tie break. Known totals, including zero, SHALL precede genuinely unavailable totals, which SHALL use gambler ID ascending. A participant without a prediction SHALL remain at their pool-total position.

#### Scenario: Pool leader earned fewer points on this match
- **GIVEN** propagated standings have converged, participant A has 120 total pool points and earned 0 on this match, while participant B has 100 total pool points and earned 4
- **WHEN** the participant list renders
- **THEN** A appears before B
- **AND** their match-award pills remain 0 pts and +4 pts respectively

#### Scenario: The leader is beyond the old match-index first page
- **GIVEN** propagated standings have converged and the pool leader's match entry would fall on a later page of the old ID-ordered feed
- **WHEN** the new ranked first page loads
- **THEN** the pool leader is first on that page
- **AND** the client does not need to load all old pages to discover the correct order

#### Scenario: Equal totals cross a page boundary
- **GIVEN** several participants share the same total and their group spans multiple pages
- **WHEN** those pages load with unchanged ranking projections
- **THEN** their combined order follows gambler ID ascending
- **AND** no participant is duplicated or omitted at the boundary

#### Scenario: Current user or leader has no prediction
- **GIVEN** the current user is fifth by pool points and the first-ranked participant has no prediction
- **WHEN** the list renders
- **THEN** the current user stays fifth and the first-ranked participant stays first with No bet
- **AND** neither is moved to a separate top or bottom group

#### Scenario: Zero and unavailable totals
- **GIVEN** one participant has a confirmed total of zero and another has no available total
- **WHEN** their ordering is determined
- **THEN** the confirmed-zero participant precedes the unavailable-total participant
- **AND** missing totals are not silently converted into zero

### Requirement: Match settlement is durable and ordered within each pool

An accepted match result SHALL durably enqueue a versioned per-pool scoring-and-position command before settlement begins. The existing per-pool FIFO consumer SHALL own scoring and position finalization as one recoverable operation. Result-handler retries SHALL rediscover affected pools even when their bets are already computed, and SHALL NOT acknowledge undispatched work. Concurrency and each processing chunk SHALL be bounded. Durable progress, operation receipts, and an active-operation fence within the existing Pool table SHALL prevent later commands for the same pool from overtaking unfinished chunks, including after a worker restart or exhausted retry. This flow SHALL reuse the existing persistence and messaging architecture without a new job table or workflow service. An empty eventually consistent match index SHALL NOT establish settlement completion.

#### Scenario: Result delivery is interrupted during pool dispatch
- **GIVEN** some affected pools received the result command before the handler failed and some of their bets are now computed
- **WHEN** result delivery is retried
- **THEN** all affected pools remain discoverable and undispatched commands are durably delivered
- **AND** repeated commands do not score a bet twice or erase unfinished position work

#### Scenario: One pool operation exceeds a processing chunk
- **GIVEN** match A has unfinished scoring or positions and match B is queued for the same pool
- **WHEN** A yields, fails, or resumes on another worker
- **THEN** durable progress allows A to resume and B cannot mutate that pool's settlement state ahead of A
- **AND** work for independent pools can continue within configured concurrency limits

#### Scenario: An index temporarily omits pending bets
- **GIVEN** eligible match details exist or remain to be materialized but have not appeared in the match index
- **WHEN** an index query reports no uncomputed bets
- **THEN** the operation does not use that response alone to claim all eligible bets and positions are complete
- **AND** undiscovered or late materialization remains covered by recoverable settlement work

### Requirement: Score settlement preserves atomicity and membership identity

Applying a match award SHALL atomically record the detail's result and award, update the authoritative membership total, and advance its score version when the total changes. Both records SHALL be guarded by the intended membership generation and result-operation identity. Repeated or older work SHALL NOT add the same award again, alter a newer result, or charge a replacement membership. A zero award SHALL still finalize the match detail without requiring propagation of an unchanged total. The existing scoring formula and late-join eligibility policy SHALL remain unchanged. Supported repairs or result corrections SHALL follow the version contract and adjust the total by the difference from the previously applied award. Conditional outcomes SHALL distinguish verified duplicate, superseded, or obsolete-membership work from unresolved conflicts and failures; blanket transaction-cancellation success SHALL NOT be permitted.

#### Scenario: A committed score is delivered again
- **GIVEN** the detail award and membership total were committed for a result operation
- **WHEN** the same operation is retried after a lost response
- **THEN** neither the award nor its total contribution is applied again
- **AND** outstanding position finalization can still resume

#### Scenario: A participant rejoins during scoring
- **GIVEN** a worker read a bet from a membership that was removed and replaced by a new membership generation
- **WHEN** the delayed scoring write executes
- **THEN** it cannot modify the replacement membership or its match detail using the old prediction
- **AND** the obsolete work is resolved separately from legitimate work for the new membership

#### Scenario: A scoring transaction is canceled for a retryable reason
- **GIVEN** a scoring write encounters contention, throttling, or another unresolved failure
- **WHEN** the transaction does not commit
- **THEN** the bet remains pending for bounded retry or retained repair
- **AND** the pool is not reported as settled merely because a conditional or transaction exception occurred

#### Scenario: A supported correction changes a previous award
- **GIVEN** a newer authoritative result or repair changes an already applied award from 10 points to 4
- **WHEN** the correction commits and is subsequently delivered again
- **THEN** the pool total decreases by 6 exactly once and its version advances for that change
- **AND** an older result replay cannot restore the superseded award

#### Scenario: A correction arrives before its prerequisite settlement
- **GIVEN** a correction expects a prior result version that has not completed settlement for the pool
- **WHEN** correction acceptance is evaluated
- **THEN** it is rejected recoverably or retained outside active settlement until its prerequisite is satisfied
- **AND** it cannot acquire the pool fence and block the prerequisite behind itself

### Requirement: Position finalization is authoritative and replay safe

Position finalization SHALL derive totals and membership eligibility from authoritative base-table reads rather than the eventually consistent leaderboard index. Strongly consistent pages SHALL NOT be treated as a frozen snapshot: concurrent membership or repair changes that invalidate the calculation SHALL be detected and reconciled before the operation is declared complete. For result settlement or correction, each current membership position and its target match-detail position, operation markers, and applicable previous-position value SHALL be committed as a guarded atomic operation. A membership-triggered current-standings reconciliation SHALL update guarded membership positions without requiring a match or rewriting historical match positions. The original previous position SHALL remain stable across retries of the same operation. Missing or replaced memberships SHALL NOT be recreated by position writes. Duplicate or superseded operations SHALL NOT overwrite newer current positions, and incomplete historical detail work SHALL remain explicitly recoverable through a new reconciliation identity without rewinding the current position. This requirement SHALL NOT introduce a frozen ranking snapshot.

#### Scenario: The leaderboard index lags committed totals
- **GIVEN** score settlement has committed but the leaderboard index still returns earlier totals
- **WHEN** positions are finalized
- **THEN** their calculation uses authoritative totals and does not persist the stale index ordering

#### Scenario: Position persistence is interrupted
- **GIVEN** a membership and match detail need positions for the same operation
- **WHEN** their guarded write fails or its successful response is lost
- **THEN** retry preserves the original previous position and completes or confirms the pair without applying the transition again
- **AND** an incomplete pair is not treated as complete solely because one record contains a marker

#### Scenario: An older operation is replayed after a newer operation
- **GIVEN** operation A completed, operation B subsequently updated the pool's positions, and another member joined afterward
- **WHEN** A is delivered again
- **THEN** A does not overwrite B's current positions or change A's recorded previous positions
- **AND** any unresolved historical detail for A remains recoverable through a new reconciliation operation without rolling the membership back to A

#### Scenario: Membership changes before any result is settled
- **GIVEN** a pool has no settled match and a membership change requests current-position reconciliation
- **WHEN** the operation completes
- **THEN** current membership positions reflect authoritative totals and eligibility
- **AND** no match detail is required or invented to update those positions

#### Scenario: Membership changes after a match is settled
- **GIVEN** an earlier match has stored positions and a participant subsequently joins or leaves
- **WHEN** current-standings reconciliation completes
- **THEN** current membership positions reflect the new membership set
- **AND** the earlier match's stored positions and previous positions remain unchanged

#### Scenario: Membership or repair changes while positions are read
- **GIVEN** a participant leaves, rejoins, or has an authoritative total repaired during a paginated position calculation
- **WHEN** that change invalidates the operation's inputs or guarded writes
- **THEN** the operation reconciles the affected calculation instead of declaring a mixed or obsolete result complete
- **AND** it neither recreates a removed membership nor attributes an older generation's position to a new membership

### Requirement: Match materialization and settlement recover from source ordering races

Match creation, late joining, and result handling SHALL reconcile against the current authoritative result and the existing eligibility policy. Processing an insertion before its result within one delivery SHALL NOT be the sole race protection. A detail materialized after result dispatch or settlement SHALL be initialized or scheduled for recoverable settlement as appropriate, without remaining permanently uncomputed or granting a late participant a prediction they did not make. A result already present when a match is created SHALL be settled for every eligible detail without requiring a later result-change event. Recovery SHALL preserve existing predictions, result versions, and membership-generation guards.

#### Scenario: Match creation and finalization share a delivery
- **GIVEN** a match is inserted and receives its final result before all participant details are created
- **WHEN** materialization and result handling run or are retried in either order
- **THEN** every eligible detail converges to the appropriate computed state and pool contribution
- **AND** no result is lost because the first scoring discovery found no details

#### Scenario: A match is inserted with its final result already present
- **GIVEN** a match record is created in the layout with its final result already set, so no result-change event will follow
- **WHEN** participant details for that match are materialized
- **THEN** each eligible detail is settled from the current authoritative result and contributes to its pool total exactly once
- **AND** the pool's settlement and position work is durably requested instead of waiting for a result event that will not occur

#### Scenario: A participant detail is created after settlement
- **GIVEN** a late join or delayed materialization creates a detail for a result that was already handled
- **WHEN** the detail is initialized and recovery finishes
- **THEN** it reflects the current result according to the existing late-join policy
- **AND** its points are not duplicated and it does not wait indefinitely for a result event that will not recur

### Requirement: Settlement failures are retained and operationally recoverable

Settlement dispatch and consumption SHALL use bounded retries with backoff, failure isolation, and observable pending age and exhaustion. Stream-batch failures SHALL be isolated through configured bisection or equivalent record-level handling, and exhausted deliveries SHALL retain their complete original event payload for replay. Failed queue commands SHALL be retained in a dead-letter destination with their operation identity and usable progress. A dead-letter transfer SHALL NOT mark settlement complete or allow later commands to bypass its active-operation fence. Failure retention, alarms, and a verified replay/reconciliation procedure SHALL be deployment prerequisites. Operators SHALL be able to resume failed work without double scoring, changing previous-position history, or losing downstream total-projection work.

#### Scenario: A source event or settlement command exhausts retries
- **GIVEN** repeated failures prevent a result from being dispatched or a pool operation from completing
- **WHEN** the configured retry limit is reached
- **THEN** the complete source event or versioned command is retained for repair and an actionable failure is observable
- **AND** the affected operation remains incomplete while unrelated pools can continue

#### Scenario: Retained work is replayed after repair
- **GIVEN** an operator repairs the cause of a failed delivery and resumes the retained work
- **WHEN** dispatch, scoring, positions, and downstream propagation recover
- **THEN** committed progress is preserved and remaining work converges without duplicate points or rewritten previous positions
- **AND** later same-pool operations proceed only after the earlier operation is safely completed or explicitly reconciled

### Requirement: Match ranking copies converge without changing scoring or privacy

Each match-bet detail eligible for the ranked feed SHALL carry a distinct current-pool-total projection, separate from its match award, maintained from the authoritative membership total. A total change SHALL eventually update all of that participant's eligible match details, including historical matches. Duplicate, delayed, or retried propagation SHALL NOT double-count points, overwrite a newer applied total with an older one, change a prediction or match award, recreate a deleted bet, or reactivate a removed membership. Partial failures SHALL remain recoverable through durable retry and repair, with observable lag and failures. New match rows and rejoined memberships SHALL converge correctly even when created during score propagation. Genuinely unavailable totals SHALL remain distinct from confirmed zero and from a failed source read. Requester authorization and the reveal gate SHALL NOT rely on these asynchronous projections.

#### Scenario: A new result changes ranking on an older match
- **GIVEN** participant A's authoritative pool total increases from 100 to 120 because of another match
- **WHEN** propagation and index updates complete and the viewer refreshes an older match
- **THEN** that older match ranks A using 120 total points
- **AND** its existing prediction and this-match award remain unchanged

#### Scenario: An older update arrives after a newer update
- **GIVEN** a newer total has already been applied to a match detail
- **WHEN** an older or duplicate propagation attempt is delivered
- **THEN** it does not replace the newer applied total or add points again
- **AND** a later authoritative correction that decreases the total is still applied when it is newer

#### Scenario: Match creation overlaps a total update
- **GIVEN** a new match row is initialized while the participant's total changes
- **WHEN** initialization and propagation finish
- **THEN** the row contains the latest authoritative total for its current membership
- **AND** it cannot remain permanently seeded with a total superseded before initialization completed

#### Scenario: A participant leaves and rejoins while work is pending
- **GIVEN** work remains from a removed membership and the same participant subsequently rejoins
- **WHEN** delayed removal and score updates are processed
- **THEN** old work cannot overwrite or remove the new membership's ranking projection
- **AND** retained historical data follows the existing retention policy while obsolete membership projections are removed after cleanup converges
- **AND** a removed requester is denied access independently of that cleanup

#### Scenario: Propagation fails after updating only some matches
- **GIVEN** a participant's total was copied to some match details before a worker failed
- **WHEN** processing resumes or reconciliation repairs the work
- **THEN** the remaining details converge without duplicate scoring or loss of predictions
- **AND** partial progress and unresolved failures are observable rather than reported as successful completion

### Requirement: Ranked participant reads scale with the requested page

The ranked endpoint SHALL obtain participant identity, prediction, match award, and ranking data together from its paginated read model. It SHALL NOT require a separate source read per participant or a batch enrichment of the page's bets or totals to construct the participant rows. Reading a page SHALL NOT require loading the full pool or complete equal-score group. Separate requester-authorization, match-visibility, and readiness checks SHALL remain permitted and mandatory where needed. Continuation, recovery, and response size SHALL remain bounded. The read model SHALL preserve confirmed No bet entries and unavailable-total entries without substituting missing or unmigrated rows for either state.

#### Scenario: Thousands of participants share zero points
- **GIVEN** a migrated pool contains thousands of eligible participants with confirmed zero totals
- **WHEN** the first participant page is requested
- **THEN** the endpoint reads the requested ranked page without fetching the entire tie group or enriching each row with another bet read
- **AND** subsequent pages retain the required ID tie order

#### Scenario: The ranked read model is unavailable
- **GIVEN** the index or required participant projection cannot be read
- **WHEN** the endpoint receives a ranked request
- **THEN** it returns a recoverable failure
- **AND** it does not silently run per-participant lookups, return a partial migration as a complete feed, or use an alternative ordering

### Requirement: Ranked rollout preserves existing clients and data

The new ranked mode SHALL be enabled only after existing eligible match details have been backfilled and coverage, values, and ordering have been verified for its rollout scope. Initialization and ongoing updates SHALL be active before this verification. Before activating generation-fenced settlement, both membership and eligible existing detail identities SHALL be safely initialized and their materialization/removal writers SHALL preserve those identities; missing legacy metadata SHALL NOT be handled by bypassing the guards. Backfill and repair SHALL be resumable and safe alongside newer score and membership changes. Unresolved migration failures SHALL prevent that scope from being declared ready. Existing prediction, result, match-award, and scoring-fan-out data SHALL retain their meanings. Legacy requests and cursors SHALL remain supported during rollout and rollback; new-mode requests to an unready scope SHALL fail recoverably rather than silently receive legacy order.

#### Scenario: Existing data has only been partially migrated
- **GIVEN** some eligible match details in the rollout scope still lack a valid ranking projection
- **WHEN** readiness is evaluated or a ranked request is made before readiness
- **THEN** the scope is not declared ready and the ranked request fails recoverably
- **AND** existing clients continue to use the legacy contract

#### Scenario: Backfill overlaps newer production changes
- **GIVEN** backfill read an earlier total or membership identity
- **WHEN** that source changes before backfill writes the projection
- **THEN** backfill retries or defers safely instead of overwriting newer state
- **AND** resuming the migration preserves existing predictions, results, and match awards

### Requirement: Ranked paging is recoverable and compatible

Ranked match-bets requests SHALL preserve authorization and visibility enforcement, use opaque continuations scoped to the selected pool, match, ordering mode, and read-model version, and preserve global ordering for unchanged ranking projections through retries and append. Malformed, incompatible, or cross-context continuations SHALL produce a recoverable error or explicit restart, never another context's data. Legacy requests SHALL retain their existing contract. Ranking-data failure SHALL NOT silently return an alternative ordering. Refresh SHALL replace the ranked feed from its first page and discard old continuations. Refresh SHALL use the latest available projection; it SHALL NOT wait for propagation or promise that a pending update is already visible. After propagation converges, a refresh SHALL reflect current authoritative standings. The screen SHALL NOT claim a frozen or transactional standings snapshot.

#### Scenario: Ranked append succeeds after a retry
- **GIVEN** ranking projections are unchanged and a ranked page failed transiently
- **WHEN** the viewer retries and continues appending
- **THEN** the complete feed retains descending pool-total order and deterministic ties
- **AND** loaded participants remain usable without duplication

#### Scenario: A continuation belongs to a different match or mode
- **GIVEN** a continuation was issued for another match, pool, or ordering mode
- **WHEN** it is submitted with the current ranked request
- **THEN** the service rejects it recoverably or requests a clean restart
- **AND** it does not interpret it as valid current-context data

#### Scenario: Pool standings change and the viewer refreshes
- **GIVEN** already loaded participants have changed pool totals and projection/index updates have converged
- **WHEN** the viewer refreshes
- **THEN** a fresh first page replaces the feed using current authoritative totals
- **AND** prior appended pages and cursors are not merged into the new ranking

#### Scenario: The viewer refreshes while propagation is pending
- **GIVEN** some detail rows still contain earlier propagated totals
- **WHEN** the viewer refreshes
- **THEN** the feed restarts using the latest available projections without waiting for the background work
- **AND** it does not claim a fully synchronized ranking or merge previous appended pages into that result

#### Scenario: Ranking cannot be read
- **GIVEN** the service cannot read the required ranking projection
- **WHEN** a ranked page is requested
- **THEN** the request exposes a recoverable failure
- **AND** it does not fall back to match points, ID ordering, or assumed zero totals

### Requirement: Participant rows distinguish identity prediction and match award

Each participant SHALL have the shared circular photo/initial avatar, their display name, and their home/away prediction or localized No bet. Computed rows SHALL add the authoritative award for this match using History's localized signed green positive pill, neutral zero pill without a plus sign, or neutral unavailable indication. The numeric pool total used for ordering SHALL NOT replace the match award. The current user SHALL be identified with the localized You marker and established current-user treatment while retaining rank position. Long names and value regions SHALL wrap or reflow without overlap; each score expression SHALL remain together.

#### Scenario: Computed positive zero and unavailable awards
- **GIVEN** three computed rows have awards of 4, 0, and unavailable
- **WHEN** they render
- **THEN** their pills distinguish +4 pts, 0 pts, and unavailable
- **AND** color is not the only distinction

#### Scenario: Missing prediction with unavailable award
- **GIVEN** a participant has no prediction and the returned award is unavailable
- **WHEN** the row renders
- **THEN** it shows No bet and an unavailable award
- **AND** it does not infer zero points from prediction absence

#### Scenario: Shared avatar and long identity
- **GIVEN** a participant has a photo or falls back to initials and a long name
- **WHEN** their row renders at large text size
- **THEN** shared avatar loading/fallback behavior is used and the full identity remains accessible
- **AND** prediction and award remain readable through wrapping or reflow

### Requirement: Rows retain native navigation and meaningful accessibility

An available other-participant row SHALL be one full-row native activation target that opens their existing Timeline in the current pool and supplies native press feedback. The signed-in participant's row SHALL have no navigation action or button semantics. Each row SHALL announce the full identity, You when applicable, the team-qualified prediction or its absence, and the match award when computed, with full localized singular/plural point names. The match heading and hidden-state own prediction SHALL have separate meaningful announcements. Decorative avatars, flags, separators, and lock imagery SHALL NOT add focus stops. Back/Home and retry controls SHALL have localized accessible names and platform minimum target sizes.

#### Scenario: Activating another participant
- **GIVEN** a loaded row belongs to another participant and Timeline navigation is available
- **WHEN** it is activated through touch, keyboard, VoiceOver, or TalkBack
- **THEN** the existing Timeline destination opens once for the correct pool and participant
- **AND** a canceled press clears feedback without navigation

#### Scenario: Reading the signed-in participant's row
- **GIVEN** the viewer's row is at its correct pool rank
- **WHEN** assistive technology focuses it
- **THEN** identity, You, prediction, and applicable match award are announced coherently
- **AND** the row is not announced as a button or given an ineffective action

#### Scenario: Reading one point in Spanish
- **GIVEN** Spanish is selected and a computed award is one point
- **WHEN** the row is announced
- **THEN** the spoken unit is singular and the prediction's team associations remain clear

### Requirement: Loading and recovery preserve accessible content

Model-backed header, participant, and own-prediction loading regions SHALL use their production components with stable placeholder models and the shared loading-placeholder treatment selected through `isPlaceholder`. Callers SHALL NOT supply an effect modifier or maintain a separate skeleton-only layout. Placeholders SHALL suppress remote requests for filler identities, navigation, and meaningful accessibility exposure. Refresh and append SHALL preserve available content subject to the visibility gate. Initial, append, header, and own-prediction failures SHALL offer localized scoped retry without repeated request loops. Empty/error content SHALL center in remaining space below the header when it fits and remain scrollable. Obsolete responses SHALL NOT update a newer account/pool/match context or refresh generation.

#### Scenario: Initial and append placeholders
- **GIVEN** a header, participant page, or own prediction is not yet available
- **WHEN** its loading state renders
- **THEN** its real production component uses a stable placeholder model and the shared treatment
- **AND** filler values are neither announced nor actionable and trigger no avatar request

#### Scenario: Refresh or append partially fails
- **GIVEN** accessible confirmed match content and rows are already shown
- **WHEN** refresh or append fails
- **THEN** still-authorized content remains usable with explicit failure feedback and scoped retry
- **AND** failure is not represented as an empty list, No bet, zero points, or an endless placeholder

#### Scenario: The viewer changes account pool or match during loading
- **GIVEN** an earlier context still has pending requests
- **WHEN** another account, pool, or match becomes active
- **THEN** earlier responses cannot replace the current heading, own prediction, or ranked feed

### Requirement: Themes localization responsive layout and icons follow app conventions

The screen SHALL support existing English and Spanish locales, light/dark appearance, narrow phones, tablets/resized windows, supported accessibility text sizes, reduced motion, and platform safe-area/system-inset behavior. Dates SHALL preserve device locale, time zone, clock preference, and a distinguishing year outside the current year. Ordinary text SHALL meet at least 4.5:1 contrast on its actual surface. Content SHALL reflow and scroll rather than shrink accessible text or fix row heights. Changed or introduced app-rendered icons SHALL prefer Material Symbols when semantically accurate; custom icons SHALL be allowed otherwise without a justification requirement. iOS and Android icon assets SHALL derive from the same committed canonical vector source. Fully operating-system-rendered controls are excluded. Reference-image icon repairs SHALL NOT be used as raster production icons.

#### Scenario: Large text and a narrow or landscape window
- **GIVEN** long translated content and large accessibility text
- **WHEN** the screen is resized or rotated
- **THEN** the start pill, teams, explanation, own prediction, rows, and retry actions wrap or stack without overlap
- **AND** first and last content remain reachable outside obstructing system chrome

#### Scenario: Light dark or reduced-motion preferences change
- **GIVEN** the screen contains text, award pills, and loading placeholders
- **WHEN** appearance or motion preferences change
- **THEN** the existing semantic theme and placeholder motion fallback apply without exposing filler values
- **AND** states remain distinguishable without color or motion alone

#### Scenario: Missing glyphs in the supplied references
- **GIVEN** a reference originally contains blank squares for navigation or lock icons
- **WHEN** the screen is implemented on both platforms
- **THEN** it uses meaningful icons derived from shared canonical vectors for affected app-rendered controls
- **AND** no blank square, screenshot crop, or platform-specific replacement icon family is prescribed
