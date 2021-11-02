namespace Pipel.Data

[<Interface>]
type IUnitOfWork =

    abstract SaveChanges : unit -> unit
