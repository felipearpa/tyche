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
import com.felipearpa.tyche.UsernameEditor
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.usernameEditorViewModel
import org.koin.compose.koinInject

fun NavGraphBuilder.profileNavView(navController: NavController) {
    composable<ProfileRoute> {
        ProfileView(
            viewModel = profileViewModel(),
            onEditUsername = { navController.navigate(route = UsernameEditorRoute) },
            onBack = { navController.navigateUp() },
        )
    }

    composable<UsernameEditorRoute> {
        val accountStorage = koinInject<AccountStorage>()
        val bundle by accountStorage.state.collectAsStateWithLifecycle()

        Scaffold { innerPadding ->
            UsernameEditor(
                initialUsername = bundle?.username.orEmpty(),
                viewModel = usernameEditorViewModel(),
                onSaved = { navController.navigateUp() },
                onDismiss = { navController.navigateUp() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
