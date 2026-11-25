package com.example.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AstroCyan
import com.example.ui.theme.AstroEmerald
import com.example.ui.theme.AstroGold
import com.example.ui.theme.AstroMagenta
import com.example.ui.theme.NebulaElevated
import com.example.ui.theme.NebulaSurface
import com.example.ui.theme.StarlightMuted
import com.example.ui.theme.StarlightWhite

private data class PolicySection(
    val title: String,
    val body: String,
    val accent: Color
)

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    val sections = listOf(
        PolicySection(
            title = "1. Scope & Contact",
            body = "This Privacy Policy applies specifically to AstroPop (com.aistudio.astropop.qvnrtx). " +
                "Effective Date: November 25, 2026. For privacy questions or data deletion requests, " +
                "contact the developer at muhammadjawwad94@gmail.com.",
            accent = AstroCyan
        ),
        PolicySection(
            title = "2. Data Collected & Stored",
            body = "• Google Sign-In (Optional Cloud Sync): Stores your Firebase User ID, Google display name, " +
                "and game progression (stage stars, scores, Star Coins, boosters, cannon skins, quests, recent " +
                "runs, and audio/haptic preferences) in Google Cloud Firestore.\n" +
                "• Guest Pilot Mode (Local Save): No account or personal data is collected or sent to servers. " +
                "Progress stays strictly on your device in local app storage.",
            accent = AstroGold
        ),
        PolicySection(
            title = "3. How We Use & Share Data",
            body = "Data is used solely to provide gameplay persistence, cloud sync across sessions, and your " +
                "in-game preferences. AstroPop contains zero third-party ad networks, zero analytics trackers, " +
                "and never sells user data. Cloud authentication and sync are handled only by Google Firebase " +
                "Authentication, Google Cloud Firestore, and Google Credential Manager.",
            accent = AstroEmerald
        ),
        PolicySection(
            title = "4. Permissions & Children",
            body = "AstroPop requests only standard INTERNET access for optional Google Sign-In and cloud sync. " +
                "AstroPop does not knowingly collect personal information from children under 13 (or the " +
                "applicable minimum age in your jurisdiction).",
            accent = AstroMagenta
        ),
        PolicySection(
            title = "5. Retention & Data Deletion",
            body = "Guest progress can be cleared anytime via Android app settings. Signed-in players may sign out " +
                "in Captain's Log & Settings or email muhammadjawwad94@gmail.com to request complete deletion of " +
                "their Firebase profile and saved progression.",
            accent = AstroCyan
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NebulaSurface,
        titleContentColor = StarlightWhite,
        textContentColor = StarlightWhite,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = AstroCyan)
                Text("AstroPop Privacy Policy", style = MaterialTheme.typography.headlineMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                sections.forEach { section ->
                    Surface(
                        color = NebulaElevated,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, section.accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = section.accent
                            )
                            Text(
                                text = section.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = StarlightMuted
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("close_privacy_policy_button")
            ) {
                Text("Close", color = AstroCyan)
            }
        }
    )
}
