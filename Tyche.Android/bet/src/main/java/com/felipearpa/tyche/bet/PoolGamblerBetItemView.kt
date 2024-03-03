package com.felipearpa.tyche.bet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.progress.BallSpinner
import com.felipearpa.tyche.ui.state.EditableViewState
import com.felipearpa.tyche.ui.state.currentValue
import com.felipearpa.tyche.ui.theme.boxSpacing
import com.felipearpa.tyche.ui.R as SharedR

private val iconSize = 24.dp

@Composable
fun PoolGamblerBetItemView(viewModel: PoolGamblerBetItemViewModel, modifier: Modifier = Modifier) {
    val viewModelState by viewModel.state.collectAsState()
    var viewState by remember { mutableStateOf(PoolGamblerBetItemViewState.emptyVisualization()) }

    PoolGamblerBetItemView(
        viewModelState = viewModelState,
        viewState = viewState,
        onViewStateChanged = { newViewState -> viewState = newViewState },
        bet = {
            viewModel.bet(
                TeamScore(
                    homeTeamValue = viewState.value.homeTeamBet.toInt(),
                    awayTeamValue = viewState.value.awayTeamBet.toInt()
                )
            )
        },
        reset = viewModel::reset,
        retryBet = viewModel::retryBet,
        edit = { viewState = PoolGamblerBetItemViewState.Edition(viewState.value) },
        modifier = modifier
    )

    LaunchedEffect(viewModelState) {
        viewState = when (val localViewModelState = viewModelState) {
            is EditableViewState.Initial -> {
                val poolGamblerBet = localViewModelState.value
                PoolGamblerBetItemViewState.Visualization(
                    PartialPoolGamblerBetModel(
                        homeTeamBet = poolGamblerBet.homeTeamBetRawValue(),
                        awayTeamBet = poolGamblerBet.awayTeamBetRawValue()
                    )
                )
            }

            else -> PoolGamblerBetItemViewState.Visualization(viewState.value)
        }
    }
}

