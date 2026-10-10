namespace Felipearpa.Tyche.Pool.Test.Fakes

/// Key schemas of the deployed tables (see docs/database-model.md), used by the fake client.
module FakeTables =

    let pool =
        { FakeTable.Name = "Pool"
          Hash = "pk"
          Range = Some "sk"
          Indexes =
            [ { FakeIndex.Name = "GetPendingPoolGamblerBets-index"
                Hash = "pk"
                Range = Some "matchDateTime"
                Projection = None }
              { Name = "GetFinishedPoolGamblerBets-index"
                Hash = "pk"
                Range = Some "computedDateTime"
                Projection = None }
              { Name = "GetPoolGamblerScoresByGambler-index"
                Hash = "sk"
                Range = Some "score"
                Projection = None }
              { Name = "GetPoolGamblerScoresByPool-index"
                Hash = "pk"
                Range = Some "score"
                Projection = None }
              { Name = "GetPoolGamblerScoresByMatch-index"
                Hash = "getPoolGamblerScoresByMatchPk"
                Range = Some "getPoolGamblerScoresByMatchSk"
                Projection = None }
              { Name = "GetGamblersByPoolLayout-index"
                Hash = "getGamblersByPoolLayoutPk"
                Range = Some "getGamblersByPoolLayoutSk"
                Projection = None }
              { Name = "GetPoolGamblersByUsername-index"
                Hash = "pk"
                Range = Some "gamblerUsername"
                Projection = None }
              { Name = "GetPoolMatchBetsByTotal-index"
                Hash = "getPoolMatchBetsByTotalPk"
                Range = Some "getPoolMatchBetsByTotalSk"
                Projection =
                  Some
                      [ "poolId"
                        "gamblerId"
                        "gamblerUsername"
                        "matchId"
                        "poolLayoutId"
                        "homeTeamId"
                        "homeTeamName"
                        "awayTeamId"
                        "awayTeamName"
                        "homeTeamBet"
                        "awayTeamBet"
                        "homeTeamScore"
                        "awayTeamScore"
                        "score"
                        "matchDateTime"
                        "round"
                        "groupName"
                        "computedRequestId"
                        "totalPoolPoints"
                        "totalPoolPointsState" ] } ] }

    let poolLayout =
        { FakeTable.Name = "PoolLayout"
          Hash = "pk"
          Range = Some "sk"
          Indexes =
            [ { FakeIndex.Name = "GetOpenedPoolLayout-index"
                Hash = "status"
                Range = Some "startDateTime"
                Projection = None } ] }

    let create () = FakeDynamoDb([ pool; poolLayout ])
