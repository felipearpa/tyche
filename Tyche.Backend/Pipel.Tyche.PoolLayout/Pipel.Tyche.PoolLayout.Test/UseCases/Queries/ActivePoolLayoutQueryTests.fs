module ActivePoolLayoutQueryTests

open System
open System.Linq
open Pipel.Tyche.PoolLayout.Data
open Pipel.Tyche.PoolLayout.Domain.UseCases.Queries
open Pipel.Type
open Xunit

let createSampleData () =
    let now = DateTime.Now

    [| { PoolLayoutEntity.PoolLayoutId = Guid.NewGuid()
         Name = "Copa America 2021"
         OpeningStartDateTime = now.Add(TimeSpan.FromDays(-1.0))
         OpeningEndDateTime = now.Add(TimeSpan.FromDays(1.0)) }
       { PoolLayoutEntity.PoolLayoutId = Guid.NewGuid()
         Name = "Mundial Qatar 2022"
         OpeningStartDateTime = now.Add(TimeSpan.FromDays(1.0))
         OpeningEndDateTime = now.Add(TimeSpan.FromDays(2.0)) }
       { PoolLayoutEntity.PoolLayoutId = Guid.NewGuid()
         Name = "UEFA Champions League 2021"
         OpeningStartDateTime = now.Add(TimeSpan.FromDays(-2.0))
         OpeningEndDateTime = now.Add(TimeSpan.FromDays(-1.0)) } |]

[<Fact>]
let ``given an empty filter text when an ActivePoolLayoutQuery is executed then all of active items are returned`` () =
    let poolsLayoutsBefore = createSampleData ()
    let poolsLayoutsQueryBefore = poolsLayoutsBefore.AsQueryable()

    let poolsLayoutsQueryAfter =
        ActivePoolLayoutQuery.execute None poolsLayoutsQueryBefore

    let poolsLayoutsAfter = poolsLayoutsQueryAfter.ToArray()

    Assert.Equal(1, poolsLayoutsAfter.Count())

[<Fact>]
let ``given a filter text when an ActivePoolLayoutQuery is executed then all of active items that matches the criteria are returned``
    ()
    =
    let poolsLayoutsBefore = createSampleData ()
    let poolsLayoutsQueryBefore = poolsLayoutsBefore.AsQueryable()

    let poolsLayoutsQueryAfter =
        ActivePoolLayoutQuery.execute (Some <| NonEmptyString.From "2021") poolsLayoutsQueryBefore

    let poolsLayoutsAfter = poolsLayoutsQueryAfter.ToArray()

    Assert.Equal(1, poolsLayoutsAfter.Count())

[<Fact>]
let ``given a filter text that doesn't match with no item when an ActivePoolLayoutQuery is executed then no item is returned``
    ()
    =
    let poolsLayoutsBefore = createSampleData ()
    let poolsLayoutsQueryBefore = poolsLayoutsBefore.AsQueryable()

    let poolsLayoutsQueryAfter =
        ActivePoolLayoutQuery.execute (Some <| NonEmptyString.From "2022") poolsLayoutsQueryBefore

    let poolsLayoutsAfter = poolsLayoutsQueryAfter.ToArray()

    Assert.Equal(0, poolsLayoutsAfter.Count())
