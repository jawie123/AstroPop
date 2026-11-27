package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.GameMode
import com.example.data.GameRun
import com.example.data.PlayerProfile
import com.example.ui.theme.AstroCyan
import com.example.ui.theme.AstroGold
import com.example.ui.theme.AstroMagenta
import com.example.ui.theme.CosmicAbyss
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.NebulaElevated
import com.example.ui.theme.NebulaSurface
import com.example.ui.theme.StarlightMuted
import com.example.ui.theme.StarlightWhite
import com.example.ui.theme.StardustOutline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsSettingsSheet(
    profile: PlayerProfile?,
    recentRuns: List<GameRun>,
    isGuest: Boolean,
    authError: String?,
    showAdPrivacyOptions: Boolean = false,
    onOpenAdPrivacyOptions: () -> Unit = {},
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onSignInWithGoogle: () -> Unit,
    onExitGuest: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NebulaSurface,
        contentColor = StarlightWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Captain's Log & Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = StarlightWhite
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatChip(
                    label = "Endless Best",
                    value = "${profile?.endlessHighScore ?: 0}",
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    label = "Blitz Best",
                    value = "${profile?.blitzHighScore ?: 0}",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatChip(
                    label = "Total Popped",
                    value = "${profile?.totalBubblesPopped ?: 0}",
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    label = "Star Coins",
                    value = "${profile?.starCoins ?: 0}",
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                color = NebulaElevated,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = AstroCyan)
                            Text("Arcade Synth Audio", style = MaterialTheme.typography.titleMedium)
                        }
                        Switch(
                            checked = profile?.soundEnabled ?: true,
                            onCheckedChange = onToggleSound,
                            colors = SwitchDefaults.colors(checkedTrackColor = AstroCyan, checkedThumbColor = CosmicAbyss),
                            modifier = Modifier.testTag("sound_toggle_switch")
                        )
                    }
                    HorizontalDivider(color = StardustOutline.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.Vibration, contentDescription = null, tint = AstroMagenta)
                            Text("Tactile Haptics", style = MaterialTheme.typography.titleMedium)
                        }
                        Switch(
                            checked = profile?.hapticsEnabled ?: true,
                            onCheckedChange = onToggleHaptics,
                            colors = SwitchDefaults.colors(checkedTrackColor = AstroMagenta, checkedThumbColor = StarlightWhite),
                            modifier = Modifier.testTag("haptics_toggle_switch")
                        )
                    }
                }
            }

            Text(
                text = "Recent Missions",
                style = MaterialTheme.typography.titleLarge,
                color = StarlightWhite
            )

            if (recentRuns.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NebulaElevated)
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No flights recorded yet. Launch a Daily, Saga, or Endless run!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StarlightMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentRuns) { run ->
                        val runMode = run.resolvedMode()
                        val runTitle = when (runMode) {
                            GameMode.ENDLESS -> "Endless Panic"
                            GameMode.BLITZ -> "Cosmic Blitz"
                            GameMode.DAILY -> "Daily Nebula"
                            GameMode.SAGA -> "Saga Stage ${run.levelNumber}"
                        }
                        Surface(
                            color = NebulaElevated,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = runTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = StarlightWhite
                                    )
                                    Text(
                                        text = "${run.bubblesPopped} popped • ${run.maxCombo}x max combo",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = StarlightMuted
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (runMode != GameMode.ENDLESS && runMode != GameMode.BLITZ && run.starsEarned > 0) {
                                        Icon(
                                            Icons.Rounded.Star,
                                            contentDescription = null,
                                            tint = AstroGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "${run.starsEarned}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = AstroGold
                                        )
                                    }
                                    Text(
                                        text = "${run.score}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = AstroCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isGuest) {
                if (authError != null) {
                    Text(
                        text = authError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DangerCrimson
                    )
                }
                Button(
                    onClick = onSignInWithGoogle,
                    colors = ButtonDefaults.buttonColors(containerColor = AstroCyan, contentColor = CosmicAbyss),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("guest_upgrade_google_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Sign in with Google for Cloud Sync", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onExitGuest,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StarlightMuted),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("exit_guest_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Exit Guest Mode", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                OutlinedButton(
                    onClick = onSignOut,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerCrimson),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Sign Out", style = MaterialTheme.typography.labelLarge)
                }
            }
            if (showAdPrivacyOptions) {
                OutlinedButton(
                    onClick = onOpenAdPrivacyOptions,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AstroGold),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_ad_consent_button")
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Ad Privacy & Consent Options", style = MaterialTheme.typography.labelLarge)
                }
            }
            OutlinedButton(
                onClick = { showPrivacyPolicy = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AstroCyan),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_privacy_policy_button")
            ) {
                Icon(Icons.Rounded.Shield, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Privacy Policy & Data Safety", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }

    if (showPrivacyPolicy) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicy = false })
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = NebulaElevated,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = StarlightMuted)
            Text(text = value, style = MaterialTheme.typography.headlineMedium, color = AstroCyan)
        }
    }
}
