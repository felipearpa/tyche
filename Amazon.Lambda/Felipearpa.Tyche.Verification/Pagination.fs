namespace Felipearpa.Tyche.Verification

#nowarn "3536"

open System
open System.Collections.Generic
open System.Diagnostics
open System.Text
open System.Threading
open System.Web
open Amazon
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Tyche.PoolLayout.Infrastructure
open Felipearpa.Type

/// Gives every query that sets no `Limit` of its own a small one, so a fixture of a few items
/// spans several storage pages. Only this client sets it; the production requests are unchanged.
type PageLimitedDynamoDbClient(region: RegionEndpoint, pageLimit: int) =
    inherit AmazonDynamoDBClient(region)

    override _.QueryAsync(request: QueryRequest, cancellationToken: CancellationToken) =
        if not request.Limit.HasValue then
            request.Limit <- Nullable pageLimit

        base.QueryAsync(request, cancellationToken)

/// Writes the `pk`/`sk` tokens that the backend issued before version 1, as a client may still
/// hold them, and reads tokens with the production `DynamoDbKeySerializer`.
type LegacyTokenKeySerializer() =
    let reader = DynamoDbKeySerializer() :> IKeySerializer

    interface IKeySerializer with
        member _.Serialize lastEvaluatedKey =
            Json.JsonSerializer.Serialize
                {| pk = lastEvaluatedKey[Key.pk].S
                   sk = lastEvaluatedKey[Key.sk].S |}

        member _.Deserialize token = reader.Deserialize token

/// One page as a client sees it: the item identities and the `next` token.
type FixturePage = { Ids: string list; Next: string option }

/// How a query orders its results by the index sort attribute carried in its tokens.
type SortOrder =
    | Ascending
    | Descending
    | DescendingNumber

/// A paginated operation under test and what the fixture data makes it return.
type PaginationOperation =
    { Name: string
      Fetch: IKeySerializer -> string option -> Async<FixturePage>
      Expected: string list
      Contract: StartKeyContract
      SortAttribute: string
      Order: SortOrder
      /// The operation's first-page request, for sending a lossy start key to DynamoDB directly.
      FirstRequest: unit -> QueryRequest
      ExpectsFilteredEmptyPage: bool
      /// True when a legacy pk/sk token is a complete start key (the base-table query).
      LegacyComplete: bool }

