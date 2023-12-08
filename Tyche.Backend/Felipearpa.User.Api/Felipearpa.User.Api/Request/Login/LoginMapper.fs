namespace Felipearpa.User.Api.Request.Login

open Felipearpa.Type
open Felipearpa.User.Application.Login

module LoginMapper =

    let mapToCommand (loginRequest: LoginRequest) =
        { LoginCommand.Email = loginRequest.Email |> Email.newOf }
