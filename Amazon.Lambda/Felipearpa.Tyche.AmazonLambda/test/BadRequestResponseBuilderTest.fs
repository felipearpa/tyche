namespace Felipearpa.Tyche.AmazonLambda.Tests

open System.Net
open System.Text.Json
open Felipearpa.Tyche.AmazonLambda
open FsUnit.Xunit
open Xunit

module BadRequestResponseBuilderTest =

    [<Fact>]
    let ``given a list of errors when are converted to a amazon lambda response then an error response is returned``
        ()
        =
        let errors = [ "error 1"; "error 2"; "error 3" ]

        let response = errors |> buildAmazonBadRequestResponse

        response.StatusCode |> should equal (int HttpStatusCode.BadRequest)

        let expectedBody = JsonSerializer.Serialize errors
        response.Body |> should equal expectedBody
