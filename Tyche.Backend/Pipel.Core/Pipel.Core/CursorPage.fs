namespace Pipel.Core

type CursorPage<'T> =
    { Items: 'T seq
      NextToken: string option }

module CursorPage =

    let empty<'T> =
        { CursorPage.Items = Seq.empty<'T>
          NextToken = None }

    let map func page =
        { CursorPage.Items = page.Items |> Seq.map func
          NextToken = page.NextToken }

    let cast<'T> page =
        { CursorPage.Items = page.Items |> Seq.cast<'T>
          NextToken = page.NextToken }

    let fromSeq seq =
        { CursorPage.Items = seq
          NextToken = None }
