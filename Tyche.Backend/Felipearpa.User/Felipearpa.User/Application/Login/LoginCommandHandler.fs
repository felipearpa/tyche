namespace Felipearpa.User.Application.Login

open Felipearpa.User.Application
open Felipearpa.User.Domain

type LoginCommandHandler(userRepository: IUserRepository) =

    member this.ExecuteAsync(loginCommand: LoginCommand) : Result<UserViewModel, string> Async =
        async {
            let! result = userRepository.LoginAsync(loginCommand.Email)

            return
                result
                |> Result.map (fun user -> user |> UserMapper.mapToViewModel)
                |> Result.mapError (fun failure ->
                    match failure with
                    | UserNotFound -> "User not found"
                    | InvalidPassword -> "Invalid Password")
        }
