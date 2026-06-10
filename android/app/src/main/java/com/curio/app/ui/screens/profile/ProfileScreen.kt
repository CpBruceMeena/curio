package com.curio.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curio.app.data.local.PreferencesHelper
import com.curio.app.ui.theme.curioColors
import com.curio.app.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    prefs: PreferencesHelper,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val cc = curioColors()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cc.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium,
            color = cc.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Help us personalize your experience",
            style = MaterialTheme.typography.bodyMedium,
            color = cc.onSurfaceVariant.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Personal Information",
            style = MaterialTheme.typography.titleLarge,
            color = cc.onSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.name,
            onValueChange = { profileViewModel.updateName(it) },
            label = { Text("Name") },
            placeholder = { Text("Your name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(cc)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.age,
                onValueChange = { profileViewModel.updateAge(it) },
                label = { Text("Age") },
                placeholder = { Text("Age") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors(cc)
            )

            OutlinedTextField(
                value = uiState.gender,
                onValueChange = { profileViewModel.updateGender(it) },
                label = { Text("Gender") },
                placeholder = { Text("Male / Female / Other") },
                modifier = Modifier.weight(1.5f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors(cc)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.likes,
            onValueChange = { profileViewModel.updateLikes(it) },
            label = { Text("Likes") },
            placeholder = { Text("What topics or activities do you enjoy?") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(cc)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.dislikes,
            onValueChange = { profileViewModel.updateDislikes(it) },
            label = { Text("Dislikes") },
            placeholder = { Text("Anything you'd like to avoid?") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(cc)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { profileViewModel.saveProfile() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = cc.secondaryContainer,
                contentColor = Color(0xFF002021)
            ),
            enabled = !uiState.isLoading
        ) {
            Text(
                text = if (uiState.isLoading) "Saving..." else "Save Profile",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.isSaved) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "✅ Profile saved successfully!",
                style = MaterialTheme.typography.bodyMedium,
                color = cc.secondaryContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "❌ ${uiState.error}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFF5252)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Divider(color = cc.onSurfaceVariant.copy(alpha = 0.15f))
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Privacy & Terms",
            style = MaterialTheme.typography.titleLarge,
            color = cc.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        val privacyText = buildString {
            appendLine("Privacy Notice")
            appendLine()
            appendLine("Curio respects your privacy. The personal information you provide (name, age, gender, interests) is used solely to personalize your content recommendations. We do not share your data with third parties.")
            appendLine()
            appendLine("Your data is stored securely on our servers and can be deleted at any time by contacting us. You can choose not to provide any personal information and still use the app's core features.")
        }

        Text(
            text = privacyText,
            style = MaterialTheme.typography.bodySmall,
            color = cc.onSurfaceVariant.copy(alpha = 0.7f),
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        val termsText = buildString {
            appendLine("Terms & Conditions")
            appendLine()
            appendLine("By using Curio, you agree to the following terms:")
            appendLine()
            appendLine("1. Content is provided for educational and entertainment purposes only.")
            appendLine("2. You may not redistribute or republish app content without permission.")
            appendLine("3. We reserve the right to modify or discontinue services at any time.")
            appendLine("4. Your use of the app is at your own risk.")
            appendLine("5. These terms may be updated. Continued use constitutes acceptance.")
        }

        Text(
            text = termsText,
            style = MaterialTheme.typography.bodySmall,
            color = cc.onSurfaceVariant.copy(alpha = 0.7f),
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun fieldColors(cc: com.curio.app.ui.theme.CurioColors) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = cc.primary,
    unfocusedBorderColor = cc.surfaceContainerHigh,
    focusedContainerColor = cc.surfaceContainerHigh.copy(alpha = 0.2f),
    unfocusedContainerColor = cc.surfaceContainerHigh.copy(alpha = 0.1f),
    focusedLabelColor = cc.primary,
    unfocusedLabelColor = cc.onSurfaceVariant.copy(alpha = 0.6f),
    cursorColor = cc.primary
)
