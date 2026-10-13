namespace Felipearpa.Tyche.Pool.Infrastructure

#nowarn "3536"

open Amazon.DynamoDBv2
open Felipearpa.Tyche.Pool.Domain

/// DynamoDB access for the kickoff copies on match details.
type DetailKickoffDynamoDbRepository(client: IAmazonDynamoDB) =

    interface IDetailKickoffRepository with

        member _.GetMatchDetailsPageAsync(matchId, poolId, next) =
            async {
                let start =
                    match next with
                    | Some cursor ->
                        StartKeyCodec.tryDecode cursor
                        |> Option.defaultWith (fun () -> invalidArg (nameof next) "Malformed match detail cursor")
                    | None -> null

                let! response =
                    client.QueryAsync(DetailKickoffRequestBuilder.matchDetailsPage matchId poolId start)
                    |> Async.AwaitTask

                let cursor =
                    match response.LastEvaluatedKey |> Option.ofObj with
                    | Some key when key.Count > 0 -> Some(StartKeyCodec.encode key)
                    | _ -> None

                return response.Items |> Seq.map DetailKickoffRequestBuilder.toDetailKickoff |> List.ofSeq, cursor
            }

        member _.GetDetailAsync(poolId, gamblerId, matchId) =
            async {
                let! response =
                    client.GetItemAsync(DetailKickoffRequestBuilder.getDetail poolId gamblerId matchId)
                    |> Async.AwaitTask

                return
                    if response.IsItemSet && not (isNull response.Item) && response.Item.Count > 0 then
                        Some(DetailKickoffRequestBuilder.toDetailKickoff response.Item)
                    else
                        None
            }

        member _.RewriteKickoffAsync(write) =
            DetailKickoffRequestBuilder.rewrite write
            |> client.UpdateItemAsync
            |> Async.AwaitTask
            |> Async.Ignore
            |> GuardedWrite.run
