namespace Felipearpa.User.Application.UserCreation

open Felipearpa.User.Type

type CreateUserCommand =
    { Username: Username
      Password: Password }
