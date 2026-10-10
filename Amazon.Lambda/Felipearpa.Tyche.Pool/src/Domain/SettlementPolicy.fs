namespace Felipearpa.Tyche.Pool.Domain

/// The persisted settlement state a guarded detail write expects to replace.
type DetailExpectation =
    | Unsettled
    /// Settled with an explicit result version.
    | SettledAt of version: int
    /// Settled before result versions existed; maps to result version 1.
    | LegacySettled

/// A settlement or correction to commit atomically with the membership total.
type SettlementChange =
    { Award: int
      PreviousAward: int option
      Expectation: DetailExpectation
      Generation: string
      ExpectedScoreVersion: int64
      PreviousTotal: int
      Total: int
      /// Each settlement records the total it started from as the membership's before score,
      /// as scoring always has, even when the award leaves the total unchanged.
      PreviousBeforeScore: int option }

    member this.TotalChanged = this.Total <> this.PreviousTotal

    member this.RefreshesBeforeScore =
        not this.TotalChanged && this.PreviousBeforeScore <> Some this.PreviousTotal

type SettlementDecision =
    /// The detail already carries this or a newer result version.
    | AlreadyApplied
    /// The detail belongs to an earlier membership of the same gambler.
    | ObsoleteMembership
    /// Identity metadata is missing; the detail waits for initialization or repair.
    | IdentityUnbound
    /// Persisted values contradict each other; the detail waits for repair.
    | Malformed of reason: string
    | Apply of SettlementChange

/// Decides how a result version applies to one membership's match detail. The scoring
/// formula itself stays in BetEvaluator.
module SettlementPolicy =

    /// The result version a detail reflects. Computed details written before result
    /// versions existed reflect version 1.
    let appliedVersion (bet: BetSettlementState) =
        match bet.ResultVersion, bet.IsComputed with
        | Some version, _ -> Some version
        | None, true -> Some 1
        | None, false -> None

    let private expectation (bet: BetSettlementState) =
        match bet.ResultVersion, bet.IsComputed with
        | Some version, _ -> SettledAt version
        | None, true -> LegacySettled
        | None, false -> Unsettled

    let private change generation scoreVersion total beforeScore (bet: BetSettlementState) (result: MatchResult) =
        let award = BetEvaluator.delta bet.Prediction result.Score
        let previousAward = if bet.IsComputed then bet.Award else None

        match bet.IsComputed, previousAward with
        | true, None -> Malformed "computed detail without an award"
        | _ ->
            Apply
                { Award = award
                  PreviousAward = previousAward
                  Expectation = expectation bet
                  Generation = generation
                  ExpectedScoreVersion = scoreVersion
                  PreviousTotal = total
                  Total = total + award - (previousAward |> Option.defaultValue 0)
                  PreviousBeforeScore = beforeScore }

    let decide (membership: MembershipState) (bet: BetSettlementState) (result: MatchResult) =
        match membership.Generation, membership.ScoreVersion, membership.Total, bet.Generation with
        | None, _, _, _
        | _, None, _, _
        | _, _, _, None -> IdentityUnbound
        | Some generation, _, _, Some betGeneration when generation <> betGeneration -> ObsoleteMembership
        | _, _, None, _ -> Malformed "membership without a total"
        | Some generation, Some scoreVersion, Some total, Some _ ->
            match appliedVersion bet with
            | Some applied when applied >= result.Version -> AlreadyApplied
            | _ -> change generation scoreVersion total membership.BeforeScore bet result
