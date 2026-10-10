namespace Felipearpa.Tyche.AmazonLambda.Event

open System
open System.IO
open System.Text
open System.Text.Json

/// Writes CloudWatch embedded metric format records to standard output, where Lambda turns
/// them into metrics without extra API calls. Dimensions stay low-cardinality (the handler
/// name); identifiers such as pool and operation IDs are logged as properties.
module OperationMetrics =

    [<Literal>]
    let metricNamespace = "Tyche/PoolOperations"

    type Metric =
        { Name: string
          Value: float
          Unit: string }

    let count name (value: int) =
        { Name = name
          Value = float value
          Unit = "Count" }

    let seconds name (value: float) = { Name = name; Value = value; Unit = "Seconds" }

    let emit (handler: string) (metrics: Metric list) (properties: (string * string) list) =
        use stream = new MemoryStream()

        do
            use writer = new Utf8JsonWriter(stream)
            writer.WriteStartObject()
            writer.WriteStartObject("_aws")
            writer.WriteNumber("Timestamp", DateTimeOffset.UtcNow.ToUnixTimeMilliseconds())
            writer.WriteStartArray("CloudWatchMetrics")
            writer.WriteStartObject()
            writer.WriteString("Namespace", metricNamespace)
            writer.WriteStartArray("Dimensions")
            writer.WriteStartArray()
            writer.WriteStringValue("Handler")
            writer.WriteEndArray()
            writer.WriteEndArray()
            writer.WriteStartArray("Metrics")

            for metric in metrics do
                writer.WriteStartObject()
                writer.WriteString("Name", metric.Name)
                writer.WriteString("Unit", metric.Unit)
                writer.WriteEndObject()

            writer.WriteEndArray()
            writer.WriteEndObject()
            writer.WriteEndArray()
            writer.WriteEndObject()
            writer.WriteString("Handler", handler)

            for metric in metrics do
                writer.WriteNumber(metric.Name, metric.Value)

            for key, value in properties do
                writer.WriteString(key, value)

            writer.WriteEndObject()

        Console.WriteLine(Encoding.UTF8.GetString(stream.ToArray()))
