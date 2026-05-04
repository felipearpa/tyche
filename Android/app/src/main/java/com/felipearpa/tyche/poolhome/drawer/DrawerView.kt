package com.felipearpa.tyche.poolhome.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.R
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModel
import com.felipearpa.tyche.pool.poolGamblerScorePlaceholderModel
import com.felipearpa.tyche.ui.exception.ExceptionView
import com.felipearpa.tyche.ui.exception.localizedOrDefault
import com.felipearpa.tyche.ui.shimmer
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.ui.state.LoadableViewState
import com.felipearpa.tyche.ui.R as SharedR

@Composable
fun DrawerView(viewModel: DrawerViewModel, onSignOut: () -> Unit) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    val poolGamblerScoreState by viewModel.state.collectAsStateWithLifecycle()

    DrawerView(
        email = email,
        poolGamblerScoreState = poolGamblerScoreState,
        logout = {
            viewModel.logout()
            onSignOut()
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun DrawerView(
    email: String,
    poolGamblerScoreState: LoadableViewState<PoolGamblerScoreModel>,
    modifier: Modifier = Modifier,
    logout: () -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
    ) {
        AccountHeader(
            email = email,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LocalBoxSpacing.current.large)
                .padding(horizontal = LocalBoxSpacing.current.medium)
                .padding(bottom = LocalBoxSpacing.current.medium),
        )

        PoolLayout(
            poolGamblerScoreState = poolGamblerScoreState,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.weight(1f))

        SignOutButton(
            onSignOut = logout,
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = LocalBoxSpacing.current.medium),
        )
    }
}

@Composable
private fun AccountHeader(email: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
    ) {
        Box(
            modifier = Modifier
                .size(AVATAR_SIZE.dp)
                .clip(CircleShape)
                .border(
                    width = AVATAR_RING_WIDTH.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = SharedR.drawable.filled_person),
                contentDescription = emptyString(),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(AVATAR_ICON_SIZE.dp),
            )
        }

        Text(
            text = email,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = stringResource(id = R.string.connected_account_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.primary,
            thickness = ACCENT_LINE_HEIGHT.dp,
            modifier = Modifier
                .width(ACCENT_LINE_WIDTH.dp)
                .clip(RoundedCornerShape(ACCENT_LINE_HEIGHT.dp)),
        )
    }
}

@Composable
private fun PoolLayout(
    poolGamblerScoreState: LoadableViewState<PoolGamblerScoreModel>,
    modifier: Modifier = Modifier,
) {
    when (poolGamblerScoreState) {
        LoadableViewState.Initial, LoadableViewState.Loading ->
            PoolLayoutItem(
                poolGamblerScore = poolGamblerScorePlaceholderModel(),
                modifier = modifier,
                placeholderModifier = Modifier.shimmer(),
            )

        is LoadableViewState.Success ->
            PoolLayoutItem(
                poolGamblerScore = poolGamblerScoreState.value,
                modifier = modifier,
            )

        is LoadableViewState.Failure ->
            ExceptionView(localizedException = poolGamblerScoreState.exception.localizedOrDefault())
    }


}

@Composable
private fun PoolLayoutItem(
    poolGamblerScore: PoolGamblerScoreModel,
    modifier: Modifier = Modifier,
    placeholderModifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
        modifier = modifier
            .background(MaterialTheme.colorScheme.primary)
            .padding(all = LocalBoxSpacing.current.large),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = SharedR.drawable.trophy),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(12.dp)
                    .then(placeholderModifier),
            )

            Text(
                text = stringResource(id = R.string.playing_now_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = placeholderModifier,
            )
        }

        Text(
            text = poolGamblerScore.poolName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = placeholderModifier,
        )

        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${poolGamblerScore.position}º",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = placeholderModifier,
            )

            poolGamblerScore.score?.let {
                VerticalDivider(
                    color = MaterialTheme.colorScheme.onPrimary,
                )

                Text(
                    text = stringResource(id = R.string.suffix_point_text, it),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = placeholderModifier,
                )
            }
        }
    }
}

@Composable
private fun SignOutButton(onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        modifier = modifier,
        onClick = onSignOut,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.sign_out),
            contentDescription = null,
        )
        Text(
            text = stringResource(id = R.string.sign_out_action),
            modifier = Modifier.padding(start = LocalBoxSpacing.current.small),
        )
    }
}

private const val AVATAR_SIZE = 96
private const val AVATAR_ICON_SIZE = 56
private const val AVATAR_RING_WIDTH = 3
private const val ACCENT_LINE_WIDTH = 48
private const val ACCENT_LINE_HEIGHT = 3

@Preview(showBackground = true)
@Composable
private fun DrawerViewPreview() {
    TycheTheme {
        Surface {
            DrawerView(
                modifier = Modifier.fillMaxSize(),
                email = "felipearpa@email.com",
                poolGamblerScoreState = LoadableViewState.Success(poolGamblerScoreDummyModel()),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingDrawerViewPreview() {
    TycheTheme {
        Surface {
            DrawerView(
                modifier = Modifier.fillMaxSize(),
                email = "felipearpa@email.com",
                poolGamblerScoreState = LoadableViewState.Loading,
            )
        }
    }
}
