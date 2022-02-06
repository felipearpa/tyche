namespace Pipel.Core

open System
open System.Net
open System.Text
open Microsoft.AspNetCore.Http
open Microsoft.AspNetCore.Diagnostics
open Pipel.Core

exception NotFoundException of string
exception AlreadyExistException of string

[<CLIMutable>]
type Error =
    { Code: string
      Message: string }

[<CLIMutable>]
type ExceptionResponse = { Error: Error }

module Exception =

    [<Literal>]
    let private prefixCode = @"CORE"

    let private exceptionsMaps =
        Map [ (typeof<NotFoundException>.Name, 1)
              (typeof<AlreadyExistException>.Name, 2) ]

    let formatException (prefix: string) (exceptionCode: int) = $"{prefix}-{string exceptionCode}"

    let private formatMyException (exceptionCode: int) =
        formatException prefixCode exceptionCode

    let private createCodeFromException (ex: Exception) (funcCreateCustomCode: Exception -> string option) =
        match funcCreateCustomCode ex with
        | Some it -> it
        | None ->
            match exceptionsMaps.TryFind <| ex.GetType().Name with
            | Some it -> formatMyException it
            | None -> formatMyException 0

    let createResponseText
        (context: HttpContext)
        (funcCreateCustomCode: Exception -> string option)
        (jsonSerializer: ISerializer)
        : string option =
        let feature =
            context.Features.Get<IExceptionHandlerPathFeature>()

        let ex = feature.Error

        match not <| isNull ex with
        | true ->
            jsonSerializer.Serialize(
                { ExceptionResponse.Error =
                      { Error.Code = createCodeFromException ex funcCreateCustomCode
                        Message = ex.Message } }
            )
            |> Some
        | false -> None

    let writeResponse (context: HttpContext) (content: string) =
        context.Response.ContentType <- "application/json"

        context.Response.StatusCode <-
            HttpStatusCode.InternalServerError
            |> LanguagePrimitives.EnumToValue

        let resultBytes = Encoding.UTF8.GetBytes(content)

        context.Response.Body.WriteAsync(resultBytes, 0, resultBytes.Length)
        |> Async.AwaitTask

    let asyncUpdateResponseToDefaultError
        (context: HttpContext)
        (funcCreateCustomCode: Exception -> string option)
        (jsonSerializer: ISerializer)
        =
        async {
            match createResponseText context funcCreateCustomCode jsonSerializer with
            | Some x -> do! writeResponse context x
            | None -> ()
        }
