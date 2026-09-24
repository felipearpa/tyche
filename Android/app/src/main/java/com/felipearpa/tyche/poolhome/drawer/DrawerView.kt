package com.felipearpa.tyche.poolhome.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipearpa.tyche.DrawerButtonRow
import com.felipearpa.tyche.DrawerGutter
import com.felipearpa.tyche.DrawerMenu
import com.felipearpa.tyche.DrawerPreviewHost
import com.felipearpa.tyche.DrawerPreviews
import com.felipearpa.tyche.DrawerSectionGap
import com.felipearpa.tyche.R
import com.felipearpa.tyche.drawerSupportingTextColor
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModel
import com.felipearpa.tyche.pool.poolGamblerScorePlaceholderModel
import com.felipearpa.tyche.pool.poolGamblerScoreWithoutPositionDummyModel
import com.felipearpa.tyche.ui.exception.ExceptionView
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.exception.localizedOrDefault
import com.felipearpa.tyche.ui.runIfStarted
import com.felipearpa.tyche.ui.shimmer
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.ui.state.LoadState
import com.felipearpa.tyche.pool.R as PoolR
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
    val routeLifecycleOwner = LocalLifecycleOwner.current

    DrawerView(
        uiState = uiState,
        onCloseDrawer = onCloseDrawer,
        logout = {
            // Signing out leaves the route, so a second press before it recomposes does nothing.
            routeLifecycleOwner.runIfStarted {
                viewModel.logout()
                onSignOut()
            }
        },
        onInvite = onInvite,
        onManageGamblers = onManageGamblers,
        onConfirmDelete = {
            onPoolDeleting()
            viewModel.deletePool(onSuccess = onPoolDeleted)
        },
        onProfile = onProfile,
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

    DrawerMenu(
        accountId = uiState.accountId,
        username = uiState.username,
        email = uiState.email,
        onProfile = {
            onCloseDrawer()
            onProfile()
        },
        onSignOut = logout,
        modifier = modifier,
    ) {
        PoolSummary(
            poolGamblerScoreState = uiState.poolGamblerScoreState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DrawerGutter)
                .padding(top = DrawerSectionGap),
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
                .padding(top = DrawerSectionGap),
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
private fun PoolSummary(
    poolGamblerScoreState: LoadState<PoolGamblerScoreModel>,
    modifier: Modifier = Modifier,
) {
    when (poolGamblerScoreState) {
        LoadState.Idle, LoadState.Loading ->
            PoolSummaryItem(
                poolGamblerScore = poolGamblerScorePlaceholderModel(),
                modifier = modifier,
                placeholderModifier = Modifier.shimmer(),
            )

        is LoadState.Loaded ->
            PoolSummaryItem(
                poolGamblerScore = poolGamblerScoreState.value,
                modifier = modifier,
            )

        is LoadState.Failure ->
            PoolSummarySurface(modifier = modifier) {
                Box(modifier = Modifier.padding(all = LocalBoxSpacing.current.large)) {
                    ExceptionView(localizedException = poolGamblerScoreState.exception.localizedOrDefault())
                }
            }
    }
}

/**
 * The current pool as a restrained, inset group: a small accent detail, the pool name, and the
 * gambler's position and points, announced together. Loading renders this same component from the
 * placeholder model with [placeholderModifier] (the shared shimmer) on each value, and keeps those
 * filler values away from assistive technology.
 */
@Composable
private fun PoolSummaryItem(
    poolGamblerScore: PoolGamblerScoreModel,
    modifier: Modifier = Modifier,
    placeholderModifier: Modifier? = null,
) {
    val isPlaceholder = placeholderModifier != null
    val placeholderStyle = placeholderModifier ?: Modifier
    val accessibilityDescription = poolSummaryAccessibilityDescription(poolGamblerScore)
    // Grows with the font scale, like the caption beside it.
    val trophySize = with(LocalDensity.current) { TrophySize.toDp() }

    PoolSummarySurface(
        modifier = modifier.clearAndSetSemantics {
            if (!isPlaceholder) contentDescription = accessibilityDescription
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
            modifier = Modifier.padding(all = LocalBoxSpacing.current.large),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.trophy),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(trophySize)
                        .then(placeholderStyle),
                )

                Text(
                    text = stringResource(id = R.string.playing_now_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = drawerSupportingTextColor(),
                    modifier = placeholderStyle,
                )
            }

            Text(
                text = poolGamblerScore.poolName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = placeholderStyle,
            )

            poolStandingText(position = poolGamblerScore.position, score = poolGamblerScore.score)
                ?.let { standing ->
                    Text(
                        text = standing,
                        style = MaterialTheme.typography.bodyMedium,
                        color = drawerSupportingTextColor(),
                        modifier = placeholderStyle,
                    )
                }
        }
    }
}

@Composable
private fun PoolSummarySurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(SummaryCornerRadius),
        modifier = modifier,
        content = content,
    )
}

/** Position and points on one line, separated by a dot; either can be missing. */
@Composable
private fun poolStandingText(position: Int?, score: Int?): String? =
    listOfNotNull(
        position?.let { stringResource(id = R.string.position_small_suffix, it) },
        score?.let { pluralStringResource(R.plurals.suffix_point_text, it, it) },
    ).takeIf { it.isNotEmpty() }?.joinToString(separator = " · ")

