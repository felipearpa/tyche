package com.felipearpa.tyche

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.felipearpa.data.account.AccountBundle
import com.felipearpa.data.account.AccountStorage
import com.felipearpa.tyche.home.ui.HomeRoute
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute
import com.felipearpa.tyche.ui.theme.TycheTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var accountStorage: AccountStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val accountBundle = runBlocking { accountStorage.retrieve() }

        setContent {
            TycheTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Content(accountBundle = accountBundle)
                }
            }
        }
    }
}

@Composable
fun Content(accountBundle: AccountBundle?) {
    Outlet(accountBundle = accountBundle)
}

@Composable
fun Outlet(accountBundle: AccountBundle?) {
    val initialRoute = if (accountBundle == null) HomeRoute.route else PoolScoreListRoute.route

    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = initialRoute) {
        homeView(navController = navController)
        loginView(navController = navController, initialRoute = initialRoute)
        accountCreationView(navController = navController, initialRoute = initialRoute)
        poolScoreListView(navController = navController, loggedInGamblerId = accountBundle?.userId)
        poolHomeView(navController = navController)
    }
}