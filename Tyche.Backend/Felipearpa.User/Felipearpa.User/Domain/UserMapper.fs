namespace Felipearpa.User.Domain

open Felipearpa.Type
open Felipearpa.User.Domain
open Felipearpa.User.Type

module UserMapper =

    let mapToEntity (user: User) =
        { UserEntity.Pk = $"#USER#{user.UserId |> Ulid.value}"
          UserId = user.UserId |> Ulid.value
          Username = user.Username |> Username.value
          Hash = user.Hash }

    let mapToDomain (userEntity: UserEntity) =
        { User.UserId = userEntity.UserId |> Ulid.newOf
          Username = userEntity.Username |> Username.newOf
          Hash = userEntity.Hash }
