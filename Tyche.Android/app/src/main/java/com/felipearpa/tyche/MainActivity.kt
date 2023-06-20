package com.felipearpa.tyche

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.felipearpa.tyche.home.ui.HomeRoute
import com.felipearpa.tyche.home.ui.HomeView
import com.felipearpa.tyche.pool.poolScoreListViewModel
import com.felipearpa.tyche.pool.ui.poolscore.PoolScoreListRoute
import com.felipearpa.tyche.pool.ui.poolscore.PoolScoreListView
import com.felipearpa.tyche.poolHome.PoolHomeView
import com.felipearpa.tyche.poolHome.PoolHomeViewRoute
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.tyche.user.LoginStorage
import com.felipearpa.tyche.user.UserProfile
import com.felipearpa.tyche.user.account.ui.UserCreatingRoute
import com.felipearpa.tyche.user.account.ui.UserCreationView
import com.felipearpa.tyche.user.login.ui.LoginRoute
import com.felipearpa.tyche.user.login.ui.LoginView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var loginStorage: LoginStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userProfile = runBlocking {
            loginStorage.get()?.user
        }

        setContent {
            TycheTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Content(userProfile = userProfile)
                }
            }
        }
    }
}

@Composable
fun Content(userProfile: UserProfile?) {
    Outlet(userProfile = userProfile)
}

@Composable
fun Outlet(userProfile: UserProfile?) {
    val initialRoute =
        if (userProfile == null) HomeRoute.route else PoolScoreListRoute.route

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = initialRoute) {
        composable(route = HomeRoute.route) {
            HomeView(
                viewModel = hiltViewModel(),
                onLoginRequested = {
                    navController.navigate(route = LoginRoute.route())
                },
                onAccountCreationRequested = {
                    navController.navigate(route = UserCreatingRoute.route())
                }
            )
        }

        composable(route = LoginRoute.route) {
            LoginView(
                viewModel = hiltViewModel(),
                onLoggedIn = { loggedInUserProfile ->
                    navController.navigate(route = PoolScoreListRoute.route(gamblerId = loggedInUserProfile.userId)) {
                        popUpTo(route = initialRoute) { inclusive = true }
                    }
                },
                back = { navController.navigateUp() }
            )
        }

        composable(route = UserCreatingRoute.route) {
            UserCreationView(
                viewModel = hiltViewModel(),
                onUserCreated = { createdUserProfile ->
                    navController.navigate(route = PoolScoreListRoute.route(gamblerId = createdUserProfile.userId)) {
                        popUpTo(route = initialRoute) { inclusive = true }
                    }
                },
                onBack = { navController.navigateUp() }
            )
        }

        composable(
            route = PoolHomeViewRoute.route,
            arguments = listOf(
                navArgument(name = PoolHomeViewRoute.Params.GAMBLER_ID.identifier) {
                    type = NavType.StringType
                    userProfile?.let { nonNullableUserProfile ->
                        defaultValue = nonNullableUserProfile.userId
                    }
                },
                navArgument(name = PoolHomeViewRoute.Params.POOL_ID.identifier) {
                    type = NavType.StringType
                }
            )
        ) { navBackStackEntry ->
            val poolId =
                navBackStackEntry.arguments!!.getString(PoolHomeViewRoute.Params.POOL_ID.identifier)
            val gamblerId =
                navBackStackEntry.arguments!!.getString(PoolHomeViewRoute.Params.GAMBLER_ID.identifier)
            PoolHomeView(
                viewModel = poolHomeViewModel(poolId = poolId!!, gamblerId = gamblerId!!),
                onPoolScoreListRequested = { navController.navigateUp() }
            )
        }

        composable(
            route = PoolScoreListRoute.route,
            arguments = listOf(
                navArgument(name = PoolScoreListRoute.Params.GAMBLER_ID.identifier) {
                    type = NavType.StringType
                    userProfile?.let { nonNullableUserProfile ->
                        defaultValue = nonNullableUserProfile.userId
                    }
                }
            )
        ) { navBackStackEntry ->
            val gamblerId =
                navBackStackEntry.arguments!!.getString(PoolScoreListRoute.Params.GAMBLER_ID.identifier)
            PoolScoreListView(
                viewModel = poolScoreListViewModel(gamblerId = gamblerId!!),
                onPoolDetailRequested = { poolId ->
                    navController.navigate(
                        route = PoolHomeViewRoute.route(
                            poolId = poolId,
                            gamblerId = gamblerId
                        )
                    )
                }
            )
        }
    }
}