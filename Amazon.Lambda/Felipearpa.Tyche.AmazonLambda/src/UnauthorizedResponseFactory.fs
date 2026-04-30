namespace Felipearpa.Tyche.AmazonLambda

open System.Net
open Amazon.Lambda.APIGatewayEvents

module UnauthorizedResponseFactory =

    let create () : APIGatewayHttpApiV2ProxyResponse =
        APIGatewayHttpApiV2ProxyResponse(StatusCode = int HttpStatusCode.Unauthorized)
