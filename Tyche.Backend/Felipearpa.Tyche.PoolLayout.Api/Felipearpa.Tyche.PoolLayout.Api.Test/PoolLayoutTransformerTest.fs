module PoolLayoutTransformerTest

open System
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Type
open Felipearpa.Tyche.PoolLayout.Api.PoolLayoutTransformer
open FsUnit.Xunit
open Xunit

[<Fact>]
let ``given a pool layout when is transformed to a response then is transformed correctly`` () =
    let id = "01K0PY8TF2CH2XV22DNN5JE1V5"
    let name = "World Cup 2026"
    let startDateTime = DateTime.Now

    let model =
        { PoolLayout.Id = id |> Ulid.newOf
          Name = name |> NonEmptyString100.newOf
          StartDateTime = startDateTime }

    let response = model.ToPoolLayoutResponse()

    response.Id |> should equal id
    response.Name |> should equal name

    response.StartDateTime.ToUniversalTime()
    |> should equal (startDateTime.ToUniversalTime())
