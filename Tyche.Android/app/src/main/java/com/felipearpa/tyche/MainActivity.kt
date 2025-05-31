package com.felipearpa.tyche

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.home.homeNavView
import com.felipearpa.tyche.home.ui.HomeRoute
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute
import com.felipearpa.tyche.poolhome.poolHomeNavView
import com.felipearpa.tyche.poolscore.poolScoreListNavView
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.signin.signInWithEmailAndPasswordNavView
import com.felipearpa.tyche.signin.signInWithEmailLinkNavView
import com.felipearpa.tyche.signin.signInWithEmailNavView
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

        val intentData = intent.data?.toString()
        val accountBundle = runBlocking { accountStorage.retrieve() }

        setContent {
            TycheTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Content(accountBundle = accountBundle, intentData = intentData)
                }
            }
        }
    }
}

@Composable
fun Content(accountBundle: AccountBundle?, intentData: String?) {
    Outlet(accountBundle = accountBundle, intentData = intentData)
}

@Composable
fun Outlet(accountBundle: AccountBundle?, intentData: String?) {
    val gamblerId = accountBundle?.accountId
    val initialRoute: Any =
        if (gamblerId == null) HomeRoute else PoolScoreListRoute(gamblerId = gamblerId)
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = initialRoute) {
        homeNavView(navController = navController)

        signInWithEmailNavView(navController = navController)

        signInWithEmailLinkNavView(
            navController = navController,
            initialRoute = initialRoute,
            emailLink = intentData ?: emptyString(),
        )

        signInWithEmailAndPasswordNavView(
            navController = navController,
            initialRoute = initialRoute,
        )

        poolScoreListNavView(
            navController = navController,
            initialRoute = initialRoute,
        )

        gamblerId?.let {
            poolHomeNavView(
                navController = navController,
                initialRoute = initialRoute,
                gamblerId = gamblerId,
            )
        }
    }
}
