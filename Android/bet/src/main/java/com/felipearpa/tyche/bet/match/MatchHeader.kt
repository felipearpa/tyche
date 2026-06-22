package com.felipearpa.tyche.bet.match

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.felipearpa.foundation.time.toShortDateTimeString
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.bet.isLive
import com.felipearpa.tyche.bet.poolGamblerBetDummyModel
import com.felipearpa.tyche.bet.poolGamblerBetFakeModel
import com.felipearpa.tyche.ui.FlagImage
import com.felipearpa.tyche.ui.shimmer
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme

@Composable
fun MatchHeader(
    bet: PoolGamblerBetModel,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LocalBoxSpacing.current.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
    ) {
        if (bet.isLive) {
            LiveIndicator(shimmerModifier = shimmerModifier)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                FlagImage(
                    teamId = bet.homeTeamId,
                    modifier = Modifier
                        .size(flagSize)
                        .then(shimmerModifier),
                )
                Text(
                    text = bet.homeTeamName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .then(shimmerModifier),
                )
            }

            if (bet.isComputed) {
                Text(
                    text = bet.matchScore?.homeTeamValue?.toString().orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = shimmerModifier,
                )

                Text(text = "-")

                Text(
                    text = bet.matchScore?.awayTeamValue?.toString().orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = shimmerModifier,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small, alignment = Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = bet.awayTeamName,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .then(shimmerModifier),
                )
                FlagImage(
                    teamId = bet.awayTeamId,
                    modifier = Modifier
                        .size(flagSize)
                        .then(shimmerModifier),
                )
            }
        }

        Text(
            text = bet.matchDateTime.toShortDateTimeString(),
            style = MaterialTheme.typography.bodySmall,
            modifier = shimmerModifier,
        )
    }
}

@Composable
private fun LiveIndicator(
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "live-alpha",
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(alpha)
                .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                .then(shimmerModifier),
        )
        Text(
            text = androidx.compose.ui.res.stringResource(id = R.string.live_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = shimmerModifier,
        )
    }
}

@Composable
fun MatchHeaderPlaceholderItem(modifier: Modifier = Modifier) {
    MatchHeader(
        bet = poolGamblerBetFakeModel().copy(isLocked = false, isComputed = false),
        modifier = modifier,
        shimmerModifier = Modifier.shimmer(),
    )
}

private val flagSize = 48.dp

@PreviewLightDark
@Composable
private fun MatchHeaderPreview() {
    TycheTheme {
        Surface {
            MatchHeader(
                bet = poolGamblerBetDummyModel().copy(isLocked = true, isComputed = true),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MatchHeaderPlaceholderItemPreview() {
    MatchHeaderPlaceholderItem(modifier = Modifier.fillMaxWidth())
}
