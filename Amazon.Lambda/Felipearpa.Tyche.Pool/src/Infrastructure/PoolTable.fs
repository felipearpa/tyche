namespace Felipearpa.Tyche.Pool.Infrastructure

open System

module PoolTable =

    /// The deployed table is "Pool". POOL_TABLE_NAME points the code at an isolated copy,
    /// such as the temporary tables used by integration and load checks.
    let name =
        Environment.GetEnvironmentVariable "POOL_TABLE_NAME"
        |> Option.ofObj
        |> Option.filter (String.IsNullOrWhiteSpace >> not)
        |> Option.defaultValue "Pool"

    module Prefix =

        [<Literal>]
        let pool = "POOL"

        [<Literal>]
        let gambler = "GAMBLER"

        [<Literal>]
        let match' = "MATCH"

        [<Literal>]
        let poolLayout = "POOLLAYOUT"

        [<Literal>]
        let settlement = "SETTLEMENT"

    module Attribute =

        [<Literal>]
        let poolId = "poolId"

        [<Literal>]
        let poolName = "poolName"

        [<Literal>]
        let gamblerId = "gamblerId"

        [<Literal>]
        let creatorGamblerId = "creatorGamblerId"

        [<Literal>]
        let gamblerUsername = "gamblerUsername"

        [<Literal>]
        let gamblerEmail = "gamblerEmail"

        [<Literal>]
        let status = "status"

        [<Literal>]
        let filter = "filter"

        [<Literal>]
        let score = "score"

        [<Literal>]
        let beforeScore = "beforeScore"

        [<Literal>]
        let position = "position"

        [<Literal>]
        let beforePosition = "beforePosition"

        [<Literal>]
        let gamblerCount = "gamblerCount"

        [<Literal>]
        let poolLayoutId = "poolLayoutId"

        [<Literal>]
        let poolLayoutVersion = "poolLayoutVersion"

        [<Literal>]
        let matchId = "matchId"

        [<Literal>]
        let matchDateTime = "matchDateTime"

        [<Literal>]
        let homeTeamId = "homeTeamId"

        [<Literal>]
        let homeTeamName = "homeTeamName"

        [<Literal>]
        let awayTeamId = "awayTeamId"

        [<Literal>]
        let awayTeamName = "awayTeamName"

        [<Literal>]
        let homeTeamBet = "homeTeamBet"

        [<Literal>]
        let awayTeamBet = "awayTeamBet"

        [<Literal>]
        let homeTeamScore = "homeTeamScore"

        [<Literal>]
        let awayTeamScore = "awayTeamScore"

        [<Literal>]
        let round = "round"

        [<Literal>]
        let groupName = "groupName"

        [<Literal>]
        let computedDateTime = "computedDateTime"

        [<Literal>]
        let computedRequestId = "computedRequestId"

        [<Literal>]
        let getPoolGamblerScoresByMatchPk = "getPoolGamblerScoresByMatchPk"

        [<Literal>]
        let getPoolGamblerScoresByMatchSk = "getPoolGamblerScoresByMatchSk"

        [<Literal>]
        let getGamblersByPoolLayoutPk = "getGamblersByPoolLayoutPk"

        [<Literal>]
        let getGamblersByPoolLayoutSk = "getGamblersByPoolLayoutSk"

        // Pool root: membership and operation ordering.

        [<Literal>]
        let membershipRevision = "membershipRevision"

        [<Literal>]
        let operationRevision = "operationRevision"

        [<Literal>]
        let activeOperation = "activeOperation"

        [<Literal>]
        let lastCompletedOperationRevision = "lastCompletedOperationRevision"

        [<Literal>]
        let rankingReadiness = "rankingReadiness"

        // Membership identity and versioned total.

        [<Literal>]
        let membershipGeneration = "membershipGeneration"

        [<Literal>]
        let scoreVersion = "scoreVersion"

        // Position markers on memberships and match details.

        [<Literal>]
        let positionOperationRevision = "positionOperationRevision"

        [<Literal>]
        let positionCalculationRevision = "positionCalculationRevision"

        // Match detail settlement identity.

        [<Literal>]
        let poolMembershipGeneration = "poolMembershipGeneration"

        [<Literal>]
        let resultVersion = "resultVersion"

        [<Literal>]
        let settlementOperationId = "settlementOperationId"

        [<Literal>]
        let settlementOperationRevision = "settlementOperationRevision"

        // Match detail pool-total projection.

        [<Literal>]
        let totalPoolPoints = "totalPoolPoints"

        [<Literal>]
        let totalPoolPointsState = "totalPoolPointsState"

        [<Literal>]
        let poolScoreVersion = "poolScoreVersion"

        [<Literal>]
        let rankingActive = "rankingActive"

        [<Literal>]
        let getPoolMatchBetsByTotalPk = "getPoolMatchBetsByTotalPk"

        [<Literal>]
        let getPoolMatchBetsByTotalSk = "getPoolMatchBetsByTotalSk"

        // Match detail held for repair by a rejoin restoration.

        [<Literal>]
        let restoreExcludedGeneration = "restoreExcludedGeneration"

        [<Literal>]
        let restoreExclusionReason = "restoreExclusionReason"

        // Settlement receipt.

        [<Literal>]
        let operationId = "operationId"

        [<Literal>]
        let operationState = "operationState"

        [<Literal>]
        let operationFingerprint = "operationFingerprint"

        [<Literal>]
        let operationCommand = "operationCommand"

        [<Literal>]
        let operationTarget = "operationTarget"

        [<Literal>]
        let admittedDateTime = "admittedDateTime"

        [<Literal>]
        let completedDateTime = "completedDateTime"

        [<Literal>]
        let deferredDateTime = "deferredDateTime"

        [<Literal>]
        let unresolvedDetails = "unresolvedDetails"

    module Index =

        [<Literal>]
        let pendingPoolGamblerBets = "GetPendingPoolGamblerBets-index"

        [<Literal>]
        let finishedPoolGamblerBets = "GetFinishedPoolGamblerBets-index"

        [<Literal>]
        let scoresByGambler = "GetPoolGamblerScoresByGambler-index"

        [<Literal>]
        let scoresByPool = "GetPoolGamblerScoresByPool-index"

        [<Literal>]
        let scoresByMatch = "GetPoolGamblerScoresByMatch-index"

        [<Literal>]
        let gamblersByPoolLayout = "GetGamblersByPoolLayout-index"

        [<Literal>]
        let gamblersByUsername = "GetPoolGamblersByUsername-index"

        [<Literal>]
        let poolMatchBetsByTotal = "GetPoolMatchBetsByTotal-index"
