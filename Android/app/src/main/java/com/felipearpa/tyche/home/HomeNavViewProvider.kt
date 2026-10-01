package com.felipearpa.tyche.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.felipearpa.tyche.account.byemail.EmailSignInRoute
import com.felipearpa.tyche.account.byemailandpassword.EmailAndPasswordSignInRoute
import com.felipearpa.tyche.account.bygoogle.GoogleSignInViewModel
import com.felipearpa.tyche.account.bygoogle.googleSignInViewModel
import com.felipearpa.tyche.account.social.SocialSignInRow
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.ui.loading.LoadingContainerView

fun NavGraphBuilder.homeNavView(navController: NavController) {
    composable<HomeRoute> {
        HomeScreen(
            googleViewModel = googleSignInViewModel(),
            onSignInWithEmail = { navController.navigate(route = EmailSignInRoute) },
            onSignInWithEmailAndPassword = { navController.navigate(route = EmailAndPasswordSignInRoute) },
            onAuthenticate = { accountBundle ->
                navController.navigate(route = PoolScoreListRoute(gamblerId = accountBundle.accountId)) {
                    popUpTo(route = HomeRoute) { inclusive = true }
                }
            },
        )
    }
}

/**
 * The welcome screen bound to its Google sign-in attempt. While the attempt is busy, the loading
 * overlay shows and the email sign-in actions are disabled so no competing sign-in starts.
 */
@Composable
internal fun HomeScreen(
    googleViewModel: GoogleSignInViewModel,
    onSignInWithEmail: () -> Unit,
    onSignInWithEmailAndPassword: () -> Unit,
    onAuthenticate: (AccountBundle) -> Unit,
) {
    val googleState by googleViewModel.state.collectAsState()
    val context = LocalContext.current
    val isSignInAvailable = !googleState.isBusy

    Box(modifier = Modifier.fillMaxSize()) {
        HomeView(
            onSignInWithEmail = onSignInWithEmail.takeIf { isSignInAvailable },
            onSignInWithEmailAndPassword = onSignInWithEmailAndPassword.takeIf { isSignInAvailable },
            socialSignInSlot = {
                SocialSignInRow(
                    googleState = googleState,
                    onSignInWithGoogle = { googleViewModel.signInWithGoogle(activityContext = context) },
                    onDismissGoogleFailure = googleViewModel::dismissFailure,
                    onAuthenticate = onAuthenticate,
                    onAuthenticationHandled = googleViewModel::authenticationHandled,
                )
            },
        )

        if (googleState.isBusy) {
            LoadingContainerView {}
        }
    }
}
