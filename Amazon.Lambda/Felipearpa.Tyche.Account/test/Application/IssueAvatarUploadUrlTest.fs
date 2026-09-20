module IssueAvatarUploadUrlTest

open Felipearpa.Tyche.Account.Application
open Felipearpa.Tyche.Account.Domain
open Felipearpa.Type
open FsUnitTyped
open Xunit

let private accountId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6A"

let private otherAccountId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6B"

let private fakeStorage (result: Result<string, unit>) =
    let issued = ResizeArray<Ulid * int64>()

    let storage =
        { new IAvatarStorage with
            member _.IssueUploadUrlAsync(accountId, contentLength) =
                issued.Add(accountId, contentLength)
                async { return result } }

    storage, issued

[<Fact>]
let ``given the account owner when executed then returns the upload url`` () =
    async {
        let storage, issued = fakeStorage (Ok "https://tyche-avatars.s3.us-east-2.amazonaws.com/avatars/x.jpg")
        let service = IssueAvatarUploadUrl(storage)

        let! result =
            service.ExecuteAsync(
                { CallerAccountId = accountId
                  AccountId = accountId
                  ContentLength = 512_000L }
            )

        result |> shouldEqual (Ok "https://tyche-avatars.s3.us-east-2.amazonaws.com/avatars/x.jpg")
        issued |> List.ofSeq |> shouldEqual [ accountId, 512_000L ]
    }

[<Fact>]
let ``given a caller other than the account owner when executed then returns NotAccountOwner and issues nothing`` () =
    async {
        let storage, issued = fakeStorage (Ok "unused")
        let service = IssueAvatarUploadUrl(storage)

        let! result =
            service.ExecuteAsync(
                { CallerAccountId = otherAccountId
                  AccountId = accountId
                  ContentLength = 512_000L }
            )

        result |> shouldEqual (Error NotAccountOwner)
        issued.Count |> shouldEqual 0
    }

[<Fact>]
let ``given a content length above the maximum when executed then returns InvalidContentLength and issues nothing`` () =
    async {
        let storage, issued = fakeStorage (Ok "unused")
        let service = IssueAvatarUploadUrl(storage)

        let! result =
            service.ExecuteAsync(
                { CallerAccountId = accountId
                  AccountId = accountId
                  ContentLength = Avatar.maxContentLength + 1L }
            )

        result |> shouldEqual (Error InvalidContentLength)
        issued.Count |> shouldEqual 0
    }

[<Fact>]
let ``given a content length of zero when executed then returns InvalidContentLength and issues nothing`` () =
    async {
        let storage, issued = fakeStorage (Ok "unused")
        let service = IssueAvatarUploadUrl(storage)

        let! result =
            service.ExecuteAsync(
                { CallerAccountId = accountId
                  AccountId = accountId
                  ContentLength = 0L }
            )

        result |> shouldEqual (Error InvalidContentLength)
        issued.Count |> shouldEqual 0
    }

[<Fact>]
let ``given a content length at the maximum when executed then returns the upload url`` () =
    async {
        let storage, issued = fakeStorage (Ok "url")
        let service = IssueAvatarUploadUrl(storage)

        let! result =
            service.ExecuteAsync(
                { CallerAccountId = accountId
                  AccountId = accountId
                  ContentLength = Avatar.maxContentLength }
            )

        result |> shouldEqual (Ok "url")
        issued |> List.ofSeq |> shouldEqual [ accountId, Avatar.maxContentLength ]
    }

[<Fact>]
let ``given a storage failure when executed then returns UploadUrlIssuanceFailed`` () =
    async {
        let storage, _ = fakeStorage (Error())
        let service = IssueAvatarUploadUrl(storage)

        let! result =
            service.ExecuteAsync(
                { CallerAccountId = accountId
                  AccountId = accountId
                  ContentLength = 512_000L }
            )

        result |> shouldEqual (Error UploadUrlIssuanceFailed)
    }
