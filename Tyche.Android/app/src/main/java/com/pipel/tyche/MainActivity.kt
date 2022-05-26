package com.pipel.tyche

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pipel.tyche.pool.ui.PoolView
import com.pipel.tyche.pool.ui.PoolViewModel
import com.pipel.tyche.pool.ui.PoolViewModelFactory
import com.pipel.tyche.pool.ui.providePoolViewModelFactory
import com.pipel.tyche.poolLayout.ui.PoolLayoutView
import com.pipel.tyche.ui.theme.TycheTheme
import com.pipel.tyche.ui.theme.appTopBar
import com.pipel.tyche.ui.theme.onAppTopBar
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface ViewModelFactoryProvider {
        fun poolViewModelFactory(): PoolViewModelFactory
    }

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
fun poolViewModel(): PoolViewModel {
    val factory = EntryPointAccessors.fromActivity(
        LocalContext.current as Activity,
        MainActivity.ViewModelFactoryProvider::class.java
    ).poolViewModelFactory()
    return viewModel(factory = providePoolViewModelFactory(factory, "01FQWJ2KC6HPFPB0CWBM624SHA"))
}

@Composable
fun Outlet() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "Pool") {
        composable(route = "PoolLayout") {
            PoolLayoutView(hiltViewModel())
        }
        composable(route = "Pool") {
            PoolView(poolViewModel())
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