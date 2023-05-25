namespace Felipearpa.Tyche.PoolLayout.Api

open Amazon.DynamoDBv2
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Configuration

module RegistersForDatabases =

    type IServiceCollection with

        member this.RegisterDatabases(configuration: IConfiguration) =
            this
                .AddDefaultAWSOptions(configuration.GetAWSOptions())
                .AddAWSService<IAmazonDynamoDB>()
