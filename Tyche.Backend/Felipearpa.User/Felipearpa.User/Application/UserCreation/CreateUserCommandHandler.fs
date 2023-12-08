namespace Felipearpa.User.Application.UserCreation

open Felipearpa.Crypto
open Felipearpa.Type
open Felipearpa.User.Application
open Felipearpa.User.Domain

type CreateUserCommandHandler(userRepository: IUserRepository, hasher: IHasher) =

    member this.ExecuteAsync(userCommand: CreateUserCommand) : Result<UserViewModel, string> Async =
        async {
            let user =
                { User.UserId = Ulid.random ()
                  Email = userCommand.Email }

            let! result = userRepository.CreateAsync user

            return
                result
                |> Result.map UserMapper.mapToViewModel
                |> Result.mapError (fun failure ->
                    match failure with
                    | UserAlreadyRegistered -> "User already registered")
        }
