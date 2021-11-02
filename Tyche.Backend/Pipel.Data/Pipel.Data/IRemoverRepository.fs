namespace Pipel.Data

[<Interface>]
type IRemoverRepository<'TEntity, 'TKey when 'TEntity: not struct and 'TKey: not struct> =

    abstract AsyncRemoveManyByKeys : 'TKey seq -> Async<unit>

    abstract AsyncRemoveMany : 'TEntity seq -> Async<unit>
