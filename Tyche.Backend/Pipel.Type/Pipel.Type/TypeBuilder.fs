namespace Pipel.Type

open System

[<RequireQualifiedAccess>]
module TypeBuilder =

    let inline tryFrom element : ^Type option =
        (^Type: (static member TryFrom : ^SourceType -> ^Type option) element)

    let inline from element : ^Type =
        (^Type: (static member From : ^SourceType -> ^Type) element)

    let inline value element : ^SourceType =
        (^Type: (member Value : ^SourceType) element)

    let inline toString (element: ^Type) : string = (element |> value).ToString()

    let inline nullableValue (element: ^Type option) : ^SourceType Nullable =
        match element with
        | Some it -> it |> value |> Nullable
        | None -> Unchecked.defaultof< ^SourceType Nullable>
