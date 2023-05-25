namespace Felipearpa.User.Application

open Felipearpa.Type
open Felipearpa.User.Domain
open Felipearpa.User.Type

module UserMapper =

    let mapToViewModel (user: User) =
        { UserViewModel.UserId = user.UserId |> Ulid.value
          Username = user.Username |> Username.value }
