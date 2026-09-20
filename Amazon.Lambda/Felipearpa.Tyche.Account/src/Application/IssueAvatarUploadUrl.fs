namespace Felipearpa.Tyche.Account.Application

open Felipearpa.Tyche.Account.Domain

type IssueAvatarUploadUrlFailure =
    | NotAccountOwner
    | InvalidContentLength
    | UploadUrlIssuanceFailed

type IssueAvatarUploadUrl(avatarStorage: IAvatarStorage) =

    member this.ExecuteAsync(input: IssueAvatarUploadUrlInput) : Result<string, IssueAvatarUploadUrlFailure> Async =
        async {
            if input.CallerAccountId <> input.AccountId then
                return Error NotAccountOwner
            elif input.ContentLength <= 0L || input.ContentLength > Avatar.maxContentLength then
                return Error InvalidContentLength
            else
                let! urlResult = avatarStorage.IssueUploadUrlAsync(input.AccountId, input.ContentLength)

                return
                    match urlResult with
                    | Ok url -> Ok url
                    | Error _ -> Error UploadUrlIssuanceFailed
        }
