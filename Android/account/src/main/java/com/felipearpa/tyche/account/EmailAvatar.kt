package com.felipearpa.tyche.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.ui.theme.TycheTheme
import org.koin.compose.koinInject

@Composable
fun AutoEmailAvatar(modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) {
        EmailAvatar(
            email = "felipearpa@tyche.com",
            modifier = modifier,
        )
        return
    }

    val accountStorage = koinInject<AccountStorage>()
    val account by accountStorage.state.collectAsStateWithLifecycle()
    AccountAvatar(
        accountId = account?.accountId.orEmpty(),
        email = account?.email.orEmpty(),
        modifier = modifier,
    )
}

@Composable
fun EmailAvatar(email: String, modifier: Modifier = Modifier) {
    InitialAvatar(
        identity = email.substringBefore('@'),
        colorKey = email,
        modifier = modifier,
    )
}

fun Modifier.navigationEmailAvatar() =
    size(NAVIGATION_AVATAR_SIZE.dp)
        .clip(CircleShape)

private const val NAVIGATION_AVATAR_SIZE = 32

@PreviewLightDark
@Composable
private fun EmailAvatarPreview() {
    TycheTheme {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            EmailAvatar(
                email = "felipearpa@email.com",
                modifier = Modifier.size(64.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun EmailAvatarFallbackPreview() {
    TycheTheme {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            EmailAvatar(
                email = "@email.com",
                modifier = Modifier.size(64.dp),
            )
        }
    }
}
