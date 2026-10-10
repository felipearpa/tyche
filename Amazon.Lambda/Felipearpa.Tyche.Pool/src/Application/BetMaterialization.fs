namespace Felipearpa.Tyche.Pool.Application

open System
open Felipearpa.Tyche.Pool.Domain

type MaterializationResult =
    | Created
    | Existing
    /// The membership no longer exists, so no detail may be created for it.
    | MembershipGone

/// Creates match details only through the guarded initializer: the new row carries the
/// membership's current generation, score version, and total, and the transaction fails if
/// any of them changed after they were read, so a total update cannot finish while the row
/// is still absent and leave it stale.
module BetMaterialization =

    [<Literal>]
    let private bindingAttempts = 6

    let rec materialize
        (betRepository: IPoolGamblerBetRepository)
        (projectionRepository: IPoolProjectionRepository)
        (bet: InitialPoolGamblerBet)
        (attempt: int)
        =
        async {
            let! membership = projectionRepository.GetMembershipAsync(bet.PoolId, bet.GamblerId)

            match membership with
            | None -> return MembershipGone
            | Some membership ->
                match! betRepository.MaterializeBetAsync(bet, MembershipBinding.tryOf membership) with
                | Materialized -> return Created
                | AlreadyMaterialized -> return Existing
                | BindingChanged when attempt + 1 < bindingAttempts ->
                    do! Async.Sleep(Random.Shared.Next(10, 40 * (attempt + 1)))
                    return! materialize betRepository projectionRepository bet (attempt + 1)
                | BindingChanged ->
                    return
                        raise (
                            InvalidOperationException
                                $"Membership {bet.GamblerId.Value} in pool {bet.PoolId.Value} kept changing during materialization"
                        )
        }

    /// An uncomputed detail for the gambler's membership and the layout match.
    let uncomputedBet (gambler: PoolLayoutGambler) (layoutMatch: PoolLayoutMatchSnapshot) : InitialPoolGamblerBet =
        { InitialPoolGamblerBet.PoolId = gambler.PoolId
          GamblerId = gambler.GamblerId
          GamblerUsername = gambler.GamblerUsername
          GamblerEmail = gambler.GamblerEmail
          MatchId = layoutMatch.MatchId
          PoolLayoutId = layoutMatch.PoolLayoutId
          HomeTeamId = layoutMatch.HomeTeamId
          HomeTeamName = layoutMatch.HomeTeamName
          AwayTeamId = layoutMatch.AwayTeamId
          AwayTeamName = layoutMatch.AwayTeamName
          MatchDateTime = layoutMatch.MatchDateTime
          PoolLayoutVersion = layoutMatch.PoolLayoutVersion
          Round = layoutMatch.Round
          GroupName = layoutMatch.GroupName
          HomeTeamScore = None
          AwayTeamScore = None
          BetScore = None
          ComputedDateTime = None
          ComputedRequestId = None
          ResultVersion = None }

    /// A detail created when a gambler joins. Matches that already have a result follow the
    /// existing late-join rule: computed with zero points and no change to the total.
    let lateJoinBet
        (gambler: PoolLayoutGambler)
        (layoutMatch: PoolLayoutMatchSnapshot)
        (computedAt: DateTime)
        (computedRequestId: string)
        : InitialPoolGamblerBet =
        let bet = uncomputedBet gambler layoutMatch

        match layoutMatch.Result with
        | Some result ->
            { bet with
                HomeTeamScore = Some result.HomeTeamScore
                AwayTeamScore = Some result.AwayTeamScore
                BetScore = Some 0
                ComputedDateTime = Some computedAt
                ComputedRequestId = Some computedRequestId
                ResultVersion = Some result.Version }
        | None -> bet
