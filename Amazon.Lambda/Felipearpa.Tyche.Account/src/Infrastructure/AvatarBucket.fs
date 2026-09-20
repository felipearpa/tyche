namespace Felipearpa.Tyche.Account.Infrastructure

open System
open Felipearpa.Type

module AvatarBucket =

    [<Literal>]
    let name = "tyche-avatars"

    [<Literal>]
    let contentType = "image/jpeg"

    [<Literal>]
    let cacheControl = "max-age=0, must-revalidate"

    let uploadUrlExpiry = TimeSpan.FromMinutes 5.0

    let keyOf (accountId: Ulid) = $"avatars/{accountId.Value}.jpg"
