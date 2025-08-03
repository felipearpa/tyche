namespace Felipearpa.Tyche.AmazonLambda.Tests.ParameterResolverTest

open Felipearpa.Tyche.AmazonLambda
open FsUnit.Xunit
open Xunit

module ParameterResolverTest =

    [<Fact>]
    let ``given an existing parameter when is queried or error then is returned`` () =
        let parameters = dict [ "existing-parameter", "value" ]
        let parameter = "existing-parameter"

        let actualParameter = parameters |> tryGetStringParamOrError parameter

        actualParameter.IsOk |> should equal true
        let expectedParameter: Result<string, string> = Ok "value"
        actualParameter |> should equal expectedParameter

    [<Fact>]
    let ``given a non existing parameter when is queried or error then error is returned`` () =
        let parameters = dict [ "existing-parameter", "value" ]
        let parameter = "non-existing-parameter"

        let actualParameter = parameters |> tryGetStringParamOrError parameter

        actualParameter.IsError |> should be True

    [<Fact>]
    let ``given an existing parameter when is queried or none then is returned`` () =
        let parameters = dict [ "existing-parameter", "value" ]
        let parameter = "existing-parameter"

        let actualParameter = parameters |> tryGetStringParamOrNone parameter

        actualParameter.IsSome |> should equal true
        let expectedParameter = Some "value"
        actualParameter |> should equal expectedParameter

    [<Fact>]
    let ``given a non existing parameter when is queried or none then error is returned`` () =
        let parameters = dict [ "existing-parameter", "value" ]
        let parameter = "non-existing-parameter"

        let actualParameter = parameters |> tryGetStringParamOrNone parameter

        actualParameter.IsNone |> should be True
