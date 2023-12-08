namespace Felipearpa.User.Api.Request.UserCreation

open Felipearpa.Type
open Felipearpa.User.Application.UserCreation

module CreateUserMapper =

    let mapToCommand (createUserRequest: CreateUserRequest) =
        { CreateUserCommand.Email = createUserRequest.Email |> Email.newOf }
