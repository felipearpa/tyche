namespace Felipearpa.User.Domain

open Felipearpa.Type

type UserCreationFailure = | UserAlreadyRegistered

type LoginFailure =
    | UserNotFound
    | InvalidPassword

type IUserRepository =

    abstract CreateAsync: User -> Result<User, UserCreationFailure> Async

    abstract LoginAsync: Email -> Result<User, LoginFailure> Async
