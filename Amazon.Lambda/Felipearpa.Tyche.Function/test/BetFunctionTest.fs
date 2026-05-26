namespace Felipearpa.Tyche.Function.Test

open Felipearpa.Tyche.Function
open Felipearpa.Tyche.Function.Request
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type
open FsUnitTyped
open Microsoft.AspNetCore.Http
open Microsoft.Extensions.DependencyInjection
open Xunit

module BetFunctionTest =

    let private request () : BetRequest =
        { PoolId = "01K1PX1TX2NM1HG851S1V0QG6T"
          GamblerId = "01K1PX1TX2NM1HG851S1V0QG6Q"
          MatchId = "01K1PX1TX2NM1HG851S1V0QG6M"
          HomeTeamBet = 1
          AwayTeamBet = 0 }

    let private fakeRepoReturningMatchLocked () =
        { new IPoolGamblerBetRepository with
            member _.GetPendingPoolGamblerBetsAsync(_, _, _, _) = failwith "not used"
            member _.GetFinishedPoolGamblerBetsAsync(_, _, _, _) = failwith "not used"
            member _.GetLivePoolGamblerBetsAsync(_, _, _, _) = failwith "not used"
            member _.GetPoolMatchGamblerBetsAsync(_, _, _) = failwith "not used"
            member _.GetPoolGamblerBetByIdAsync(_, _, _) = failwith "not used"
            member _.AddPoolGamblerMatchAsync(_) = failwith "not used"
            member _.AddPoolGamblerMatchesAsync(_) = failwith "not used"
            member _.MaterializeMatchForGamblerAsync(_, _) = failwith "not used"
            member _.DeleteUncomputedBetsAsync(_, _) = failwith "not used"

            member _.BetAsync(_, _, _, _) =
                async { return Error BetFailure.MatchLocked } }

    let private buildContextWithoutAuthServices () =
        let context = DefaultHttpContext()
        context.RequestServices <- ServiceCollection().AddLogging().BuildServiceProvider()
        context

    [<Fact>]
    let ``given a locked match when bet then returns 403 without invoking the auth pipeline`` () =
        async {
            let bet = Bet(fakeRepoReturningMatchLocked ())

            let! result = BetFunction.betAsync (request ()) bet
            let context = buildContextWithoutAuthServices ()

            do! result.ExecuteAsync(context) |> Async.AwaitTask

            context.Response.StatusCode |> shouldEqual 403
        }
