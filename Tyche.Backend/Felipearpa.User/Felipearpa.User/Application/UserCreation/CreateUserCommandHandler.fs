namespace Felipearpa.User.Application.UserCreation

open Felipearpa.Crypto
open Felipearpa.Type
open Felipearpa.User.Application
open Felipearpa.User.Domain
open Felipearpa.User.Type

type CreateUserCommandHandler(userRepository: IUserRepository, hasher: IHasher) =

    member this.ExecuteAsync(userCommand: CreateUserCommand) : Result<UserViewModel, string> Async =
        async {
            let user =
                { User.UserId = Ulid.random ()
                  Username = userCommand.Username
                  Hash = userCommand.Password |> Password.value |> hasher.Hash }

            let! result = userRepository.CreateAsync user

            return
                result
                |> Result.map UserMapper.mapToViewModel
                |> Result.mapError (fun failure ->
                    match failure with
                    | UserAlreadyRegistered -> "User already registered")
        }
