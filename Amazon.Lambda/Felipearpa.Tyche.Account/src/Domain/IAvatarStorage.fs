namespace Felipearpa.Tyche.Account.Domain

open Felipearpa.Type

type IAvatarStorage =
    abstract IssueUploadUrlAsync: Ulid * int64 -> Result<string, unit> Async
