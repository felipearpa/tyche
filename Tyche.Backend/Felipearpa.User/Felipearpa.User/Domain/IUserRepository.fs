namespace Felipearpa.User.Domain

open Felipearpa.User.Type

type UserCreationFailure = | UserAlreadyRegistered

type LoginFailure =
    | UserNotFound
    | InvalidPassword

type IUserRepository =

    abstract CreateAsync: User -> Result<User, UserCreationFailure> Async

    abstract LoginAsync: Username * string -> Result<User, LoginFailure> Async
