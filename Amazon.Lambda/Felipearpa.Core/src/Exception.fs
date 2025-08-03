namespace Felipearpa.Core

open System
open System.Net
open System.Text
open Microsoft.AspNetCore.Http
open Microsoft.AspNetCore.Diagnostics

module ExceptionWriter =

    type IExceptionTransformer =

        abstract GetHttpStatusCode: Exception -> HttpStatusCode

        abstract GetContentType: Exception -> string

        abstract GetContent: Exception -> string

    let asyncCreateResponseForError (context: HttpContext) (exceptionTransformer: IExceptionTransformer) =
        async {

            let feature =
                context.Features.Get<IExceptionHandlerPathFeature>()

            let error = feature.Error

            context.Response.ContentType <- exceptionTransformer.GetContentType(error)

            context.Response.StatusCode <-
                exceptionTransformer.GetHttpStatusCode(error)
                |> LanguagePrimitives.EnumToValue

            let resultBytes =
                Encoding.UTF8.GetBytes(exceptionTransformer.GetContent(error))

            context.Response.Body.WriteAsync(resultBytes, 0, resultBytes.Length)
            |> Async.AwaitTask
            |> ignore
        }
