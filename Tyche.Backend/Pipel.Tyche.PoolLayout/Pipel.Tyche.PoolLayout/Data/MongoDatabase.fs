namespace Pipel.Tyche.PoolLayout.Data

open MongoDB.Bson.Serialization.Conventions
open MongoDB.Driver

[<Interface>]
type IMongoDatabaseSettings =

    abstract member ConnectionString : string

    abstract member DatabaseName : string

[<CLIMutable>]
type MongoDatabaseSettings =
    { ConnectionString: string
      DatabaseName: string }
    interface IMongoDatabaseSettings with
        member this.ConnectionString = this.ConnectionString
        member this.DatabaseName = this.DatabaseName

[<Interface>]
type IMongoContext =

    abstract GetCollection<'T> : string -> IMongoCollection<'T>

type MongoContext(settings: IMongoDatabaseSettings) =

    let client = MongoClient(settings.ConnectionString)

    let database =
        client.GetDatabase(settings.DatabaseName)

    do
        let pack = ConventionPack()
        pack.Add(CamelCaseElementNameConvention())
        ConventionRegistry.Register("default", pack, (fun x -> true))

    interface IMongoContext with

        member this.GetCollection<'T> name = database.GetCollection<'T> name
