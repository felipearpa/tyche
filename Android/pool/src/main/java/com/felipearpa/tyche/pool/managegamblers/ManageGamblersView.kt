package com.felipearpa.tyche.pool.managegamblers

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.core.JoinPoolUrlTemplateProvider
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.ui.lazy.RefreshableLazyPagingColumn
import com.felipearpa.tyche.ui.lazy.lazyPagingConcatenateError
import com.felipearpa.tyche.ui.shimmer
import com.felipearpa.ui.state.MutationState
import com.felipearpa.ui.state.activeValue
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import org.koin.compose.koinInject
import com.felipearpa.tyche.ui.R as SharedR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageGamblersView(
    poolId: String,
    gamblerId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = manageGamblersViewModel(poolId = poolId)
    val lazyMembers = viewModel.poolMembers.collectAsLazyPagingItems()
    val removalStates by viewModel.removalStates.collectAsStateWithLifecycle()
    val failedGambler = removalStates.values
        .firstOrNull { state -> state is MutationState.Failure }
        ?.activeValue()

    var isEditing by rememberSaveable { mutableStateOf(false) }
    var gamblerPendingRemoval by remember { mutableStateOf<PoolMemberModel?>(null) }

    val joinPoolUrlTemplate = koinInject<JoinPoolUrlTemplateProvider>()
    var invitePoolUrl by remember { mutableStateOf(emptyString()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = SharedR.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { isEditing = !isEditing }) {
                        Text(
                            text = stringResource(
                                id = if (isEditing) {
                                    R.string.manage_gamblers_done_action
                                } else {
                                    R.string.manage_gamblers_edit_action
                                },
                            ),
                            color = if (isEditing) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            fontWeight = if (isEditing) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            ManageGamblersList(
                lazyMembers = lazyMembers,
                isEditing = isEditing,
                removalStates = removalStates,
                onRequestRemove = { gamblerPendingRemoval = it },
                onInvite = { invitePoolUrl = String.format(joinPoolUrlTemplate(), poolId) },
                modifier = Modifier.fillMaxSize(),
            )

            failedGambler?.let { failed ->
                ManageGamblerErrorBanner(
                    username = failed.gamblerUsername,
                    onRetry = { viewModel.remove(failed) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(LocalBoxSpacing.current.medium),
                )
            }
        }
    }

    gamblerPendingRemoval?.let { member ->
        RemoveGamblerConfirmationDialog(
            username = member.gamblerUsername,
            onConfirm = {
                viewModel.remove(member)
                gamblerPendingRemoval = null
            },
            onDismiss = { gamblerPendingRemoval = null },
        )
    }

    if (invitePoolUrl.isNotEmpty()) {
        InvitePoolSharer(url = invitePoolUrl, onClose = { invitePoolUrl = emptyString() })
    }
}

@Composable
private fun ManageGamblersList(
    lazyMembers: LazyPagingItems<PoolMemberModel>,
    isEditing: Boolean,
    removalStates: Map<String, MutationState<PoolMemberModel>>,
    onRequestRemove: (PoolMemberModel) -> Unit,
    onInvite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RefreshableLazyPagingColumn(
        modifier = modifier,
        lazyPagingItems = lazyMembers,
        loadingContent = { managePlaceholderList(count = 8) },
        emptyContent = {
            item {
                ManageGamblersEmptyView(
                    onInvite = onInvite,
                    modifier = Modifier.fillParentMaxSize(),
                )
            }
        },
        appendLoadingContent = { managePlaceholderItemRow() },
        appendErrorContent = { exception ->
            lazyPagingConcatenateError(
                exception = exception,
                onRetry = { lazyMembers.retry() },
            )
        },
    ) {
        items(
            count = lazyMembers.itemCount,
            key = lazyMembers.itemKey { member -> member.gamblerId },
            contentType = lazyMembers.itemContentType { "PoolMember" },
        ) { index ->
            val member = lazyMembers[index] ?: return@items
            val state = removalStates[member.gamblerId] ?: MutationState.Idle(member)
            if (state is MutationState.Mutated) return@items
            ManageGamblerRow(
                isEditing = isEditing,
                state = state,
                onRequestRemove = { onRequestRemove(member) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageGamblerRow(
    isEditing: Boolean,
    state: MutationState<PoolMemberModel>,
    onRequestRemove: () -> Unit,
) {
    val isDeleting = state is MutationState.Mutating
    val removeLabel = stringResource(id = R.string.remove_gambler_action)

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && !isDeleting) {
                onRequestRemove()
            }
            false
        },
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                onClick(label = removeLabel) {
                    if (!isDeleting) onRequestRemove()
                    true
                }
            }
            .padding(horizontal = LocalBoxSpacing.current.medium),
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = !isDeleting,
            backgroundContent = {
                ManageGamblerSwipeBackground(
                    isPastThreshold = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart,
                )
            },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(all = LocalBoxSpacing.current.medium),
                horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isEditing && !isDeleting) {
                    IconButton(onClick = onRequestRemove) {
                        Icon(
                            painter = painterResource(id = SharedR.drawable.delete_forever),
                            contentDescription = removeLabel,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                ManageGamblerItem(
                    state = state,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        HorizontalDivider()
    }
}

@Composable
private fun ManageGamblerSwipeBackground(isPastThreshold: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.error),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            painter = painterResource(id = SharedR.drawable.delete_forever),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .padding(end = 26.dp)
                .size(if (isPastThreshold) 28.dp else 24.dp),
        )
    }
}

private fun LazyListScope.managePlaceholderList(count: Int) {
    repeat(count) { index ->
        item {
            ManageGamblerPlaceholderRow(modifier = Modifier.alpha(1f - index * 0.04f))
        }
    }
}

private fun LazyListScope.managePlaceholderItemRow() {
    item { ManageGamblerPlaceholderRow() }
}

@Composable
private fun ManageGamblerPlaceholderRow(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LocalBoxSpacing.current.medium),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = LocalBoxSpacing.current.medium),
            horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .shimmer(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small)) {
                Box(
                    modifier = Modifier
                        .size(width = 160.dp, height = 14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .shimmer(),
                )
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .shimmer(),
                )
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun ManageGamblersEmptyView(onInvite: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(LocalBoxSpacing.current.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = LocalBoxSpacing.current.large,
            alignment = Alignment.CenterVertically,
        ),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = SharedR.drawable.filled_person),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
        ) {
            Text(
                text = stringResource(id = R.string.no_other_gamblers_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(id = R.string.no_other_gamblers_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 240.dp),
            )
        }

        Button(
            onClick = onInvite,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.Black,
            ),
        ) {
            Icon(
                painter = painterResource(id = SharedR.drawable.person_add),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(id = R.string.invite_gamblers_action),
                modifier = Modifier.padding(start = LocalBoxSpacing.current.small),
            )
        }
    }
}

@Composable
private fun ManageGamblerErrorBanner(
    username: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(LocalBoxSpacing.current.medium),
            horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = SharedR.drawable.error),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.remove_gambler_failure_title, "@$username"),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(id = R.string.remove_gambler_failure_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Button(
                onClick = onRetry,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = stringResource(id = SharedR.string.retry_action),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun RemoveGamblerConfirmationDialog(
    username: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.remove_gambler_alert_title, "@$username")) },
        text = { Text(text = stringResource(id = R.string.remove_gambler_alert_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(id = R.string.remove_gambler_action),
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
private fun InvitePoolSharer(url: String, onClose: () -> Unit) {
    val activityResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { _ -> onClose() }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, url)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)

    LaunchedEffect(Unit) {
        activityResultLauncher.launch(shareIntent)
    }
}
