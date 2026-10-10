namespace Felipearpa.Tyche.Pool.Domain

/// The authoritative layout state of a retained detail's match.
type RetainedMatch =
    | MatchMissing
    | MatchUnscored
    | MatchScored of MatchResult

/// Moves one retained detail to the rejoined membership, committed atomically with the
/// membership total.
type RebindChange =
    { /// The generation the detail was read with; None for a detail read without one.
      ReadGeneration: string option
      /// The award the detail was read with, which the write expects to replace.
      ReadAward: int
      /// The result version the detail was read with; None for a detail scored before versions.
      ReadResultVersion: int option
      /// The rejoined membership's generation.
      Generation: string
      ExpectedScoreVersion: int64
      PreviousTotal: int
      /// The award the membership receives.
      Award: int
      /// The current result, when the detail reflects an older one and its award is
      /// re-evaluated.
      Reevaluated: MatchResult option
      /// The result version the restored detail reflects.
      ResultVersion: int }

    member this.Total = this.PreviousTotal + this.Award

    member this.TotalChanged = this.Award <> 0

    /// The membership score version after the write; it advances only when the total changes.
    member this.ScoreVersion =
        if this.TotalChanged then
            this.ExpectedScoreVersion + 1L
        else
            this.ExpectedScoreVersion

type RestoreDecision =
    /// The detail already belongs to the membership, for example after a lost response.
    | AlreadyRestored
    /// The detail is already held for repair against this membership.
    | AlreadyHeld
    /// The detail is not a retained computed detail.
    | NotRetained
    /// The membership no longer exists or has another generation; nothing may be added to it.
    | MembershipEnded
    /// The membership has the generation but no score version or total; persisted data is
    /// inconsistent.
    | InvalidMembership of reason: string
    /// The detail cannot be restored without repair; it gets the exclusion marker.
    | HoldForRepair of reason: string
    | Rebind of RebindChange

/// Decides how one retained detail is restored to a rejoined membership. The scoring formula
/// itself stays in BetEvaluator.
module RestorePolicy =

    /// A computed detail of another or no generation, which restoration has to resolve for the
    /// membership with the generation.
    let isRetained (generation: string) (bet: RetainedBetState) =
        bet.Settlement.IsComputed && bet.Settlement.Generation <> Some generation

    let private rebind (generation: string) scoreVersion total (bet: BetSettlementState) award (result: MatchResult) =
        match SettlementPolicy.appliedVersion bet with
        | Some applied when applied > result.Version -> HoldForRepair "detail reflects a newer result than its match"
        | Some applied when applied = result.Version ->
            Rebind
                { ReadGeneration = bet.Generation
                  ReadAward = award
                  ReadResultVersion = bet.ResultVersion
                  Generation = generation
                  ExpectedScoreVersion = scoreVersion
                  PreviousTotal = total
                  Award = award
                  Reevaluated = None
                  ResultVersion = applied }
        | _ ->
            Rebind
                { ReadGeneration = bet.Generation
                  ReadAward = award
                  ReadResultVersion = bet.ResultVersion
                  Generation = generation
                  ExpectedScoreVersion = scoreVersion
                  PreviousTotal = total
                  Award = BetEvaluator.delta bet.Prediction result.Score
                  Reevaluated = Some result
                  ResultVersion = result.Version }

    let private decideRetained generation scoreVersion total (bet: BetSettlementState) (matchState: RetainedMatch) =
        match matchState, bet.Award with
        | MatchMissing, _ -> HoldForRepair "match missing"
        | MatchUnscored, _ -> HoldForRepair "match has no result"
        | MatchScored _, None -> HoldForRepair "computed detail without an award"
        | MatchScored result, Some award -> rebind generation scoreVersion total bet award result

    let decide
        (generation: string)
        (membership: MembershipState option)
        (bet: RetainedBetState)
        (matchState: RetainedMatch)
        =
        match membership with
        | None -> MembershipEnded
        | Some membership when membership.Generation <> Some generation -> MembershipEnded
        | Some membership ->
            if bet.Settlement.Generation = Some generation then
                AlreadyRestored
            elif not (isRetained generation bet) then
                NotRetained
            elif bet.RestoreExcludedGeneration = Some generation then
                AlreadyHeld
            else
                match membership.ScoreVersion, membership.Total with
                | Some scoreVersion, Some total -> decideRetained generation scoreVersion total bet.Settlement matchState
                | _ -> InvalidMembership "membership without a score version or total"
