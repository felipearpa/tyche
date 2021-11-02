namespace Pipel.Data

[<Interface>]
type IAdderRepository<'TEntity when 'TEntity: not struct> =

    abstract AsyncAddMany : 'TEntity seq -> Async<'TEntity seq>