/** The summary's single announcement, spelling the position out instead of reading "8º". */
@Composable
private fun poolSummaryAccessibilityDescription(poolGamblerScore: PoolGamblerScoreModel): String =
    listOfNotNull(
        stringResource(id = R.string.playing_now_text),
        poolGamblerScore.poolName,
        poolGamblerScore.position?.let { stringResource(id = PoolR.string.leaderboard_rank_accessibility, it) },
        poolGamblerScore.score?.let { pluralStringResource(R.plurals.suffix_point_text, it, it) },
    ).joinToString(separator = ", ")

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
    Column(modifier = modifier) {
        Text(
            text = stringResource(id = R.string.pool_section_title).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = drawerSupportingTextColor(),
            modifier = Modifier
                .padding(horizontal = DrawerGutter)
                .padding(bottom = LocalBoxSpacing.current.small)
                .semantics { heading() },
        )

        DrawerButtonRow(
            iconResId = SharedR.drawable.person_add,
            title = stringResource(id = R.string.invite_action),
            onClick = onInvite,
        )

        if (isOwner) {
            DrawerButtonRow(
                iconResId = SharedR.drawable.group,
                title = stringResource(id = R.string.gamblers_action),
                onClick = onManageGamblers,
                accessory = { gamblerCount?.let { GamblerCountBadge(gamblerCount = it) } },
            )

            DrawerButtonRow(
                iconResId = SharedR.drawable.delete_forever,
                title = stringResource(id = R.string.delete_pool_action),
                onClick = onDeletePool,
                tint = MaterialTheme.colorScheme.error,
                enabled = !isDeleting,
            )
        }
    }
}

@Composable
private fun GamblerCountBadge(gamblerCount: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = gamblerCount.toString(),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(
                horizontal = LocalBoxSpacing.current.medium,
                vertical = 2.dp,
            ),
        )
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

private val TrophySize = 14.sp
private val SummaryCornerRadius = 12.dp

private fun drawerPreviewUiState(
    poolGamblerScoreState: LoadState<PoolGamblerScoreModel>,
    isOwner: Boolean,
    isDeleting: Boolean = false,
) = PoolHomeDrawerUiState(
    accountId = "account-1",
    email = "felipearpa@email.com",
    username = "felipearpa",
    poolGamblerScoreState = poolGamblerScoreState,
    isOwner = isOwner,
    gamblerCount = 19,
    isDeleting = isDeleting,
)

@DrawerPreviews
@Composable
private fun DrawerViewPreview() {
    DrawerPreviewHost {
        DrawerView(
            uiState = drawerPreviewUiState(
                poolGamblerScoreState = LoadState.Loaded(poolGamblerScoreDummyModel()),
                isOwner = true,
            ),
            onCloseDrawer = {},
        )
    }
}

@Preview(name = "Right-to-left")
@Composable
private fun RightToLeftDrawerViewPreview() {
    DrawerPreviewHost(layoutDirection = LayoutDirection.Rtl) {
        DrawerView(
            uiState = drawerPreviewUiState(
                poolGamblerScoreState = LoadState.Loaded(poolGamblerScoreDummyModel()),
                isOwner = true,
            ),
            onCloseDrawer = {},
        )
    }
}

@Preview(name = "Member")
@Composable
private fun NonOwnerDrawerViewPreview() {
    DrawerPreviewHost {
        DrawerView(
            uiState = drawerPreviewUiState(
                poolGamblerScoreState = LoadState.Loaded(poolGamblerScoreDummyModel()),
                isOwner = false,
            ),
            onCloseDrawer = {},
        )
    }
}

@Preview(name = "Without position")
@Composable
private fun WithoutPositionDrawerViewPreview() {
    DrawerPreviewHost {
        DrawerView(
            uiState = drawerPreviewUiState(
                poolGamblerScoreState = LoadState.Loaded(poolGamblerScoreWithoutPositionDummyModel()),
                isOwner = true,
            ),
            onCloseDrawer = {},
        )
    }
}

@Preview(name = "Loading")
@Composable
private fun LoadingDrawerViewPreview() {
    DrawerPreviewHost {
        DrawerView(
            uiState = drawerPreviewUiState(poolGamblerScoreState = LoadState.Loading, isOwner = true),
            onCloseDrawer = {},
        )
    }
}

@Preview(name = "Failure")
@Composable
private fun FailedDrawerViewPreview() {
    DrawerPreviewHost {
        DrawerView(
            uiState = drawerPreviewUiState(
                poolGamblerScoreState = LoadState.Failure(UnknownLocalizedException()),
                isOwner = true,
            ),
            onCloseDrawer = {},
        )
    }
}

@Preview(name = "Deleting")
@Composable
private fun DeletingDrawerViewPreview() {
    DrawerPreviewHost {
        DrawerView(
            uiState = drawerPreviewUiState(
                poolGamblerScoreState = LoadState.Loaded(poolGamblerScoreDummyModel()),
                isOwner = true,
                isDeleting = true,
            ),
            onCloseDrawer = {},
        )
    }
}
