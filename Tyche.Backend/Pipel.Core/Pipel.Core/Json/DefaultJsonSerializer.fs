namespace Pipel.Core.Json

open System.Text.Json
open System.Text.Json.Serialization

type DefaultJsonSerializer() =

    let createDefaultOptions () =
        let options =
            JsonSerializerOptions(PropertyNamingPolicy = JsonNamingPolicy.CamelCase)

        options.Converters.Add(JsonFSharpConverter())
        options

    interface IJsonSerializer with

        member this.Serialize<'T>(value: 'T) : string =
            JsonSerializer.Serialize<'T>(value, createDefaultOptions ())

        member this.Deserialize<'T>(value: string) : 'T =
            JsonSerializer.Deserialize<'T>(value, createDefaultOptions ())
