package jobs.procrush.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import jobs.procrush.auth.AuthUserDto
import jobs.procrush.auth.AuthViewModel
import jobs.procrush.ui.adaptive.AdaptiveContent
import jobs.procrush.ui.displayLabel

@Composable
fun HomeScreen(
    user: AuthUserDto,
    viewModel: AuthViewModel,
) {
    AdaptiveContent {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Вы вошли",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "${user.email} · ${user.role?.displayLabel() ?: "неизвестно"}",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
            )
            Button(onClick = { viewModel.logout() }) {
                Text("Выйти")
            }
        }
    }
}
