namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Core.Paging
open Felipearpa.Tyche.Pool.Domain

/// Attaches each pool's trend kickoff group to its score rows, so every surface that shows a
/// pool's movement reads the same group and counts. A page reads its pool roots once and each
/// distinct layout once.
type PoolStandings(poolGamblerScoreRepository: IPoolGamblerScoreRepository, kickoffGroupReader: KickoffGroupReader) =

    member this.WithTrendGroupsAsync(page: PoolGamblerScore CursorPage) : Async<PoolStanding CursorPage> =
        async {
            let scores = page.Items |> List.ofSeq
            let! trends = poolGamblerScoreRepository.GetPoolTrendsAsync(scores |> List.map _.PoolId |> List.distinct)

            let tracked =
                trends
                |> List.choose (fun trend -> trend.TrendKickoffDateTime |> Option.map (fun kickoff -> trend, kickoff))

            let! groupsByLayout = kickoffGroupReader.ReadAsync(tracked |> List.map (fun (trend, _) -> trend.PoolLayoutId))

            let trendGroups =
                tracked
                |> List.map (fun (trend, kickoff) ->
                    trend.PoolId, groupsByLayout |> Map.find trend.PoolLayoutId |> KickoffGroup.at kickoff)
                |> Map.ofList

            return
                { Items =
                    scores
                    |> List.map (fun score ->
                        { Score = score
                          TrendGroup = trendGroups |> Map.tryFind score.PoolId })
                  Next = page.Next }
        }

    member this.WithTrendGroupAsync(score: PoolGamblerScore) : Async<PoolStanding> =
        async {
            let! page = this.WithTrendGroupsAsync { Items = [ score ]; Next = None }
            return Seq.exactlyOne page.Items
        }
