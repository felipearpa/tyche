namespace Pipel.Tyche.PoolLayout.Api

open Microsoft.Extensions.Configuration
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Options
open Pipel.Tyche.PoolLayout.Data

module RegistersForDatabases =

    type IServiceCollection with

        member this.RegisterDatabases(configuration: IConfiguration) =
            this
                .Configure<MongoDatabaseSettings>(configuration.GetSection("DatabaseSettings"))
                .AddSingleton<IMongoDatabaseSettings>(fun sp ->
                    sp
                        .GetRequiredService<IOptions<MongoDatabaseSettings>>()
                        .Value
                    :> IMongoDatabaseSettings)
                .AddSingleton<IMongoContext, MongoContext>()
