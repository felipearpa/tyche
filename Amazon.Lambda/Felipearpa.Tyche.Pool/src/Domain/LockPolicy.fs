namespace Felipearpa.Tyche.Pool.Domain

open System

module LockPolicy =

    let lockOffset = TimeSpan.FromMinutes 10.0

    let lockTimeOf (matchDateTime: DateTime) = matchDateTime - lockOffset

    let isLockedAt (now: DateTime) (matchDateTime: DateTime) = now >= lockTimeOf matchDateTime
