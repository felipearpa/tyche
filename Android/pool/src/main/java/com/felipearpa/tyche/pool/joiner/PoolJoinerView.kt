package com.felipearpa.tyche.pool.joiner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onAbort: () -> Unit,
) {
    val poolState by viewModel.poolState.collectAsState()
    val joinPoolState by viewModel.joinPoolState.collectAsState()

    PoolJoinerContainer(
        joinPoolState = joinPoolState,
        poolState = poolState,
        onJoinPool = { viewModel.joinPool(poolId = poolId, gamblerId = gamblerId) },
        onAbort = onAbort,
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
    onAbort: () -> Unit,
) {
    when (joinPoolState) {
        LoadableViewState.Initial ->
            PoolLoadContainer(
                poolState = poolState,
                onJoinPool = onJoinPool,
                onAbort = onAbort,
            )

        LoadableViewState.Loading, is LoadableViewState.Success -> LoadingContainerView {
            PoolLoadContainer(
                poolState = poolState,
                onJoinPool = {},
                onAbort = {},
            )
        }

        is LoadableViewState.Failure -> JoinFailureContent(
            localizedException = joinPoolState().localizedOrDefault(),
            onRetry = onJoinPool,
            onAbort = onAbort,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = LocalBoxSpacing.current.medium),
        )
    }
}

@Composable
private fun PoolLoadContainer(
    poolState: LoadableViewState<PoolModel>,
    onJoinPool: () -> Unit,
    onAbort: () -> Unit,
) {
    val viewStyle = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = LocalBoxSpacing.current.medium)

    when (poolState) {
        LoadableViewState.Initial, LoadableViewState.Loading -> LoadingContainerView {}
        is LoadableViewState.Success ->
            SuccessContent(
                pool = poolState(),
                onJoinPool = onJoinPool,
                onAbort = onAbort,
                modifier = viewStyle,
            )

        is LoadableViewState.Failure -> LoadFailureContent(
            localizedException = poolState().localizedOrDefault(),
            onAbort = onAbort,
            modifier = viewStyle,
        )
    }
}

@Composable
private fun SuccessContent(
    pool: PoolModel,
    onJoinPool: () -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.emoji_people),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )

            Spacer(modifier = Modifier.height(LocalBoxSpacing.current.large))

            Text(
                text = stringResource(id = R.string.ready_to_join_title),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(LocalBoxSpacing.current.medium))

            Text(
                text = stringResource(id = R.string.ready_to_join_subtitle),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(LocalBoxSpacing.current.large))

            PoolPill(pool = pool)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = LocalBoxSpacing.current.medium),
            verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
        ) {
            Button(
                onClick = onJoinPool,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.join_pool_action))
            }

            OutlinedButton(
                onClick = onAbort,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.go_to_my_pools_action))
            }
        }
    }
}

@Composable
private fun PoolPill(pool: PoolModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(LocalBoxSpacing.current.large))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(
                horizontal = LocalBoxSpacing.current.medium,
                vertical = LocalBoxSpacing.current.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
    ) {
        Icon(
            painter = painterResource(id = SharedR.drawable.trophy),
            contentDescription = null,
            modifier = Modifier.size(pillIconSize),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = pool.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun JoinFailureContent(
    localizedException: LocalizedException,
    onRetry: () -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            ExceptionView(localizedException = localizedException)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = LocalBoxSpacing.current.medium),
            verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
        ) {
            if (localizedException !is JoinPoolLocalizedException) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(id = SharedR.string.retry_action))
                }
            }

            OutlinedButton(
                onClick = onAbort,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.go_to_my_pools_action))
            }
        }
    }
}

@Composable
private fun LoadFailureContent(
    localizedException: LocalizedException,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            ExceptionView(localizedException = localizedException)
        }

        Button(
            onClick = onAbort,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = LocalBoxSpacing.current.medium),
        ) {
            Text(text = stringResource(id = R.string.go_to_my_pools_action))
        }
    }
}

private val iconSize = 64.dp
private val pillIconSize = 16.dp

@Preview(showBackground = true)
@Composable
private fun PoolJoinerInitialPreview() {
    PoolJoinerContainer(
        poolState = LoadableViewState.Success(
            PoolModel(id = "id", name = "American Cup 2024"),
        ),
        joinPoolState = LoadableViewState.Initial,
        onJoinPool = {},
        onAbort = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolJoinerLoadingPreview() {
    PoolJoinerContainer(
        poolState = LoadableViewState.Success(
            PoolModel(id = "id", name = "American Cup 2024"),
        ),
        joinPoolState = LoadableViewState.Loading,
        onJoinPool = {},
        onAbort = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolJoinerJoinFailurePreview() {
    PoolJoinerContainer(
        poolState = LoadableViewState.Success(
            PoolModel(id = "id", name = "American Cup 2024"),
        ),
        joinPoolState = LoadableViewState.Failure(UnknownLocalizedException()),
        onJoinPool = {},
        onAbort = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolJoinerLoadFailurePreview() {
    PoolJoinerContainer(
        poolState = LoadableViewState.Failure(UnknownLocalizedException()),
        joinPoolState = LoadableViewState.Initial,
        onJoinPool = {},
        onAbort = {},
    )
}
