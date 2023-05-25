namespace Felipearpa.Tyche.PoolLayout.Api

open Felipearpa.Type.Validation

module Validation =

    let isSkip value =
        match value with
        | Less 0 -> Error "Skip: It must be greater or equal than zero"
        | _ -> Ok value

    let isTake value =
        match value with
        | LessOrEqual 0 -> Error "Take: It must be greater than zero"
        | Greater 1000 -> Error "Take: It must be less or equal than 1000"
        | _ -> Ok value
