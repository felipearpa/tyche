namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Core.Paging
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type GetPoolMembersFailure =
    | PoolNotFound
    | NotOwner

type GetPoolMembers(poolRepository: IPoolRepository) =

    member this.ExecuteAsync
        (poolId: Ulid, callerGamblerId: Ulid, next: string option)
        : Result<PoolMember CursorPage, GetPoolMembersFailure> Async =
        async {
            let! poolResult = poolRepository.GetPoolByIdAsync poolId

            match poolResult with
            | Error _ -> return failwith "Error querying the pool data"
            | Ok None -> return Error GetPoolMembersFailure.PoolNotFound
            | Ok(Some pool) when pool.CreatorGamblerId <> callerGamblerId -> return Error GetPoolMembersFailure.NotOwner
            | Ok(Some pool) ->
                let! page = poolRepository.GetPoolMembersAsync(poolId, next)

                let withOwnership =
                    { page with
                        Items =
                            page.Items
                            |> Seq.map (fun gambler ->
                                { gambler with IsOwner = gambler.GamblerId = pool.CreatorGamblerId }) }

                return Ok withOwnership
        }
