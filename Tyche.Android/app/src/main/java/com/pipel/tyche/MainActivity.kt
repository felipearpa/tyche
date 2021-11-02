package com.pipel.tyche

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pipel.tyche.ui.poollayout.PoolLayoutView
import com.pipel.tyche.ui.theme.TycheTheme
import com.pipel.tyche.ui.theme.appTopBar
import com.pipel.tyche.ui.theme.onAppTopBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TycheTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Content()
                }
            }
        }
    }

}

@Composable
fun Content() {
    Scaffold(
        topBar = { AppTopBar(title = { Text(text = "Tyche") }) },
        content = { Outlet() }
    )
}

@Composable
fun AppTopBar(title: @Composable () -> Unit) {
    TopAppBar(
        title = title,
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.appTopBar,
        contentColor = MaterialTheme.colors.onAppTopBar
    )
}

@Composable
fun Outlet() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "PoolLayout") {
        composable(route = "PoolLayout") {
            PoolLayoutView()
        }
    }
}

@Preview
@Composable
fun AppTopBarPreview() {
    TycheTheme {
        AppTopBar { Text(text = stringResource(R.string.app_name)) }
    }
}