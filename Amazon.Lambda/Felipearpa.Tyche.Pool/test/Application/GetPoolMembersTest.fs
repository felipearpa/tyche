namespace Felipearpa.Tyche.Pool.Test.Application

open Felipearpa.Core.Paging
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type
open FsUnitTyped
open Xunit

module GetPoolMembersTest =

    let private ownerId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6C"
    let private poolId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6A"

    let private pool () : Pool =
        { PoolId = poolId
          PoolName = NonEmptyString100.newOf "Polla 2026"
          PoolLayoutId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6B"
          CreatorGamblerId = ownerId
          GamblerCount = Some 3 }

    let private gambler (id: string) (username: string) : PoolMember =
        { GamblerId = Ulid.newOf id
          GamblerUsername = NonEmptyString100.newOf username
          GamblerEmail = $"{username}@tyche.com"
          IsOwner = false }

    let private fakeRepo (maybePool: Pool option) (members: PoolMember list) =
        { new IPoolRepository with
            member _.GetPoolByIdAsync(_) = async { return Ok maybePool }
            member _.CreatePoolAsync(_) = failwith "not used"
            member _.JoinPoolAsync(_) = failwith "not used"
            member _.IsPoolMemberAsync(_, _) = failwith "not used"
            member _.GetGamblersByPoolLayoutAsync(_, _) = failwith "not used"
            member _.DeletePoolAsync(_) = failwith "not used"
            member _.RemoveGamblerAsync(_, _) = failwith "not used"
            member _.PropagateGamblerUsernameAsync(_, _) = failwith "not used"

            member _.GetPoolMembersAsync(_, _) =
                async { return { CursorPage.Items = members; Next = None } } }

    [<Fact>]
    let ``given a non-owner caller when execute then returns NotOwner`` () =
        async {
            let repo = fakeRepo (Some(pool ())) []
            let service = GetPoolMembers(repo)
            let caller = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6X"

            let! result = service.ExecuteAsync(poolId, caller, None)

            match result with
            | Error GetPoolMembersFailure.NotOwner -> ()
            | _ -> failwith "expected NotOwner"
        }

    [<Fact>]
    let ``given the owner caller when execute then includes the owner flagged as owner`` () =
        async {
            let ownerRow =
                { PoolMember.GamblerId = ownerId
                  GamblerUsername = NonEmptyString100.newOf "owner"
                  GamblerEmail = "owner@tyche.com"
                  IsOwner = false }

            let alice = gambler "01K1PX1TX2NM1HG851S1V0QG6Q" "alice"
            let bob = gambler "01K1PX1TX2NM1HG851S1V0QG6R" "bob"

            let repo = fakeRepo (Some(pool ())) [ ownerRow; alice; bob ]
            let service = GetPoolMembers(repo)

            let! result = service.ExecuteAsync(poolId, ownerId, None)

            match result with
            | Ok page ->
                let items = page.Items |> List.ofSeq
                items.Length |> shouldEqual 3
                items |> List.filter (fun gambler -> gambler.IsOwner) |> List.map (fun gambler -> gambler.GamblerId) |> shouldEqual [ ownerId ]
            | Error _ -> failwith "expected Ok"
        }
