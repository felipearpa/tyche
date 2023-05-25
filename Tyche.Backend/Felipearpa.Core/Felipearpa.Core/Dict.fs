namespace Felipearpa.Core

open System.Collections.Generic
open System.Linq

module Dict =

    let union (dict1: IDictionary<'TKey, 'TValue>) (dict2: IDictionary<'TKey, 'TValue>) =
        dict1
            .Union(dict2)
            .ToDictionary((fun it -> it.Key), (fun it -> it.Value))
