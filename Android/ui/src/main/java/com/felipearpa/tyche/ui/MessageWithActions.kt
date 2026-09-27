package com.felipearpa.tyche.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * A screen-filling [message] with its [actions] below it, as in a confirmation or failure screen.
 * The message is centered in the space the actions leave and the actions rest at the bottom.
 *
 * When both don't fit (large text, a short or landscape window), they scroll together instead of
 * the message spilling over the actions, so every action stays reachable. Safe-area padding
 * belongs on [modifier]; the scrolling area stays inside it.
 */
@Composable
fun MessageWithActions(
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit,
    message: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Spacers rather than a weighted message: a weighted child gets no height once the
            // content overflows the viewport, while spacers just shrink to nothing.
            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = message,
            )

            Spacer(modifier = Modifier.weight(1f))

            actions()
        }
    }
}
