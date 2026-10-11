namespace Felipearpa.Tyche.Pool.Test.Infrastructure

#nowarn "3536"

open System
open System.Collections.Generic
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Tyche.Pool.Test.Fakes
open Felipearpa.Tyche.Pool.Test.Infrastructure.PaginationSupport
open Felipearpa.Tyche.PoolLayout.Infrastructure
open Felipearpa.Type
open FsUnitTyped
open Xunit

/// Traverses every shared-codec query through the fake table with two-item storage pages and
/// asserts the exact start key each next-page request carries.
module SharedCodecPaginationTest =

    [<Literal>]
    let private pageSize = 2

    let private id (prefix: char) (number: int) = $"01K1PX1TX2NM1HG851S1V0{prefix}{number:D3}"
    let private short (value: string) = value.Substring 22
    let private pool = id 'P' 1
    let private otherPool = id 'P' 2
    let private gambler = id 'G' 1
    let private layout = id 'Y' 1
    let private otherLayout = id 'Y' 2
    let private matchOne = id 'M' 1
    let private kickoff = DateTime(2026, 6, 12, 17, 0, 0, DateTimeKind.Utc)
    let private iso (value: DateTime) = value.ToString("o")
    let private future = DateTime(2099, 1, 1, 0, 0, 0, DateTimeKind.Utc)

    type SeedItem =
        { Table: string
          /// The short ID the fetch reports for this item; empty for items no query returns.
          Id: string
          Item: IDictionary<string, AttributeValue> }

    type PagedQuery =
        { Name: string
          Seed: SeedItem list
          /// The table and index key attributes of the query's start key.
          KeyNames: string list
          Fetch: IKeySerializer -> IAmazonDynamoDB -> string option -> Async<CursorPage<string>>
          /// The IDs each page returns, in order.
          Pages: string list list
          /// The ID of the last item each nonfinal storage page evaluated.
          Boundaries: string list }

    let private item (attributes: (string * AttributeValue) list) =
        Dictionary(dict attributes) :> IDictionary<_, _>

    let private bet (poolId: string) (gamblerId: string) (matchId: string) (matchDateTime: DateTime) extra =
        item (
            [ "pk", s $"GAMBLER#{gamblerId}#POOL#{poolId}"
              "sk", s $"MATCH#{matchId}"
              "poolId", s poolId
              "gamblerId", s gamblerId
              "gamblerUsername", s "ana"
              "gamblerEmail", s "ana@example.com"
              "matchId", s matchId
              "poolLayoutId", s layout
              "homeTeamId", s "COL"
              "homeTeamName", s "Colombia"
              "awayTeamId", s "ARG"
              "awayTeamName", s "Argentina"
              "matchDateTime", s (iso matchDateTime)
              "getPoolGamblerScoresByMatchPk", s $"MATCH#{matchId}"
              "getPoolGamblerScoresByMatchSk", s $"POOL#{poolId}#GAMBLER#{gamblerId}" ]
            @ extra
        )

    let private computedAt (computedDateTime: DateTime) =
        [ "computedRequestId", s "01K1PX1TX2NM1HG851S1V0R000"
          "computedDateTime", s (iso computedDateTime)
          "score", n 3 ]

    let private membership (poolId: string) (gamblerId: string) (username: string) (score: int) (status: string) =
        item
            [ "pk", s $"POOL#{poolId}"
              "sk", s $"GAMBLER#{gamblerId}"
              "poolId", s poolId
              "poolName", s "Polla"
              "gamblerId", s gamblerId
              "gamblerUsername", s username
              "gamblerEmail", s $"{short gamblerId}@example.com"
              "status", s status
              "score", n score
              "poolLayoutId", s layout
              "poolLayoutVersion", n 1
              "getGamblersByPoolLayoutPk", s $"POOLLAYOUT#{layout}"
              "getGamblersByPoolLayoutSk", s $"POOL#{poolId}#GAMBLER#{gamblerId}" ]

    let private layoutRoot (layoutId: string) (status: string) (startDateTime: DateTime) =
        item
            [ "pk", s $"POOLLAYOUT#{layoutId}"
              "sk", s $"POOLLAYOUT#{layoutId}"
              "poolLayoutId", s layoutId
              "poolLayoutName", s "Mundial"
              "status", s status
              "startDateTime", s (iso startDateTime) ]

    let private layoutMatch (layoutId: string) (matchId: string) (version: int) =
        item
            [ "pk", s $"POOLLAYOUT#{layoutId}"
              "sk", s $"MATCH#{matchId}"
              "matchId", s matchId
              "poolLayoutId", s layoutId
              "homeTeamId", s "COL"
              "homeTeamName", s "Colombia"
              "awayTeamId", s "ARG"
              "awayTeamName", s "Argentina"
              "matchDateTime", s (iso kickoff)
              "poolLayoutVersion", n version ]

    let private seed table itemId value = { Table = table; Id = itemId; Item = value }
    let private poolItem = seed "Pool"

    let private bets serializer client =
        PoolGamblerBetDynamoDbRepository(serializer, client) :> IPoolGamblerBetRepository

    let private scores serializer client =
        PoolGamblerScoreDynamoDbRepository(serializer, client) :> IPoolGamblerScoreRepository

    let private pools serializer client =
        PoolDynamoDbRepository(serializer, client) :> IPoolRepository

    let private layouts serializer client =
        PoolLayoutDynamoDbRepository(serializer, client) :> Felipearpa.Tyche.PoolLayout.Domain.IPoolLayoutRepository

    let private ids (toId: 'T -> string) (work: Async<CursorPage<'T>>) =
        async {
            let! page = work
            return page |> CursorPage.map toId
        }

    let private matchIdOf (bet: PoolGamblerBet) = short bet.MatchId.Value

    /// Eight bets of one gambler: a tie on M002/M003, and M005/M006 computed so the pending
    /// and live filters each empty one storage page. Another gambler's bet shares the pool.
    let private gamblerBets =
        let at hours = kickoff.AddHours hours

        [ poolItem "M001" (bet pool gambler (id 'M' 1) (at 0.0) [])
          poolItem "M002" (bet pool gambler (id 'M' 2) (at 1.0) [])
          poolItem "M003" (bet pool gambler (id 'M' 3) (at 1.0) [])
          poolItem "M004" (bet pool gambler (id 'M' 4) (at 2.0) [])
          poolItem "M005" (bet pool gambler (id 'M' 5) (at 3.0) (computedAt (at 5.0)))
          poolItem "M006" (bet pool gambler (id 'M' 6) (at 4.0) (computedAt (at 6.0)))
          poolItem "M007" (bet pool gambler (id 'M' 7) (at 5.0) [])
          poolItem "M008" (bet pool gambler (id 'M' 8) (at 6.0) [])
          poolItem "" (bet pool (id 'G' 2) (id 'M' 1) (at 0.0) [])
          poolItem "" (bet otherPool gambler (id 'M' 1) (at 0.0) []) ]

    /// Seven computed bets, newest first: a tie on K004/K003 across a boundary, K005/K004
    /// outside the search text, and one uncomputed bet the index does not hold.
    let private finishedBets =
        let computed number hours filter =
            bet pool gambler (id 'K' number) kickoff (computedAt (kickoff.AddHours hours) @ [ "filter", s filter ])

        [ poolItem "K001" (computed 1 1.0 "colombia argentina")
          poolItem "K002" (computed 2 2.0 "colombia argentina")
          poolItem "K003" (computed 3 3.0 "colombia argentina")
          poolItem "K004" (computed 4 3.0 "brasil peru")
          poolItem "K005" (computed 5 5.0 "brasil peru")
          poolItem "K006" (computed 6 6.0 "colombia argentina")
          poolItem "K007" (computed 7 7.0 "colombia argentina")
          poolItem "" (bet pool gambler (id 'K' 8) kickoff []) ]

    /// Bets on one match from seven members of the pool; G003/G004 are not locked yet, so the
    /// lock filter empties one storage page. Another pool's bet on the match is out of scope.
    let private matchBets =
        [ for number in 1..7 do
              let matchDateTime = if number = 3 || number = 4 then future else kickoff
              yield poolItem (short (id 'G' number)) (bet pool (id 'G' number) matchOne matchDateTime [])
          yield poolItem "" (bet otherPool (id 'G' 1) matchOne kickoff []) ]

    /// Scores 12, 5, 5, 5, 0, 0, -3 with the middle tie partly closed, so the status filter
    /// empties one storage page and a page ends on score 0.
    let private rankedScores (membershipOf: int -> int -> string -> IDictionary<string, AttributeValue>) =
        [ 1, 12, "OPENED"; 2, 5, "CLOSED"; 3, 5, "CLOSED"; 4, 5, "OPENED"; 5, 0, "OPENED"; 6, 0, "OPENED"; 7, -3, "OPENED" ]
        |> List.map (fun (number, score, status) -> number, membershipOf number score status)

    let private gamblerScores =
        rankedScores (fun number score status -> membership (id 'P' number) gambler "ana" score status)
        |> List.map (fun (number, value) -> poolItem (short (id 'P' number)) value)
        |> List.append [ poolItem "" (membership pool (id 'G' 2) "bea" 99 "OPENED") ]

    let private leaderboard =
        rankedScores (fun number score status -> membership pool (id 'G' number) $"user{number}" score status)
        |> List.map (fun (number, value) -> poolItem (short (id 'G' number)) value)
        |> List.append [ poolItem "" (membership otherPool gambler "ana" 99 "OPENED") ]

    /// Usernames in ordinal order with a "bea" tie across a boundary and a non-ASCII name.
    let private members =
        [ 1, "ana"; 2, "bea"; 3, "bea"; 4, "carlos"; 5, "dora"; 6, "eva"; 7, "zoë" ]
        |> List.map (fun (number, username) ->
            poolItem (short (id 'G' number)) (membership pool (id 'G' number) username number "OPENED"))
        |> List.append [ poolItem "" (membership otherPool (id 'G' 9) "aaron" 0 "OPENED") ]

    let private layoutGamblers =
        let layoutGambler poolId gamblerId = membership poolId gamblerId "ana" 0 "OPENED"

        [ for number in 1..4 do
              yield poolItem $"P001/G00{number}" (layoutGambler pool (id 'G' number))
          for number in 1..3 do
              yield poolItem $"P002/G00{number}" (layoutGambler otherPool (id 'G' number))
          yield
              poolItem
                  ""
                  (item
                      [ "pk", s $"POOL#{id 'P' 3}"
                        "sk", s $"GAMBLER#{gambler}"
                        "getGamblersByPoolLayoutPk", s $"POOLLAYOUT#{otherLayout}"
                        "getGamblersByPoolLayoutSk", s $"POOL#{id 'P' 3}#GAMBLER#{gambler}" ]) ]

    let private openLayouts =
        let layoutItem = seed "PoolLayout"
        let starting days = kickoff.AddDays days

        [ layoutItem "Y001" (layoutRoot (id 'Y' 1) "OPENED" (starting 1.0))
          layoutItem "Y002" (layoutRoot (id 'Y' 2) "OPENED" (starting 2.0))
          layoutItem "Y003" (layoutRoot (id 'Y' 3) "OPENED" (starting 2.0))
          layoutItem "Y004" (layoutRoot (id 'Y' 4) "OPENED" (starting 3.0))
          layoutItem "Y005" (layoutRoot (id 'Y' 5) "OPENED" (starting 4.0))
          layoutItem "Y006" (layoutRoot (id 'Y' 6) "OPENED" (starting 5.0))
          layoutItem "Y007" (layoutRoot (id 'Y' 7) "OPENED" (starting 6.0))
          layoutItem "" (layoutRoot (id 'Y' 8) "CLOSED" (starting 0.0)) ]

    /// Eight matches; M003/M004 belong to layout version 2, so a version 1 read filters out a
    /// whole storage page. The layout root shares the partition but is not a match.
    let private layoutMatches =
        let layoutItem = seed "PoolLayout"

        [ for number in 1..8 do
              let version = if number = 3 || number = 4 then 2 else 1
              yield layoutItem (short (id 'M' number)) (layoutMatch layout (id 'M' number) version)
          yield layoutItem "" (layoutRoot layout "OPENED" kickoff) ]

    let private poolId = Ulid.newOf pool
    let private gamblerId = Ulid.newOf gambler

    let queries: PagedQuery list =
        [ { Name = "pending bets"
            Seed = gamblerBets
            KeyNames = [ "pk"; "sk"; "matchDateTime" ]
            Fetch =
              fun serializer client next ->
                  (bets serializer client).GetPendingPoolGamblerBetsAsync(poolId, gamblerId, None, next) |> ids matchIdOf
            Pages = [ [ "M001"; "M002" ]; [ "M003"; "M004" ]; []; [ "M007"; "M008" ] ]
            Boundaries = [ "M002"; "M004"; "M006" ] }
          { Name = "live bets"
            Seed = gamblerBets
            KeyNames = [ "pk"; "sk"; "matchDateTime" ]
            Fetch =
              fun serializer client next ->
                  (bets serializer client).GetLivePoolGamblerBetsAsync(poolId, gamblerId, None, next) |> ids matchIdOf
            Pages = [ [ "M008"; "M007" ]; []; [ "M004"; "M003" ]; [ "M002"; "M001" ] ]
            Boundaries = [ "M007"; "M005"; "M003" ] }
          { Name = "finished bets"
            Seed = finishedBets
            KeyNames = [ "pk"; "sk"; "computedDateTime" ]
            Fetch =
              fun serializer client next ->
                  (bets serializer client).GetFinishedPoolGamblerBetsAsync(poolId, gamblerId, Some "Colombia", next)
                  |> ids matchIdOf
            Pages = [ [ "K007"; "K006" ]; []; [ "K003"; "K002" ]; [ "K001" ] ]
            Boundaries = [ "K006"; "K004"; "K002" ] }
          { Name = "legacy match bets"
            Seed = matchBets
            KeyNames = [ "pk"; "sk"; "getPoolGamblerScoresByMatchPk"; "getPoolGamblerScoresByMatchSk" ]
            Fetch =
              fun serializer client next ->
                  (bets serializer client).GetPoolMatchGamblerBetsAsync(poolId, Ulid.newOf matchOne, next)
                  |> ids (fun bet -> short bet.GamblerId.Value)
            Pages = [ [ "G001"; "G002" ]; []; [ "G005"; "G006" ]; [ "G007" ] ]
            Boundaries = [ "G002"; "G004"; "G006" ] }
          { Name = "gambler pool scores"
            Seed = gamblerScores
            KeyNames = [ "pk"; "sk"; "score" ]
            Fetch =
              fun serializer client next ->
                  (scores serializer client).GetGamblerScoresAsync(gamblerId, next)
                  |> ids (fun score -> short score.PoolId.Value)
            Pages = [ [ "P001"; "P004" ]; []; [ "P006"; "P005" ]; [ "P007" ] ]
            Boundaries = [ "P004"; "P002"; "P005" ] }
          { Name = "pool leaderboard"
            Seed = leaderboard
            KeyNames = [ "pk"; "sk"; "score" ]
            Fetch =
              fun serializer client next ->
                  (scores serializer client).GetPoolScoresAsync(poolId, next)
                  |> ids (fun score -> short score.GamblerId.Value)
            Pages = [ [ "G001"; "G004" ]; []; [ "G006"; "G005" ]; [ "G007" ] ]
            Boundaries = [ "G004"; "G002"; "G005" ] }
          { Name = "pool members"
            Seed = members
            KeyNames = [ "pk"; "sk"; "gamblerUsername" ]
            Fetch =
              fun serializer client next ->
                  (pools serializer client).GetPoolMembersAsync(poolId, next)
                  |> ids (fun poolMember -> short poolMember.GamblerId.Value)
            Pages = [ [ "G001"; "G002" ]; [ "G003"; "G004" ]; [ "G005"; "G006" ]; [ "G007" ] ]
            Boundaries = [ "G002"; "G004"; "G006" ] }
          { Name = "gamblers by layout"
            Seed = layoutGamblers
            KeyNames = [ "pk"; "sk"; "getGamblersByPoolLayoutPk"; "getGamblersByPoolLayoutSk" ]
            Fetch =
              fun serializer client next ->
                  (pools serializer client).GetGamblersByPoolLayoutAsync(Ulid.newOf layout, next)
                  |> ids (fun layoutGambler -> $"{short layoutGambler.PoolId.Value}/{short layoutGambler.GamblerId.Value}")
            Pages = [ [ "P001/G001"; "P001/G002" ]; [ "P001/G003"; "P001/G004" ]; [ "P002/G001"; "P002/G002" ]; [ "P002/G003" ] ]
            Boundaries = [ "P001/G002"; "P001/G004"; "P002/G002" ] }
          { Name = "open layouts"
            Seed = openLayouts
            KeyNames = [ "pk"; "sk"; "status"; "startDateTime" ]
            Fetch =
              fun serializer client next ->
                  (layouts serializer client).GetOpenPoolLayoutsAsync(next) |> ids (fun layout -> short layout.Id.Value)
            Pages = [ [ "Y001"; "Y002" ]; [ "Y003"; "Y004" ]; [ "Y005"; "Y006" ]; [ "Y007" ] ]
            Boundaries = [ "Y002"; "Y004"; "Y006" ] }
          { Name = "layout matches"
            Seed = layoutMatches
            KeyNames = [ "pk"; "sk" ]
            Fetch =
              fun serializer client next ->
                  (layouts serializer client).GetPoolLayoutMatchesThroughVersionAsync(Ulid.newOf layout, 1, next)
                  |> ids (fun layoutMatch -> short layoutMatch.MatchId.Value)
            Pages = [ [ "M001"; "M002" ]; []; [ "M005"; "M006" ]; [ "M007"; "M008" ] ]
            Boundaries = [ "M002"; "M004"; "M006" ] } ]

    let queryNames: obj array seq = queries |> Seq.map (fun query -> [| box query.Name |])

    /// The nine index queries; the base-table layout-match query is complete with pk/sk alone.
    let indexQueryNames: obj array seq =
        queries
        |> Seq.filter (fun query -> query.Name <> "layout matches")
        |> Seq.map (fun query -> [| box query.Name |])

    let private named (name: string) =
        queries |> List.find (fun query -> query.Name = name)

    let private seeded (query: PagedQuery) =
        let fake = FakeTables.create ()
        query.Seed |> List.iter (fun seedItem -> fake.Put(seedItem.Table, seedItem.Item))
        fake.LimitQueryPages pageSize
        fake

    /// The start key DynamoDB returns after evaluating the boundary item.
    let private expectedStartKey (query: PagedQuery) (boundary: string) =
        let boundaryItem = (query.Seed |> List.find (fun seedItem -> seedItem.Id = boundary)).Item

        query.KeyNames
        |> List.map (fun name -> name, boundaryItem[name])
        |> dict
        |> describeKey

    [<Theory; MemberData(nameof queryNames)>]
    let ``given forced storage pages when traversed then every next request starts at the exact previous boundary`` (name: string) =
        async {
            let query = named name
            let fake = seeded query

            let! pages = traverse (query.Fetch typed fake.Client)

            pages |> List.map fst |> shouldEqual query.Pages
            pages |> List.map snd |> List.last |> shouldEqual None

            takeStartKeys fake
            |> shouldEqual (None :: (query.Boundaries |> List.map (expectedStartKey query >> Some)))
        }

    [<Theory; MemberData(nameof queryNames)>]
    let ``given a final empty key map when traversed then the last page has no next token`` (name: string) =
        async {
            let query = named name
            let fake = seeded query
            fake.EndQueriesWithEmptyKey()

            let! pages = traverse (query.Fetch typed fake.Client)

            pages |> List.map fst |> shouldEqual query.Pages
            pages |> List.map snd |> List.last |> shouldEqual None
        }

    [<Theory; MemberData(nameof queryNames)>]
    let ``given a next token when the request fails and is retried then it resumes the same boundary`` (name: string) =
        async {
            let query = named name
            let fake = seeded query
            let! first = query.Fetch typed fake.Client None
            fake.TakeQueries() |> ignore

            fake.Intercept(fun _ -> FailBefore(AmazonDynamoDBException "Throughput exceeded"))
            let! _ = failureOf (query.Fetch typed fake.Client first.Next)
            fake.ClearInterception()
            let! retried = query.Fetch typed fake.Client first.Next
            let! repeated = query.Fetch typed fake.Client first.Next

            let expected = Some(expectedStartKey query query.Boundaries.Head)
            takeStartKeys fake |> shouldEqual [ expected; expected; expected ]
            List.ofSeq retried.Items |> shouldEqual query.Pages[1]
            List.ofSeq repeated.Items |> shouldEqual query.Pages[1]
            repeated.Next |> shouldEqual retried.Next
        }

    [<Theory; MemberData(nameof indexQueryNames)>]
    let ``given a legacy token without the index keys when an index page is requested then it is rejected before querying``
        (name: string)
        =
        async {
            let query = named name
            let fake = seeded query
            let! first = query.Fetch legacyTokens fake.Client None
            fake.TakeQueries() |> ignore

            let! reason = invalidContinuationOf (query.Fetch legacyTokens fake.Client first.Next)

            let missing = query.KeyNames |> List.except [ "pk"; "sk" ] |> List.min
            reason |> shouldEqual (MissingKeyAttribute missing)
            fake.TakeQueries() |> shouldEqual []
        }

    [<Fact>]
    let ``given legacy pk and sk tokens when layout matches are traversed then every page is reached`` () =
        async {
            let query = named "layout matches"
            let fake = seeded query

            let! pages = traverse (query.Fetch legacyTokens fake.Client)

            pages |> List.map fst |> shouldEqual query.Pages

            takeStartKeys fake
            |> shouldEqual (None :: (query.Boundaries |> List.map (expectedStartKey query >> Some)))
        }

    let private rejectionOf (name: string) (token: string) =
        async {
            let query = named name
            let fake = seeded query
            let! reason = invalidContinuationOf (query.Fetch typed fake.Client (Some token))
            fake.TakeQueries() |> shouldEqual []
            return reason
        }

    let private betKey (poolId: string) (gamblerId: string) (dateName: string) =
        [ "pk", s $"GAMBLER#{gamblerId}#POOL#{poolId}"
          "sk", s $"MATCH#{id 'M' 2}"
          dateName, s (iso kickoff) ]

    let private scoreKey (poolId: string) (gamblerId: string) (score: AttributeValue option) =
        [ "pk", s $"POOL#{poolId}"; "sk", s $"GAMBLER#{gamblerId}" ]
        @ (score |> Option.map (fun value -> "score", value) |> Option.toList)

    let invalidTokens: obj array seq =
        [ "pending bets", betKey pool (id 'G' 2) "matchDateTime", KeyOutOfScope("pk", "the value belongs to another partition or key")
          "live bets", betKey otherPool gambler "matchDateTime", KeyOutOfScope("pk", "the value belongs to another partition or key")
          "live bets", betKey pool gambler "computedDateTime", MissingKeyAttribute "matchDateTime"
          "finished bets", betKey pool gambler "matchDateTime", MissingKeyAttribute "computedDateTime"
          "legacy match bets",
          [ "pk", s $"GAMBLER#{gambler}#POOL#{otherPool}"
            "sk", s $"MATCH#{matchOne}"
            "getPoolGamblerScoresByMatchPk", s $"MATCH#{matchOne}"
            "getPoolGamblerScoresByMatchSk", s $"POOL#{otherPool}#GAMBLER#{gambler}" ],
          KeyOutOfScope("getPoolGamblerScoresByMatchSk", $"the value does not start with 'POOL#{pool}#'")
          "legacy match bets",
          [ "pk", s $"GAMBLER#{gambler}#POOL#{pool}"
            "sk", s $"MATCH#{id 'M' 9}"
            "getPoolGamblerScoresByMatchPk", s $"MATCH#{id 'M' 9}"
            "getPoolGamblerScoresByMatchSk", s $"POOL#{pool}#GAMBLER#{gambler}" ],
          KeyOutOfScope("getPoolGamblerScoresByMatchPk", "the value belongs to another partition or key")
          "gambler pool scores", scoreKey pool gambler (Some(s "5")), WrongKeyAttributeType("score", "N", "S")
          "gambler pool scores", scoreKey pool gambler None, MissingKeyAttribute "score"
          "gambler pool scores",
          scoreKey pool (id 'G' 2) (Some(n 5)),
          KeyOutOfScope("sk", "the value belongs to another partition or key")
          "pool leaderboard", scoreKey pool gambler (Some(s "120")), WrongKeyAttributeType("score", "N", "S")
          "pool leaderboard", scoreKey pool gambler None, MissingKeyAttribute "score"
          "pool leaderboard",
          scoreKey otherPool gambler (Some(n 0)),
          KeyOutOfScope("pk", "the value belongs to another partition or key")
          "pool leaderboard",
          scoreKey pool gambler (Some(n 5)) @ [ "gamblerUsername", s "ana" ],
          UnexpectedKeyAttribute "gamblerUsername"
          "pool members",
          [ "pk", s $"POOL#{otherPool}"; "sk", s $"GAMBLER#{gambler}"; "gamblerUsername", s "ana" ],
          KeyOutOfScope("pk", "the value belongs to another partition or key")
          "pool members",
          [ "pk", s $"POOL#{pool}"; "sk", s $"GAMBLER#{gambler}"; "gamblerUsername", n 7 ],
          WrongKeyAttributeType("gamblerUsername", "S", "N")
          "gamblers by layout",
          [ "pk", s $"POOL#{pool}"
            "sk", s $"GAMBLER#{gambler}"
            "getGamblersByPoolLayoutPk", s $"POOLLAYOUT#{otherLayout}"
            "getGamblersByPoolLayoutSk", s $"POOL#{pool}#GAMBLER#{gambler}" ],
          KeyOutOfScope("getGamblersByPoolLayoutPk", "the value belongs to another partition or key")
          "open layouts",
          [ "pk", s $"POOLLAYOUT#{layout}"
            "sk", s $"POOLLAYOUT#{layout}"
            "status", s "CLOSED"
            "startDateTime", s (iso kickoff) ],
          KeyOutOfScope("status", "the value belongs to another partition or key")
          "layout matches",
          [ "pk", s $"POOLLAYOUT#{otherLayout}"; "sk", s $"MATCH#{matchOne}" ],
          KeyOutOfScope("pk", "the value belongs to another partition or key")
          "layout matches",
          [ "pk", s $"POOLLAYOUT#{layout}"; "sk", s $"POOLLAYOUT#{layout}" ],
          KeyOutOfScope("sk", "the value does not start with 'MATCH#'") ]
        |> Seq.map (fun (name, attributes, reason) -> [| box name; box (tokenOf attributes); box reason |])

    [<Theory; MemberData(nameof invalidTokens)>]
    let ``given a token that does not fit the query when a page is requested then it is rejected before querying``
        (name: string)
        (token: string)
        (expected: InvalidContinuation)
        =
        async {
            let! reason = rejectionOf name token
            reason |> shouldEqual expected
        }

    [<Fact>]
    let ``given a leaderboard page ending on a score when its token is decoded then the score stays a number`` () =
        async {
            let query = named "pool leaderboard"
            let fake = seeded query
            let! first = query.Fetch typed fake.Client None
            let! second = query.Fetch typed fake.Client first.Next
            let! third = query.Fetch typed fake.Client second.Next

            let decoded token =
                typed.Deserialize(Option.get token) |> describeKey |> Map.find "score"

            [ decoded first.Next; decoded second.Next; decoded third.Next ]
            |> shouldEqual [ "N", "5"; "N", "5"; "N", "0" ]
        }
