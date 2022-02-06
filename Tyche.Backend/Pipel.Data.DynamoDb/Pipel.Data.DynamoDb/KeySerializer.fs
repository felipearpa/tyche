namespace Pipel.Data.DynamoDb

open System.Collections.Generic
open Amazon.DynamoDBv2.Model

type IKeySerializer =

    abstract Serialize : IDictionary<string, AttributeValue> -> string

    abstract Deserialize : string -> IDictionary<string, AttributeValue>
