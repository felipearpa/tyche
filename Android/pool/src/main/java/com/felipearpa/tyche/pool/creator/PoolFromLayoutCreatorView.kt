package com.felipearpa.tyche.pool.creator

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.ui.exception.ExceptionAlertDialog
import com.felipearpa.tyche.ui.exception.localizedOrDefault
import com.felipearpa.tyche.ui.loading.LoadingContainerView
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.ui.state.EditableViewState
import com.felipearpa.ui.state.isSuccess
import com.felipearpa.tyche.ui.R as SharedR

@Composable
fun PoolFromLayoutCreatorView(
    viewModel: PoolFromLayoutCreatorViewModel,
    onPoolCreated: (poolId: String) -> Unit,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    PoolFromLayoutCreatorView(
        state = state,
        onSaveClick = { newCreatePoolModel ->
            viewModel.createPool(newCreatePoolModel)
        },
        onPoolCreated = onPoolCreated,
        onBackClick = onBackClick,
        reset = viewModel::reset,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoolFromLayoutCreatorView(
    state: EditableViewState<CreatePoolModel>,
    onSaveClick: (createPoolModel: CreatePoolModel) -> Unit,
    onPoolCreated: (poolId: String) -> Unit,
    onBackClick: () -> Unit,
    reset: () -> Unit,
) {
    var step by remember { mutableStateOf<Step>(Step.One) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    var createPoolModel by remember { mutableStateOf(emptyCreatePoolModel()) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBar(
                title = { Text(text = stringResource(id = R.string.pool_from_layout_creator_title)) },
                scrollBehavior = scrollBehavior,
                onBackClick = {
                    when (step) {
                        Step.One -> onBackClick()
                        Step.Two -> step = Step.One
                    }
                },
            )
        },
    ) { paddingValues ->
        when (state) {
            is EditableViewState.Initial ->
                Stepper(
                    step = step,
                    onStepChange = { newStep -> step = newStep },
                    onSaveClick = { onSaveClick(createPoolModel) },
                    createPoolModel = createPoolModel,
                    onCreatePoolModelChange = { newCreatePoolModel ->
                        createPoolModel = newCreatePoolModel
                    },
                    modifier = Modifier
                        .padding(paddingValues = paddingValues)
                        .fillMaxSize()
                        .padding(all = LocalBoxSpacing.current.medium),
                )

            is EditableViewState.Saving -> LoadingContainerView {
                Stepper(
                    step = step,
                    onStepChange = {},
                    onSaveClick = {},
                    createPoolModel = createPoolModel,
                    onCreatePoolModelChange = {},
                    modifier = Modifier
                        .padding(paddingValues = paddingValues)
                        .fillMaxSize()
                        .padding(all = LocalBoxSpacing.current.medium),
                )
            }

            is EditableViewState.Success ->
                Stepper(
                    step = step,
                    onStepChange = {},
                    onSaveClick = {},
                    createPoolModel = createPoolModel,
                    onCreatePoolModelChange = {},
                    modifier = Modifier
                        .padding(paddingValues = paddingValues)
                        .fillMaxSize()
                        .padding(all = LocalBoxSpacing.current.medium),
                )

            is EditableViewState.Failure -> {
                Stepper(
                    step = step,
                    onStepChange = {},
                    onSaveClick = {},
                    createPoolModel = createPoolModel,
                    onCreatePoolModelChange = {},
                    modifier = Modifier
                        .padding(paddingValues = paddingValues)
                        .fillMaxSize()
                        .padding(all = LocalBoxSpacing.current.medium),
                )
                ExceptionAlertDialog(
                    exception = state.exception.localizedOrDefault(),
                    onDismiss = { reset() },
                )
            }
        }
    }

    LaunchedEffect(state) {
        if (state.isSuccess()) {
            state.succeeded.poolId?.let { poolId ->
                onPoolCreated(poolId)
            }
        }
    }
}

@Composable
private fun Stepper(
    step: Step,
    onStepChange: (Step) -> Unit,
    onSaveClick: () -> Unit,
    createPoolModel: CreatePoolModel,
    onCreatePoolModelChange: (CreatePoolModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(targetState = step) { currentStep ->
        when (currentStep) {
            Step.One -> StepOneView(
                viewModel = stepOneViewModel(),
                createPoolModel = createPoolModel,
                onNextClick = { newCreatePoolModel ->
                    onCreatePoolModelChange(newCreatePoolModel)
                    onStepChange(Step.Two)
                },
                modifier = modifier,
            )

            Step.Two -> StepTwoView(
                createPoolModel = createPoolModel,
                onSaveClick = { newCreatePoolModel ->
                    onCreatePoolModelChange(newCreatePoolModel)
                    onSaveClick()
                },
                modifier = modifier,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    title: @Composable () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    onBackClick: () -> Unit,
) {
    TopAppBar(
        title = title,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(SharedR.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}

private sealed class Step {
    object One : Step()
    object Two : Step()
}
