# Base project for REST services in F#

## Exceptions

It let to manage the response of rest service when an error occurs. An example output is:

```json
{
    "Error": {
        "Code": "CORE-0",
        "Message": "Internal server error"
    }
}
```

Configuring the response, adds this lines to the Startup.fs

```f#
member this.Configure(app: IApplicationBuilder, env: IWebHostEnvironment) =
        ...

        app.UseExceptionHandler(fun builder ->
            builder.Run(fun context ->
                ExceptionResponse.asyncUpdateResponseToDefaultError
                    context
                    (funcCreateCustomCode)
                    (JsonSerializer())
                |> Async.StartAsTask :> Task))
        |> ignore

        ...
```

**_NOTE:_** `funcCreateCustomCode` is a function with type `Exception -> string` used for return the code of custom exceptions. 

The lines must be before of `app.UseHttpsRedirection()`.

## Page

It's a record to return a result of query as a paged result.

```f#
type Page<'T> =
    { CurrentPage: int
      ItemsCount: int
      PageSize: int
      AllItemsCount: int64
      Items: 'T seq }
```

## IQueryable

It has extension methods to select, filter, group, and sort an `IQueryable` through string expressions.

```f#
 let items = query.Where("percentage > @0", 0.1).ToList()
```

## IJsonSerializer

It's an interface to serialize and deserialize objects.

```f#
[<Interface>]
type IJsonSerializer =

    abstract Serialize<'T> : 'T -> string
    
    abstract Deserialize<'T> : string -> 'T
```

## DefaultJsonSerializer

It's an implementation of `IJsonSerializer` that uses the serializer of `System.Text.Json`. and configured to use camel case.

```f#
let jsonSerializer = DefaultJsonSerializer() :> IJsonSerializer
let obj = {| Code = "c1"; Name = "Code c1" |}
let serializedObj = jsonSerializer.Serialize(obj)
```
