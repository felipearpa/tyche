package com.felipearpa.tyche.home.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.R
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.tyche.ui.theme.boxSpacing

private val titleIconSize = 64.dp

@Composable
fun HomeView(
    viewModel: HomeViewModel,
    onSignInRequested: () -> Unit
) {
    HomeView(
        onSignInRequested = onSignInRequested,
        modifier = Modifier.padding(all = 8.dp)
    )
}

@Composable
private fun HomeView(
    onSignInRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(all = MaterialTheme.boxSpacing.large),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        HeaderSection()
        InformationSection()
        SignInSection(onLoginRequested = onSignInRequested)
    }
}

@Composable
private fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            MaterialTheme.boxSpacing.small,
            Alignment.CenterHorizontally
        )
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_tyche_logo),
            contentDescription = emptyString(),
            modifier = Modifier.size(titleIconSize)
        )

        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = R.drawable.ic_tyche_title),
                contentDescription = emptyString(),
                modifier = Modifier.height(titleIconSize)
            )
        }
    }
}

@Composable
private fun InformationSection() {
    Box {
        Text(
            text = stringResource(id = R.string.play_pool_text),
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SignInSection(onLoginRequested: () -> Unit) {
    Button(
        onClick = onLoginRequested,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = stringResource(id = R.string.sign_in_with_email_action))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeViewPreview() {
    TycheTheme(dynamicColor = false) {
        Surface {
            HomeView(
                modifier = Modifier.padding(all = 8.dp),
                onSignInRequested = {}
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun HomeViewDarkPreview() {
    TycheTheme(dynamicColor = false) {
        Surface {
            HomeView(
                modifier = Modifier.padding(all = 8.dp),
                onSignInRequested = {}
            )
        }
    }
}