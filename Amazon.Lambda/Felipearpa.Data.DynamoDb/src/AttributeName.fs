namespace Felipearpa.Data.DynamoDb

module ExpressionAttribute =

    let name attr = $"#{attr}"

    let names attrs =
        attrs |> List.map (fun a -> $"#{a}", a) |> dict

module Key =

    [<Literal>]
    let pk = "pk"

    [<Literal>]
    let sk = "sk"

module KeyPrefix =

    let build (prefix: string) (id: string) = $"{prefix}#{id}"
