namespace Felipearpa.Tyche.Pool.Test.Application

open System
open Felipearpa.Core.Paging
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type
open FsUnitTyped
open Xunit

module FanOutMatchToGamblersTest =

    let private poolLayoutId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6K"
    let private newVersion = 5

    let private input () : FanOutMatchInput =
        { MatchId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6M"
          PoolLayoutId = poolLayoutId
          PoolLayoutVersion = newVersion
          HomeTeamId = "01K1PX1TX2NM1HG851S1V0QG6N"
          HomeTeamName = NonEmptyString100.newOf "Tigre"
          AwayTeamId = "01K1PX1TX2NM1HG851S1V0QG6P"
          AwayTeamName = NonEmptyString100.newOf "América"
          MatchDateTime = DateTime(2026, 5, 1, 0, 0, 0, DateTimeKind.Utc)
          Round = "Fase de grupos"
          GroupName = None }

    let private gambler (id: string) (poolId: string) (version: int) : PoolLayoutGambler =
        { PoolId = Ulid.newOf poolId
          PoolLayoutId = poolLayoutId
          PoolLayoutVersion = version
          GamblerId = Ulid.newOf id
          GamblerUsername = NonEmptyString100.newOf $"gambler-{id}@tyche.com"
          GamblerEmail = NonEmptyString100.newOf $"gambler-{id}@tyche.com" }

    let private fakePoolRepo (pages: PoolLayoutGambler list list) =
        let mutable index = 0
        let queryCalls = ResizeArray<Ulid * string option>()

        let repo =
            { new IPoolRepository with
                member _.GetPoolByIdAsync(_) = failwith "not used"
                member _.CreatePoolAsync(_) = failwith "not used"
                member _.JoinPoolAsync(_) = failwith "not used"
                member _.IsPoolMemberAsync(_, _) = failwith "not used"
                member _.GetPoolMembersAsync(_, _) = failwith "not used"
                member _.DeletePoolAsync(_) = failwith "not used"
                member _.RemoveGamblerAsync(_, _) = failwith "not used"
                member _.PropagateGamblerUsernameAsync(_, _) = failwith "not used"

                member _.GetGamblersByPoolLayoutAsync(layoutId, next) =
                    queryCalls.Add(layoutId, next)
                    let current = pages[index]
                    let isLast = index = pages.Length - 1
                    index <- index + 1

                    async {
                        return
                            { CursorPage.Items = current
                              Next = if isLast then None else Some $"cursor-{index}" }
                    } }

        repo, queryCalls

    let private fakeBetRepo () =
        let captured = ResizeArray<InitialPoolGamblerBet * int>()

        let repo =
            { new IPoolGamblerBetRepository with
                member _.GetPendingPoolGamblerBetsAsync(_, _, _, _) = failwith "not used"
                member _.GetFinishedPoolGamblerBetsAsync(_, _, _, _) = failwith "not used"
                member _.GetLivePoolGamblerBetsAsync(_, _, _, _) = failwith "not used"
                member _.GetPoolMatchGamblerBetsAsync(_, _, _) = failwith "not used"
                member _.GetPoolGamblerBetByIdAsync(_, _, _) = failwith "not used"
                member _.BetAsync(_, _, _, _) = failwith "not used"
                member _.AddPoolGamblerMatchAsync(_) = failwith "not used"
                member _.AddPoolGamblerMatchesAsync(_) = failwith "not used"
                member _.DeleteUncomputedBetsAsync(_, _) = failwith "not used"

                member _.MaterializeMatchForGamblerAsync(bet, version) =
                    captured.Add(bet, version)
                    async { return () } }

        repo, captured

    [<Fact>]
    let ``given gamblers with version less than new when execute then materializes for each`` () =
        async {
            let g1 = gambler "01K1PX1TX2NM1HG851S1V0QG6Q" "01K1PX1TX2NM1HG851S1V0QG6T" 1
            let g2 = gambler "01K1PX1TX2NM1HG851S1V0QG6R" "01K1PX1TX2NM1HG851S1V0QG6W" 2

            let poolRepo, _ = fakePoolRepo [ [ g1; g2 ] ]
            let betRepo, captured = fakeBetRepo ()

            let service = FanOutMatchToGamblers(poolRepo, betRepo)

            do! service.ExecuteAsync(input ())

            captured.Count |> shouldEqual 2

            let bet1, version1 = captured[0]
            bet1.GamblerId |> shouldEqual g1.GamblerId
            bet1.PoolId |> shouldEqual g1.PoolId
            bet1.PoolLayoutVersion |> shouldEqual newVersion
            bet1.BetScore |> shouldEqual None
            bet1.ComputedRequestId |> shouldEqual None
            version1 |> shouldEqual newVersion

            let bet2, _ = captured[1]
            bet2.GamblerId |> shouldEqual g2.GamblerId
        }

    [<Fact>]
    let ``given gamblers at any version when execute then materializes for each`` () =
        async {
            let alreadyAtVersion =
                gambler "01K1PX1TX2NM1HG851S1V0QG6Q" "01K1PX1TX2NM1HG851S1V0QG6T" newVersion

            let aheadOfVersion =
                gambler "01K1PX1TX2NM1HG851S1V0QG6R" "01K1PX1TX2NM1HG851S1V0QG6W" (newVersion + 1)

            let needsUpdate =
                gambler "01K1PX1TX2NM1HG851S1V0QG6S" "01K1PX1TX2NM1HG851S1V0QG6V" 1

            let poolRepo, _ = fakePoolRepo [ [ alreadyAtVersion; aheadOfVersion; needsUpdate ] ]
            let betRepo, captured = fakeBetRepo ()

            let service = FanOutMatchToGamblers(poolRepo, betRepo)

            do! service.ExecuteAsync(input ())

            captured.Count |> shouldEqual 3
        }

    [<Fact>]
    let ``given paginated results when execute then materializes across all pages`` () =
        async {
            let g1 = gambler "01K1PX1TX2NM1HG851S1V0QG6Q" "01K1PX1TX2NM1HG851S1V0QG6T" 1
            let g2 = gambler "01K1PX1TX2NM1HG851S1V0QG6R" "01K1PX1TX2NM1HG851S1V0QG6W" 2

            let poolRepo, queryCalls = fakePoolRepo [ [ g1 ]; [ g2 ] ]
            let betRepo, captured = fakeBetRepo ()

            let service = FanOutMatchToGamblers(poolRepo, betRepo)

            do! service.ExecuteAsync(input ())

            queryCalls.Count |> shouldEqual 2
            captured.Count |> shouldEqual 2
        }
