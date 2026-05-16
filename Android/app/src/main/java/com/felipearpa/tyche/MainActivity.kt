package com.felipearpa.tyche

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.bet.match.matchBetListNavView
import com.felipearpa.tyche.bet.timeline.betTimelineListView
import com.felipearpa.tyche.core.JoinPoolUrlTemplateProvider
import com.felipearpa.tyche.home.HomeRoute
import com.felipearpa.tyche.home.homeNavView
import com.felipearpa.tyche.pool.poolJoinerView
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute
import com.felipearpa.tyche.poolcreator.poolFromLayoutCreatorNavView
import com.felipearpa.tyche.poolhome.PoolHomeViewRoute
import com.felipearpa.tyche.poolhome.poolHomeNavView
import com.felipearpa.tyche.poolscore.poolScoreListNavView
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.signin.signInWithEmailAndPasswordNavView
import com.felipearpa.tyche.signin.signInWithEmailLinkNavView
import com.felipearpa.tyche.signin.signInWithEmailNavView
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val accountStorage: AccountStorage by inject()
    private val joinUrlTemplate: JoinPoolUrlTemplateProvider by inject()

    private var deepLinkIntent by mutableStateOf<Intent?>(null)
    private var isReady by mutableStateOf(false)
    private var accountBundle by mutableStateOf<AccountBundle?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        makeEdgeToEdge()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { !isReady }

        lifecycleScope.launch {
            accountBundle = withContext(Dispatchers.IO) {
                accountStorage.retrieve()
            }
            isReady = true
        }

        val intentData = intent.data?.toString()

        setContent {
            if (isReady) {
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkIntent = intent
    }

    private fun makeEdgeToEdge() {
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ),
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
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
    val initialRoute: Any = if (maybeGamblerId == null) HomeRoute else PoolScoreListRoute(gamblerId = maybeGamblerId)
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

        val popToPoolHome: () -> Unit = {
            navController.popBackStack<PoolHomeViewRoute>(inclusive = false)
        }

        betTimelineListView(
            navController = navController,
            onHome = popToPoolHome,
        )

        matchBetListNavView(
            navController = navController,
            signedInGamblerId = maybeGamblerId,
            onHome = popToPoolHome,
        )

        poolFromLayoutCreatorNavView(navController = navController)

        poolJoinerView(
            navController = navController,
            gamblerId = maybeGamblerId,
            initialRoute = initialRoute,
            joinPoolUrlTemplate = joinPoolUrlTemplate,
        )
    }
}
