module AvatarUploadUrlRequestBuilderTest

open System
open Amazon.S3
open Felipearpa.Tyche.Account.Infrastructure
open Felipearpa.Type
open FsUnitTyped
open Xunit

let private accountId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6A"

let private now = DateTime(2026, 7, 26, 12, 0, 0, DateTimeKind.Utc)

[<Fact>]
let ``given an account id when built then targets the fixed avatar key in the avatars bucket`` () =
    let request = AvatarUploadUrlRequestBuilder.build accountId 512_000L now

    request.BucketName |> shouldEqual "tyche-avatars"
    request.Key |> shouldEqual "avatars/01K1PX1TX2NM1HG851S1V0QG6A.jpg"

[<Fact>]
let ``given an account id when built then signs a jpeg PUT`` () =
    let request = AvatarUploadUrlRequestBuilder.build accountId 512_000L now

    request.Verb |> shouldEqual HttpVerb.PUT
    request.ContentType |> shouldEqual "image/jpeg"

[<Fact>]
let ``given a content length when built then signs it`` () =
    let request = AvatarUploadUrlRequestBuilder.build accountId 512_000L now

    request.Headers.ContentLength |> shouldEqual 512_000L

[<Fact>]
let ``given a time when built then expires five minutes later`` () =
    let request = AvatarUploadUrlRequestBuilder.build accountId 512_000L now

    request.Expires |> shouldEqual (now.AddMinutes 5.0)

[<Fact>]
let ``given an account id when built then requires revalidation on the stored object`` () =
    let request = AvatarUploadUrlRequestBuilder.build accountId 512_000L now

    request.Headers.CacheControl |> shouldEqual "max-age=0, must-revalidate"
