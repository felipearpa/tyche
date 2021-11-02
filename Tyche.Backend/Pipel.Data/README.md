# Base project to manage data persistence in F#

## Interfaces

### `QueryFunc`

It's a type of function to convert an `IQueryable` into other.

```f#
type QueryFunc = IQueryable -> IQueryable
```

### `MapFunc`

It's a type of function to map a object into other.

```f#
type MapFunc<'TA, 'TB> = 'TA -> 'TB
```

### `IQueryableRepository`

It's an interface with the basic functions to read data.

```f#
[<Interface>]
type IQueryableRepository<'TEntity> =

    abstract AsyncFind : QueryFunc -> Async<'TEntity seq>

    abstract AsyncFindAndPaginate : QueryFunc * int * int -> Async<'TEntity Page>
```

### `IReaderRepository`

It's an interface with the basic functions to read data and query some object by its primary key.

```f#
[<Interface>]
type IReaderRepository<'TEntity, 'TKey when 'TEntity: not struct and 'TKey: not struct> =

    abstract AsyncFindByKey : 'TKey -> Async<'TEntity>

    abstract AsyncFind : QueryFunc -> Async<'TEntity seq>

    abstract AsyncFindAndPaginate : QueryFunc * int * int -> Async<'TEntity Page>
```

### `IAdderRepository`

It's an interface with the basic functions to persist objects.

```f#
[<Interface>]
type IAdderRepository<'TEntity when 'TEntity: not struct> =

    abstract AsyncAddMany : 'TEntity seq -> Async<'TEntity seq>
```

### `IUpdaterRepository`

It's an interface with the basic functions to update objects.

```f#
[<Interface>]
type IUpdaterRepository<'TEntity when 'TEntity: not struct> =

    abstract AsyncUpdateMany : 'TEntity seq -> Async<'TEntity seq>
```

### `IRemoverRepository`

It's an interface with the basic functions to remove objects.

```f#
[<Interface>]
type IRemoverRepository<'TEntity, 'TKey when 'TEntity: not struct and 'TKey: not struct> =

    abstract AsyncRemoveManyByKeys : 'TKey seq -> Async<unit>

    abstract AsyncRemoveMany : 'TEntity seq -> Async<unit>
```

### `IUnitOfWork`

It's an interface to manage the changes to the persistence.

```f#
[<Interface>]
type IUnitOfWork =

    abstract SaveChanges: unit -> unit
```

## Implementations

All types of repositories has implementations for Entity Framework Core, and MongoDB.