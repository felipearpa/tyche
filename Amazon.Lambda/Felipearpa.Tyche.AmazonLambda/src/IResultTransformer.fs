namespace Felipearpa.Tyche.AmazonLambda

open System.Collections.Generic
open System.IO
open System.Text
open Amazon.Lambda.APIGatewayEvents
open Microsoft.AspNetCore.Http
open Microsoft.Extensions.DependencyInjection

[<AutoOpen>]
module IResultTransformer =

    type IResult with

        member this.ToAmazonProxyResponse() : APIGatewayHttpApiV2ProxyResponse Async =
            async {
                let httpContext = DefaultHttpContext()

                use bodyStream = new MemoryStream()
                httpContext.Response.Body <- bodyStream

                httpContext.RequestServices <- ServiceCollection().AddLogging().BuildServiceProvider()

                do! this.ExecuteAsync(httpContext) |> Async.AwaitTask

                bodyStream.Position <- 0L
                use reader = new StreamReader(bodyStream, Encoding.UTF8)
                let! body = reader.ReadToEndAsync() |> Async.AwaitTask

                let headers =
                    httpContext.Response.Headers
                    |> Seq.map (fun kv -> kv.Key, kv.Value.ToString())
                    |> dict

                return
                    APIGatewayHttpApiV2ProxyResponse(
                        StatusCode = httpContext.Response.StatusCode,
                        Body = body,
                        Headers = Dictionary(headers)
                    )
            }
