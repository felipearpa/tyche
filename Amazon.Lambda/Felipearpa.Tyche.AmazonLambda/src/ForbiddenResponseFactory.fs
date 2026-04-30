namespace Felipearpa.Tyche.AmazonLambda

open System.Net
open Amazon.Lambda.APIGatewayEvents

module ForbiddenResponseFactory =

    let create () : APIGatewayHttpApiV2ProxyResponse =
        APIGatewayHttpApiV2ProxyResponse(StatusCode = int HttpStatusCode.Forbidden)
