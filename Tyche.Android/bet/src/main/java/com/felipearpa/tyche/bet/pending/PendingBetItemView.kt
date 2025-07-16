package com.felipearpa.tyche.bet.pending

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.awayTeamBetRawValue
import com.felipearpa.tyche.bet.homeTeamBetRawValue
import com.felipearpa.tyche.bet.poolGamblerBetDummyModel
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.loading.BallSpinner
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.ui.state.EditableViewState
import com.felipearpa.ui.state.relevantValue
import com.felipearpa.tyche.ui.R as SharedR

private val ICON_SIZE = 24.dp

@Composable
fun PendingBetItemView(viewModel: PendingBetItemViewModel, modifier: Modifier = Modifier) {
    val viewModelState by viewModel.state.collectAsState()
    var viewState by remember { mutableStateOf(PendingBetItemViewState.emptyVisualization()) }

    PendingBetItemView(
        viewModelState = viewModelState,
        viewState = viewState,
        onViewStateChanged = { newViewState -> viewState = newViewState },
        bet = {
            viewModel.bet(
                TeamScore(
                    homeTeamValue = viewState.value.homeTeamBet.toInt(),
                    awayTeamValue = viewState.value.awayTeamBet.toInt(),
                ),
            )
        },
        reset = viewModel::reset,
        retryBet = viewModel::retryBet,
        edit = { viewState = PendingBetItemViewState.Edition(viewState.value) },
        modifier = modifier,
    )

    LaunchedEffect(viewModelState) {
        viewState = when (val localViewModelState = viewModelState) {
            is EditableViewState.Initial -> {
                val poolGamblerBet = localViewModelState.value
                PendingBetItemViewState.Visualization(
                    PartialPoolGamblerBetModel(
                        homeTeamBet = poolGamblerBet.homeTeamBetRawValue(),
                        awayTeamBet = poolGamblerBet.awayTeamBetRawValue(),
                    ),
                )
            }

            else -> PendingBetItemViewState.Visualization(viewState.value)
        }
    }
}

