namespace Felipearpa.Core

module Seq =

    let iterAsync (action: 'T -> Async<unit>) (sequence: seq<'T>) : Async<unit> =
        async {
            for item in sequence do
                do! action item
        }

    let mapAsync (action: 'T -> Async<'R>) (sequence: seq<'T>) : Async<'R seq> =
        async {
            let results = ResizeArray<'R>()

            for item in sequence do
                let! r = action item
                results.Add r

            return results :> seq<'R>
        }

    let chunkByKey (keySelector: 'T -> 'K) (sequence: seq<'T>) : ('K * 'T list) list =
        sequence
        |> Seq.fold
            (fun acc item ->
                let key = keySelector item

                match acc with
                | (k, items) :: rest when k = key -> (k, item :: items) :: rest
                | _ -> (key, [ item ]) :: acc)
            []
        |> List.map (fun (k, items) -> (k, List.rev items))
        |> List.rev
