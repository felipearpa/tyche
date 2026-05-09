package com.felipearpa.tyche

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.R as SharedR

@Composable
fun AccountHeaderDrawer(email: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
    ) {
        Box(
            modifier = Modifier
                .size(AVATAR_SIZE.dp)
                .clip(CircleShape)
                .border(
                    width = AVATAR_RING_WIDTH.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = SharedR.drawable.filled_person),
                contentDescription = emptyString(),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(AVATAR_ICON_SIZE.dp),
            )
        }

        Text(
            text = email,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = stringResource(id = R.string.connected_account_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.primary,
            thickness = ACCENT_LINE_HEIGHT.dp,
            modifier = Modifier
                .width(ACCENT_LINE_WIDTH.dp)
                .clip(RoundedCornerShape(ACCENT_LINE_HEIGHT.dp)),
        )
    }
}

private const val AVATAR_SIZE = 96
private const val AVATAR_ICON_SIZE = 56
private const val AVATAR_RING_WIDTH = 3
private const val ACCENT_LINE_WIDTH = 48
private const val ACCENT_LINE_HEIGHT = 3
