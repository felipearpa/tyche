namespace Felipearpa.Tyche.Function

open System.Runtime.CompilerServices
open Microsoft.AspNetCore.Http

[<AutoOpen>]
module ResultExtensions =

    type Results with

        [<Extension>]
        static member InternalServerError() : IResult = Results.StatusCode(500)

        [<Extension>]
        static member InternalServerError(body: string) : IResult =
            Results.Text(content = body, statusCode = 500)
