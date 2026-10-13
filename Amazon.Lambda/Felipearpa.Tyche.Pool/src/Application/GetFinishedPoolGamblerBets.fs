namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Core.Paging
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

/// The gambler's History: kickoff groups with at least one computed detail, newest kickoff
/// first, each with the layout's counts, the gambler's movement, and the computed rows. A page
/// holds whole groups.
type GetFinishedPoolGamblerBets(poolGamblerBetRepository: IPoolGamblerBetRepository, kickoffGroupReader: KickoffGroupReader) =

    member this.ExecuteAsync(poolId: Ulid, gamblerId: Ulid, next: string option) : Async<KickoffGroupItem CursorPage> =
        async {
            let! page = poolGamblerBetRepository.GetFinishedBetsByKickoffAsync(poolId, gamblerId, next)

            let! runs, next =
                GroupedBetFeed.completeLastGroup
                    (fun storedKickoff ->
                        poolGamblerBetRepository.GetFinishedBetsAtKickoffAsync(poolId, gamblerId, storedKickoff))
                    page

            let! layoutGroups = GroupedBetFeed.layoutGroupsOf kickoffGroupReader page.Items

            return
                { Items = runs |> List.map (fun (kickoff, children) -> KickoffGroupFeed.item layoutGroups kickoff children)
                  Next = next }
        }
