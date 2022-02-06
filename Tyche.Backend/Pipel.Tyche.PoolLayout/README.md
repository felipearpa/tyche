# Pipel.Tyche.PoolLayout

It contents the business rules to manage the pools layouts.

## Models

### `PoolLayout`

It's the model to manage the pools layouts.

```f#
type PoolLayoutPK =
    { PoolLayoutId: Ulid }

type PoolLayout =
    { PoolLayoutPK: PoolLayoutPK
      Name: NonEmptyString100
      OpeningStartDateTime: DateTime
      OpeningEndDateTime: DateTime }
```

## Use cases

### `FindActivePoolsLayoutsUseCase`

Return the active pools layouts.