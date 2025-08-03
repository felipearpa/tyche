namespace Felipearpa.Tyche.AmazonLambda

open System.Text.Json
open System.Text.Json.Serialization
open Microsoft.AspNetCore.Http
open Microsoft.Extensions.DependencyInjection

module HttpContextFactory =

    let create () : HttpContext =
        let options = JsonSerializerOptions()
        options.Converters.Add(JsonFSharpConverter())

        let services =
            ServiceCollection()
                .AddLogging()
                .AddSingleton<JsonSerializerOptions>(options)
                .BuildServiceProvider()

        let httpContext = DefaultHttpContext()
        httpContext.RequestServices <- services

        httpContext