@Composable
private fun PoolGamblerBetItemView(
    modifier: Modifier = Modifier,
    viewModelState: EditableViewState<PoolGamblerBetModel>,
    viewState: PoolGamblerBetItemViewState,
    onViewStateChanged: (PoolGamblerBetItemViewState) -> Unit = {},
    bet: () -> Unit = {},
    reset: () -> Unit = {},
    retryBet: () -> Unit = {},
    edit: () -> Unit = {}
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.boxSpacing.medium)
    ) {
        when (viewModelState) {
            is EditableViewState.Initial, is EditableViewState.Success -> {
                val poolGamblerBet = when (viewModelState) {
                    is EditableViewState.Initial -> viewModelState.value
                    is EditableViewState.Success -> viewModelState.succeeded
                    else -> return
                }
                PoolGamblerBetItem(
                    poolGamblerBet = poolGamblerBet,
                    viewState = viewState,
                    onBetChanged = { newPartialPoolGamblerBet ->
                        onViewStateChanged(
                            viewState.copy(
                                newPartialPoolGamblerBet
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                DefaultActionBar(
                    viewModelState = viewModelState,
                    viewState = viewState,
                    bet = bet,
                    reset = reset,
                    edit = edit,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            is EditableViewState.Loading -> {
                PoolGamblerBetItem(
                    poolGamblerBet = viewModelState.target,
                    viewState = PoolGamblerBetItemViewState.Visualization(viewState.value),
                    modifier = Modifier.fillMaxWidth()
                )
                LoadingActionBar(
                    viewModelState = viewModelState,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            is EditableViewState.Failure -> {
                PoolGamblerBetItem(
                    poolGamblerBet = viewModelState.failed,
                    viewState = PoolGamblerBetItemViewState.Visualization(viewState.value),
                    modifier = Modifier.fillMaxWidth()
                )
                FailureActionBar(
                    viewModelState = viewModelState,
                    retryBet = retryBet,
                    reset = reset,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DefaultActionBar(
    viewModelState: EditableViewState<PoolGamblerBetModel>,
    viewState: PoolGamblerBetItemViewState,
    bet: () -> Unit,
    reset: () -> Unit,
    edit: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (viewState is PoolGamblerBetItemViewState.Edition) {
        EditableDefaultActionBar(
            viewModelState = viewModelState,
            viewState = viewState,
            bet = bet,
            reset = reset,
            modifier = modifier
        )
    } else {
        NonEditableDefaultActionBar(
            viewModelState = viewModelState,
            viewState = viewState,
            edit = edit,
            modifier = modifier
        )
    }
}

@Composable
private fun EditableDefaultActionBar(
    viewModelState: EditableViewState<PoolGamblerBetModel>,
    viewState: PoolGamblerBetItemViewState,
    bet: () -> Unit,
    reset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val poolGamblerBet = viewModelState.currentValue()

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        StateIndicator(viewModelState = viewModelState, isEditable = true)

        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.boxSpacing.medium)) {
            OutlinedButton(onClick = reset) {
                Text(text = stringResource(id = SharedR.string.cancel_action))
            }

            Button(
                onClick = bet,
                enabled = viewState.value != PartialPoolGamblerBetModel(
                    homeTeamBet = poolGamblerBet.homeTeamBetRawValue(),
                    awayTeamBet = poolGamblerBet.awayTeamBetRawValue()
                )
            ) {
                Text(text = stringResource(id = SharedR.string.save_action))
            }
        }
    }
}

@Composable
private fun NonEditableDefaultActionBar(
    viewModelState: EditableViewState<PoolGamblerBetModel>,
    viewState: PoolGamblerBetItemViewState,
    edit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        StateIndicator(
            viewModelState = viewModelState,
            isEditable = viewState is PoolGamblerBetItemViewState.Edition
        )

        OutlinedButton(onClick = edit) {
            Text(text = stringResource(id = SharedR.string.edit_action))
        }
    }
}

@Composable
private fun LoadingActionBar(
    viewModelState: EditableViewState<PoolGamblerBetModel>,
    modifier: Modifier = Modifier
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StateIndicator(viewModelState = viewModelState)

        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.boxSpacing.medium)) {
            OutlinedButton(onClick = reset) {
                Text(text = stringResource(id = SharedR.string.cancel_action))
            }

            Button(onClick = retryBet) {
                Text(text = stringResource(id = SharedR.string.retry_action))
            }
        }
    }
}

@Composable
private fun StateIndicator(
    viewModelState: EditableViewState<PoolGamblerBetModel>,
    isEditable: Boolean = false
) {
    if (isEditable) {
        Icon(
            painter = painterResource(id = SharedR.drawable.pending),
            contentDescription = emptyString()
        )
    } else {
        when (viewModelState) {
            is EditableViewState.Failure ->
                Icon(
                    painter = painterResource(id = SharedR.drawable.error),
                    contentDescription = emptyString(),
                    tint = MaterialTheme.colorScheme.error
                )

            is EditableViewState.Loading -> BallSpinner(
                modifier = Modifier.size(iconSize)
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
            contentDescription = emptyString()
        )
    } else {
        Icon(
            painter = painterResource(id = SharedR.drawable.done),
            contentDescription = emptyString()
        )
    }
}

@Preview
@Composable
private fun NonEditableInitialPoolGamblerBetItemViewPreview() {
    MaterialTheme {
        Surface {
            PoolGamblerBetItemView(
                viewModelState = EditableViewState.Initial(poolGamblerBetDummyModel()),
                viewState = PoolGamblerBetItemViewState.Visualization(
                    partialPoolGamblerBetDummyModel()
                )
            )
        }
    }
}

@Preview
@Composable
private fun EditableInitialPoolGamblerBetItemViewPreview() {
    MaterialTheme {
        Surface {
            PoolGamblerBetItemView(
                viewModelState = EditableViewState.Initial(poolGamblerBetDummyModel()),
                viewState = PoolGamblerBetItemViewState.Edition(partialPoolGamblerBetDummyModel())
            )
        }
    }
}

@Preview
@Composable
private fun NonEditableLoadingPoolGamblerBetItemViewPreview() {
    MaterialTheme {
        Surface {
            PoolGamblerBetItemView(
                viewModelState = EditableViewState.Loading(
                    current = poolGamblerBetDummyModel(),
                    target = poolGamblerBetDummyModel()
                ),
                viewState = PoolGamblerBetItemViewState.Visualization(
                    partialPoolGamblerBetDummyModel()
                )
            )
        }
    }
}

@Preview
@Composable
private fun EditableLoadingPoolGamblerBetItemViewPreview() {
    MaterialTheme {
        Surface {
            PoolGamblerBetItemView(
                viewModelState = EditableViewState.Loading(
                    current = poolGamblerBetDummyModel(),
                    target = poolGamblerBetDummyModel()
                ),
                viewState = PoolGamblerBetItemViewState.Edition(partialPoolGamblerBetDummyModel())
            )
        }
    }
}

@Preview
@Composable
private fun NonEditableFailurePoolGamblerBetItemViewPreview() {
    MaterialTheme {
        Surface {
            PoolGamblerBetItemView(
                viewModelState = EditableViewState.Failure(
                    current = poolGamblerBetDummyModel(),
                    failed = poolGamblerBetDummyModel(),
                    exception = UnknownLocalizedException()
                ),
                viewState = PoolGamblerBetItemViewState.Visualization(
                    partialPoolGamblerBetDummyModel()
                ),
                onViewStateChanged = {},
                bet = {},
                reset = {},
                retryBet = {},
                edit = {}
            )
        }
    }
}

@Preview
@Composable
private fun EditableFailurePoolGamblerBetItemViewPreview() {
    MaterialTheme {
        Surface {
            PoolGamblerBetItemView(
                viewModelState = EditableViewState.Failure(
                    current = poolGamblerBetDummyModel(),
                    failed = poolGamblerBetDummyModel(),
                    exception = UnknownLocalizedException()
                ),
                viewState = PoolGamblerBetItemViewState.Edition(partialPoolGamblerBetDummyModel()),
                onViewStateChanged = {},
                bet = {},
                reset = {},
                retryBet = {},
                edit = {}
            )
        }
    }
}