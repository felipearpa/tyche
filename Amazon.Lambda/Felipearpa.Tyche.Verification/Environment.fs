namespace Felipearpa.Tyche.Verification

open System

/// Names of the isolated resources. The handlers read the same variables, so they never
/// touch the production tables or queues while this program runs.
module VerificationEnvironment =

    let private required (name: string) =
        match Environment.GetEnvironmentVariable name with
        | null
        | "" -> failwith $"Set {name} to an isolated verification resource"
        | value -> value

    let poolTable () = required "POOL_TABLE_NAME"
    let poolLayoutTable () = required "POOL_LAYOUT_TABLE_NAME"
    let commandQueueUrl () = required "POOL_COMMAND_QUEUE_URL"
    let maintenanceQueueUrl () = required "POOL_MAINTENANCE_QUEUE_URL"
    let commandDeadLetterQueueUrl () = required "POOL_COMMAND_DLQ_URL"

    /// Refuses to run against the production table names.
    let assertIsolatedTables () =
        if poolTable () = "Pool" || poolLayoutTable () = "PoolLayout" then
            failwith "Refusing to run verification against production tables"

    /// Refuses to run against the production names.
    let assertIsolated () =
        if poolTable () = "Pool" || poolLayoutTable () = "PoolLayout" || commandQueueUrl().EndsWith "/UpdatePositionsQueue.fifo" then
            failwith "Refusing to run verification against production resources"
