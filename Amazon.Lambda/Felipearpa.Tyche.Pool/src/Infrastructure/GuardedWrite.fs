namespace Felipearpa.Tyche.Pool.Infrastructure

open System
open System.Collections.Generic
open System.Runtime.ExceptionServices
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Domain

/// Translates DynamoDB write failures into guarded-write outcomes at the data-access
/// boundary. Only condition failures and transaction contention become outcomes;
/// throttling, validation, and transport failures keep propagating with their original
/// exception after the SDK's own bounded retries.
module GuardedWrite =

    let rec baseException (error: exn) =
        match error with
        | :? AggregateException as aggregate when aggregate.InnerExceptions.Count = 1 ->
            baseException aggregate.InnerException
        | _ -> error

    let private rethrow (error: exn) : 'T =
        ExceptionDispatchInfo.Capture(error).Throw()
        Unchecked.defaultof<'T>

    let private classify (error: exn) =
        match baseException error with
        | :? ConditionalCheckFailedException -> Some ConditionNotMet
        | :? TransactionConflictException -> Some Contended
        | :? TransactionCanceledException as cancelled ->
            let codes =
                cancelled.CancellationReasons
                |> Option.ofObj
                |> Option.map (Seq.map _.Code >> List.ofSeq)
                |> Option.defaultValue []

            // A condition failure is decisive; contention alone is retryable. Any other code
            // (throttling, validation) is not a guarded outcome.
            if codes |> List.contains "ConditionalCheckFailed" then
                Some ConditionNotMet
            elif codes |> List.forall (fun code -> code = "None" || code = "TransactionConflict") then
                Some Contended
            else
                None
        | _ -> None

    let run (write: Async<unit>) : Async<GuardedWriteOutcome> =
        async {
            let! outcome = write |> Async.Catch

            match outcome with
            | Choice1Of2() -> return Written
            | Choice2Of2 error ->
                match classify error with
                | Some guarded -> return guarded
                | None -> return rethrow (baseException error)
        }

    /// Runs a conditional write whose condition failure means "someone else got there first".
    let succeeded (write: Async<unit>) : Async<bool> =
        async {
            match! run write with
            | Written -> return true
            | ConditionNotMet -> return false
            | Contended -> return false
        }

    let emptyIfNull (dictionary: Dictionary<string, AttributeValue>) =
        dictionary |> Option.ofObj |> Option.defaultWith Dictionary
