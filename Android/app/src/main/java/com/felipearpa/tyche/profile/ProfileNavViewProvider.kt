package com.felipearpa.tyche.profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.felipearpa.tyche.UsernameEditorScreen
import com.felipearpa.tyche.session.CurrentAccountCoordinator
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
        val currentAccountCoordinator = koinInject<CurrentAccountCoordinator>()
        val bundle by currentAccountCoordinator.state.collectAsStateWithLifecycle()

        UsernameEditorScreen(
            accountId = bundle?.accountId.orEmpty(),
            initialUsername = bundle?.username.orEmpty(),
            viewModel = usernameEditorViewModel(),
            // Leaving only pops the route: the draft lives in the editor, so it is discarded
            // without submitting a username update or a retry.
            onBack = { navController.navigateUp() },
            onSaved = { navController.navigateUp() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
