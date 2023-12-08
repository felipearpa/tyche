namespace Felipearpa.User.Application

open Felipearpa.Type
open Felipearpa.User.Domain

module UserMapper =

    let mapToViewModel (user: User) =
        { UserViewModel.UserId = user.UserId |> Ulid.value
          Email = user.Email |> Email.value }
