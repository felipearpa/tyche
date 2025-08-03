namespace Felipearpa.Core.Jwt

open System
open System.IdentityModel.Tokens.Jwt
open System.Security.Claims
open System.Text
open Microsoft.IdentityModel.Tokens

type JwtSubject =
    { NameIdentifier: string; Name: string }

type IJwtGenerator =

    abstract GenerateToken: JwtSubject -> string

type LocalJWTGenerator(jwtSettings: IJwtSetting) =

    interface IJwtGenerator with

        member this.GenerateToken(jwtSubject) =
            let tokenHandler = JwtSecurityTokenHandler()

            let key = Encoding.ASCII.GetBytes(jwtSettings.Key)

            let tokenDescriptor =
                SecurityTokenDescriptor(
                    Subject =
                        ClaimsIdentity(
                            [ Claim(ClaimTypes.NameIdentifier, jwtSubject.NameIdentifier)
                              Claim(ClaimTypes.Name, jwtSubject.Name) ]
                        ),
                    Expires = DateTime.UtcNow.AddDays(7),
                    SigningCredentials =
                        SigningCredentials(SymmetricSecurityKey(key), SecurityAlgorithms.HmacSha256Signature)
                )

            let token = tokenHandler.CreateToken(tokenDescriptor)

            tokenHandler.WriteToken(token)
