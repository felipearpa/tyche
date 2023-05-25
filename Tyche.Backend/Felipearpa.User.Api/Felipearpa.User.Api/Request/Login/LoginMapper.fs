namespace Felipearpa.User.Api.Request.Login

open Felipearpa.User.Application.Login
open Felipearpa.User.Type

module LoginMapper =

    let mapToCommand (loginRequest: LoginRequest) =
        { LoginCommand.Username = loginRequest.Username |> Username.newOf
          Password = loginRequest.Password |> Password.newOf }
