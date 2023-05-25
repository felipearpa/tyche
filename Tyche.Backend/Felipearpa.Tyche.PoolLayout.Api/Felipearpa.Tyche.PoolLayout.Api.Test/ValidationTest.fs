module ValidationTest

open Felipearpa.Tyche.PoolLayout.Api
open Xunit

let validSkips: obj [] seq =
    seq {
        yield [| 0 |]
        yield [| 1 |]
        yield [| 100 |]
        yield [| 1000 |]
        yield [| 10000 |]
    }

let invalidSkips: obj [] seq =
    seq {
        yield [| -1 |]
        yield [| -100 |]
    }

let validTakes: obj [] seq =
    seq {
        yield [| 1 |]
        yield [| 100 |]
        yield [| 1000 |]
    }

let invalidTakes: obj [] seq =
    seq {
        yield [| 0 |]
        yield [| -1 |]
        yield [| -100 |]
        yield [| 1001 |]
        yield [| 2000 |]
    }

[<Theory>]
[<MemberData(nameof validSkips)>]
let ``given a valid value for a skip when the skip is validated then a result is returned`` (skip: int) =
    let result = Validation.isSkip skip

    match result with
    | Ok value -> Assert.Equal(skip, value)
    | Error _ -> Assert.True(false, "Skip must have a valid skip")

[<Theory>]
[<MemberData(nameof invalidSkips)>]
let ``given an invalid value for a skip when the skip is validated then an error is returned`` (skip: int) =
    let result = Validation.isSkip skip

    match result with
    | Ok _ -> Assert.True(false, "Skip must have an invalid value")
    | Error _ -> Assert.True(true)

[<Theory>]
[<MemberData(nameof validTakes)>]
let ``given a valid value for a take when the take is validated then a result is returned`` (take: int) =
    let result = Validation.isTake take

    match result with
    | Ok value -> Assert.Equal(take, value)
    | Error _ -> Assert.True(false, "Take must have a valid skip")

[<Theory>]
[<MemberData(nameof invalidTakes)>]
let ``given an invalid value for a take when the take is validated then an error is returned`` (take: int) =
    let result = Validation.isTake take

    match result with
    | Ok _ -> Assert.True(false, "Take must have an invalid value")
    | Error _ -> Assert.True(true)
