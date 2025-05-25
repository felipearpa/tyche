package com.felipearpa.tyche.poolhome.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.R
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModel
import com.felipearpa.tyche.pool.poolscore.spotlight.PoolSpotlightItem
import com.felipearpa.tyche.ui.exception.ExceptionView
import com.felipearpa.tyche.ui.exception.LocalizedException
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.exception.localizedOrNull
import com.felipearpa.tyche.ui.loading.LoadingContainerView
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.ui.state.LoadableViewState

@Composable
fun DrawerView(viewModel: DrawerViewModel, changePool: () -> Unit, onLogout: () -> Unit) {
    val viewState by viewModel.state.collectAsState()

    DrawerView(
        modifier = Modifier.fillMaxSize(),
        viewState = viewState,
        changePool = changePool,
        logout = {
            viewModel.logout()
            onLogout()
        }
    )
}

@Composable
private fun DrawerView(
    modifier: Modifier = Modifier,
    viewState: LoadableViewState<PoolGamblerScoreModel>,
    changePool: () -> Unit = {},
    logout: () -> Unit = {}
) {
    when (viewState) {
        is LoadableViewState.Failure ->
            FailureContent(
                localizedException = viewState().localizedOrNull()!!,
                modifier = modifier
            )

        LoadableViewState.Initial, LoadableViewState.Loading ->
            LoadingContainerView {}

        is LoadableViewState.Success ->
            DrawerView(
                currentPoolGamblerScore = viewState(),
                changePool = changePool,
                logout = logout,
                modifier = modifier.padding(all = LocalBoxSpacing.current.large)
            )
    }
}

@Composable
private fun FailureContent(modifier: Modifier = Modifier, localizedException: LocalizedException) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(all = LocalBoxSpacing.current.large)
    ) {
        ExceptionView(localizedException = localizedException)
    }
}

@Composable
private fun DrawerView(
    modifier: Modifier = Modifier,
    currentPoolGamblerScore: PoolGamblerScoreModel,
    changePool: () -> Unit = {},
    logout: () -> Unit = {}
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium)
        ) {
            PoolSpotlightItem(
                poolGamblerScore = currentPoolGamblerScore,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LocalBoxSpacing.current.medium)
            )
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LocalBoxSpacing.current.medium),
                onClick = changePool
            ) {
                Text(text = stringResource(id = R.string.change_pool_action))
            }
            HorizontalDivider()
        }

        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LocalBoxSpacing.current.medium),
            onClick = logout
        ) {
            Icon(
                painter = painterResource(id = R.drawable.logout),
                contentDescription = emptyString()
            )
            Text(text = stringResource(id = R.string.log_out_action))
        }
    }
}

@Preview(showBackground = true, name = "Initial")
@Composable
private fun InitialDrawerViewPreview() {
    Surface {
        DrawerView(
            modifier = Modifier.fillMaxSize(),
            viewState = LoadableViewState.Initial
        )
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun LoadingDrawerViewPreview() {
    Surface {
        DrawerView(
            modifier = Modifier.fillMaxSize(),
            viewState = LoadableViewState.Loading
        )
    }
}

@Preview(showBackground = true, name = "Success")
@Composable
private fun SuccessDrawerViewPreview() {
    Surface {
        DrawerView(
            modifier = Modifier.fillMaxSize(),
            viewState = LoadableViewState.Success(poolGamblerScoreDummyModel())
        )
    }
}

@Preview(showBackground = true, name = "Failure")
@Composable
private fun FailureDrawerViewPreview() {
    Surface {
        DrawerView(
            modifier = Modifier.fillMaxSize(),
            viewState = LoadableViewState.Failure(UnknownLocalizedException())
        )
    }
}