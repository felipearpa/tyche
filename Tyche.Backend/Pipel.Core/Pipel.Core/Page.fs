namespace Pipel.Core

type Page<'T> =
    { Items: 'T seq
      ItemsCount: int
      Skip: int
      Take: int
      HasNext: bool }

module Page =

    let map func page =
        { Page.Items = page.Items |> Seq.map func
          ItemsCount = page.ItemsCount
          Skip = page.Skip
          Take = page.Take
          HasNext = page.HasNext }

    let cast<'T> page =
        { Page.Items = page.Items |> Seq.cast<'T>
          ItemsCount = page.ItemsCount
          Skip = page.Skip
          Take = page.Take
          HasNext = page.HasNext }

    let fromSeq seq =
        let count = seq |> Seq.length

        { Page.Items = seq
          ItemsCount = count
          Skip = 0
          Take = count
          HasNext = false }
