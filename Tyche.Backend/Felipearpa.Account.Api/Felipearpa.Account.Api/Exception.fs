namespace Felipearpa.Account.Api

open System.Net
open Felipearpa.Core.ExceptionWriter

exception DomainException of error: string with
    override this.Message = this.error

type ExceptionTransformer() =

    interface IExceptionTransformer with

        member this.GetHttpStatusCode(error) =
            match error with
            | :? DomainException -> HttpStatusCode.Conflict
            | _ -> HttpStatusCode.InternalServerError

        member this.GetContentType(_) = "text/plain"

        member this.GetContent(error) = error.Message
