namespace Felipearpa.Tyche.AmazonLambda

[<AutoOpen>]
module ResultTransformer =

    let toErrorOption result =
        match result with
        | Ok _ -> None
        | Error error -> Some error
