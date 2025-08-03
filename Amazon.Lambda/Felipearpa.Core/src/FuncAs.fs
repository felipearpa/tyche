namespace Felipearpa.Core

open System
open System.Linq.Expressions

type FuncAs() =

    static member LinqExpression<'T, 'TResult>(e: Expression<Func<'T, 'TResult>>) = e
