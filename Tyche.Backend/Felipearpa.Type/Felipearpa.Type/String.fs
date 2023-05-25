[<RequireQualifiedAccess>]
module String

let tryOf (element: string) =
    match isNull element with
    | false -> Some element
    | _ -> None
