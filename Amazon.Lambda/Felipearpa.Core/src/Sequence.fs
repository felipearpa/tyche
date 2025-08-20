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
