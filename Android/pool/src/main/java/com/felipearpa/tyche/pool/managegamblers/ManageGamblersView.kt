package com.felipearpa.tyche.pool.managegamblers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.lazy.RefreshableLazyPagingColumn
import com.felipearpa.tyche.ui.lazy.lazyPagingColumnError
import com.felipearpa.tyche.ui.lazy.lazyPagingConcatenateError
import com.felipearpa.tyche.ui.lazy.viewportFillingItem
import com.felipearpa.tyche.ui.theme.LocalExtendedColorScheme
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.ui.state.LoadState
import com.felipearpa.ui.state.MutationState
import com.felipearpa.ui.state.activeValue
import com.revenuecat.placeholder.placeholder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import androidx.paging.LoadState as PagingLoadState
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
    val removedGamblerIds by viewModel.removedGamblerIds.collectAsStateWithLifecycle()
    val gamblerCount by viewModel.gamblerCount.collectAsStateWithLifecycle()
    val rowHolders by viewModel.rowHolders.collectAsStateWithLifecycle()

    FailureHaptics(failedGamblerIds = viewModel.failedGamblerIds.collectAsStateWithLifecycle().value)
    ReportMembersRefreshed(
        refreshLoadState = lazyMembers.loadState.refresh,
        onMembersRefreshed = viewModel::onMembersRefreshed,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = SharedR.drawable.arrow_back),
                            contentDescription = stringResource(id = SharedR.string.back_action),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        // The list scrolls under the top app bar and the navigation bar; its content padding
        // keeps the first and last rows clear of both.
        ManageGamblersList(
            lazyMembers = lazyMembers,
            gamblerCount = gamblerCount,
            removedGamblerIds = removedGamblerIds,
            removableRow = { member ->
                RemovableGamblerRow(
                    member = member,
                    holder = rowHolders[member.gamblerId],
                    isRemoved = member.gamblerId in removedGamblerIds,
                    onRemove = { viewModel.remove(member) },
                )
            },
            onRefresh = viewModel::refresh,
            contentPadding = innerPadding,
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding),
        )
    }
}

/**
 * Plays the error haptic when a gambler joins [failedGamblerIds], wherever their row is. Failures
 * already present when the screen is composed (after rotation, say) were felt before.
 */
@Composable
private fun FailureHaptics(failedGamblerIds: Set<String>) {
    val haptics = LocalHapticFeedback.current
    var feltFailures by remember { mutableStateOf(failedGamblerIds) }
    LaunchedEffect(failedGamblerIds) {
        if ((failedGamblerIds - feltFailures).isNotEmpty()) {
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
        }
        feltFailures = failedGamblerIds
    }
}

/** Calls [onMembersRefreshed] each time a members reload finishes and its first page is shown. */
@Composable
private fun ReportMembersRefreshed(refreshLoadState: PagingLoadState, onMembersRefreshed: () -> Unit) {
    var isReloading by remember { mutableStateOf(false) }
    LaunchedEffect(refreshLoadState) {
        when (refreshLoadState) {
            is PagingLoadState.Loading -> isReloading = true
            is PagingLoadState.Error -> isReloading = false
            is PagingLoadState.NotLoading -> {
                if (isReloading) onMembersRefreshed()
                isReloading = false
            }
        }
    }
}

/**
 * The heading followed by one row per member. The heading scrolls with the rows and starts every
 * list state (first load, loaded, failed), so the count stays visible whatever the rows do.
 */
