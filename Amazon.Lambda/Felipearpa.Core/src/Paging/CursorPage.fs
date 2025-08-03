namespace Felipearpa.Core.Paging

open System.Linq

type CursorPage<'T> = { Items: 'T seq; Next: string option }

module CursorPage =

    let empty<'T> () =
        { CursorPage.Items = Seq.empty<'T>
          Next = None }

    let map func page =
        { CursorPage.Items = page.Items |> Seq.map func
          Next = page.Next }

    let cast<'T> page =
        { CursorPage.Items = page.Items |> Seq.cast<'T>
          Next = page.Next }

    let fromSeq seq = { CursorPage.Items = seq; Next = None }

    let isEmpty<'T> (page: CursorPage<'T>) = page.Items.Count() = 0
