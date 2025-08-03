namespace Felipearpa.Tyche.AmazonLambda.Tests.ResultTransformerTest

open Felipearpa.Tyche.AmazonLambda
open FsUnit.Xunit
open Xunit

module ResultTransformerTest =

    [<Fact>]
    let ``given an error result when is converted to option then some error is returned`` () =
        let result = Error "error"
        let option = result |> toErrorOption
        option |> should equal (Some "error")

    [<Fact>]
    let ``given an ok result when is converted to option then none is returned`` () =
        let result = Ok "ok"
        let option = result |> toErrorOption
        option |> should equal None
