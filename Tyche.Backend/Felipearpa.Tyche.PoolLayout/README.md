# Pipel.Tyche.Pool

It contents the business rules to manage the pools.

## Models

### `Pool`

It's the model to manage the Pools.

```f#
type PoolLayoutPK = { PoolLayoutId: Ulid }

type PoolPK = { PoolId: Ulid }

type Pool =
    { PoolPK: PoolPK
      PoolLayoutPK: PoolLayoutPK
      PoolName: NonEmptyString100
      CurrentPosition: PositiveInt option
      BeforePosition: PositiveInt option }
```

### `PoolGambler`

It's the model to manage the PoolsGamblers.

```f#
type GamblerPK = { GamblerId: Ulid }

type PoolGamblerPK =
    { PoolPK: PoolPK
      GamblerPK: GamblerPK }

type PoolGambler =
    { PoolGamblerPK: PoolGamblerPK
      GamblerEmail: Email
      Score: PositiveInt option
      CurrentPosition: PositiveInt option
      BeforePosition: PositiveInt option }
```

### `PoolGame`

It's the model to manage the PoolsGames.

```f#
type GamePK = { GameId: Ulid }

type TeamPK = { TeamId: Ulid }

type PoolGamePK =
    { PoolPK: PoolPK
      GamePK: GamePK }

type PoolGame =
    { PoolGamePK: PoolGamePK
      HomeTeamPK: TeamPK
      HomeTeamName: NonEmptyString100
      HomeTeamScore: PositiveInt option
      HomeTeamBet: PositiveInt option
      AwayTeamPK: TeamPK
      AwayTeamName: NonEmptyString100
      AwayTeamScore: PositiveInt option
      AwayTeamBet: PositiveInt option
      BetScore: PositiveInt option
      MatchDateTime: DateTime }
```

## Use cases

### `FindPoolsUseCase`

Return the Pool list associated to a PoolLayout.

### `FindPoolsGamblersUseCase`

Return the PoolGambler list associated to a Pool.

### `FindPoolsGamesUseCase`

Return the PoolGame list associated to a Pool.