@Composable
private fun ManageGamblersList(
    lazyMembers: LazyPagingItems<PoolMemberModel>,
    gamblerCount: LoadState<Int>,
    removedGamblerIds: Set<String>,
    removableRow: @Composable (PoolMemberModel) -> Unit,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val headerItem: LazyListScope.() -> Unit = {
        item(key = HEADING_KEY, contentType = HEADING_KEY) { ManageGamblersHeader(count = gamblerCount) }
    }

    RefreshableLazyPagingColumn(
        modifier = modifier,
        lazyPagingItems = lazyMembers,
        contentPadding = contentPadding,
        onRefresh = onRefresh,
        loadingContent = {
            headerItem()
            managePlaceholderList(count = FIRST_LOAD_PLACEHOLDER_COUNT)
        },
        errorContent = { exception ->
            headerItem()
            lazyPagingColumnError(exception = exception, onRetry = lazyMembers::refresh)
        },
        emptyContent = { headerItem() },
        appendLoadingContent = { managePlaceholderItemRow() },
        appendErrorContent = { exception ->
            lazyPagingConcatenateError(
                exception = exception,
                onRetry = { lazyMembers.retry() },
            )
        },
    ) {
        headerItem()
        items(
            count = lazyMembers.itemCount,
            key = lazyMembers.itemKey { member -> member.gamblerId },
            contentType = lazyMembers.itemContentType { MEMBER_CONTENT_TYPE },
        ) { index ->
            val member = lazyMembers[index] ?: return@items
            if (member.isOwner) {
                ManageGamblerRow(
                    state = MutationState.Idle(member),
                    onRequestRemove = {},
                    onRetry = {},
                    onDismissFailure = {},
                )
                return@items
            }
            // A row composed while its gambler is still listed plays its own exit when the
            // removal lands; one composed after the removal (scrolled back, rotated, re-paged)
            // draws nothing and creates no removal state.
            val isRemoved = member.gamblerId in removedGamblerIds
            val wasListedWhenComposed = remember { !isRemoved }
            if (wasListedWhenComposed) removableRow(member)
        }
        if (holdsOnlyTheOwner(lazyMembers, removedGamblerIds)) {
            viewportFillingItem { NoOtherGamblers() }
        }
    }
}

/**
 * Whether every member has loaded and, apart from gamblers removed here, only the owner is left.
 * Never while a page is loading or after a load error.
 */
private fun holdsOnlyTheOwner(
    lazyMembers: LazyPagingItems<PoolMemberModel>,
    removedGamblerIds: Set<String>,
): Boolean {
    val loadState = lazyMembers.loadState
    if (loadState.refresh !is PagingLoadState.NotLoading) return false
    if (!loadState.append.endOfPaginationReached) return false
    val remaining = lazyMembers.itemSnapshotList.items.filterNot { member -> member.gamblerId in removedGamblerIds }
    return remaining.isNotEmpty() && remaining.all { member -> member.isOwner }
}

/**
 * The scrolling "Gamblers" heading with the pool's gambler count beneath it. The count line is a
 * loading placeholder until the count arrives, and is left out when it could not be loaded.
 */
@Composable
private fun ManageGamblersHeader(count: LoadState<Int>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = HORIZONTAL_PADDING, end = HORIZONTAL_PADDING, bottom = HORIZONTAL_PADDING),
    ) {
        Text(
            text = stringResource(id = R.string.manage_gamblers_title),
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        when (count) {
            is LoadState.Loaded -> GamblerCountLine(count = count.value)
            LoadState.Idle, LoadState.Loading -> GamblerCountLine(count = PLACEHOLDER_COUNT, isPlaceholder = true)
            is LoadState.Failure -> Unit
        }
    }
}

/**
 * "N gamblers in this pool". With [isPlaceholder] the line is masked with the shared
 * [LocalLoadingPlaceholderPulse] and hidden from TalkBack; [count] is then filler.
 */
@Composable
private fun GamblerCountLine(count: Int, isPlaceholder: Boolean = false) {
    val pulse = LocalLoadingPlaceholderPulse.current
    Text(
        text = pluralStringResource(id = R.plurals.manage_gamblers_count, count = count, count),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = if (isPlaceholder) {
            Modifier
                .placeholder(color = pulse.color, shape = pulse.shape, highlight = pulse.highlight)
                .clearAndSetSemantics {}
        } else {
            Modifier
        },
    )
}

/**
 * A non-owner row. Its removal state comes from [holder], which the screen's view model keeps so
 * the removal outlives the row leaving composition; a row without a holder is idle, or removed when
 * [isRemoved]. Once the row has started its exit it stays hidden, even after the gambler leaves the
 * removed set.
 */
