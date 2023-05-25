# Common types for F#

This project has the next types:

- `Barcode`
- `DateTime`
- `NonEmptyString`
- `NonEmptyString100`
- `Percentage`
- `PositiveInt`
- `Uuid`
- `Email`

The next one is an example of a model that uses this data types:

```f#
type Vat =
    { ID: Uuid
      Name: NonEmptyString
      Percentage: Percentage }
```

the next one is an example for creating it

```f#
let uuid = Uuid.From(Guid("9798ff07-f9d5-462d-acb1-bd58b553ff2e"))
```

and the next one is an example for getting its value

```f#
uuid |> Uuid.value
```
