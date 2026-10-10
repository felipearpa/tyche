namespace Felipearpa.Tyche.Pool.Infrastructure

open System
open System.Collections.Generic
open System.IO
open System.Text
open System.Text.Json
open Amazon.DynamoDBv2.Model

/// Encodes a complete DynamoDB start key, including index key attributes, as an opaque
/// base64url string. Only string and number key attributes are supported.
module StartKeyCodec =

    let encode (key: IDictionary<string, AttributeValue>) =
        use stream = new MemoryStream()

        do
            use writer = new Utf8JsonWriter(stream)
            writer.WriteStartObject()

            for pair in key |> Seq.sortBy _.Key do
                writer.WriteStartObject(pair.Key)

                if not (isNull pair.Value.S) then
                    writer.WriteString("S", pair.Value.S)
                elif not (isNull pair.Value.N) then
                    writer.WriteString("N", pair.Value.N)
                else
                    raise (InvalidDataException $"Unsupported start key attribute {pair.Key}")

                writer.WriteEndObject()

            writer.WriteEndObject()

        Convert.ToBase64String(stream.ToArray()).TrimEnd('=').Replace('+', '-').Replace('/', '_')

    let tryDecode (encoded: string) : Dictionary<string, AttributeValue> option =
        try
            let padded =
                encoded.Replace('-', '+').Replace('_', '/')
                + String('=', (4 - encoded.Length % 4) % 4)

            use document = JsonDocument.Parse(Encoding.UTF8.GetString(Convert.FromBase64String padded))

            let entries =
                document.RootElement.EnumerateObject()
                |> Seq.map (fun property ->
                    let value =
                        match property.Value.TryGetProperty "S", property.Value.TryGetProperty "N" with
                        | (true, text), _ -> AttributeValue(S = text.GetString())
                        | _, (true, number) -> AttributeValue(N = number.GetString())
                        | _ -> raise (InvalidDataException "Unsupported start key value")

                    KeyValuePair(property.Name, value))

            Some(Dictionary(entries))
        with
        | :? FormatException
        | :? JsonException
        | :? InvalidDataException
        | :? InvalidOperationException
        | :? ArgumentException -> None
