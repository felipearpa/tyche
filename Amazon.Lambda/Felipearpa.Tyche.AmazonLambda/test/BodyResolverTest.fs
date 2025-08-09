namespace Felipearpa.Tyche.AmazonLambda.Tests.ParameterResolverTest

open Felipearpa.Tyche.AmazonLambda
open FsUnitTyped
open Xunit

module BodyResolverTest =

    type Data = { a: string }

    [<Fact>]
    let ``given a valid body when is queried or error then a result is returned`` () =
        let json = "{ \"a\": \"value\"}"
        let deserializedObjResult = tryGetOrError<Data> json
        deserializedObjResult.IsOk |> shouldEqual true
        deserializedObjResult |> shouldEqual (Ok { Data.a = "value" })

    [<Fact>]
    let ``given an invalid body when is queried or error then an error is returned`` () =
        let json = "{ \"b\": \"value\"}"
        let deserializedObjResult = tryGetOrError<Data> json
        deserializedObjResult.IsError |> shouldEqual true

    [<Fact>]
    let ``given a valid body when is queried or none then some object is returned`` () =
        let json = "{ \"a\": \"value\"}"
        let deserializedObjResult = tryGetOrNone<Data> json
        deserializedObjResult.IsSome |> shouldEqual true
        deserializedObjResult |> shouldEqual (Some { Data.a = "value" })

    [<Fact>]
    let ``given an invalid body when is queried or none then none is returned`` () =
        let json = "{ \"b\": \"value\"}"
        let deserializedObjResult = tryGetOrNone<Data> json
        deserializedObjResult.IsNone |> shouldEqual true
