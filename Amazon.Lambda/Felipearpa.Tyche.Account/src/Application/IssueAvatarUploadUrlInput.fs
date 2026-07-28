namespace Felipearpa.Tyche.Account.Application

open Felipearpa.Type

type IssueAvatarUploadUrlInput =
    { CallerAccountId: Ulid
      AccountId: Ulid
      ContentLength: int64 }