/// Service-backed pagination fixture for the shared continuation codec, the eight indexes it
/// serves, and the base-table layout-match query, with the ranked, username-propagation, and
/// settlement-discovery continuations as regression controls. Runs only on isolated tables
/// (`POOL_TABLE_NAME`, `POOL_LAYOUT_TABLE_NAME`); see README.md.
module Pagination =

    [<Literal>]
    let pageLimit = 2

    let private convergenceDeadline = TimeSpan.FromMinutes 3.0

    let private s (value: string) = AttributeValue(S = value)
    let private n (value: int) = AttributeValue(N = string value)
    let private item (entries: (string * AttributeValue) list) = Dictionary(dict entries)
    let private instant (value: DateTime) = value.ToString("o")
    let private ids count = List.init count (fun _ -> Ulid.random().Value) |> List.sort

    let private check name passed detail : Check =
        printfn "%s %s — %s" (if passed then "PASS" else "FAIL") name detail
        { Name = name; Passed = passed; Detail = detail }

    /// Fixture identities. Gambler 0 is the gambler whose bets, pools, and timeline are read.
    type Fixture =
        { PoolId: string
          OtherPoolIds: string list
          LayoutId: string
          OtherLayoutIds: string list
          ClosedLayoutId: string
          Gamblers: string list
          Matches: string list
          Base: DateTime }

        member this.Gambler = this.Gamblers.Head
        member this.FirstMatch = this.Matches.Head

    let newFixture () =
        { PoolId = Ulid.random().Value
          OtherPoolIds = ids 6
          LayoutId = Ulid.random().Value
          OtherLayoutIds = ids 6
          ClosedLayoutId = Ulid.random().Value
          Gamblers = ids 8
          Matches = ids 12
          Base = DateTime.UtcNow.Date.AddDays(-30.0) }

    // Pool memberships, by gambler index. Members 4 and 5 left the pool (CLOSED); they sit
    // together in score order, so a leaderboard page holds only them and is filtered empty.
    let private memberScores = [ 9; 5; 5; 5; 2; 1; 0; 0 ]
    let private closedMembers = set [ 4; 5 ]
    let private usernames = [ "ana"; "bea"; "bea"; "bea"; "carla"; "dani"; "émile"; "zoë" ]

    // Gambler 0's memberships in the other pools; pools 1 and 2 are CLOSED and adjacent in
    // score order after gambler 0's score of 9 in the main pool.
    let private otherPoolScores = [ 7; 5; 5; 3; 3; 0 ]
    let private closedOtherPools = set [ 1; 2 ]

    // Gambler 0's bets, by match index. Computed bets are filtered out of pending and live
    // reads and are the only finished bets; their pairs fill whole pages in every order.
    let private computedMatches = set [ 2; 3; 6; 7; 8; 9 ]

    /// Finished order (newest settlement first); matches 7 and 6 share a settlement time.
    let private finishedOrder = [ 9; 8; 7; 6; 3; 2 ]

    let private searchedMatches = set [ 9; 8; 3; 2 ]
    let private searchText = "brasil"

    /// Layout matches 2 and 3 belong to version 2, so a version 1 read has an empty page.
    let private versionTwoMatches = set [ 2; 3 ]

    /// Gamblers whose bet on the first match is dated in the future (synthetic: one match has
    /// one kickoff in real data), so the legacy match-bets lock filter empties one page.
    let private unlockedMatchBets = set [ 2; 3 ]

    let private matchDate (fixture: Fixture) index =
        fixture.Base.AddHours(float (if index = 5 then 4 else index))

    let private settledAt (fixture: Fixture) index =
        let rank = finishedOrder |> List.findIndex ((=) index)
        let rank = if rank = 3 then 2 else rank
        fixture.Base.AddDays(20.0).AddMinutes(-(float rank))

    let private membership (fixture: Fixture) poolId (gamblerId: string) username score closed =
        item
            [ "pk", s $"POOL#{poolId}"
              "sk", s $"GAMBLER#{gamblerId}"
              "poolId", s poolId
              "poolName", s "Verification"
              "gamblerId", s gamblerId
              "gamblerUsername", s username
              "gamblerEmail", s $"{gamblerId.ToLowerInvariant()}@verification.invalid"
              "status", s (if closed then "CLOSED" else "OPENED")
              "filter", s "verification"
              "poolLayoutId", s fixture.LayoutId
              "score", n score
              "poolLayoutVersion", n 1
              "getGamblersByPoolLayoutPk", s $"POOLLAYOUT#{fixture.LayoutId}"
              "getGamblersByPoolLayoutSk", s $"POOL#{poolId}#GAMBLER#{gamblerId}" ]

    let private bet
        (fixture: Fixture)
        poolId
        (gamblerId: string)
        username
        (matchIndex: int)
        (kickoff: DateTime)
        (settled: DateTime option)
        (rankedTotal: int option)
        =
        let matchId = fixture.Matches[matchIndex]
        let searched = searchedMatches.Contains matchIndex

        let home, away =
            if searched then ("BRA", "Brasil"), ("PER", "Peru") else ("COL", "Colombia"), ("ARG", "Argentina")

        let common =
            [ "pk", s $"GAMBLER#{gamblerId}#POOL#{poolId}"
              "sk", s $"MATCH#{matchId}"
              "poolId", s poolId
              "gamblerId", s gamblerId
              "gamblerUsername", s username
              "gamblerEmail", s $"{gamblerId.ToLowerInvariant()}@verification.invalid"
              "matchId", s matchId
              "homeTeamId", s (fst home)
              "homeTeamName", s (snd home)
              "awayTeamId", s (fst away)
              "awayTeamName", s (snd away)
              "matchDateTime", s (instant kickoff)
              "poolLayoutId", s fixture.LayoutId
              "poolLayoutVersion", n 1
              "round", s "Fase de grupos"
              "filter", s $"{(snd home).ToLowerInvariant()} {(snd away).ToLowerInvariant()}"
              "homeTeamBet", n 1
              "awayTeamBet", n 0
              "getPoolGamblerScoresByMatchPk", s $"MATCH#{matchId}"
              "getPoolGamblerScoresByMatchSk", s $"POOL#{poolId}#GAMBLER#{gamblerId}" ]

        let computed =
            settled
            |> Option.map (fun at ->
                [ "homeTeamScore", n 1
                  "awayTeamScore", n 0
                  "score", n 5
                  "computedRequestId", s "VERIFICATION"
                  "computedDateTime", s (instant at) ])
            |> Option.defaultValue []

        let ranked =
            rankedTotal
            |> Option.map (fun total ->
                [ "poolMembershipGeneration", s "VERIFICATION"
                  "poolScoreVersion", n 1
                  "totalPoolPoints", n total
                  "totalPoolPointsState", s "KNOWN"
                  "rankingActive", AttributeValue(BOOL = true)
                  "getPoolMatchBetsByTotalPk", s (RankingKey.partition (Ulid.newOf poolId) (Ulid.newOf matchId))
                  "getPoolMatchBetsByTotalSk", s (RankingKey.sortKey (KnownTotal total) (Ulid.newOf gamblerId)) ])
            |> Option.defaultValue []

        item (common @ computed @ ranked)

    let private layoutRoot (fixture: Fixture) layoutId status (start: DateTime) =
        item
            [ "pk", s $"POOLLAYOUT#{layoutId}"
              "sk", s $"POOLLAYOUT#{layoutId}"
              "poolLayoutId", s layoutId
              "poolLayoutName", s "Verification"
              "poolLayoutVersion", n 2
              "status", s status
              "startDateTime", s (instant start) ]

    let private layoutMatch (fixture: Fixture) index =
        let matchId = fixture.Matches[index]

        item
            [ "pk", s $"POOLLAYOUT#{fixture.LayoutId}"
              "sk", s $"MATCH#{matchId}"
              "matchId", s matchId
              "poolLayoutId", s fixture.LayoutId
              "homeTeamId", s "COL"
              "homeTeamName", s "Colombia"
              "awayTeamId", s "ARG"
              "awayTeamName", s "Argentina"
              "matchDateTime", s (instant (matchDate fixture index))
              "poolLayoutVersion", n (if versionTwoMatches.Contains index then 2 else 1)
              "round", s "Fase de grupos" ]

    let poolItems (fixture: Fixture) =
        let gambler = fixture.Gambler

        let members =
            fixture.Gamblers
            |> List.mapi (fun index gamblerId ->
                membership fixture fixture.PoolId gamblerId usernames[index] memberScores[index] (closedMembers.Contains index))

        let otherMemberships =
            fixture.OtherPoolIds
            |> List.mapi (fun index poolId ->
                membership fixture poolId gambler usernames.Head otherPoolScores[index] (closedOtherPools.Contains index))

        let gamblerBets =
            [ 0 .. fixture.Matches.Length - 1 ]
            |> List.map (fun index ->
                bet
                    fixture
                    fixture.PoolId
                    gambler
                    usernames.Head
                    index
                    (matchDate fixture index)
                    (if computedMatches.Contains index then Some(settledAt fixture index) else None)
                    (if index = 0 then Some memberScores.Head else None))

        let firstMatchBets =
            fixture.Gamblers
            |> List.mapi (fun index gamblerId -> index, gamblerId)
            |> List.tail
            |> List.map (fun (index, gamblerId) ->
                let kickoff =
                    if unlockedMatchBets.Contains index then DateTime.UtcNow.AddDays 30.0 else matchDate fixture 0

                bet fixture fixture.PoolId gamblerId usernames[index] 0 kickoff None (Some memberScores[index]))

        let otherPoolBets =
            fixture.OtherPoolIds
            |> List.map (fun poolId -> bet fixture poolId gambler usernames.Head 0 (matchDate fixture 0) None None)

        members @ otherMemberships @ gamblerBets @ firstMatchBets @ otherPoolBets

    let layoutItems (fixture: Fixture) =
        let opened =
            fixture.LayoutId :: fixture.OtherLayoutIds
            |> List.mapi (fun index layoutId -> layoutRoot fixture layoutId "OPENED" (fixture.Base.AddDays(float ((index + 1) / 2))))

        let closed = layoutRoot fixture fixture.ClosedLayoutId "CLOSED" fixture.Base
        let matches = [ 0 .. fixture.Matches.Length - 1 ] |> List.map (layoutMatch fixture)
        opened @ [ closed ] @ matches

    let private matchIds (fixture: Fixture) indexes =
        indexes |> List.map (fun index -> fixture.Matches[index])

    let private pendingMatches (fixture: Fixture) =
        [ 0 .. fixture.Matches.Length - 1 ]
        |> List.filter (computedMatches.Contains >> not)
        |> matchIds fixture

    let private unused: string -> IDictionary<string, AttributeValue> =
        fun _ -> invalidOp "a first-page request has no token"

    let private page (items: 'T seq) (id: 'T -> string) (next: string option) =
        { Ids = items |> Seq.map id |> List.ofSeq; Next = next }

    /// The ten shared-codec operations with what the fixture makes them return.
    let operations (client: IAmazonDynamoDB) (fixture: Fixture) : PaginationOperation list =
        let bets serializer = PoolGamblerBetDynamoDbRepository(serializer, client) :> IPoolGamblerBetRepository
        let scores serializer = PoolGamblerScoreDynamoDbRepository(serializer, client) :> IPoolGamblerScoreRepository
        let pools serializer = PoolDynamoDbRepository(serializer, client) :> IPoolRepository
        let layouts serializer =
            PoolLayoutDynamoDbRepository(serializer, client) :> Felipearpa.Tyche.PoolLayout.Domain.IPoolLayoutRepository
        let poolId = Ulid.newOf fixture.PoolId
        let gamblerId = Ulid.newOf fixture.Gambler
        let layoutId = Ulid.newOf fixture.LayoutId
        let firstMatch = Ulid.newOf fixture.FirstMatch
        let betMatch (bet: PoolGamblerBet) = bet.MatchId.Value

        let betPage (read: IPoolGamblerBetRepository -> Async<PoolGamblerBet CursorPage>) serializer =
            async {
                let! result = read (bets serializer)
                return page result.Items betMatch result.Next
            }

        let pendingContract = GetPendingPoolGamblerBetsRequestBuilder.startKeyContract poolId gamblerId
        let finishedContract = GetFinishedPoolGamblerBetsRequestBuilder.startKeyContract poolId gamblerId

        let finishedRequest search () =
            GetFinishedPoolGamblerBetsRequestBuilder.build poolId gamblerId search None unused

        [ { Name = "pending bets"
            Fetch = fun serializer next -> betPage (fun r -> r.GetPendingPoolGamblerBetsAsync(poolId, gamblerId, None, next)) serializer
            Expected = pendingMatches fixture
            Contract = pendingContract
            SortAttribute = "matchDateTime"
            Order = Ascending
            FirstRequest = fun () -> GetPendingPoolGamblerBetsRequestBuilder.build poolId gamblerId None None unused
            ExpectsFilteredEmptyPage = true
            LegacyComplete = false }
          { Name = "live bets"
            Fetch = fun serializer next -> betPage (fun r -> r.GetLivePoolGamblerBetsAsync(poolId, gamblerId, None, next)) serializer
            Expected = pendingMatches fixture
            Contract = pendingContract
            SortAttribute = "matchDateTime"
            Order = Descending
            FirstRequest = fun () -> GetLivePoolGamblerBetsRequestBuilder.build poolId gamblerId None None unused
            ExpectsFilteredEmptyPage = true
            LegacyComplete = false }
          { Name = "finished bets"
            Fetch =
              fun serializer next -> betPage (fun r -> r.GetFinishedPoolGamblerBetsAsync(poolId, gamblerId, None, next)) serializer
            Expected = matchIds fixture finishedOrder
            Contract = finishedContract
            SortAttribute = "computedDateTime"
            Order = Descending
            FirstRequest = finishedRequest None
            ExpectsFilteredEmptyPage = false
            LegacyComplete = false }
          { Name = "finished bets, search text"
            Fetch =
              fun serializer next ->
                  betPage (fun r -> r.GetFinishedPoolGamblerBetsAsync(poolId, gamblerId, Some searchText, next)) serializer
            Expected = finishedOrder |> List.filter searchedMatches.Contains |> matchIds fixture
            Contract = finishedContract
            SortAttribute = "computedDateTime"
            Order = Descending
            FirstRequest = finishedRequest (Some searchText)
            ExpectsFilteredEmptyPage = true
            LegacyComplete = false }
          { Name = "legacy match bets"
            Fetch =
              fun serializer next ->
                  async {
                      let! result = (bets serializer).GetPoolMatchGamblerBetsAsync(poolId, firstMatch, next)
                      return page result.Items (fun bet -> bet.GamblerId.Value) result.Next
                  }
            Expected =
              fixture.Gamblers
              |> List.indexed
              |> List.filter (fst >> unlockedMatchBets.Contains >> not)
              |> List.map snd
            Contract = GetPoolMatchGamblerBetsRequestBuilder.startKeyContract poolId firstMatch
            SortAttribute = "getPoolGamblerScoresByMatchSk"
            Order = Ascending
            FirstRequest = fun () -> GetPoolMatchGamblerBetsRequestBuilder.build poolId firstMatch None unused
            ExpectsFilteredEmptyPage = true
            LegacyComplete = false }
          { Name = "gambler pool scores"
            Fetch =
              fun serializer next ->
                  async {
                      let! result = (scores serializer).GetGamblerScoresAsync(gamblerId, next)
                      return page result.Items (fun score -> score.PoolId.Value) result.Next
                  }
            Expected =
              fixture.PoolId
              :: (fixture.OtherPoolIds
                  |> List.indexed
                  |> List.filter (fst >> closedOtherPools.Contains >> not)
                  |> List.map snd)
            Contract = GetGamblerScoresRequestBuilder.startKeyContract gamblerId
            SortAttribute = "score"
            Order = DescendingNumber
            FirstRequest = fun () -> GetGamblerScoresRequestBuilder.build gamblerId None unused
            ExpectsFilteredEmptyPage = true
            LegacyComplete = false }
          { Name = "pool leaderboard"
            Fetch =
              fun serializer next ->
                  async {
                      let! result = (scores serializer).GetPoolScoresAsync(poolId, next)
                      return page result.Items (fun score -> score.GamblerId.Value) result.Next
                  }
            Expected =
              fixture.Gamblers
              |> List.indexed
              |> List.filter (fst >> closedMembers.Contains >> not)
              |> List.map snd
            Contract = GetPoolScoresRequestBuilder.startKeyContract poolId
            SortAttribute = "score"
            Order = DescendingNumber
            FirstRequest = fun () -> GetPoolScoresRequestBuilder.build poolId None unused
            ExpectsFilteredEmptyPage = true
            LegacyComplete = false }
          { Name = "pool members"
            Fetch =
              fun serializer next ->
                  async {
                      let! result = (pools serializer).GetPoolMembersAsync(poolId, next)
                      return page result.Items (fun member' -> member'.GamblerId.Value) result.Next
                  }
            Expected = fixture.Gamblers
            Contract = GetPoolMembersRequestBuilder.startKeyContract poolId
            SortAttribute = "gamblerUsername"
            Order = Ascending
            FirstRequest = fun () -> GetPoolMembersRequestBuilder.build poolId None unused
            ExpectsFilteredEmptyPage = false
            LegacyComplete = false }
          { Name = "gamblers by layout (fan-out)"
            Fetch =
              fun serializer next ->
                  async {
                      let! result = (pools serializer).GetGamblersByPoolLayoutAsync(layoutId, next)
                      return page result.Items (fun gambler -> $"{gambler.PoolId.Value}/{gambler.GamblerId.Value}") result.Next
                  }
            Expected =
              (fixture.Gamblers |> List.map (fun gambler -> $"{fixture.PoolId}/{gambler}"))
              @ (fixture.OtherPoolIds |> List.map (fun pool -> $"{pool}/{fixture.Gambler}"))
            Contract = GetGamblersByPoolLayoutRequestBuilder.startKeyContract layoutId
            SortAttribute = "getGamblersByPoolLayoutSk"
            Order = Ascending
            FirstRequest = fun () -> GetGamblersByPoolLayoutRequestBuilder.build layoutId None unused
            ExpectsFilteredEmptyPage = false
            LegacyComplete = false }
          { Name = "open layouts"
            Fetch =
              fun serializer next ->
                  async {
                      let! result = (layouts serializer).GetOpenPoolLayoutsAsync next
                      return page result.Items (fun layout -> layout.Id.Value) result.Next
                  }
            Expected = fixture.LayoutId :: fixture.OtherLayoutIds
            Contract = GetOpenPoolLayoutRequestBuilder.startKeyContract
            SortAttribute = "startDateTime"
            Order = Ascending
            FirstRequest = fun () -> GetOpenPoolLayoutRequestBuilder.build None unused
            ExpectsFilteredEmptyPage = false
            LegacyComplete = false }
          { Name = "layout matches (base table)"
            Fetch =
              fun serializer next ->
                  async {
                      let! result = (layouts serializer).GetPoolLayoutMatchesThroughVersionAsync(layoutId, 1, next)
                      return page result.Items (fun match' -> match'.MatchId.Value) result.Next
                  }
            Expected =
              [ 0 .. fixture.Matches.Length - 1 ]
              |> List.filter (versionTwoMatches.Contains >> not)
              |> matchIds fixture
            Contract = GetPendingPoolLayoutMatchesRequestBuilder.startKeyContract layoutId
            SortAttribute = "sk"
            Order = Ascending
            FirstRequest = fun () -> GetPendingPoolLayoutMatchesRequestBuilder.build layoutId 1 None unused
            ExpectsFilteredEmptyPage = true
            LegacyComplete = true } ]

    /// Sends a token the way a client does: URL-encoded in the `next` query parameter, then
    /// decoded by the host.
    let httpTransport (token: string) =
        let query = $"?next={Uri.EscapeDataString token}"
        HttpUtility.ParseQueryString(query)["next"]

    /// Follows `next` from the first page until it is absent, sending each token through
    /// `httpTransport`. Fails after 50 pages.
    let traverse (fetch: string option -> Async<FixturePage>) =
        let rec loop next pages =
            async {
                if List.length pages >= 50 then
                    failwith "the traversal did not end"

                let! current = fetch (next |> Option.map httpTransport)
                let pages = pages @ [ current ]

                match current.Next with
                | Some _ -> return! loop current.Next pages
                | None -> return pages
            }

        loop None []

    let private decodeKey (token: string) =
        match ContinuationToken.decode token with
        | Ok key -> key
        | Error error -> failwith $"an issued token does not decode: %A{error}"

    let private typeLetter value =
        match value with
        | S _ -> "S"
        | N _ -> "N"
        | B _ -> "B"

    let private describeKey (key: Map<string, ScalarKeyValue>) =
        key |> Map.toList |> List.map (fun (name, value) -> $"{name}:{typeLetter value}") |> String.concat ","

    let private sortValue (attribute: string) (key: Map<string, ScalarKeyValue>) =
        match key[attribute] with
        | S value
        | N value -> value
        | B bytes -> Convert.ToBase64String bytes

    let private inOrder (order: SortOrder) (values: string list) =
        let compare (left: string) (right: string) =
            match order with
            | Ascending -> Seq.compareWith compare (Encoding.UTF8.GetBytes left) (Encoding.UTF8.GetBytes right) <= 0
            | Descending -> String.CompareOrdinal(left, right) >= 0
            | DescendingNumber -> Decimal.Parse left >= Decimal.Parse right

        values |> List.pairwise |> List.forall (fun (left, right) -> compare left right)

    let private pagesText (pages: FixturePage list) =
        pages |> List.map (fun current -> string current.Ids.Length) |> String.concat ","

    /// Polls the full traversal with a deadline until it returns the expected identities;
    /// index writes are eventually consistent.
    let private converge (operation: PaginationOperation) (serializer: IKeySerializer) =
        async {
            let watch = Stopwatch.StartNew()

            let rec poll attempt =
                async {
                    let! pages = traverse (operation.Fetch serializer)
                    let found = pages |> List.collect _.Ids

                    if List.sort found = List.sort operation.Expected then
                        return Ok(pages, attempt, watch.Elapsed)
                    elif watch.Elapsed > convergenceDeadline then
                        return Error(found, attempt)
                    else
                        do! Async.Sleep 1000
                        return! poll (attempt + 1)
                }

            return! poll 1
        }

    /// A legacy token held from the earlier backend, and its two-attribute start key sent to
    /// DynamoDB.
    let private legacyChecks (client: IAmazonDynamoDB) (operation: PaginationOperation) (firstKey: Map<string, ScalarKeyValue>) =
        async {
            let legacyWriter = LegacyTokenKeySerializer() :> IKeySerializer
            let! first = operation.Fetch legacyWriter None
            let legacyToken = first.Next |> Option.get
            let! second = operation.Fetch legacyWriter (Some(httpTransport legacyToken)) |> Async.Catch

            let lossyKey =
                Dictionary(dict [ "pk", AttributeValue(S = sortValue "pk" firstKey); "sk", AttributeValue(S = sortValue "sk" firstKey) ])

            let request = operation.FirstRequest()
            request.ExclusiveStartKey <- lossyKey
            let! service = client.QueryAsync request |> Async.AwaitTask |> Async.Catch

            if operation.LegacyComplete then
                let! pages = traverse (operation.Fetch legacyWriter)
                let found = pages |> List.collect _.Ids

                return
                    [ check
                          $"{operation.Name}: legacy pk/sk token"
                          (List.sort found = List.sort operation.Expected
                           && pages.Length >= 3
                           && (match service with
                               | Choice1Of2 _ -> true
                               | Choice2Of2 _ -> false))
                          $"legacy tokens traverse all {pages.Length} pages; DynamoDB accepts the pk/sk start key" ]
            else
                let rejectedByContract =
                    match second with
                    | Choice2Of2(InvalidContinuationException(MissingKeyAttribute name)) -> Some name
                    | _ -> None

                let serviceError =
                    match service with
                    | Choice2Of2 error -> Some(error.GetBaseException().Message)
                    | Choice1Of2 _ -> None

                return
                    [ check
                          $"{operation.Name}: legacy pk/sk token"
                          (rejectedByContract.IsSome
                           && (serviceError |> Option.exists (fun message -> message.Contains "starting key is invalid")))
                          $"rejected before the query with MissingKeyAttribute {rejectedByContract}; the same pk/sk start key sent to DynamoDB fails: {serviceError}" ]
        }

    let private traversalChecks (client: IAmazonDynamoDB) (operation: PaginationOperation) =
        async {
            let typed = DynamoDbKeySerializer() :> IKeySerializer
            let! converged = converge operation typed

            match converged with
            | Error(found, attempts) ->
                return
                    [ check
                          $"{operation.Name}: converged traversal"
                          false
                          $"after {attempts} attempts found {found.Length} of {operation.Expected.Length} expected items" ]
            | Ok(pages, attempts, elapsed) ->
                let found = pages |> List.collect _.Ids
                let tokens = pages |> List.choose _.Next
                let decoded = tokens |> List.map decodeKey
                let keys = decoded |> List.map ContinuationKey.attributes

                let contractFailures =
                    decoded
                    |> List.choose (fun key ->
                        match StartKeyContract.check operation.Contract (ContinuationKey.toAttributeValues key) with
                        | Ok() -> None
                        | Error error -> Some(sprintf "%A" error))

                let transportExact = tokens |> List.forall (fun token -> httpTransport token = token)
                let ordered = keys |> List.map (sortValue operation.SortAttribute) |> inOrder operation.Order
                let filteredEmpty = pages |> List.exists (fun current -> current.Ids.IsEmpty && current.Next.IsSome)
                let! repeated = traverse (operation.Fetch typed)

                let traversal =
                    check
                        $"{operation.Name}: typed traversal"
                        (pages.Length >= 3
                         && List.length found = List.length (List.distinct found)
                         && List.sort found = List.sort operation.Expected
                         && (List.last pages).Next.IsNone)
                        $"{pages.Length} storage pages (items per page {pagesText pages}), {found.Length} items, no duplicates; converged on attempt {attempts} after {elapsed.TotalSeconds:F1} s"

                let complete =
                    check
                        $"{operation.Name}: complete keys"
                        (contractFailures.IsEmpty && transportExact && ordered)
                        $"{tokens.Length} tokens, each {describeKey keys.Head}; all fit the query's start-key contract; unchanged by URL transport; boundaries in index order"

                let filtered =
                    if operation.ExpectsFilteredEmptyPage then
                        [ check $"{operation.Name}: filtered-empty page" filteredEmpty "a page with no items kept its token and the traversal continued" ]
                    else
                        []

                let repeatable =
                    check
                        $"{operation.Name}: repeated traversal"
                        (repeated = pages)
                        "following the same tokens again returned the same pages and tokens"

                let! legacy = legacyChecks client operation keys.Head
                return [ traversal; complete ] @ filtered @ [ repeatable ] @ legacy
        }

    let private timelineCheck (client: IAmazonDynamoDB) (fixture: Fixture) =
        async {
            let repository =
                PoolGamblerBetDynamoDbRepository(DynamoDbKeySerializer(), client) :> IPoolGamblerBetRepository

            let timeline =
                GetGamblerBetsTimeline(GetLivePoolGamblerBets repository, GetFinishedPoolGamblerBets repository)

            let! pages =
                traverse (fun next ->
                    async {
                        let! result = timeline.ExecuteAsync(Ulid.newOf fixture.PoolId, Ulid.newOf fixture.Gambler, next)
                        return page result.Items (fun bet -> bet.MatchId.Value) result.Next
                    })

            let live = set (pendingMatches fixture)
            let finished = set (matchIds fixture finishedOrder)
            let found = pages |> List.collect _.Ids
            let firstFinished = found |> List.tryFindIndex finished.Contains |> Option.defaultValue found.Length
            let tags = pages |> List.map (fun current -> current.Next |> Option.map (fun token -> token.Split(':')[0]) |> Option.defaultValue "end")
            let tagText = String.concat "," tags
            let emptyLive = pages |> List.exists (fun current -> current.Ids.IsEmpty && current.Next |> Option.exists (fun token -> token.StartsWith "live:"))

            return
                check
                    "timeline: live then finished phases"
                    (List.length (List.distinct found) = found.Length
                     && set found = live + finished
                     && found |> List.take firstFinished |> List.forall live.Contains
                     && found |> List.skip firstFinished |> List.forall finished.Contains
                     && emptyLive
                     && (List.last pages).Next.IsNone)
                    $"{pages.Length} pages (items {pagesText pages}), tokens {tagText}; every live entry precedes every finished entry; an empty live page kept its live: token"
        }

    let private rankedCheck (client: IAmazonDynamoDB) (fixture: Fixture) =
        async {
            let repository = RankedMatchBetsDynamoDbRepository(client) :> IRankedMatchBetsRepository
            let poolId = Ulid.newOf fixture.PoolId
            let matchId = Ulid.newOf fixture.FirstMatch

            let! pages =
                traverse (fun next ->
                    async {
                        match! repository.GetPageAsync(poolId, matchId, next, pageLimit) with
                        | Ok result -> return page result.Items (fun bet -> bet.GamblerId.Value) result.Next
                        | Error error -> return failwith $"ranked page failed: %A{error}"
                    })

            let expected =
                List.zip fixture.Gamblers memberScores
                |> List.sortWith (fun (lg, lt) (rg, rt) -> if lt <> rt then compare rt lt else String.CompareOrdinal(lg, rg))
                |> List.map fst

            let found = pages |> List.collect _.Ids

            return
                check
                    "regression control: ranked match bets"
                    (found = expected && pages.Length >= 3)
                    $"RankedMatchBetsCursor over {pages.Length} pages (items {pagesText pages}) returned all {found.Length} participants in total order"
        }

    let private discoveryCheck (client: IAmazonDynamoDB) (fixture: Fixture) =
        async {
            let settlement = PoolSettlementDynamoDbRepository(client) :> IPoolSettlementRepository

            let rec loop next pages =
                async {
                    let! poolIds, cursor = settlement.GetPoolsWithMatchPageAsync(Ulid.newOf fixture.FirstMatch, next)
                    let pages = pages @ [ poolIds |> List.map _.Value ]

                    match cursor with
                    | Some _ when pages.Length < 50 -> return! loop cursor pages
                    | _ -> return pages
                }

            let! pages = loop None []
            let found = pages |> List.concat |> List.distinct |> List.sort

            return
                check
                    "regression control: settlement match-index discovery"
                    (found = List.sort (fixture.PoolId :: fixture.OtherPoolIds) && pages.Length >= 3)
                    $"raw-key discovery cursor over {pages.Length} pages found all {found.Length} pools with the match"
        }

    let private propagationCheck (client: IAmazonDynamoDB) (fixture: Fixture) =
        async {
            let renamed = "renamed-ünï"
            let pools = PoolDynamoDbRepository(DynamoDbKeySerializer(), client) :> IPoolRepository
            do! pools.PropagateGamblerUsernameAsync(Ulid.newOf fixture.Gambler, NonEmptyString100.newOf renamed)

            let keys =
                ((fixture.PoolId :: fixture.OtherPoolIds) |> List.map (fun pool -> $"POOL#{pool}", $"GAMBLER#{fixture.Gambler}"))
                @ (fixture.Matches |> List.map (fun matchId -> $"GAMBLER#{fixture.Gambler}#POOL#{fixture.PoolId}", $"MATCH#{matchId}"))
                @ (fixture.OtherPoolIds |> List.map (fun pool -> $"GAMBLER#{fixture.Gambler}#POOL#{pool}", $"MATCH#{fixture.FirstMatch}"))

            let! usernames =
                keys
                |> List.map (fun (pk, sk) ->
                    async {
                        let! response =
                            client.GetItemAsync(
                                GetItemRequest(
                                    TableName = PoolTable.name,
                                    Key = Dictionary(dict [ "pk", AttributeValue(S = pk); "sk", AttributeValue(S = sk) ]),
                                    ConsistentRead = true
                                )
                            )
                            |> Async.AwaitTask

                        return response.Item["gamblerUsername"].S
                    })
                |> Async.Sequential

            let updated = usernames |> Array.filter ((=) renamed) |> Array.length

            return
                check
                    "regression control: username propagation"
                    (updated = keys.Length)
                    $"raw-key queries over {fixture.OtherPoolIds.Length + 1} memberships and {fixture.Matches.Length} bets in one pool ({pageLimit}-item pages) updated {updated} of {keys.Length} items"
        }

    /// Seeds the fixture, then runs every check. Returns the checks in order.
    let runAsync (client: IAmazonDynamoDB) =
        async {
            let fixture = newFixture ()
            printfn "Fixture pool %s, layout %s, gambler %s" fixture.PoolId fixture.LayoutId fixture.Gambler
            do! Seed.writeAll client PoolTable.name (poolItems fixture) 4
            do! Seed.writeAll client PoolLayoutTable.name (layoutItems fixture) 4

            let! traversals =
                operations client fixture
                |> List.map (traversalChecks client)
                |> Async.Sequential

            let! timeline = timelineCheck client fixture
            let! ranked = rankedCheck client fixture
            let! discovery = discoveryCheck client fixture
            // Last: it rewrites gambler 0's username, which the members index orders by.
            let! propagation = propagationCheck client fixture

            return (traversals |> List.ofArray |> List.concat) @ [ timeline; ranked; discovery; propagation ]
        }
