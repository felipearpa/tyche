package com.felipearpa.tyche

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.core.JoinPoolUrlTemplateProvider
import com.felipearpa.tyche.home.HomeRoute
import com.felipearpa.tyche.home.homeNavView
import com.felipearpa.tyche.pool.poolJoinerView
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute
import com.felipearpa.tyche.poolcreator.poolFromLayoutCreatorNavView
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

    @Inject
    lateinit var joinUrlTemplate: JoinPoolUrlTemplateProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentData = intent.data?.toString()
        val accountBundle = runBlocking { accountStorage.retrieve() }

        enableEdgeToEdge()

        setContent {
            TycheTheme {
                Surface {
                    Content(
                        accountBundle = accountBundle,
                        intentData = intentData,
                        joinPoolUrlTemplate = joinUrlTemplate,
                    )
                }
            }
        }
    }
}

@Composable
fun Content(
    accountBundle: AccountBundle?,
    intentData: String?,
    joinPoolUrlTemplate: JoinPoolUrlTemplateProvider,
) {
    Outlet(
        accountBundle = accountBundle,
        intentData = intentData,
        joinPoolUrlTemplate = joinPoolUrlTemplate,
    )
}

@Composable
fun Outlet(
    accountBundle: AccountBundle?,
    intentData: String?,
    joinPoolUrlTemplate: JoinPoolUrlTemplateProvider,
) {
    val maybeGamblerId = accountBundle?.accountId
    val initialRoute: Any =
        if (maybeGamblerId == null) HomeRoute else PoolScoreListRoute(gamblerId = maybeGamblerId)
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

        maybeGamblerId?.let { gamblerId ->
            poolHomeNavView(
                navController = navController,
                initialRoute = initialRoute,
                gamblerId = gamblerId,
            )

            poolFromLayoutCreatorNavView(navController = navController, gamblerId = gamblerId)

            poolJoinerView(
                navController = navController,
                gamblerId = gamblerId,
                initialRoute = initialRoute,
                joinPoolUrlTemplate = joinPoolUrlTemplate,
            )
        }
    }
}
