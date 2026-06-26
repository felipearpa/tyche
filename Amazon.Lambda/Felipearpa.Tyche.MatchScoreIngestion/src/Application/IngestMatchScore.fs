namespace Felipearpa.Tyche.MatchScoreIngestion.Application

open System
open Felipearpa.Tyche.MatchScoreIngestion.Domain
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Type

/// One poll tick: stamp the poll, classify the match, and act.
/// - Finished  → write the 90-minute score onto the pool layout match and mark the row completed.
/// - Abandoned → mark the row expired (cancelled/postponed/awarded).
/// - NotYetFinished → keep polling, unless the match is past the cutoff window, then mark it expired.
/// Writing the score is the only new persistence; the existing pool layout stream fan-out recomputes
/// bets and positions. Marking completed/expired drives schedule teardown via the control table stream.
type IngestMatchScore
    (
        matchResultProvider: IMatchResultProvider,
        ingestionRepository: IMatchScoreIngestionRepository,
        poolLayoutRepository: IPoolLayoutRepository
    ) =

    /// Past this many hours after kickoff a still-unfinished match is considered abandoned.
    /// Kept below the schedule's end-date cap so a live tick gets to mark it before the schedule self-deletes.
    [<Literal>]
    let expireAfterHours = 3.0

    member _.ExecuteAsync(matchId: Ulid, poolLayoutId: Ulid, externalMatchId: string, matchDateTime: DateTime) =
        async {
            do! ingestionRepository.RecordPollAsync(matchId)

            let! pollResult = matchResultProvider.GetMatchPollResultAsync(externalMatchId)

            match pollResult with
            | Finished result ->
                do!
                    poolLayoutRepository.SetMatchFinalScoreAsync(
                        poolLayoutId,
                        matchId,
                        result.HomeTeamScore,
                        result.AwayTeamScore
                    )

                do! ingestionRepository.MarkCompletedAsync(matchId)
            | Abandoned -> do! ingestionRepository.MarkExpiredAsync(matchId)
            | NotYetFinished ->
                let cutoff = matchDateTime.ToUniversalTime().AddHours(expireAfterHours)

                if DateTime.UtcNow > cutoff then
                    do! ingestionRepository.MarkExpiredAsync(matchId)
                else
                    return ()
        }
