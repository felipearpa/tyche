namespace Felipearpa.Tyche.Function.Response

open Felipearpa.Tyche.Pool.Domain

module KickoffGroupTransformer =

    let toResponse (item: KickoffGroupItem) =
        { KickoffGroupResponse.KickoffDateTime = item.Group.KickoffDateTime.ToUniversalTime()
          TotalMatches = item.Group.TotalMatches
          ComputedMatches = item.Group.ComputedMatches
          Movement =
            item.Movement
            |> Option.map (fun movement ->
                { GroupMovementResponse.BeforePosition = movement.BeforePosition
                  Position = movement.Position })
          Matches = item.Bets |> List.map PoolGamblerBetTransformer.toResponse }
