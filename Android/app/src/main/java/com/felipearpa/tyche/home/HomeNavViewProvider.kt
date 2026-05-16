package com.felipearpa.tyche.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.felipearpa.tyche.account.byemail.EmailSignInRoute
import com.felipearpa.tyche.account.byemailandpassword.EmailAndPasswordSignInRoute
import com.felipearpa.tyche.account.bygoogle.googleSignInViewModel
import com.felipearpa.tyche.account.social.SocialSignInRow
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute
import com.felipearpa.tyche.ui.loading.LoadingContainerView
import com.felipearpa.ui.state.LoadableViewState
import com.felipearpa.ui.state.isLoading

fun NavGraphBuilder.homeNavView(navController: NavController) {
    composable<HomeRoute> {
        val googleViewModel = googleSignInViewModel()
        val googleState by googleViewModel.state.collectAsState(initial = LoadableViewState.Initial)
        val context = LocalContext.current

        val isLoadingOverlayVisible =
            googleState.isLoading() || googleState is LoadableViewState.Success

        Box(modifier = Modifier.fillMaxSize()) {
            HomeView(
                onSignInWithEmail = { navController.navigate(route = EmailSignInRoute) },
                onSignInWithEmailAndPassword = { navController.navigate(route = EmailAndPasswordSignInRoute) },
                socialSignInSlot = {
                    SocialSignInRow(
                        googleState = googleState,
                        onSignInWithGoogle = { googleViewModel.signInWithGoogle(context = context) },
                        onResetGoogleState = { googleViewModel.reset() },
                        onAuthenticate = { accountBundle ->
                            navController.navigate(route = PoolScoreListRoute(gamblerId = accountBundle.accountId)) {
                                popUpTo(route = HomeRoute) { inclusive = true }
                            }
                        },
                    )
                },
            )

            if (isLoadingOverlayVisible) {
                LoadingContainerView {}
            }
        }
    }
}
