namespace Felipearpa.Tyche.Pool.Test.Application

open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type
open FsUnitTyped
open Xunit

module RemovePoolGamblerTest =

    let private ownerId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6C"
    let private poolId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6A"

    let private pool () : Pool =
        { PoolId = poolId
          PoolName = NonEmptyString100.newOf "Polla 2026"
          PoolLayoutId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6B"
          CreatorGamblerId = ownerId
          GamblerCount = Some 3 }

    let private fakeRepo (maybePool: Pool option) =
        let removed = ResizeArray<Ulid * Ulid>()

        let repo =
            { new IPoolRepository with
                member _.GetPoolByIdAsync(_) = async { return Ok maybePool }
                member _.CreatePoolAsync(_) = failwith "not used"
                member _.JoinPoolAsync(_) = failwith "not used"
                member _.IsPoolMemberAsync(_, _) = failwith "not used"
                member _.GetGamblersByPoolLayoutAsync(_, _) = failwith "not used"
                member _.GetPoolMembersAsync(_, _) = failwith "not used"
                member _.DeletePoolAsync(_) = failwith "not used"
                member _.PropagateGamblerUsernameAsync(_, _) = failwith "not used"

                member _.RemoveGamblerAsync(poolId, gamblerId) =
                    removed.Add(poolId, gamblerId)
                    async { return () } }

        repo, removed

    [<Fact>]
    let ``given a non-owner caller when execute then returns NotOwner and removes nothing`` () =
        async {
            let repo, removed = fakeRepo (Some(pool ()))
            let service = RemovePoolGambler(repo)
            let caller = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6X"
            let target = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6Y"

            let! result = service.ExecuteAsync(poolId, caller, target)

            result |> shouldEqual (Error RemovePoolGamblerFailure.NotOwner)
            removed.Count |> shouldEqual 0
        }

    [<Fact>]
    let ``given the owner removing themselves when execute then returns CannotRemoveOwner`` () =
        async {
            let repo, removed = fakeRepo (Some(pool ()))
            let service = RemovePoolGambler(repo)

            let! result = service.ExecuteAsync(poolId, ownerId, ownerId)

            result |> shouldEqual (Error RemovePoolGamblerFailure.CannotRemoveOwner)
            removed.Count |> shouldEqual 0
        }

    [<Fact>]
    let ``given a missing pool when execute then returns PoolNotFound`` () =
        async {
            let repo, _ = fakeRepo None
            let service = RemovePoolGambler(repo)
            let target = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6Y"

            let! result = service.ExecuteAsync(poolId, ownerId, target)

            result |> shouldEqual (Error RemovePoolGamblerFailure.PoolNotFound)
        }

    [<Fact>]
    let ``given the owner removing another gambler when execute then removes the master row`` () =
        async {
            let repo, removed = fakeRepo (Some(pool ()))
            let service = RemovePoolGambler(repo)
            let target = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6Y"

            let! result = service.ExecuteAsync(poolId, ownerId, target)

            result |> shouldEqual (Ok())
            removed.Count |> shouldEqual 1
            removed[0] |> shouldEqual (poolId, target)
        }
