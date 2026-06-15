package jobs.procrush

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import jobs.procrush.auth.AuthState
import jobs.procrush.auth.AuthViewModel
import jobs.procrush.auth.currentAuthPath
import jobs.procrush.ui.auth.HomeScreen
import jobs.procrush.ui.auth.LoginScreen
import jobs.procrush.ui.auth.RoleSelectionScreen
import jobs.procrush.ui.theme.ProCrushTheme

@Composable
fun WebAuthApp(viewModel: AuthViewModel = viewModel { AuthViewModel() }) {
    val authState by viewModel.state.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val path = remember { currentAuthPath() }

    LaunchedEffect(Unit) {
        viewModel.refreshSession()
    }

    ProCrushTheme {
        when {
            path.endsWith("/auth/role") -> {
                when (val state = authState) {
                    is AuthState.NeedsRegistration ->
                        RoleSelectionScreen(state.user, viewModel, isBusy, errorMessage)
                    is AuthState.Authenticated -> HomeScreen(state.user, viewModel)
                    AuthState.Loading, AuthState.Unauthenticated -> LoadingBox()
                }
            }
            else -> {
                when (val state = authState) {
                    AuthState.Loading -> LoadingBox()
                    AuthState.Unauthenticated ->
                        LoginScreen(viewModel, isBusy, errorMessage)
                    is AuthState.NeedsRegistration ->
                        RoleSelectionScreen(state.user, viewModel, isBusy, errorMessage)
                    is AuthState.Authenticated -> HomeScreen(state.user, viewModel)
                }
            }
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
