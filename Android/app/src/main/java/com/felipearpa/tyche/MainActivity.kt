package com.felipearpa.tyche

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val accountStorage: AccountStorage by inject()
    private val joinUrlTemplate: JoinPoolUrlTemplateProvider by inject()

    private var deepLinkIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
                        deepLinkIntent = deepLinkIntent,
                        onHandleDeepLink = { deepLinkIntent = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkIntent = intent
    }
}

@Composable
fun Content(
    accountBundle: AccountBundle?,
    intentData: String?,
    joinPoolUrlTemplate: JoinPoolUrlTemplateProvider,
    deepLinkIntent: Intent?,
    onHandleDeepLink: () -> Unit,
) {
    Outlet(
        accountBundle = accountBundle,
        intentData = intentData,
        joinPoolUrlTemplate = joinPoolUrlTemplate,
        deepLinkIntent = deepLinkIntent,
        onDeepLinkHandled = onHandleDeepLink,
    )
}

@Composable
fun Outlet(
    accountBundle: AccountBundle?,
    intentData: String?,
    joinPoolUrlTemplate: JoinPoolUrlTemplateProvider,
    deepLinkIntent: Intent?,
    onDeepLinkHandled: () -> Unit,
) {
    val maybeGamblerId = accountBundle?.accountId
    val initialRoute: Any =
        if (maybeGamblerId == null) HomeRoute else PoolScoreListRoute(gamblerId = maybeGamblerId)
    val navController = rememberNavController()

    LaunchedEffect(deepLinkIntent) {
        deepLinkIntent?.let { intent ->
            navController.handleDeepLink(intent)
            onDeepLinkHandled()
        }
    }

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

        poolHomeNavView(
            navController = navController,
            initialRoute = initialRoute,
        )

        poolFromLayoutCreatorNavView(navController = navController)

        maybeGamblerId?.let { gamblerId ->
            poolJoinerView(
                navController = navController,
                gamblerId = gamblerId,
                initialRoute = initialRoute,
                joinPoolUrlTemplate = joinPoolUrlTemplate,
            )
        }
    }
}
