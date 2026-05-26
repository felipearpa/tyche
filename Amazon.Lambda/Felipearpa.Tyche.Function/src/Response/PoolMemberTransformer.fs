namespace Felipearpa.Tyche.Function.Response

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module PoolMemberTransformer =

    let toResponse (poolMember: PoolMember) =
        { PoolMemberResponse.GamblerId = poolMember.GamblerId |> Ulid.value
          GamblerUsername = poolMember.GamblerUsername |> NonEmptyString100.value
          GamblerEmail = poolMember.GamblerEmail }