@Composable
private fun PendingBetItemView(
    modifier: Modifier = Modifier,
    viewModelState: EditableViewState<PoolGamblerBetModel>,
    viewState: PendingBetItemViewState,
    onViewStateChanged: (PendingBetItemViewState) -> Unit = {},
    bet: () -> Unit = {},
    reset: () -> Unit = {},
    retryBet: () -> Unit = {},
    edit: () -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
    ) {
        when (viewModelState) {
            is EditableViewState.Initial, is EditableViewState.Success -> {
                val poolGamblerBet = when (viewModelState) {
                    is EditableViewState.Initial -> viewModelState.value
                    is EditableViewState.Success -> viewModelState.succeeded
                    else -> return
                }
                PendingBetItem(
                    poolGamblerBet = poolGamblerBet,
                    viewState = viewState,
                    onBetChanged = { newPartialPoolGamblerBet ->
                        onViewStateChanged(
                            viewState.copy(
                                newPartialPoolGamblerBet,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                DefaultActionBar(
                    viewModelState = viewModelState,
                    viewState = viewState,
                    bet = bet,
                    reset = reset,
                    edit = edit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is EditableViewState.Saving -> {
                PendingBetItem(
                    poolGamblerBet = viewModelState.target,
                    viewState = PendingBetItemViewState.Visualization(viewState.value),
                    modifier = Modifier.fillMaxWidth(),
                )
                LoadingActionBar(
                    viewModelState = viewModelState,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is EditableViewState.Failure -> {
                PendingBetItem(
                    poolGamblerBet = viewModelState.failed,
                    viewState = PendingBetItemViewState.Visualization(viewState.value),
                    modifier = Modifier.fillMaxWidth(),
                )
                FailureActionBar(
                    viewModelState = viewModelState,
                    retryBet = retryBet,
                    reset = reset,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DefaultActionBar(
    viewModelState: EditableViewState<PoolGamblerBetModel>,
    viewState: PendingBetItemViewState,
    bet: () -> Unit,
    reset: () -> Unit,
    edit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (viewState is PendingBetItemViewState.Edition) {
        EditableDefaultActionBar(
            viewModelState = viewModelState,
            viewState = viewState,
            bet = bet,
            reset = reset,
            modifier = modifier,
        )
    } else {
        NonEditableDefaultActionBar(
            viewModelState = viewModelState,
            viewState = viewState,
            edit = edit,
            modifier = modifier,
        )
    }
}

@Composable
private fun EditableDefaultActionBar(
    viewModelState: EditableViewState<PoolGamblerBetModel>,
    viewState: PendingBetItemViewState,
    bet: () -> Unit,
    reset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val poolGamblerBet = viewModelState.relevantValue()
    var isRecomposition by remember { mutableStateOf(false) }
    var maxButtonWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val buttonWidthModifier = if (isRecomposition) Modifier.width(maxButtonWidth) else Modifier

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        StateIndicator(viewModelState = viewModelState, isEditable = true)

        Row(horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium)) {
            FilledTonalButton(
                onClick = reset,
                modifier = Modifier
                    .onSizeChanged { intSize ->
                        if (!isRecomposition) {
                            maxButtonWidth =
                                max(maxButtonWidth, with(density) { intSize.width.toDp() })
                        }
                    }
                    .then(buttonWidthModifier),
            ) {
                Text(text = stringResource(id = SharedR.string.cancel_action))
            }

            Button(
                onClick = bet,
                enabled = viewState.value != PartialPoolGamblerBetModel(
                    homeTeamBet = poolGamblerBet.homeTeamBetRawValue(),
                    awayTeamBet = poolGamblerBet.awayTeamBetRawValue(),
                ),
                modifier = Modifier
                    .onSizeChanged { intSize ->
                        if (!isRecomposition) {
                            maxButtonWidth =
                                max(maxButtonWidth, with(density) { intSize.width.toDp() })
                        }
                    }
                    .then(buttonWidthModifier),
            ) {
                Text(text = stringResource(id = SharedR.string.save_action))
            }
        }
    }

    LaunchedEffect(Unit) {
        isRecomposition = true
    }
}

@Composable
private fun NonEditableDefaultActionBar(
    viewModelState: EditableViewState<PoolGamblerBetModel>,
    viewState: PendingBetItemViewState,
    edit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        StateIndicator(
            viewModelState = viewModelState,
            isEditable = viewState is PendingBetItemViewState.Edition,
        )

        TextButton(onClick = edit) {
            Text(text = stringResource(id = SharedR.string.edit_action))
        }
    }
}

@Composable
private fun LoadingActionBar(
    viewModelState: EditableViewState<PoolGamblerBetModel>,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        StateIndicator(viewModelState = viewModelState)
    }
}

@Composable
private fun FailureActionBar(
    viewModelState: EditableViewState<PoolGamblerBetModel>,
    retryBet: () -> Unit,
    reset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isRecomposition by remember { mutableStateOf(false) }
    var maxButtonWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val buttonWidthModifier = if (isRecomposition) Modifier.width(maxButtonWidth) else Modifier

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StateIndicator(viewModelState = viewModelState)

        Row(horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium)) {
            FilledTonalButton(
                onClick = reset,
                modifier = Modifier
                    .onSizeChanged { intSize ->
                        if (!isRecomposition) {
                            maxButtonWidth =
                                max(maxButtonWidth, with(density) { intSize.width.toDp() })
                        }
                    }
                    .then(buttonWidthModifier),
            ) {
                Text(text = stringResource(id = SharedR.string.cancel_action))
            }

            Button(
                onClick = retryBet,
                modifier = Modifier
                    .onSizeChanged { intSize ->
                        if (!isRecomposition) {
                            maxButtonWidth =
                                max(maxButtonWidth, with(density) { intSize.width.toDp() })
                        }
                    }
                    .then(buttonWidthModifier),
            ) {
                Text(text = stringResource(id = SharedR.string.retry_action))
            }
        }
    }
    LaunchedEffect(Unit) {
        isRecomposition = true
    }
}

@Composable
private fun StateIndicator(
    viewModelState: EditableViewState<PoolGamblerBetModel>,
    isEditable: Boolean = false,
) {
    if (isEditable) {
        Icon(
            painter = painterResource(id = SharedR.drawable.pending),
            contentDescription = emptyString(),
        )
    } else {
        when (viewModelState) {
            is EditableViewState.Failure ->
                Icon(
                    painter = painterResource(id = SharedR.drawable.error),
                    contentDescription = emptyString(),
                    tint = MaterialTheme.colorScheme.error,
                )

            is EditableViewState.Saving -> BallSpinner(
                modifier = Modifier.size(ICON_SIZE),
            )

            is EditableViewState.Initial, is EditableViewState.Success -> {
                val poolGamblerBet = when (viewModelState) {
                    is EditableViewState.Initial -> viewModelState.value
                    is EditableViewState.Success -> viewModelState.succeeded
                    else -> return
                }
                ContentIndicator(poolGamblerBet = poolGamblerBet)
            }
        }
    }
}

@Composable
private fun ContentIndicator(poolGamblerBet: PoolGamblerBetModel) {
    if (poolGamblerBet.isLocked) {
        Icon(
            painter = painterResource(id = SharedR.drawable.pending),
            contentDescription = emptyString(),
        )
    } else {
        Icon(
            painter = painterResource(id = SharedR.drawable.done),
            contentDescription = emptyString(),
        )
    }
}

@Preview
@Composable
private fun NonEditableInitialPendingBetItemViewPreview() {
    MaterialTheme {
        Surface {
            PendingBetItemView(
                viewModelState = EditableViewState.Initial(poolGamblerBetDummyModel()),
                viewState = PendingBetItemViewState.Visualization(
                    partialPoolGamblerBetDummyModel(),
                ),
            )
        }
    }
}

@Preview
@Composable
private fun EditableInitialPendingBetItemViewPreview() {
    MaterialTheme {
        Surface {
            PendingBetItemView(
                viewModelState = EditableViewState.Initial(poolGamblerBetDummyModel()),
                viewState = PendingBetItemViewState.Edition(partialPoolGamblerBetDummyModel()),
            )
        }
    }
}

@Preview
@Composable
private fun NonEditableLoadingPendingBetItemViewPreview() {
    MaterialTheme {
        Surface {
            PendingBetItemView(
                viewModelState = EditableViewState.Saving(
                    current = poolGamblerBetDummyModel(),
                    target = poolGamblerBetDummyModel(),
                ),
                viewState = PendingBetItemViewState.Visualization(
                    partialPoolGamblerBetDummyModel(),
                ),
            )
        }
    }
}

@Preview
@Composable
private fun EditableLoadingPendingBetItemViewPreview() {
    MaterialTheme {
        Surface {
            PendingBetItemView(
                viewModelState = EditableViewState.Saving(
                    current = poolGamblerBetDummyModel(),
                    target = poolGamblerBetDummyModel(),
                ),
                viewState = PendingBetItemViewState.Edition(partialPoolGamblerBetDummyModel()),
            )
        }
    }
}

@Preview
@Composable
private fun NonEditableFailurePendingBetItemViewPreview() {
    MaterialTheme {
        Surface {
            PendingBetItemView(
                viewModelState = EditableViewState.Failure(
                    current = poolGamblerBetDummyModel(),
                    failed = poolGamblerBetDummyModel(),
                    exception = UnknownLocalizedException(),
                ),
                viewState = PendingBetItemViewState.Visualization(
                    partialPoolGamblerBetDummyModel(),
                ),
            )
        }
    }
}

@Preview
@Composable
private fun EditableFailurePendingBetItemViewPreview() {
    MaterialTheme {
        Surface {
            PendingBetItemView(
                viewModelState = EditableViewState.Failure(
                    current = poolGamblerBetDummyModel(),
                    failed = poolGamblerBetDummyModel(),
                    exception = UnknownLocalizedException(),
                ),
                viewState = PendingBetItemViewState.Edition(partialPoolGamblerBetDummyModel()),
            )
        }
    }
}
