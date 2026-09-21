## ADDED Requirements

### Requirement: Counted nouns agree with their counts
Every counted value the leaderboard announces SHALL take the grammatical number its count requires, in every supported locale. A value of one SHALL be announced in the singular and any other value in that locale's plural form. Rank is exempt: it names a position, not a quantity.

This requirement exists because the leaderboard row and the "My pools" row share one set of localized strings. A change to their grammatical number on either surface changes both, so the invariant is stated here as well as in `pool-score-list`.

#### Scenario: Gambler moved exactly one place
- **GIVEN** a gambler's rank improved by one place since the previous standing
- **WHEN** VoiceOver or TalkBack announces the row
- **THEN** the movement is announced in the singular ("Up 1 place"), not "Up 1 places"

#### Scenario: Gambler has exactly one point
- **GIVEN** a gambler's score is 1
- **WHEN** the row is announced
- **THEN** the points value is announced in the singular ("1 point"), not "1 points"

#### Scenario: Counts other than one
- **GIVEN** a counted value of zero, two, or more
- **WHEN** the row is announced in any supported locale
- **THEN** that locale's plural form is used

#### Scenario: Rank is not pluralized
- **GIVEN** a gambler is in rank 1
- **WHEN** the row is announced
- **THEN** the rank is announced as a position ("Rank 1") with no singular or plural inflection applied to it

#### Scenario: A locale is added later
- **GIVEN** a new locale is added to the leaderboard's string tables
- **WHEN** the counted strings are translated
- **THEN** each one provides every plural category that locale requires, rather than a single flat form
