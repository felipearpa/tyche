namespace Felipearpa.Tyche.Account.Infrastructure

open System
open Amazon.S3
open Amazon.S3.Model
open Felipearpa.Type

module AvatarUploadUrlRequestBuilder =

    let build (accountId: Ulid) (contentLength: int64) (now: DateTime) =
        let request =
            GetPreSignedUrlRequest(
                BucketName = AvatarBucket.name,
                Key = AvatarBucket.keyOf accountId,
                Verb = HttpVerb.PUT,
                Expires = now.Add AvatarBucket.uploadUrlExpiry,
                ContentType = AvatarBucket.contentType
            )

        request.Headers.ContentLength <- contentLength
        request.Headers.CacheControl <- AvatarBucket.cacheControl
        request
