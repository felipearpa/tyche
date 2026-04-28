namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Core.Paging
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module private TimelineCursor =

    [<Literal>]
    let liveTag = "live:"

    [<Literal>]
    let finishedTag = "finished:"

    type Phase =
        | Live
        | Finished

    let parse (maybeCursor: string option) : Phase * string option =
        match maybeCursor with
        | None -> Live, None
        | Some cursor when cursor.StartsWith(liveTag) ->
            let inner = cursor.Substring(liveTag.Length)
            Live, (if System.String.IsNullOrEmpty inner then None else Some inner)
        | Some cursor when cursor.StartsWith(finishedTag) ->
            let inner = cursor.Substring(finishedTag.Length)
            Finished, (if System.String.IsNullOrEmpty inner then None else Some inner)
        | Some cursor -> Live, Some cursor

    let formatLive (next: string option) : string option =
        next |> Option.map (fun n -> liveTag + n)

    let formatFinished (next: string option) : string option =
        next |> Option.map (fun n -> finishedTag + n)

type GetGamblerBetsTimeline
    (
        getLivePoolGamblerBets: GetLivePoolGamblerBets,
        getFinishedPoolGamblerBets: GetFinishedPoolGamblerBets
    ) =

    member this.ExecuteAsync(poolId: Ulid, gamblerId: Ulid, maybeCursor: string option) =
        async {
            let phase, innerCursor = TimelineCursor.parse maybeCursor

            match phase with
            | TimelineCursor.Live ->
                let! livePage = getLivePoolGamblerBets.ExecuteAsync(poolId, gamblerId, None, innerCursor)

                match livePage.Next, Seq.isEmpty livePage.Items with
                | Some _, _ ->
                    return
                        { CursorPage.Items = livePage.Items
                          Next = TimelineCursor.formatLive livePage.Next }
                | None, false ->
                    return
                        { CursorPage.Items = livePage.Items
                          Next = Some TimelineCursor.finishedTag }
                | None, true ->
                    let! finishedPage = getFinishedPoolGamblerBets.ExecuteAsync(poolId, gamblerId, None, None)

                    return
                        { CursorPage.Items = finishedPage.Items
                          Next = TimelineCursor.formatFinished finishedPage.Next }
            | TimelineCursor.Finished ->
                let! finishedPage = getFinishedPoolGamblerBets.ExecuteAsync(poolId, gamblerId, None, innerCursor)

                return
                    { CursorPage.Items = finishedPage.Items
                      Next = TimelineCursor.formatFinished finishedPage.Next }
        }
