namespace Felipearpa.Core

module Seq =
    let iterAsync (action: 'T -> Async<unit>) (sequence: seq<'T>) : Async<unit> =
        async {
            for item in sequence do
                do! action item
        }
