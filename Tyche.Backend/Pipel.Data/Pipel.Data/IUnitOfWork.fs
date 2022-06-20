namespace Pipel.Data

type IUnitOfWork =

    abstract SaveChanges : unit -> unit
