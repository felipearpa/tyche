namespace Pipel.Data

[<Interface>]
type IUpdaterRepository<'TEntity when 'TEntity: not struct> =

    abstract AsyncUpdateMany : 'TEntity seq -> Async<'TEntity seq>
