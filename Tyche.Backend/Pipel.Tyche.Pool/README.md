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

## Use cases

### `FindPoolsUseCase`

Return the Pool list associated to a PoolLayout.

### `FindPoolsGamblersUseCase`

Return the PoolGambler list associated to a Pool.