package com.felipearpa.tyche.pool.joiner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.ui.exception.ExceptionView
import com.felipearpa.tyche.ui.exception.LocalizedException
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.exception.localizedOrDefault
import com.felipearpa.tyche.ui.loading.LoadingContainerView
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.ui.state.LoadableViewState
import com.felipearpa.ui.state.isSuccess
import com.felipearpa.tyche.ui.R as SharedR

@Composable
fun PoolJoinerView(
    viewModel: PoolJoinerViewModel,
    poolId: String,
    gamblerId: String,
    onJoinPool: () -> Unit,
) {
    val poolState by viewModel.poolState.collectAsState()
    val joinPoolState by viewModel.joinPoolState.collectAsState()

    PoolJoinerContainer(
        joinPoolState = joinPoolState,
        poolState = poolState,
        onJoinPool = { viewModel.joinPool(poolId = poolId, gamblerId = gamblerId) },
        modifier = Modifier
            .fillMaxSize()
            .padding(all = LocalBoxSpacing.current.medium),
    )

    LaunchedEffect(Unit) {
        viewModel.loadPool(poolId = poolId)
    }

    LaunchedEffect(joinPoolState) {
        if (joinPoolState.isSuccess()) {
            onJoinPool()
        }
    }
}

@Composable
private fun PoolJoinerContainer(
    joinPoolState: LoadableViewState<Unit>,
    poolState: LoadableViewState<PoolModel>,
    onJoinPool: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (joinPoolState) {
        LoadableViewState.Initial ->
            PoolJoinerContent(
                poolState = poolState,
                onJoinPool = onJoinPool,
                modifier = modifier,
            )

        LoadableViewState.Loading, is LoadableViewState.Success -> LoadingContainerView {
            PoolJoinerContent(
                poolState = poolState,
                onJoinPool = {},
                modifier = modifier,
            )
        }

        is LoadableViewState.Failure -> PoolJoinerContainerFailure(
            exception = joinPoolState().localizedOrDefault(),
            onRetry = onJoinPool,
            modifier = modifier,
        )
    }
}

@Composable
private fun PoolJoinerContent(
    poolState: LoadableViewState<PoolModel>,
    onJoinPool: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (poolState) {
        LoadableViewState.Initial, LoadableViewState.Loading -> LoadingContainerView {}
        is LoadableViewState.Success ->
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.large),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.emoji_people),
                        contentDescription = null,
                        modifier = Modifier.size(width = 64.dp, height = 64.dp),
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.ready_to_join_title,
                                poolState().name,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Text(
                            text = stringResource(
                                id = R.string.ready_to_join_subtitle,
                                poolState().name,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Button(
                        onClick = onJoinPool,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(id = R.string.join_pool_action))
                    }
                }
            }

        is LoadableViewState.Failure -> Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            ExceptionView(localizedException = poolState().localizedOrDefault())
        }
    }
}

@Composable
private fun PoolJoinerContainerFailure(
    exception: LocalizedException,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.large),
        ) {
            ExceptionView(localizedException = exception)

            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = SharedR.string.retry_action))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PoolJoinerContentPreview() {
    PoolJoinerContent(
        poolState = LoadableViewState.Success(
            PoolModel(
                id = "id",
                name = "American Cup 2024",
            ),
        ),
        onJoinPool = {},
        modifier = Modifier
            .fillMaxSize()
            .padding(all = LocalBoxSpacing.current.medium),
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolJoinerContainerLoadingPreview() {
    PoolJoinerContainer(
        poolState = LoadableViewState.Success(
            PoolModel(
                id = "id",
                name = "American Cup 2024",
            ),
        ),
        joinPoolState = LoadableViewState.Loading,
        onJoinPool = {},
        modifier = Modifier
            .fillMaxSize()
            .padding(all = LocalBoxSpacing.current.medium),
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolJoinerContainerFailurePreview() {
    PoolJoinerContainerFailure(
        exception = UnknownLocalizedException(),
        onRetry = {},
        modifier = Modifier
            .fillMaxSize()
            .padding(all = LocalBoxSpacing.current.medium),
    )
}
