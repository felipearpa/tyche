namespace Felipearpa.Crypto

type IHasher =

    abstract Hash: string -> string

    abstract Verify: string * string -> bool

type BCryptHasher() =

    interface IHasher with

        member this.Hash text = BCrypt.Net.BCrypt.HashPassword(text)

        member this.Verify(text, hash) = BCrypt.Net.BCrypt.Verify(text, hash)
