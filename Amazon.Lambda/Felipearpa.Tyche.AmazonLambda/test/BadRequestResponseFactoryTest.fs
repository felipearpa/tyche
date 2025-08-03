namespace Felipearpa.Tyche.AmazonLambda.Tests

open System.Net
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Tyche.AmazonLambda
open FsUnit.Xunit
open Xunit

module BadRequestResponseFactoryTest =

    [<Fact>]
    let ``given a list of errors when are converted to a amazon lambda response then an error response is returned``
        ()
        =
        let errors = [ "error 1"; "error 2"; "error 3" ]

        let response = errors |> BadRequestResponseFactory.create

        response.StatusCode |> should equal (int HttpStatusCode.BadRequest)

        let serializer = JsonSerializer() :> ISerializer
        let expectedBody = serializer.Serialize {| errors = errors |}
        response.Body |> should equal expectedBody
