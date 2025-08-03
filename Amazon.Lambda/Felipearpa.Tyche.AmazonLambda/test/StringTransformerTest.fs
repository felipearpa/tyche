namespace Felipearpa.Tyche.AmazonLambda.Tests

open Felipearpa.Tyche.AmazonLambda
open FsUnit.Xunit
open Xunit

module StringTransformerTest =

    [<Fact>]
    let ``given an empty string when nonIfEmpty is applied then none is returned`` () =
        let string = ""
        let result = string |> noneIfEmpty
        result.IsNone |> should equal true

    [<Fact>]
    let ``given a multiple blank space string when nonIfEmpty is applied then none is returned`` () =
        let string = "  "
        let result = string |> noneIfEmpty
        result.IsNone |> should equal true

    [<Fact>]
    let ``given a non empty string when nonIfEmpty is applied then the value is not modified`` () =
        let string = "value"
        let result = string |> noneIfEmpty
        result |> should equal (Some "value")
