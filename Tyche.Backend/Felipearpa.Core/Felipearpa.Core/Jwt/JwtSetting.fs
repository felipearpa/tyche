namespace Felipearpa.Core.Jwt

open Microsoft.Extensions.Configuration

type IJwtSetting =

    abstract Key: string

type LocalJwtSetting(configuration: IConfiguration) =

    let section = configuration.GetSection("Jwt")

    interface IJwtSetting with

        member this.Key = section.GetValue("Key")