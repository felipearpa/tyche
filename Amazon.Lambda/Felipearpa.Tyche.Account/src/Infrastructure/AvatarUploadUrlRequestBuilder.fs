namespace Felipearpa.Tyche.Account.Infrastructure

open System
open Amazon.S3
open Amazon.S3.Model
open Felipearpa.Type

module AvatarUploadUrlRequestBuilder =

    [<Literal>]
    let private contentType = "image/jpeg"

    [<Literal>]
    let private cacheControl = "max-age=0, must-revalidate"

    let private uploadUrlExpiry = TimeSpan.FromMinutes 5.0

    let private keyOf (accountId: Ulid) = $"avatars/{accountId.Value}.jpg"

    let build (bucketName: string) (accountId: Ulid) (contentLength: int64) (now: DateTime) =
        let request =
            GetPreSignedUrlRequest(
                BucketName = bucketName,
                Key = keyOf accountId,
                Verb = HttpVerb.PUT,
                Expires = now.Add uploadUrlExpiry,
                ContentType = contentType
            )

        request.Headers.ContentLength <- contentLength
        request.Headers.CacheControl <- cacheControl
        request
