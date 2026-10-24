namespace Felipearpa.Tyche.Function

open System

module AvatarBucketSetting =

    [<Literal>]
    let private variable = "AVATAR_BUCKET_NAME"

    let fromEnvironment () =
        Environment.GetEnvironmentVariable variable
        |> Option.ofObj
        |> Option.filter (String.IsNullOrWhiteSpace >> not)
        |> Option.defaultWith (fun () ->
            invalidOp $"{variable} is not set; it must name the avatar bucket (tyche-avatars-<accountId>).")
