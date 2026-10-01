package com.felipearpa.tyche.home

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.R
import com.felipearpa.tyche.account.bygoogle.GoogleSignInState
import com.felipearpa.tyche.account.social.SocialSignInRow
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme

@Composable
fun HomeView(
    onSignInWithEmail: (() -> Unit)?,
    onSignInWithEmailAndPassword: (() -> Unit)?,
    socialSignInSlot: @Composable () -> Unit,
) {
    HomeView(
        onSignInWithEmail = onSignInWithEmail,
        onSignInWithEmailAndPassword = onSignInWithEmailAndPassword,
        socialSignInSlot = socialSignInSlot,
        modifier = Modifier
            .fillMaxSize()
            .padding(all = LocalBoxSpacing.current.medium),
    )
}

@Composable
private fun HomeView(
    onSignInWithEmail: (() -> Unit)?,
    onSignInWithEmailAndPassword: (() -> Unit)?,
    socialSignInSlot: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.surface,
        ),
    )

    LightStatusBarIcons()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
    ) {
        Scaffold(containerColor = Color.Transparent) { innerPadding ->
            // The sections spread over the safe area; when they don't fit (landscape, a short
            // window, large text), they scroll so the sign-in actions stay reachable.
            BoxWithConstraints(
                modifier = modifier
                    .padding(paddingValues = innerPadding)
                    .consumeWindowInsets(paddingValues = innerPadding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(all = LocalBoxSpacing.current.large),
                    verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
                ) {
                    HeaderSection()
                    Spacer(modifier = Modifier.weight(1f))
                    InformationSection()
                    Spacer(modifier = Modifier.weight(1f))
                    SignInSection(
                        onSignInWithEmail = onSignInWithEmail,
                        onSignInWithEmailAndPassword = onSignInWithEmailAndPassword,
                        socialSignInSlot = socialSignInSlot,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            LocalBoxSpacing.current.small,
            Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_tyche_logo),
            contentDescription = emptyString(),
            tint = headerColor,
            modifier = Modifier.size(titleIconSize),
        )

        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = R.drawable.tyche_title),
                contentDescription = emptyString(),
                tint = headerColor,
                modifier = Modifier.height(titleIconSize / 2),
            )
        }
    }
}

@Composable
private fun InformationSection() {
    Box {
        Text(
            text = stringResource(id = R.string.play_pool_text),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SignInSection(
    onSignInWithEmail: (() -> Unit)?,
    onSignInWithEmailAndPassword: (() -> Unit)?,
    socialSignInSlot: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
    ) {
        Text(
            text = stringResource(id = R.string.continue_with_text),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(LocalBoxSpacing.current.small))

        socialSignInSlot()

        Button(
            onClick = onSignInWithEmail ?: {},
            enabled = onSignInWithEmail != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = R.string.sign_in_with_email_action))
        }

        OutlinedButton(
            onClick = onSignInWithEmailAndPassword ?: {},
            enabled = onSignInWithEmailAndPassword != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = R.string.sign_in_with_email_and_password))
        }
    }
}

/**
 * Light status bar icons while the welcome screen shows, since its gradient starts from the dark
 * primary container in both themes; the activity's theme-based icons return when it leaves.
 */
@Composable
private fun LightStatusBarIcons() {
    val window = LocalActivity.current?.window ?: return
    val view = LocalView.current
    DisposableEffect(window, view) {
        val controller = WindowCompat.getInsetsController(window, view)
        val hadLightStatusBars = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = hadLightStatusBars }
    }
}

// The header sits on the top of the gradient, the dark primary container in both themes.
private val headerColor = Color.White

private val titleIconSize = 64.dp

@PreviewLightDark
@Composable
fun HomeViewWithSocialInitialPreview() {
    TycheTheme(dynamicColor = false) {
        Surface {
            HomeView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = LocalBoxSpacing.current.medium),
                onSignInWithEmail = {},
                onSignInWithEmailAndPassword = {},
                socialSignInSlot = {
                    SocialSignInRow(
                        googleState = GoogleSignInState.Idle,
                        onSignInWithGoogle = {},
                        onDismissGoogleFailure = {},
                        onAuthenticate = {},
                        onAuthenticationHandled = {},
                    )
                },
            )
        }
    }
}

@PreviewLightDark
@Composable
fun HomeViewWithSocialLoadingPreview() {
    TycheTheme(dynamicColor = false) {
        Surface {
            HomeView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = LocalBoxSpacing.current.medium),
                onSignInWithEmail = {},
                onSignInWithEmailAndPassword = {},
                socialSignInSlot = {
                    SocialSignInRow(
                        googleState = GoogleSignInState.InProgress,
                        onSignInWithGoogle = {},
                        onDismissGoogleFailure = {},
                        onAuthenticate = {},
                        onAuthenticationHandled = {},
                    )
                },
            )
        }
    }
}

@PreviewLightDark
@Composable
fun HomeViewWithSocialFailurePreview() {
    TycheTheme(dynamicColor = false) {
        Surface {
            HomeView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = LocalBoxSpacing.current.medium),
                onSignInWithEmail = {},
                onSignInWithEmailAndPassword = {},
                socialSignInSlot = {
                    SocialSignInRow(
                        googleState = GoogleSignInState.Failed(UnknownLocalizedException()),
                        onSignInWithGoogle = {},
                        onDismissGoogleFailure = {},
                        onAuthenticate = {},
                        onAuthenticationHandled = {},
                    )
                },
            )
        }
    }
}
