module ExceptionTests

open System
open System.Net
open Microsoft.AspNetCore.Http
open Microsoft.AspNetCore.Diagnostics
open Moq
open Xunit
open Pipel.Core
open Pipel.Core.Json

[<Fact>]
let ``given an exception when a text that represents the exception is created then an option with the text is returned``
    ()
    =
    let context = DefaultHttpContext() :> HttpContext

    let exceptionHandlerPathFeatureMock = Mock<IExceptionHandlerPathFeature>()

    exceptionHandlerPathFeatureMock
        .Setup(fun x -> x.Error)
        .Returns(Exception("Occurs an error"))
    |> ignore

    context.Features.Set<IExceptionHandlerPathFeature>(exceptionHandlerPathFeatureMock.Object)

    let jsonOpt =
        Exception.createResponseText context (fun _ -> Some "GENERIC-0") (DefaultJsonSerializer())

    Assert.True(jsonOpt.IsSome)

[<Fact>]
let ``given an exception when the response is updated then an the body of the response is updated`` () =
    let context = DefaultHttpContext() :> HttpContext

    let exceptionHandlerPathFeatureMock = Mock<IExceptionHandlerPathFeature>()

    exceptionHandlerPathFeatureMock
        .Setup(fun x -> x.Error)
        .Returns(Exception("Occurs an error"))
    |> ignore

    context.Features.Set<IExceptionHandlerPathFeature>(exceptionHandlerPathFeatureMock.Object)

    Exception.asyncUpdateResponseToDefaultError context (fun _ -> Some "GENERIC-0") (DefaultJsonSerializer())
    |> Async.RunSynchronously

    let buffer: byte array = Array.zeroCreate 4096

    let _ =
        context.Response.Body.Read(buffer, 0, buffer.Length)

    Assert.Equal(
        context.Response.StatusCode,
        HttpStatusCode.InternalServerError
        |> LanguagePrimitives.EnumToValue
    )
