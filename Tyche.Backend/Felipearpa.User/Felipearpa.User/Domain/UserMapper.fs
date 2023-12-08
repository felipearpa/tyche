namespace Felipearpa.User.Domain

open Felipearpa.Type
open Felipearpa.User.Domain

module UserMapper =
    [<Literal>]
    let prefixAccount = "ACCOUNT"

    let mapToEntity (user: User) =
        { UserEntity.Pk = $"{prefixAccount}#{user.UserId |> Ulid.value}"
          AccountId = user.UserId |> Ulid.value
          Email = user.Email |> Email.value }

    let mapToDomain (userEntity: UserEntity) =
        { User.UserId = userEntity.AccountId |> Ulid.newOf
          Email = userEntity.Email |> Email.newOf }
