namespace Felipearpa.Tyche.Pool.Domain

type IPoolRepository =
    abstract createPool: ResolvedCreatePoolInput -> Result<Pool, unit> Async