@Composable
private fun RemovableGamblerRow(
    member: PoolMemberModel,
    holder: GamblerRowHolder?,
    isRemoved: Boolean,
    onRemove: () -> Unit,
) {
    val state = holder?.removal?.collectAsStateWithLifecycle()?.value
        ?: if (isRemoved) MutationState.Mutated(original = member, updated = member) else MutationState.Idle(member)
    var isConfirming by remember { mutableStateOf(false) }
    val visibility = remember { MutableTransitionState(initialState = true) }
    if (state is MutationState.Mutated) visibility.targetState = false

    AnimatedVisibility(
        visibleState = visibility,
        exit = shrinkVertically() + fadeOut(),
    ) {
        ManageGamblerRow(
            state = state,
            onRequestRemove = { isConfirming = true },
            onRetry = { holder?.retry() },
            onDismissFailure = { holder?.dismissFailure() },
        )
    }

    if (isConfirming) {
        RemoveGamblerConfirmationDialog(
            username = member.gamblerUsername,
            onConfirm = {
                onRemove()
                isConfirming = false
            },
            onDismiss = { isConfirming = false },
        )
    }
}

/**
 * One member row with its separator. A non-owner row is removed by swiping it toward the leading
 * edge or through its TalkBack "Remove" action; both call [onRequestRemove] unless a removal is
 * running. The owner's row is the current-user row and offers neither.
 *
 * Loading rows pass [isPlaceholder] with `poolMemberPlaceholderModel()`: the row keeps its
 * production padding, background, and divider but offers no swipe or accessibility action, and its
 * [ManageGamblerItem] renders the shared loading placeholder.
 */
@Composable
private fun ManageGamblerRow(
    state: MutationState<PoolMemberModel>,
    onRequestRemove: () -> Unit,
    onRetry: () -> Unit,
    onDismissFailure: () -> Unit,
    isPlaceholder: Boolean = false,
) {
    val isOwner = state.activeValue().isOwner && !isPlaceholder
    val rowContent: @Composable () -> Unit = {
        ManageGamblerItem(
            state = state,
            onRetry = onRetry,
            onDismissFailure = onDismissFailure,
            isPlaceholder = isPlaceholder,
            modifier = Modifier
                .background(
                    if (isOwner) {
                        LocalExtendedColorScheme.current.currentUserContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                )
                .padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING),
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isOwner || isPlaceholder) {
            rowContent()
        } else {
            SwipeToRemove(
                isRemoving = state is MutationState.Mutating,
                onRequestRemove = onRequestRemove,
                content = rowContent,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING))
    }
}

