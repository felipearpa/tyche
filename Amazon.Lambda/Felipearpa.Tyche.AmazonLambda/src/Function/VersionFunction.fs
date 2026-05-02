namespace Felipearpa.Tyche.AmazonLambda.Function

open System.Collections.Generic
open System.Reflection
open System.Text.Json
open System.Threading.Tasks
open Amazon.Lambda.APIGatewayEvents
open Amazon.Lambda.Core

type VersionFunction() =

    static let version =
        let assembly = typeof<VersionFunction>.Assembly
        let info = assembly.GetCustomAttribute<AssemblyInformationalVersionAttribute>()

        let raw =
            if isNull info then
                string (assembly.GetName().Version)
            else
                info.InformationalVersion

        raw.Split('+').[0]

    // GET /version
    member this.GetVersionAsync
        (_: APIGatewayHttpApiV2ProxyRequest, _: ILambdaContext)
        : APIGatewayHttpApiV2ProxyResponse Task =
        async {
            let body = JsonSerializer.Serialize {| version = version |}
            let headers = dict [ "Content-Type", "application/json" ]

            return
                APIGatewayHttpApiV2ProxyResponse(
                    StatusCode = 200,
                    Body = body,
                    Headers = Dictionary(headers)
                )
        }
        |> Async.StartAsTask
