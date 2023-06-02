package com.felipearpa.tyche.home.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.constraintlayout.compose.Dimension
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.R
import com.felipearpa.tyche.ui.theme.TycheTheme

@Composable
fun HomeView(
    viewModel: HomeViewModel,
    onLoginRequested: () -> Unit,
    onAccountCreationRequested: () -> Unit
) {
    HomeView(
        onLoginClick = onLoginRequested,
        onCreateAccountClick = onAccountCreationRequested,
        modifier = Modifier.padding(all = 8.dp)
    )
}

@Composable
private fun HomeView(
    onCreateAccountClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConstraintLayout(modifier = modifier.fillMaxSize()) {
        val (titleView, descriptionView, creationAccountView, loginView) = createRefs()

        Title(viewRef = titleView)

        Description(
            viewRef = descriptionView,
            titleView = titleView,
            creationAccountView = creationAccountView
        )

        CreationAccount(
            onCreateAccountClick = onCreateAccountClick,
            viewRef = creationAccountView,
            descriptionView = descriptionView,
            loginView = loginView
        )

        LoginView(onLoginClick = onLoginClick, viewRef = loginView)
    }
}

@Composable
private fun ConstraintLayoutScope.Title(viewRef: ConstrainedLayoutReference) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.constrainAs(ref = viewRef) {
            top.linkTo(parent.top)
            start.linkTo(parent.start)
            width = Dimension.matchParent
        }
    ) {

        Icon(
            painter = painterResource(id = R.drawable.ic_tyche_logo),
            contentDescription = emptyString(),
            modifier = Modifier.height(64.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Box(
            modifier = Modifier.height(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_tyche_title),
                contentDescription = emptyString(),
                modifier = Modifier.height(48.dp)
            )
        }
    }
}

@Composable
private fun ConstraintLayoutScope.Description(
    viewRef: ConstrainedLayoutReference,
    titleView: ConstrainedLayoutReference,
    creationAccountView: ConstrainedLayoutReference
) {
    Box(modifier = Modifier.constrainAs(ref = viewRef) {
        top.linkTo(titleView.bottom)
        bottom.linkTo(creationAccountView.top)
        width = Dimension.matchParent
    }) {
        Text(
            text = stringResource(id = R.string.play_pool_text),
            style = MaterialTheme.typography.displaySmall
        )
    }
}

@Composable
private fun ConstraintLayoutScope.CreationAccount(
    onCreateAccountClick: () -> Unit,
    viewRef: ConstrainedLayoutReference,
    descriptionView: ConstrainedLayoutReference,
    loginView: ConstrainedLayoutReference
) {
    Box(
        modifier = Modifier.constrainAs(ref = viewRef) {
            top.linkTo(descriptionView.bottom)
            bottom.linkTo(loginView.top)
            start.linkTo(parent.start)
        }, contentAlignment = Alignment.Center
    ) {
        Button(modifier = Modifier.fillMaxWidth(), onClick = onCreateAccountClick) {
            Text(text = stringResource(id = R.string.create_account_action))
        }
    }
}

@Composable
private fun ConstraintLayoutScope.LoginView(
    onLoginClick: () -> Unit,
    viewRef: ConstrainedLayoutReference
) {
    Row(modifier = Modifier.constrainAs(viewRef) {
        bottom.linkTo(parent.bottom)
        start.linkTo(parent.start)
    }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(id = R.string.account_exists_text))

        Text(
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.primary
                    )
                ) {
                    append(stringResource(id = R.string.log_in_action))
                }
            },
            modifier = Modifier.clickable { onLoginClick() }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeViewPreview() {
    TycheTheme(dynamicColor = false) {
        Surface {
            HomeView(
                modifier = Modifier.padding(all = 8.dp),
                onLoginClick = {},
                onCreateAccountClick = {}
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun HomeViewDarkPreview() {
    TycheTheme(dynamicColor = false) {
        HomeView(
            modifier = Modifier.padding(all = 8.dp),
            onLoginClick = {},
            onCreateAccountClick = {}
        )
    }
}