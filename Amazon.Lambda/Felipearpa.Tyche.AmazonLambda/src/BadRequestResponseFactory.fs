namespace Felipearpa.Tyche.AmazonLambda

open System.Net
open System.Text.Json
open Amazon.Lambda.APIGatewayEvents

module BadRequestResponseFactory =

    let create (errors: string list) : APIGatewayHttpApiV2ProxyResponse =
        let statusCode = int HttpStatusCode.BadRequest

        let bodyJson = JsonSerializer.Serialize {| errors = errors |}

        let headers =
            dict
                [ "Content-Type", "application/json"
                  "x-amzn-ErrorType", "ValidationException" ]

        APIGatewayHttpApiV2ProxyResponse(StatusCode = statusCode, Body = bodyJson, Headers = headers)
