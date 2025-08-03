namespace Felipearpa.Core.Jwt

open Microsoft.Extensions.Configuration

type IJwtSetting =
    abstract Key: string
    abstract Authority: string
    abstract Issuer: string
    abstract Audience: string

type LocalJwtSetting(configuration: IConfiguration) =

    let section = configuration.GetSection("Jwt")

    interface IJwtSetting with
        member this.Key = section.GetValue("Key")
        member this.Authority = section.GetValue("Authority")
        member this.Issuer = section.GetValue("Issuer")
        member this.Audience = section.GetValue("Audience")
