package com.felipearpa.tyche.pool.creator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.data.pool.type.PoolName
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.R as SharedR

/**
 * The pool-name form.
 *
 * [contentPadding] is the screen's inset padding, already consumed by the caller. The form is
 * padded by it and then fits inside the IME ruler: with the keyboard closed that changes nothing,
 * and with it open the form ends at the keyboard's top edge instead of the navigation bar's, so
 * the two heights are not added together. The form scrolls within what remains, with the design
 * spacing inside the scrolling content. [modifier] must give the form a fixed size, such as
 * `fillMaxSize()`, for the keyboard fitting to apply.
 */
@Composable
internal fun StepTwoView(
    createPoolModel: CreatePoolModel,
    onSaveClick: (createPoolModel: CreatePoolModel) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var poolName by remember { mutableStateOf(createPoolModel.poolName) }
    val isValid by remember { derivedStateOf { poolName.isNotBlank() } }

    Column(
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.large),
        modifier = modifier
            .padding(contentPadding)
            .fitInside(WindowInsetsRulers.Ime.current)
            .verticalScroll(rememberScrollState())
            .padding(LocalBoxSpacing.current.medium),
    ) {
        Text(
            text = stringResource(id = R.string.fill_pool_fields_title),
            style = MaterialTheme.typography.titleMedium,
        )

        FormBody(
            poolName = poolName,
            onPoolNameChange = { newPoolName -> poolName = newPoolName },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            enabled = isValid,
            onClick = {
                onSaveClick(createPoolModel.copy(poolName = poolName))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painter = painterResource(SharedR.drawable.done), contentDescription = null)
                Text(text = stringResource(SharedR.string.done_action))
            }
        }
    }
}

@Composable
private fun FormBody(
    poolName: String,
    onPoolNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        PoolNameTextField(
            value = poolName,
            onValueChange = { newPoolName -> onPoolNameChange(newPoolName) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PoolNameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isTouched by interactionSource.collectIsFocusedAsState()
    val shouldShowError by remember(value, isTouched) {
        derivedStateOf { isTouched && !PoolName.isValid(value) }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue -> onValueChange(newValue) },
            label = {
                Text(text = stringResource(id = R.string.pool_name_label))
            },
            interactionSource = interactionSource,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        AnimatedVisibility(visible = shouldShowError) {
            Text(
                text = stringResource(id = R.string.pool_name_length_validation_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StepTwoViewPreview() {
    StepTwoView(
        createPoolModel = emptyCreatePoolModel(),
        onSaveClick = {},
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.fillMaxSize(),
    )
}
