# Pipel.Tyche.Pool

It contents the business rules to manage the pools.

## Models

### `Pool`

It's the model to manage the pools.

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

## Use cases

### `FindPoolsUseCase`

Return the pools associated to a pool layout.