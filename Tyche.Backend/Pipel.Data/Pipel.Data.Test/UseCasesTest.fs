module UseCasesTest

open Data
open Pipel.Core
open Pipel.Data
open System.Linq
open Xunit

[<Fact>]
let ``given a none next when find function is executed then all items are returned`` () =
    let next = None

    let asyncFind =
        fun (filter: string option) ->
            async {
                return
                    { CursorPage.NextToken = None
                      Items = Seq.empty<Product> }
            }

    let page =
        UseCases.asyncFindWithCursorPagination next id asyncFind
        |> Async.RunSynchronously

    Assert.Equal(0, page.Items.Count())
