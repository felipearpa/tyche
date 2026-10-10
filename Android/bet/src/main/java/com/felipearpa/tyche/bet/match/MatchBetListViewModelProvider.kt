package com.felipearpa.tyche.bet.match

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The match screen's view model for one signed-in account, pool, and match. [gamblerId] is the
 * gambler whose entry opened the screen; [viewerAccountId] is the signed-in account.
 */
@Composable
fun matchBetListViewModel(
    poolId: String,
    gamblerId: String,
    matchId: String,
    viewerAccountId: String,
): MatchBetListViewModel = koinViewModel(
    // Another signed-in account never reuses this account's heading, own bet, or rows.
    key = "MatchBetListViewModel:$viewerAccountId:$poolId:$matchId:$gamblerId",
    parameters = { parametersOf(poolId, gamblerId, matchId, viewerAccountId) },
)
