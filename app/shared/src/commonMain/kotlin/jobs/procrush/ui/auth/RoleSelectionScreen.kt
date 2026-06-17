package jobs.procrush.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import jobs.procrush.auth.AuthUserDto
import jobs.procrush.auth.AuthViewModel
import jobs.procrush.auth.UserRole
import jobs.procrush.ui.adaptive.AdaptiveContent

@Composable
fun RoleSelectionScreen(
    user: AuthUserDto,
    viewModel: AuthViewModel,
    isBusy: Boolean,
    errorMessage: String?,
) {
    AdaptiveContent {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Выберите тип аккаунта",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Добро пожаловать. Этот выбор нельзя изменить для ${user.email}.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (isBusy) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.completeRegistration(UserRole.SEEKER) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Ищу работу")
                }
                OutlinedButton(
                    onClick = { viewModel.completeRegistration(UserRole.EMPLOYER) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Нанимаю сотрудников")
                }
            }
        }
    }
}
