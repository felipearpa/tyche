package com.felipearpa.tyche.bet.finished

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.revenuecat.placeholder.placeholder
import com.felipearpa.tyche.ui.R as SharedR

/**
 * The points the signed-in gambler has earned in the current pool: a large value and a smaller
 * label. The initial load renders this same component from [historyPointsPlaceholderModel] with
 * [isPlaceholder] set; its text is then masked with the shared pulse and it stays out of the
 * accessibility tree.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryPointsSummary(
    points: HistoryPoints,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
) {
    val leafMask = if (isPlaceholder) {
        val pulse = LocalLoadingPlaceholderPulse.current
        Modifier.placeholder(color = pulse.color, shape = pulse.shape, highlight = pulse.highlight)
    } else {
        Modifier
    }
    val announcement = historyPointsAnnouncement(points)

    // The label follows the value on one baseline and moves below it when they no longer fit.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(LABEL_SPACING),
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { if (!isPlaceholder) contentDescription = announcement },
    ) {
        Text(
            text = historyPointsShortText(points),
            fontSize = VALUE_SIZE,
            lineHeight = VALUE_LINE_HEIGHT,
            fontWeight = FontWeight.Bold,
            color = if (points.isPositive) HistoryStyle.positiveText else HistoryStyle.primaryText,
            style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
            modifier = Modifier.alignByBaseline().then(leafMask),
        )
        Text(
            text = stringResource(
                if (points == HistoryPoints.Unavailable) R.string.history_total_unavailable_label
                else R.string.history_earned_label,
            ),
            style = MaterialTheme.typography.titleLarge,
            color = HistoryStyle.secondaryText,
            modifier = Modifier.alignByBaseline().then(leafMask),
        )
    }
}

/** Stable filler for the initial summary placeholder; never shown as real content. */
val historyPointsPlaceholderModel: HistoryPoints = HistoryPoints.Earned(100)

@Composable
private fun historyPointsAnnouncement(points: HistoryPoints): String = when (points) {
    is HistoryPoints.Earned ->
        pluralStringResource(R.plurals.history_summary_points_accessibility, points.value, points.value)

    HistoryPoints.Unavailable -> stringResource(R.string.history_summary_unavailable_accessibility)
}

/**
 * History's summary for every state of the total: the placeholder while the first request runs,
 * the confirmed points (with refresh progress or a refresh failure beneath them), or a compact
 * error with a retry that requests only the total. Pull to refresh shows its own indicator, so
 * the inline progress appears only for a summary retry.
 */
@Composable
fun HistoryPointsHeader(
    state: HistoryPointsSummaryState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HistoryStyle.horizontalPadding)
            .padding(top = LocalBoxSpacing.current.medium, bottom = LocalBoxSpacing.current.large),
    ) {
        when (val presentation = state.presentation) {
            HistoryPointsSummaryState.Presentation.Placeholder ->
                HistoryPointsSummary(points = historyPointsPlaceholderModel, isPlaceholder = true)

            HistoryPointsSummaryState.Presentation.Failed ->
                SummaryFailure(message = stringResource(R.string.history_summary_load_failure), onRetry = onRetry)

            is HistoryPointsSummaryState.Presentation.Points -> {
                HistoryPointsSummary(points = presentation.points)
                RefreshStatus(
                    status = presentation.refreshStatus,
                    isPullRefreshing = state.isPullRefreshing,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun RefreshStatus(
    status: HistoryPointsSummaryState.RefreshStatus,
    isPullRefreshing: Boolean,
    onRetry: () -> Unit,
) {
    when (status) {
        HistoryPointsSummaryState.RefreshStatus.CURRENT -> Unit
        HistoryPointsSummaryState.RefreshStatus.REFRESHING -> if (!isPullRefreshing) RefreshProgress()
        HistoryPointsSummaryState.RefreshStatus.FAILED ->
            SummaryFailure(message = stringResource(R.string.history_summary_refresh_failure), onRetry = onRetry)
    }
}

@Composable
private fun RefreshProgress() {
    val label = stringResource(R.string.history_summary_refreshing)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        CircularProgressIndicator(modifier = Modifier.size(PROGRESS_SIZE), strokeWidth = 2.dp)
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = HistoryStyle.secondaryText)
    }
}

@Composable
private fun SummaryFailure(message: String, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium)) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = HistoryStyle.secondaryText)
        Button(onClick = onRetry) {
            Text(text = stringResource(SharedR.string.retry_action))
        }
    }
}

private val LABEL_SPACING = 8.dp
private val VALUE_SIZE = 52.sp
private val VALUE_LINE_HEIGHT = 60.sp
private val PROGRESS_SIZE = 16.dp

@PreviewLightDark
@Preview(locale = "es-rCO", fontScale = 2f)
@Composable
private fun HistoryPointsSummaryPreview() {
    TycheTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(16.dp)) {
                HistoryPointsSummary(points = HistoryPoints.Earned(120))
                HistoryPointsSummary(points = HistoryPoints.Earned(1))
                HistoryPointsSummary(points = HistoryPoints.Earned(0))
                HistoryPointsSummary(points = HistoryPoints.Unavailable)
                HistoryPointsSummary(points = historyPointsPlaceholderModel, isPlaceholder = true)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun HistoryPointsHeaderPreview() {
    TycheTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                val loaded = HistoryPointsSummaryState.initial.loaded(HistoryPoints.Earned(4))
                HistoryPointsHeader(
                    state = HistoryPointsSummaryState.initial.requesting(HistoryPointsSummaryState.Request.LOADING),
                    onRetry = {},
                )
                HistoryPointsHeader(state = HistoryPointsSummaryState.initial.failed(), onRetry = {})
                HistoryPointsHeader(
                    state = loaded.requesting(HistoryPointsSummaryState.Request.LOADING),
                    onRetry = {},
                )
                HistoryPointsHeader(state = loaded.failed(), onRetry = {})
            }
        }
    }
}
