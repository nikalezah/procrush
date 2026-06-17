package jobs.procrush.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import jobs.procrush.auth.AuthViewModel
import jobs.procrush.ui.adaptive.AdaptiveContent

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    isBusy: Boolean,
    errorMessage: String?,
) {
    var email by rememberSaveable { mutableStateOf("") }

    AdaptiveContent {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "ProCrush",
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = "Найдите работу или наймите специалистов. Войдите, чтобы продолжить.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Электронная почта") },
                singleLine = true,
                enabled = !isBusy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (isBusy) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.signInDev(email) },
                    enabled = email.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Продолжить")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Роль (соискатель или работодатель) выбирается один раз после входа и позже не меняется.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
