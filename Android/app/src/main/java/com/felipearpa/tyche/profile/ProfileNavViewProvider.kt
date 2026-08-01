package com.felipearpa.tyche.profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.felipearpa.tyche.UsernameEditor
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.usernameEditorViewModel
import org.koin.compose.koinInject

fun NavGraphBuilder.profileNavView(navController: NavController) {
    composable<ProfileRoute> {
        val accountStorage = koinInject<AccountStorage>()
        val bundle by accountStorage.state.collectAsStateWithLifecycle()

        ProfileView(
            viewModel = profileViewModel(),
            onEditUsername = {
                navController.navigate(
                    route = UsernameEditorRoute(accountId = bundle?.accountId.orEmpty()),
                )
            },
            onBack = { navController.navigateUp() },
        )
    }

    composable<UsernameEditorRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<UsernameEditorRoute>()
        val accountStorage = koinInject<AccountStorage>()
        val bundle by accountStorage.state.collectAsStateWithLifecycle()

        Scaffold { innerPadding ->
            UsernameEditor(
                accountId = route.accountId,
                initialUsername = bundle?.username.orEmpty(),
                viewModel = usernameEditorViewModel(),
                onSaved = { navController.navigateUp() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
