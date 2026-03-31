package io.github.degipe.youtubewhitelist.ui.screen.credentials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialSettingsScreen(
    viewModel: CredentialSettingsViewModel,
    onNavigateBack: () -> Unit,
    onCredentialsSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSecret by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Credentials") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Instructions card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Setup Required",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This app needs your own Google API credentials to work. " +
                                "It takes about 5 minutes to set up. " +
                                "See the Setup Guide in the app's GitHub page for step-by-step instructions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // YouTube API Key (optional — has built-in fallback)
            Text(
                text = "YouTube API Key (optional)",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Leave empty to use the built-in key. Only needed if you hit quota limits.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = uiState.youtubeApiKey,
                onValueChange = viewModel::onYouTubeApiKeyChanged,
                label = { Text("YouTube API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Google Client ID (required)
            Text(
                text = "Google OAuth Credentials (required)",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Required for Google Sign-In. Create at console.cloud.google.com.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = uiState.googleClientId,
                onValueChange = viewModel::onGoogleClientIdChanged,
                label = { Text("Google Client ID") },
                placeholder = { Text("xxxxx.apps.googleusercontent.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
            )

            // Google Client Secret (required)
            OutlinedTextField(
                value = uiState.googleClientSecret,
                onValueChange = viewModel::onGoogleClientSecretChanged,
                label = { Text("Google Client Secret") },
                placeholder = { Text("GOCSPX-xxxxx") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showSecret = !showSecret }) {
                        Icon(
                            imageVector = if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showSecret) "Hide" else "Show"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save button
            Button(
                onClick = {
                    viewModel.save()
                    onCredentialsSaved()
                },
                enabled = viewModel.canSave(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save & Continue")
            }

            if (uiState.isSaved) {
                Text(
                    text = "Credentials saved securely!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Security note
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Security",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your credentials are stored locally on this device using AES-256 encryption. " +
                                "They are never sent anywhere except to Google's servers for authentication.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
