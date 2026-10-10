namespace Felipearpa.Tyche.Pool.Test.Settlement

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Core.Json
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Tyche.Pool.Test.Fakes
open Felipearpa.Tyche.PoolLayout.Application
open Felipearpa.Tyche.PoolLayout.Infrastructure
open Felipearpa.Type

type HarnessOptions =
    { Settings: PoolOperationSettings
      /// How far the clock moves on every reading; zero keeps it still.
      Tick: TimeSpan
      /// The time an invocation may work before it must yield.
      InvocationBudget: TimeSpan
      RankedPageSize: int
      CommandRedriveLimit: int }

    static member Default =
        { Settings = PoolOperationSettings.Default
          Tick = TimeSpan.Zero
          InvocationBudget = TimeSpan.FromHours 1.0
          RankedPageSize = 50
          CommandRedriveLimit = 25 }

/// A command delivery that yielded, with the wait the handler asked the queue for.
exception CommandYielded of TimeSpan option

/// Wires the versioned settlement flow to the fake table and queues. Stream delivery is
/// simulated from the fake's change log, the command queue keeps per-pool FIFO order, and
/// the maintenance queue runs jobs in any order.
type VersionedSettlementHarness(options: HarnessOptions) =
    let fake = FakeTables.create ()
    let commands = FakeFifoQueue(options.CommandRedriveLimit)
    let jobs = FakeFifoQueue(25)
    let mutable clockOffset = TimeSpan.Zero
    let mutable commandOutcomes: PoolCommandReport list = []

    let now () =
        clockOffset <- clockOffset + options.Tick
        DateTime.UtcNow + clockOffset

    let keySerializer = DynamoDbKeySerializer(JsonSerializer())
    let repository = PoolSettlementDynamoDbRepository(fake.Client)
    let settlement = repository :> IPoolSettlementRepository
    let projection = repository :> IPoolProjectionRepository
    let bets = PoolGamblerBetDynamoDbRepository(keySerializer, fake.Client) :> IPoolGamblerBetRepository
    let pools = PoolDynamoDbRepository(keySerializer, fake.Client) :> IPoolRepository
    let layoutRepository =
        PoolLayoutDynamoDbRepository(keySerializer, fake.Client) :> Felipearpa.Tyche.PoolLayout.Domain.IPoolLayoutRepository
    let layoutReader = PoolLayoutMatchReader(layoutRepository) :> IPoolLayoutMatchReader

    let commandPublisher =
        { new IPoolCommandPublisher with
            member _.PublishAsync(published) =
                async {
                    for command in published do
                        commands.Send(
                            PoolMessageJson.encodeCommand command,
                            command.PoolId.Value,
                            PoolMessageJson.deduplicationId command
                        )
                }

            member _.RepublishAsync(published) =
                async {
                    for command in published do
                        commands.Send(PoolMessageJson.encodeCommand command, command.PoolId.Value, Guid.NewGuid().ToString())
                } }

    let maintenancePublisher =
        { new IPoolMaintenancePublisher with
            member _.PublishAsync(published) =
                async {
                    for job in published do
                        let id = Guid.NewGuid().ToString()
                        jobs.Send(PoolMessageJson.encodeJob job, id, id)
                } }

    let processCommand =
        ProcessPoolCommand(settlement, layoutReader, commandPublisher, options.Settings, now)

    let dispatch = DispatchMatchResult(settlement, commandPublisher)
    let fanOut = FanOutMatchToGamblers(pools, bets, projection, layoutReader, commandPublisher)
    let materializeJoin = MaterializeJoinedGambler(bets, projection, layoutReader, commandPublisher, now)
    let cleanUp = CleanUpRemovedMembership(projection)
    let react = ReactToMembershipChanges(commandPublisher, maintenancePublisher)
    let propagate = PropagateMembershipTotal(projection, maintenancePublisher)
    let identities = InitializePoolIdentities(projection, maintenancePublisher)
    let backfill = BackfillPoolProjection(projection, maintenancePublisher)
    let readiness = VerifyPoolRankingReadiness(settlement, projection)
    let reconcile =
        Felipearpa.Tyche.Pool.Application.ReconcilePool(
            settlement,
            projection,
            bets,
            layoutReader,
            commandPublisher,
            maintenancePublisher
        )

    let ranked =
        GetRankedPoolMatchBets(
            settlement,
            projection,
            layoutReader,
            RankedMatchBetsDynamoDbRepository(fake.Client),
            { PageSize = options.RankedPageSize },
            now
        )

    let identityReports = List<IdentityInitializationReport>()
    let readinessReports = List<RankingReadinessReport option>()

    let s (value: string) = AttributeValue(S = value)

    let layoutKey matchId =
        dict [ "pk", s $"POOLLAYOUT#{PoolScenario.poolLayoutId}"; "sk", s $"MATCH#{matchId}" ]

    let resultOf matchId =
        let item = fake.TryGet("PoolLayout", layoutKey matchId) |> Option.get

        { HomeTeamScore = int item["homeTeamScore"].N
          AwayTeamScore = int item["awayTeamScore"].N
          Version =
            match item.TryGetValue "resultVersion" with
            | true, value -> int value.N
            | _ -> 1 }

    let writeLayout (record: LayoutRecord) =
        match record with
        | MatchInserted seed ->
            let item = PoolScenario.layoutMatchItem seed
            if seed.Result.IsSome then item["resultVersion"] <- AttributeValue(N = "1")
            fake.Put("PoolLayout", item)
        | ResultChanged(matchId, home, away) ->
            let item = fake.TryGet("PoolLayout", layoutKey matchId) |> Option.get
            item["homeTeamScore"] <- AttributeValue(N = string home)
            item["awayTeamScore"] <- AttributeValue(N = string away)
            item["resultVersion"] <- AttributeValue(N = "1")
            fake.Put("PoolLayout", item)

    let handleCommand (message: FakeQueueMessage) =
        async {
            let command =
                match PoolMessageJson.decodePoolMessage message.Body with
                | PoolMessageJson.Command command -> command
                | PoolMessageJson.LegacyPositions(poolId, matchId) ->
                    PoolCommand.reconcileMatch poolId matchId $"LEGACY#{message.DeduplicationId}"

            let deadline = now () + options.InvocationBudget
            let! report = processCommand.ExecuteAsync(command, deadline)
            commandOutcomes <- commandOutcomes @ [ report ]

            match report.Outcome with
            | PoolCommandOutcome.Yielded -> return raise (CommandYielded report.RetryAfter)
            | PoolCommandOutcome.Blocked ->
                return raise (InvalidOperationException $"Command {command.OperationId} not finished: {report.Outcome}")
            | _ -> return ()
        }

    /// Models the queue's redelivery timing: a yielded message comes back after the wait the
    /// handler asked for, a failed one only after the visibility timeout, which outlasts the
    /// crashed worker's lease.
    let handleCommandWithLeaseRecovery (message: FakeQueueMessage) =
        async {
            let! outcome = handleCommand message |> Async.Catch

            match outcome with
            | Choice1Of2() -> return ()
            | Choice2Of2(CommandYielded retryAfter) ->
                clockOffset <- clockOffset + (retryAfter |> Option.defaultValue TimeSpan.Zero)
                return raise (InvalidOperationException "Command yielded; delivered again")
            | Choice2Of2 error ->
                clockOffset <- clockOffset + options.Settings.LeaseDuration + TimeSpan.FromSeconds 1.0
                return raise error
        }

    let handleJob (message: FakeQueueMessage) =
        async {
            match PoolMessageJson.decodeJob message.Body with
            | PropagateTotal(poolId, gamblerId, cursor) ->
                let! _ = propagate.ExecuteAsync(poolId, gamblerId, cursor)
                ()
            | InitializeIdentities(poolId, cursor) ->
                let! report = identities.ExecuteAsync(poolId, cursor)
                identityReports.Add report
            | BackfillProjection(poolId, cursor) ->
                let! _ = backfill.ExecuteAsync(poolId, cursor)
                ()
            | VerifyReadiness poolId ->
                let! report = readiness.ExecuteAsync poolId
                readinessReports.Add report
            | ReconcilePool(poolId, cursor) ->
                let! _ = reconcile.ExecuteAsync(poolId, cursor, $"RECONCILE#{message.DeduplicationId}")
                ()
            | RetireMembership(poolId, gamblerId, generation) -> do! cleanUp.ExecuteAsync(poolId, gamblerId, generation)
        }

    let isMembership (image: Dictionary<string, AttributeValue> option) =
        image
        |> Option.exists (fun item -> item["pk"].S.StartsWith "POOL#" && item["sk"].S.StartsWith "GAMBLER#")

    let attribute (name: string) (image: Dictionary<string, AttributeValue> option) =
        image
        |> Option.bind (fun item ->
            match item.TryGetValue name with
            | true, value -> Some(if isNull value.S then value.N else value.S)
            | _ -> None)

    let gamblerOf (image: Dictionary<string, AttributeValue>) =
        { PoolLayoutGambler.PoolId = Ulid.newOf image["poolId"].S
          PoolLayoutId = Ulid.newOf image["poolLayoutId"].S
          PoolLayoutVersion = int image["poolLayoutVersion"].N
          GamblerId = Ulid.newOf image["gamblerId"].S
          GamblerUsername = NonEmptyString100.newOf image["gamblerUsername"].S
          GamblerEmail = NonEmptyString100.newOf image["gamblerEmail"].S }

    /// Delivers the committed Pool changes to the membership stream handlers.
    let deliverStream () =
        async {
            let changes =
                fake.TakeChanges()
                |> List.filter (fun change -> change.TableName = "Pool" && (isMembership change.OldImage || isMembership change.NewImage))

            for change in changes do
                let source = $"EVENT{change.Sequence}"

                match change.OldImage, change.NewImage with
                | None, Some image -> do! materializeJoin.ExecuteAsync(gamblerOf image, source)
                | Some image, None ->
                    do!
                        cleanUp.ExecuteAsync(
                            Ulid.newOf image["poolId"].S,
                            Ulid.newOf image["gamblerId"].S,
                            attribute "membershipGeneration" change.OldImage
                        )
                | _ -> ()

            let membershipChanges =
                changes
                |> List.choose (fun change ->
                    let image = change.NewImage |> Option.orElse change.OldImage |> Option.get
                    let poolId = Ulid.newOf image["poolId"].S
                    let gamblerId = Ulid.newOf image["gamblerId"].S

                    match change.OldImage, change.NewImage with
                    | None, Some _ -> Some(MembershipCreated(poolId, gamblerId), $"EVENT{change.Sequence}")
                    | Some _, None -> Some(MembershipRemoved(poolId, gamblerId), $"EVENT{change.Sequence}")
                    | old, current when
                        attribute "scoreVersion" old <> attribute "scoreVersion" current
                        || attribute "membershipGeneration" old <> attribute "membershipGeneration" current
                        ->
                        Some(MembershipTotalChanged(poolId, gamblerId), $"EVENT{change.Sequence}")
                    | _ -> None)

            do! react.ExecuteAsync membershipChanges
            return not changes.IsEmpty
        }

    new() = VersionedSettlementHarness(HarnessOptions.Default)

    member _.Fake = fake
    member _.Commands = commands
    member _.Jobs = jobs
    member _.Settlement = settlement
    member _.Projection = projection
    member _.Bets = bets
    member _.Pools = pools
    member _.LayoutRepository = layoutRepository
    member _.ProcessCommand = processCommand
    member _.Dispatch = dispatch
    member _.CommandPublisher = commandPublisher
    member _.MaintenancePublisher = maintenancePublisher
    member _.Ranked = ranked
    member _.Now() = now ()
    member _.AdvanceClock(by: TimeSpan) = clockOffset <- clockOffset + by
    member _.CommandOutcomes = commandOutcomes
    member _.IdentityReports = identityReports |> List.ofSeq
    member _.ReadinessReports = readinessReports |> List.ofSeq

    member _.DeliverCommandsAsync() = commands.Drain(handleCommandWithLeaseRecovery, 200)

    member _.DeliverJobsAsync() = jobs.Drain(handleJob, 200)

    member _.DeliverStreamAsync() = deliverStream ()

    /// Runs stream delivery, commands, and jobs until nothing is pending.
    member this.RunUntilQuiescentAsync() =
        let rec loop round =
            async {
                let! streamed = deliverStream ()
                do! commands.Drain(handleCommandWithLeaseRecovery, 200)
                do! jobs.Drain(handleJob, 200)

                let pending =
                    streamed
                    || fake.HasPendingChanges
                    || not commands.Pending.IsEmpty
                    || not jobs.Pending.IsEmpty

                if pending && round < 50 then
                    return! loop (round + 1)
            }

        loop 0

    /// Publishes jobs as an operator would, then runs everything to quiescence.
    member this.RunJobsAsync(published: PoolMaintenanceJob list) =
        async {
            do! maintenancePublisher.PublishAsync published
            do! this.RunUntilQuiescentAsync()
        }

    /// Seeds legacy records, then performs the identity initialization and projection
    /// backfill that the migration runs before the new flow is activated.
    member this.SeedAndMigrateAsync(members, matches) =
        async {
            PoolScenario.seedLegacy fake members matches
            let poolId = Ulid.newOf PoolScenario.poolId
            do! this.RunJobsAsync [ InitializeIdentities(poolId, None) ]
            do! this.RunJobsAsync [ BackfillProjection(poolId, None) ]
            fake.TakeChanges() |> ignore
        }

    member this.HandleLayoutRecordsAsync(records: LayoutRecord list) =
        async {
            records |> List.iter writeLayout

            for record in records do
                match record with
                | ResultChanged(matchId, _, _) ->
                    let! _ = dispatch.ExecuteAsync(Ulid.newOf matchId, resultOf matchId)
                    ()
                | MatchInserted seed ->
                    do!
                        fanOut.ExecuteAsync(
                            { FanOutMatchInput.MatchId = Ulid.newOf seed.MatchId
                              PoolLayoutId = Ulid.newOf PoolScenario.poolLayoutId
                              PoolLayoutVersion = 1
                              HomeTeamId = "COL"
                              HomeTeamName = NonEmptyString100.newOf "Colombia"
                              AwayTeamId = "ARG"
                              AwayTeamName = NonEmptyString100.newOf "Argentina"
                              MatchDateTime = seed.MatchDateTime
                              Round = "Fase de grupos"
                              GroupName = None },
                            $"INSERT#{seed.MatchId}"
                        )
        }

    /// Publishes a corrected result through the supported versioned write, then dispatches it.
    member this.CorrectResultAsync(matchId: string, home: int, away: int) =
        async {
            let current = resultOf matchId

            let! accepted =
                layoutRepository.CorrectMatchResultAsync(
                    Ulid.newOf PoolScenario.poolLayoutId,
                    Ulid.newOf matchId,
                    home,
                    away,
                    current.Version
                )

            if accepted then
                let! _ = dispatch.ExecuteAsync(Ulid.newOf matchId, resultOf matchId)
                ()

            return accepted
        }

    /// Writes a first result (version 1) to the layout match without running any handler.
    member this.SetLayoutResult(matchId: string, home: int, away: int) =
        let item = fake.TryGet("PoolLayout", layoutKey matchId) |> Option.get
        item["homeTeamScore"] <- AttributeValue(N = string home)
        item["awayTeamScore"] <- AttributeValue(N = string away)
        item["resultVersion"] <- AttributeValue(N = "1")
        fake.Put("PoolLayout", item)

    /// Writes a first result and settles it through the command queue and stream.
    member this.SettleAsync(matchId: string, home: int, away: int) =
        async {
            this.SetLayoutResult(matchId, home, away)
            let! _ = dispatch.ExecuteAsync(Ulid.newOf matchId, resultOf matchId)
            do! this.RunUntilQuiescentAsync()
        }

    member this.JoinAsync(gamblerId: string) =
        pools.JoinPoolAsync
            { ResolvedJoinPoolInput.PoolId = Ulid.newOf PoolScenario.poolId
              PoolName = NonEmptyString100.newOf "Polla"
              GamblerId = Ulid.newOf gamblerId
              GamblerUsername = NonEmptyString100.newOf $"user-{gamblerId.Substring(22)}"
              GamblerEmail = NonEmptyString100.newOf $"{gamblerId.Substring(22)}@example.com"
              PoolLayoutId = Ulid.newOf PoolScenario.poolLayoutId
              PoolLayoutVersion = 1 }

    member this.RemoveAsync(gamblerId: string) =
        pools.RemoveGamblerAsync(Ulid.newOf PoolScenario.poolId, Ulid.newOf gamblerId)

    interface ISettlementHarness with
        member _.Fake = fake
        member _.Queue = commands

        member this.SeedAsync(members, matches) = this.SeedAndMigrateAsync(members, matches)

        member this.HandleLayoutAsync(records) = this.HandleLayoutRecordsAsync records

        member this.DeliverAsync() = this.RunUntilQuiescentAsync()

        member this.RedeliverAsync(matchId) =
            async {
                commands.ExpireDeduplication()
                let result = resultOf matchId
                let command = PoolCommand.settleResult (Ulid.newOf PoolScenario.poolId) (Ulid.newOf matchId) result
                commands.Send(PoolMessageJson.encodeCommand command, command.PoolId.Value, PoolMessageJson.deduplicationId command)
            }