/**
 * Swiping [content] from the trailing edge past [SWIPE_THRESHOLD] (a fixed distance, whatever the
 * row's width) calls [onRequestRemove] and returns the row to rest; the row's TalkBack "Remove"
 * action does the same. Neither is offered while [isRemoving].
 *
 * The request is made when the finger lifts past the threshold, not when the box settles: a
 * release without velocity settles on the nearest anchor, so the box alone would only dismiss
 * past half the row. A dismissal the box does reach only returns the row to rest.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToRemove(
    isRemoving: Boolean,
    onRequestRemove: () -> Unit,
    content: @Composable () -> Unit,
) {
    val removeLabel = stringResource(id = R.string.remove_gambler_action)
    val thresholdPx = with(LocalDensity.current) { SWIPE_THRESHOLD.toPx() }
    val dismissState = rememberSwipeToDismissBoxState(positionalThreshold = { thresholdPx })
    val scope = rememberCoroutineScope()
    val isPastThreshold by remember(dismissState, thresholdPx) {
        derivedStateOf {
            // A settled row has no offset to read yet; a dragged one has a real number.
            dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart &&
                -dismissState.requireOffset() >= thresholdPx
        }
    }
    val currentIsRemoving by rememberUpdatedState(isRemoving)
    val currentOnRequestRemove by rememberUpdatedState(onRequestRemove)

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !isRemoving,
        onDismiss = { scope.launch { dismissState.reset() } },
        backgroundContent = { ManageGamblerSwipeBackground(isPastThreshold = isPastThreshold) },
        modifier = Modifier
            .pointerInput(dismissState) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                    do {
                        val event = awaitPointerEvent(pass = PointerEventPass.Final)
                    } while (event.changes.any { change -> change.pressed })
                    if (isPastThreshold && !currentIsRemoving) currentOnRequestRemove()
                }
            }
            .semantics {
                if (!isRemoving) {
                    customActions = listOf(
                        CustomAccessibilityAction(label = removeLabel) {
                            onRequestRemove()
                            true
                        },
                    )
                }
            },
        content = { content() },
    )
}

@Composable
private fun ManageGamblerSwipeBackground(isPastThreshold: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Drawn behind every row; the row's own "Remove" action is what TalkBack offers.
            .clearAndSetSemantics {}
            .background(MaterialTheme.colorScheme.error)
            .padding(end = HORIZONTAL_PADDING),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(SWIPE_GLYPH_PAST_THRESHOLD_SIZE), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.person_remove),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(
                        if (isPastThreshold) SWIPE_GLYPH_PAST_THRESHOLD_SIZE else SWIPE_GLYPH_SIZE,
                    ),
                )
            }
            Text(
                text = stringResource(id = R.string.remove_gambler_action),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onError,
            )
        }
    }
}

/** Shown below the owner's row when nobody else is in the pool. */
@Composable
private fun NoOtherGamblers() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = EMPTY_STATE_HORIZONTAL_PADDING, vertical = HORIZONTAL_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(EMPTY_STATE_BADGE_SIZE),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.group),
                    contentDescription = null,
                    modifier = Modifier.size(EMPTY_STATE_GLYPH_SIZE),
                )
            }
        }
        Text(
            text = stringResource(id = R.string.no_other_gamblers_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(id = R.string.no_other_gamblers_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun LazyListScope.managePlaceholderList(count: Int) {
    repeat(count) { managePlaceholderItemRow() }
}

private fun LazyListScope.managePlaceholderItemRow() {
    item(contentType = MEMBER_CONTENT_TYPE) {
        ManageGamblerRow(
            state = MutationState.Idle(poolMemberPlaceholderModel()),
            onRequestRemove = {},
            onRetry = {},
            onDismissFailure = {},
            isPlaceholder = true,
        )
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

private const val HEADING_KEY = "manageGamblersHeading"
private const val MEMBER_CONTENT_TYPE = "PoolMember"
private const val FIRST_LOAD_PLACEHOLDER_COUNT = 8

/** Filler for the count line's loading placeholder; never shown. */
private const val PLACEHOLDER_COUNT = 12
private val HORIZONTAL_PADDING = 16.dp
private val VERTICAL_PADDING = 12.dp
private val SWIPE_THRESHOLD = 56.dp
private val SWIPE_GLYPH_SIZE = 24.dp
private val SWIPE_GLYPH_PAST_THRESHOLD_SIZE = 28.dp
private val EMPTY_STATE_HORIZONTAL_PADDING = 32.dp
private val EMPTY_STATE_BADGE_SIZE = 64.dp
private val EMPTY_STATE_GLYPH_SIZE = 32.dp

private fun previewMembers() = listOf(
    PoolMemberModel(gamblerId = "1", gamblerUsername = "felipearpa", gamblerEmail = "felipe@example.com", isOwner = true),
    PoolMemberModel(gamblerId = "2", gamblerUsername = "jaime", gamblerEmail = "jaime@example.com"),
    PoolMemberModel(gamblerId = "3", gamblerUsername = "santiago", gamblerEmail = "santiago@example.com"),
)

private val fullyLoaded = LoadStates(
    refresh = PagingLoadState.NotLoading(endOfPaginationReached = false),
    prepend = PagingLoadState.NotLoading(endOfPaginationReached = true),
    append = PagingLoadState.NotLoading(endOfPaginationReached = true),
)

@Composable
private fun ManageGamblersListPreviewContent(members: List<PoolMemberModel>, count: LoadState<Int>) {
    val items = MutableStateFlow(PagingData.from(members, sourceLoadStates = fullyLoaded))
        .collectAsLazyPagingItems()
    TycheTheme {
        Surface {
            ManageGamblersList(
                lazyMembers = items,
                gamblerCount = count,
                removedGamblerIds = emptySet(),
                removableRow = { member ->
                    ManageGamblerRow(
                        state = MutationState.Idle(member),
                        onRequestRemove = {},
                        onRetry = {},
                        onDismissFailure = {},
                    )
                },
                onRefresh = {},
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@PreviewLightDark
@Preview(name = "Loaded, Spanish, large text", locale = "es-rCO", fontScale = 2f)
@Composable
private fun ManageGamblersListPreview() {
    ManageGamblersListPreviewContent(members = previewMembers(), count = LoadState.Loaded(3))
}

@PreviewLightDark
@Composable
private fun ManageGamblersOnlyOwnerPreview() {
    ManageGamblersListPreviewContent(members = previewMembers().take(1), count = LoadState.Loaded(1))
}

@PreviewLightDark
@Composable
private fun ManageGamblersFirstLoadPreview() {
    val loading = LoadStates(
        refresh = PagingLoadState.Loading,
        prepend = PagingLoadState.NotLoading(endOfPaginationReached = false),
        append = PagingLoadState.NotLoading(endOfPaginationReached = false),
    )
    val items = MutableStateFlow(PagingData.empty<PoolMemberModel>(sourceLoadStates = loading))
        .collectAsLazyPagingItems()
    TycheTheme {
        Surface {
            ManageGamblersList(
                lazyMembers = items,
                gamblerCount = LoadState.Loading,
                removedGamblerIds = emptySet(),
                removableRow = {},
                onRefresh = {},
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ManageGamblersHeaderLoadedPreview() {
    TycheTheme {
        Surface { ManageGamblersHeader(count = LoadState.Loaded(3)) }
    }
}

@PreviewLightDark
@Composable
private fun ManageGamblersHeaderPlaceholderPreview() {
    TycheTheme {
        Surface { ManageGamblersHeader(count = LoadState.Loading) }
    }
}

@PreviewLightDark
@Composable
private fun ManageGamblersHeaderOmittedCountPreview() {
    TycheTheme {
        Surface { ManageGamblersHeader(count = LoadState.Failure(UnknownLocalizedException())) }
    }
}

@PreviewLightDark
@Composable
private fun ManageGamblerOwnerRowPreview() {
    TycheTheme {
        Surface {
            ManageGamblerRow(
                state = MutationState.Idle(previewMembers().first()),
                onRequestRemove = {},
                onRetry = {},
                onDismissFailure = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ManageGamblerFailedRowPreview() {
    val member = previewMembers().last()
    TycheTheme {
        Surface {
            ManageGamblerRow(
                state = MutationState.Failure(original = member, updated = member, exception = UnknownLocalizedException()),
                onRequestRemove = {},
                onRetry = {},
                onDismissFailure = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ManageGamblerPlaceholderRowPreview() {
    TycheTheme {
        Surface {
            ManageGamblerRow(
                state = MutationState.Idle(poolMemberPlaceholderModel()),
                onRequestRemove = {},
                onRetry = {},
                onDismissFailure = {},
                isPlaceholder = true,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun RemoveGamblerConfirmationDialogPreview() {
    TycheTheme {
        RemoveGamblerConfirmationDialog(
            username = "felipearpa",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ManageGamblerSwipeBackgroundPreview() {
    TycheTheme {
        Box(modifier = Modifier.size(width = 120.dp, height = 64.dp)) {
            ManageGamblerSwipeBackground(isPastThreshold = false)
        }
    }
}

@PreviewLightDark
@Composable
private fun ManageGamblerSwipeBackgroundPastThresholdPreview() {
    TycheTheme {
        Box(modifier = Modifier.size(width = 120.dp, height = 64.dp)) {
            ManageGamblerSwipeBackground(isPastThreshold = true)
        }
    }
}
