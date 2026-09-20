package com.felipearpa.tyche.poolhome.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipearpa.tyche.AccountHeaderDrawer
import com.felipearpa.tyche.DrawerButtonRow
import com.felipearpa.tyche.R
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModel
import com.felipearpa.tyche.pool.poolGamblerScorePlaceholderModel
import com.felipearpa.tyche.pool.poolGamblerScoreWithoutPositionDummyModel
import com.felipearpa.tyche.ui.exception.ExceptionView
import com.felipearpa.tyche.ui.exception.localizedOrDefault
import com.felipearpa.tyche.ui.shimmer
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.ui.state.LoadState
import com.felipearpa.tyche.ui.R as SharedR

@Composable
fun DrawerView(
    viewModel: DrawerViewModel,
    onCloseDrawer: () -> Unit,
    onSignOut: () -> Unit,
    onInvite: () -> Unit,
    onManageGamblers: () -> Unit,
    onPoolDeleting: () -> Unit,
    onPoolDeleted: () -> Unit,
    onProfile: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DrawerView(
        uiState = uiState,
        onCloseDrawer = onCloseDrawer,
        logout = {
            viewModel.logout()
            onSignOut()
        },
        onInvite = onInvite,
        onManageGamblers = onManageGamblers,
        onConfirmDelete = {
            onPoolDeleting()
            viewModel.deletePool(onSuccess = onPoolDeleted)
        },
        onProfile = onProfile,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun DrawerView(
    uiState: PoolHomeDrawerUiState,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    logout: () -> Unit = {},
    onInvite: () -> Unit = {},
    onManageGamblers: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onProfile: () -> Unit = {},
) {
    var isConfirmingDelete by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
    ) {
        AccountHeaderDrawer(
            accountId = uiState.accountId,
            username = uiState.username,
            email = uiState.email,
            onProfile = {
                onCloseDrawer()
                onProfile()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LocalBoxSpacing.current.large)
                .padding(horizontal = LocalBoxSpacing.current.medium)
                .padding(bottom = LocalBoxSpacing.current.medium),
        )

        PoolLayout(
            poolGamblerScoreState = uiState.poolGamblerScoreState,
            modifier = Modifier.fillMaxWidth(),
        )

        PoolMenuSection(
            isOwner = uiState.isOwner,
            isDeleting = uiState.isDeleting,
            onInvite = onInvite,
            onDeletePool = { isConfirmingDelete = true },
            gamblerCount = uiState.gamblerCount,
            onManageGamblers = {
                onCloseDrawer()
                onManageGamblers()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LocalBoxSpacing.current.medium)
                .padding(top = LocalBoxSpacing.current.medium),
        )

        Spacer(modifier = Modifier.weight(1f))

        SignOutButton(
            onSignOut = logout,
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = LocalBoxSpacing.current.medium),
        )
    }

    if (isConfirmingDelete) {
        DeletePoolConfirmationDialog(
            onConfirm = {
                isConfirmingDelete = false
                onConfirmDelete()
            },
            onDismiss = { isConfirmingDelete = false },
        )
    }
}

@Composable
private fun PoolLayout(
    poolGamblerScoreState: LoadState<PoolGamblerScoreModel>,
    modifier: Modifier = Modifier,
) {
    when (poolGamblerScoreState) {
        LoadState.Idle, LoadState.Loading ->
            PoolLayoutItem(
                poolGamblerScore = poolGamblerScorePlaceholderModel(),
                modifier = modifier,
                placeholderModifier = Modifier.shimmer(),
            )

        is LoadState.Loaded ->
            PoolLayoutItem(
                poolGamblerScore = poolGamblerScoreState.value,
                modifier = modifier,
            )

        is LoadState.Failure ->
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
            poolGamblerScore.position?.let { position ->
                Text(
                    text = stringResource(id = R.string.position_small_suffix, position),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = placeholderModifier,
                )

                VerticalDivider(color = MaterialTheme.colorScheme.onPrimary)
            }

            poolGamblerScore.score?.let { score ->
                Text(
                    text = stringResource(id = R.string.suffix_point_text, score),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = placeholderModifier,
                )
            }
        }
    }
}

@Composable
private fun PoolMenuSection(
    isOwner: Boolean,
    isDeleting: Boolean,
    onInvite: () -> Unit,
    onDeletePool: () -> Unit,
    gamblerCount: Int?,
    onManageGamblers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(id = R.string.pool_section_title).uppercase(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            DrawerButtonRow(
                iconResId = SharedR.drawable.person_add,
                title = stringResource(id = R.string.invite_action),
                onClick = onInvite,
            )

            if (isOwner) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onManageGamblers)
                        .padding(all = LocalBoxSpacing.current.medium),
                ) {
                    Icon(
                        painter = painterResource(id = SharedR.drawable.group),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = stringResource(id = R.string.gamblers_action),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    gamblerCount?.let { count ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    horizontal = LocalBoxSpacing.current.small,
                                    vertical = 2.dp,
                                ),
                            )
                        }
                    }
                }

                DrawerButtonRow(
                    iconResId = SharedR.drawable.delete_forever,
                    title = stringResource(id = R.string.delete_pool_action),
                    tint = MaterialTheme.colorScheme.error,
                    enabled = !isDeleting,
                    onClick = onDeletePool,
                )
            }
        }
    }
}

@Composable
private fun DeletePoolConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.delete_pool_alert_title)) },
        text = { Text(text = stringResource(id = R.string.delete_pool_alert_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(id = R.string.delete_action),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = SharedR.string.cancel_action))
            }
        },
    )
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

private fun drawerPreviewUiState(
    poolGamblerScoreState: LoadState<PoolGamblerScoreModel>,
    isOwner: Boolean,
) = PoolHomeDrawerUiState(
    accountId = "account-1",
    email = "felipearpa@email.com",
    username = "felipearpa",
    poolGamblerScoreState = poolGamblerScoreState,
    isOwner = isOwner,
    gamblerCount = 19,
    isDeleting = false,
)

@PreviewLightDark
@Composable
private fun DrawerViewPreview() {
    TycheTheme {
        Surface {
            DrawerView(
                uiState = drawerPreviewUiState(
                    poolGamblerScoreState = LoadState.Loaded(poolGamblerScoreDummyModel()),
                    isOwner = true,
                ),
                onCloseDrawer = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun DrawerViewNonOwnerPreview() {
    TycheTheme {
        Surface {
            DrawerView(
                uiState = drawerPreviewUiState(
                    poolGamblerScoreState = LoadState.Loaded(poolGamblerScoreDummyModel()),
                    isOwner = false,
                ),
                onCloseDrawer = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DrawerViewWithoutPositionPreview() {
    DrawerView(
        uiState = drawerPreviewUiState(
            poolGamblerScoreState = LoadState.Loaded(poolGamblerScoreWithoutPositionDummyModel()),
            isOwner = true,
        ),
        onCloseDrawer = {},
        modifier = Modifier.fillMaxSize(),
    )
}

@PreviewLightDark
@Composable
private fun LoadingDrawerViewPreview() {
    TycheTheme {
        Surface {
            DrawerView(
                uiState = drawerPreviewUiState(
                    poolGamblerScoreState = LoadState.Loading,
                    isOwner = true,
                ),
                onCloseDrawer = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
