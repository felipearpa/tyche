namespace Felipearpa.User.Api.Request.UserCreation

open Felipearpa.User.Api.Request
open Felipearpa.User.Application.UserCreation
open Felipearpa.User.Type

module CreateUserMapper =

    let mapToCommand (createUserRequest: CreateUserRequest) =
        { CreateUserCommand.Username = createUserRequest.Username |> Username.newOf
          Password = createUserRequest.Password |> Password.newOf }
