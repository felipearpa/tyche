namespace Felipearpa.Tyche.Account.Infrastructure

#nowarn "3536"

open System
open Amazon.S3
open Felipearpa.Tyche.Account.Domain

type AvatarS3Storage(client: IAmazonS3) =
    interface IAvatarStorage with
        member this.IssueUploadUrlAsync(accountId, contentLength) =
            async {
                try
                    let request = AvatarUploadUrlRequestBuilder.build accountId contentLength DateTime.UtcNow

                    let! url = client.GetPreSignedURLAsync(request) |> Async.AwaitTask
                    return Ok url
                with _ ->
                    return Error()
            }
