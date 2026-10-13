namespace Felipearpa.Tyche.Pool.Test.Application

#nowarn "3536"

open System
open System.Collections.Generic
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Tyche.Pool.Test.Fakes
open Felipearpa.Tyche.Pool.Test.Infrastructure.PaginationSupport
open Felipearpa.Tyche.Pool.Test.Settlement
open Felipearpa.Tyche.Pool.Test.Settlement.PoolScenario
open Felipearpa.Type
open FsUnitTyped
open Xunit

/// Application consumers of shared-codec continuations across forced storage pages: added-match
/// fan-out and joined-gambler layout-match traversal. The grouped Timeline's phases are covered
/// by GroupedBetFeedTest.
module ContinuationTraversalTest =

    let private addedMatch result =
        { MatchId = match3
          MatchDateTime = pastKickoff.AddDays(1.0)
          Result = result }

    let private migrated (serializer: IKeySerializer) =
        async {
            let harness =
                VersionedSettlementHarness(
                    { HarnessOptions.Default with
                        KeySerializer = serializer }
                )

            do! harness.SeedAndMigrateAsync(defaultMembers, defaultMatches)
            harness.Fake.LimitQueryPages 1
            harness.Fake.TakeQueries() |> ignore
            return harness
        }

    let private failLaterPages (indexOrTable: string) (request: obj) =
        match request with
        | :? QueryRequest as query when
            (query.IndexName = indexOrTable || (isNull query.IndexName && query.TableName = indexOrTable))
            && not (isNull query.ExclusiveStartKey)
            && query.ExclusiveStartKey.Count > 0
            ->
            FailBefore(AmazonDynamoDBException "Throughput exceeded")
        | _ -> Proceed

    let private reconciliation = $"{poolId}#MATCH#{match3}#INSERT#{match3}"

    [<Fact>]
    let ``given members on separate gambler pages when an added match is fanned out then every page is reached`` () =
        async {
            let! harness = migrated typed

            do! harness.HandleLayoutRecordsAsync [ MatchInserted(addedMatch (Some(1, 1))) ]

            for gamblerId in [ gamblerA; gamblerB; gamblerC ] do
                detail harness.Fake gamblerId match3 |> Option.isSome |> shouldEqual true

            harness.Commands.Pending |> List.map _.DeduplicationId |> shouldEqual [ reconciliation ]

            harness.Fake.TakeQueries()
            |> List.filter (fun query -> query.IndexName = PoolTable.Index.gamblersByPoolLayout)
            |> List.map (fun query ->
                query.ExclusiveStartKey
                |> Option.ofObj
                |> Option.filter (fun key -> key.Count > 0)
                |> Option.map (fun key -> key["getGamblersByPoolLayoutSk"].S))
            |> shouldEqual
                [ None
                  Some $"POOL#{poolId}#GAMBLER#{gamblerA}"
                  Some $"POOL#{poolId}#GAMBLER#{gamblerB}" ]
        }

    [<Fact>]
    let ``given a later gambler page fails when an added match is fanned out then the work fails and redelivery completes it`` () =
        async {
            let! harness = migrated typed
            harness.Fake.Intercept(failLaterPages PoolTable.Index.gamblersByPoolLayout)

            let! _ = failureOf (harness.HandleLayoutRecordsAsync [ MatchInserted(addedMatch (Some(1, 1))) ])

            detail harness.Fake gamblerA match3 |> Option.isSome |> shouldEqual true
            detail harness.Fake gamblerC match3 |> shouldEqual None
            harness.Commands.Pending |> shouldEqual []

            harness.Fake.ClearInterception()
            do! harness.HandleLayoutRecordsAsync [ MatchInserted(addedMatch (Some(1, 1))) ]

            for gamblerId in [ gamblerA; gamblerB; gamblerC ] do
                detail harness.Fake gamblerId match3 |> Option.isSome |> shouldEqual true

            harness.Commands.Pending |> List.map _.DeduplicationId |> shouldEqual [ reconciliation ]
        }

    [<Fact>]
    let ``given a pk and sk only writer when fan-out crosses a gambler page then it fails instead of completing`` () =
        async {
            let! harness = migrated legacyTokens

            let! reason = invalidContinuationOf (harness.HandleLayoutRecordsAsync [ MatchInserted(addedMatch None) ])

            reason |> shouldEqual (MissingKeyAttribute "getGamblersByPoolLayoutPk")
            detail harness.Fake gamblerC match3 |> shouldEqual None
        }

    let private joinedD (harness: VersionedSettlementHarness) =
        async {
            let! joined = harness.JoinAsync gamblerD
            joined |> shouldEqual (Ok())
            harness.Fake.TakeChanges() |> ignore

            let materializer =
                MaterializeJoinedGambler(
                    harness.Bets,
                    harness.Projection,
                    harness.LayoutReader,
                    harness.CommandPublisher,
                    fun () -> harness.Now()
                )

            let gambler =
                { PoolLayoutGambler.PoolId = Ulid.newOf poolId
                  PoolLayoutId = Ulid.newOf poolLayoutId
                  PoolLayoutVersion = 1
                  GamblerId = Ulid.newOf gamblerD
                  GamblerUsername = NonEmptyString100.newOf $"user-{gamblerD.Substring 22}"
                  GamblerEmail = NonEmptyString100.newOf $"{gamblerD.Substring 22}@example.com" }

            return fun () -> materializer.ExecuteAsync(gambler, "EVENT1")
        }

    let serializers: obj array seq = [ [| box "version 1" |]; [| box "legacy pk/sk" |] ]

    [<Theory; MemberData(nameof serializers)>]
    let ``given layout matches on separate pages when a joined gambler is materialized then every match is reached``
        (writer: string)
        =
        async {
            let! harness = migrated (if writer = "version 1" then typed else legacyTokens)
            let! materialize = joinedD harness

            do! materialize ()

            for matchId in [ match0; match1; match2 ] do
                detail harness.Fake gamblerD matchId |> Option.isSome |> shouldEqual true
        }

    [<Fact>]
    let ``given a later layout-match page fails when a joined gambler is materialized then the work fails and a retry completes it`` () =
        async {
            let! harness = migrated typed
            let! materialize = joinedD harness
            harness.Fake.Intercept(failLaterPages "PoolLayout")

            let! _ = failureOf (materialize ())

            detail harness.Fake gamblerD match0 |> Option.isSome |> shouldEqual true
            detail harness.Fake gamblerD match2 |> shouldEqual None

            harness.Fake.ClearInterception()
            do! materialize ()

            for matchId in [ match0; match1; match2 ] do
                detail harness.Fake gamblerD matchId |> Option.isSome |> shouldEqual true
        }
