package com.felipearpa.tyche.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.R
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme

@Composable
fun HomeView(onSignInWithEmail: () -> Unit, onSignInWithEmailAndPassword: () -> Unit) {
    HomeView(
        onSignInWithEmail = onSignInWithEmail,
        onSignInWithEmailAndPassword = onSignInWithEmailAndPassword,
        modifier = Modifier
            .fillMaxSize()
            .padding(all = LocalBoxSpacing.current.medium),
    )
}

@Composable
private fun HomeView(
    onSignInWithEmail: () -> Unit,
    onSignInWithEmailAndPassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.surface,
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
    ) {
        Scaffold(containerColor = Color.Transparent) { innerPadding ->
            Column(
                modifier = modifier
                    .padding(paddingValues = innerPadding)
                    .padding(all = LocalBoxSpacing.current.large),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                HeaderSection()
                InformationSection()
                SignInSection(
                    onSignInWithEmail = onSignInWithEmail,
                    onSignInWithEmailAndPassword = onSignInWithEmailAndPassword,
                )
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
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_tyche_logo),
            contentDescription = emptyString(),
            modifier = Modifier.size(titleIconSize),
        )

        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = R.drawable.ic_tyche_title),
                contentDescription = emptyString(),
                modifier = Modifier.height(titleIconSize),
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
private fun SignInSection(onSignInWithEmail: () -> Unit, onSignInWithEmailAndPassword: () -> Unit) {
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

        Button(
            onClick = onSignInWithEmail,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = R.string.sign_in_with_email_action))
        }

        OutlinedButton(
            onClick = onSignInWithEmailAndPassword,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = R.string.sign_in_with_email_and_password))
        }
    }
}

private val titleIconSize = 64.dp

@PreviewLightDark
@Composable
fun HomeViewPreview() {
    TycheTheme(dynamicColor = false) {
        Surface {
            HomeView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = LocalBoxSpacing.current.medium),
                onSignInWithEmail = {},
                onSignInWithEmailAndPassword = {},
            )
        }
    }
}
