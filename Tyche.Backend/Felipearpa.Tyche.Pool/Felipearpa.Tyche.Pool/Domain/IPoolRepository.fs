namespace Felipearpa.Tyche.Pool.Domain

type IPoolRepository =
    abstract createPool: ResolvedCreatePoolInput -> Result<CreatePoolOutput, unit> Async
