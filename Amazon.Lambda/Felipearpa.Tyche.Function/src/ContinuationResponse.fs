namespace Felipearpa.Tyche.Function

open Felipearpa.Data.DynamoDb
open Microsoft.AspNetCore.Http

/// HTTP handling of `next` continuation tokens, shared by the HttpApi and Lambda hosts. See
/// docs/backend-pagination.md.
module ContinuationResponse =

    /// The error code of a `next` token that cannot be used for the request.
    [<Literal>]
    let invalidCursorCode = "INVALID_CURSOR"

    /// HTTP 400 `INVALID_CURSOR`. The client loads the list again from the first page, without
    /// `next`; sending the same token again fails the same way.
    let invalidCursor () : IResult =
        Results.Json(
            {| error = invalidCursorCode
               message = "The continuation is not valid; restart from the first page" |},
            statusCode = 400
        )

    /// Runs a paginated request and answers `invalidCursor` when its `next` token cannot be
    /// decoded or does not fit the requested query. The token is checked before the query is
    /// sent, so no query runs with it. Every other failure, including a DynamoDB failure for a
    /// valid token, propagates unchanged.
    let orInvalidCursor (response: IResult Async) : IResult Async =
        async {
            try
                return! response
            with InvalidContinuationException _ ->
                return invalidCursor ()
        }
