package com.example.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Toll
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BoosterType
import com.example.data.GameMode
import com.example.data.LeaderboardEntry
import com.example.data.MysteryChestReward
import com.example.data.PlayerProfile
import com.example.data.SeasonPassCatalog
import com.example.data.SeasonPassTier
import com.example.data.UiState
import com.example.ui.theme.AstroCyan
import com.example.ui.theme.AstroEmerald
import com.example.ui.theme.AstroGold
import com.example.ui.theme.AstroMagenta
import com.example.ui.theme.AstroViolet
import com.example.ui.theme.CosmicAbyss
import com.example.ui.theme.NebulaElevated
import com.example.ui.theme.NebulaSurface
import com.example.ui.theme.StarlightMuted
import com.example.ui.theme.StarlightWhite
import com.example.ui.theme.StardustOutline

data class PendingMissionLaunch(
    val mode: GameMode,
    val levelNumber: Int = 1,
    val title: String,
    val subtitle: String
)

@Composable
fun LeaderboardDialog(
    profile: PlayerProfile?,
    leaderboardState: UiState<List<LeaderboardEntry>>,
    isGuest: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var sortTab by remember { mutableIntStateOf(0) } // 0 = Endless, 1 = Blitz, 2 = Stars

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
                Icon(Icons.Rounded.Leaderboard, contentDescription = null, tint = AstroGold)
                Text("Galactic Leaderboard", style = MaterialTheme.typography.headlineMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isGuest) {
                    Surface(
                        color = AstroGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AstroGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "Playing in Guest Mode. Sign in with Google from Settings to compete globally!",
                            style = MaterialTheme.typography.bodySmall,
                            color = AstroGold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LeaderboardFilterPill("Endless", sortTab == 0, { sortTab = 0 }, Modifier.weight(1f).testTag("lb_tab_endless"))
                    LeaderboardFilterPill("Blitz", sortTab == 1, { sortTab = 1 }, Modifier.weight(1f).testTag("lb_tab_blitz"))
                    LeaderboardFilterPill("Stars", sortTab == 2, { sortTab = 2 }, Modifier.weight(1f).testTag("lb_tab_stars"))
                }

                when (leaderboardState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AstroCyan)
                        }
                    }
                    is UiState.Error -> {
                        Text(
                            text = leaderboardState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = StarlightMuted
                        )
                    }
                    is UiState.Success -> {
                        val sorted = remember(leaderboardState.data, sortTab) {
                            when (sortTab) {
                                1 -> leaderboardState.data.sortedByDescending { it.blitzHighScore }
                                2 -> leaderboardState.data.sortedByDescending { it.totalStars }
                                else -> leaderboardState.data.sortedByDescending { it.endlessHighScore }
                            }
                        }
                        if (sorted.isEmpty()) {
                            Text(
                                text = "Play a mission to post your first score on the galactic board!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StarlightMuted,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            sorted.forEachIndexed { idx, entry ->
                                val isSelf = profile != null && entry.userId == profile.userId
                                val rankColor = when (idx) {
                                    0 -> AstroGold
                                    1 -> AstroCyan
                                    2 -> AstroMagenta
                                    else -> StarlightMuted
                                }
                                val metricValue = when (sortTab) {
                                    1 -> "${entry.blitzHighScore}"
                                    2 -> "${entry.totalStars} ★"
                                    else -> "${entry.endlessHighScore}"
                                }
                                Surface(
                                    color = if (isSelf) AstroCyan.copy(alpha = 0.16f) else NebulaElevated,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (isSelf) 1.5.dp else 0.dp,
                                            color = if (isSelf) AstroCyan else Color.Transparent,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(rankColor.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "#${idx + 1}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = rankColor
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = entry.displayName + if (isSelf) " (You)" else "",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = StarlightWhite,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Rank ${entry.pilotRankNumber} • ${entry.pilotRankTitle}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = StarlightMuted
                                                )
                                            }
                                        }
                                        Text(
                                            text = metricValue,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = rankColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        val p = profile ?: PlayerProfile()
                        val rank = p.pilotRank()
                        val shareText = "AstroPop Pilot Card — ${p.displayName} (Rank ${rank.rankNumber} ${rank.title}) | Endless Best: ${p.endlessHighScore} | Blitz Best: ${p.blitzHighScore} | Stars: ${p.totalStars}. Join me: https://play.google.com/store/apps/details?id=com.aistudio.astropop.qvnrtx"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "My AstroPop Cosmic Rank")
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share AstroPop Rank"))
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AstroCyan),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("share_rank_button")
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Share My Pilot Card", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp).testTag("close_leaderboard_button")
            ) {
                Text("Close", color = AstroCyan)
            }
        }
    )
}

@Composable
private fun LeaderboardFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) AstroGold.copy(alpha = 0.22f) else NebulaElevated,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = if (selected) AstroGold else StardustOutline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) AstroGold else StarlightMuted
            )
        }
    }
}

@Composable
fun SeasonPassDialog(
    profile: PlayerProfile?,
    feedback: String?,
    isRewardedAdReady: Boolean,
    onClaimFree: (SeasonPassTier) -> Unit,
    onWatchAdForBonus: (SeasonPassTier) -> Unit,
    onDismiss: () -> Unit
) {
    val p = profile ?: PlayerProfile()
    val xp = p.pilotRank().xp
    val claimed = p.claimedPassTiers

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NebulaSurface,
        titleContentColor = StarlightWhite,
        textContentColor = StarlightWhite,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = AstroCyan)
                    Text("Cosmic Pass", style = MaterialTheme.typography.headlineMedium)
                }
                Text("$xp XP", style = MaterialTheme.typography.labelLarge, color = AstroGold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pop bubbles, earn Stars & set Endless/Blitz scores to unlock all 12 Cosmic Pass tiers!",
                    style = MaterialTheme.typography.bodySmall,
                    color = StarlightMuted
                )

                if (feedback != null) {
                    Surface(
                        color = AstroEmerald.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = feedback,
                            style = MaterialTheme.typography.labelLarge,
                            color = AstroEmerald,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                SeasonPassCatalog.tiers.forEach { tier ->
                    val unlocked = xp >= tier.requiredXp
                    val freeClaimed = tier.freeKey in claimed
                    val bonusClaimed = tier.bonusKey in claimed
                    val freeRewardLabel = "+${tier.freeCoins}c" + (tier.freeBooster?.let { " + ${it.label}" } ?: "")
                    val bonusRewardLabel = "+${tier.bonusCoins}c" + (tier.bonusBooster?.let { " + ${it.label}" } ?: "")

                    Surface(
                        color = NebulaElevated,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (unlocked && (!freeClaimed || !bonusClaimed)) 1.5.dp else 1.dp,
                                color = if (unlocked) AstroCyan.copy(alpha = 0.65f) else StardustOutline.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tier ${tier.tier} • ${tier.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (unlocked) StarlightWhite else StarlightMuted
                                )
                                Text(
                                    text = "${tier.requiredXp} XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (unlocked) AstroEmerald else StarlightMuted
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onClaimFree(tier) },
                                    enabled = unlocked && !freeClaimed,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (unlocked && !freeClaimed) AstroEmerald else NebulaSurface,
                                        contentColor = if (unlocked && !freeClaimed) CosmicAbyss else StarlightMuted
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("pass_free_${tier.tier}")
                                ) {
                                    Text(
                                        text = when {
                                            freeClaimed -> "Free ✓"
                                            unlocked -> "Free: $freeRewardLabel"
                                            else -> freeRewardLabel
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Button(
                                    onClick = { onWatchAdForBonus(tier) },
                                    enabled = unlocked && !bonusClaimed && isRewardedAdReady,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (unlocked && !bonusClaimed) AstroGold else NebulaSurface,
                                        contentColor = if (unlocked && !bonusClaimed) CosmicAbyss else StarlightMuted
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("pass_bonus_${tier.tier}")
                                ) {
                                    if (unlocked && !bonusClaimed) {
                                        Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.size(3.dp))
                                    }
                                    Text(
                                        text = when {
                                            bonusClaimed -> "VIP ✓"
                                            unlocked && !isRewardedAdReady -> "Ad Wait"
                                            unlocked -> bonusRewardLabel
                                            else -> "VIP: $bonusRewardLabel"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp).testTag("close_season_pass_button")
            ) {
                Text("Close", color = AstroCyan)
            }
        }
    )
}

@Composable
fun MysteryChestDialog(
    profile: PlayerProfile?,
    todayKey: String,
    feedback: String?,
    isRewardedAdReady: Boolean,
    onOpenFreeChest: ((MysteryChestReward) -> Unit) -> Unit,
    onWatchAdForChest: ((MysteryChestReward) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val freeAvailable = profile?.lastMysteryChestDate != todayKey
    var lastReward by remember { mutableStateOf<MysteryChestReward?>(null) }

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
                Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = AstroMagenta)
                Text("Mystery Star Chest", style = MaterialTheme.typography.headlineMedium)
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = AstroViolet.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, AstroMagenta.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Mystery Chest",
                            tint = AstroGold,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = lastReward?.title ?: "Cosmic Supply Vault",
                            style = MaterialTheme.typography.titleLarge,
                            color = AstroGold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = lastReward?.description
                                ?: "Unlock Star Coins, Nova Bombs, Prism Orbs, Star Lasers, or a 220c Jackpot!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (lastReward != null) AstroEmerald else StarlightMuted,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Chests Opened: ${profile?.mysteryChestsOpened ?: 0}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AstroCyan
                        )
                    }
                }

                if (feedback != null && lastReward == null) {
                    Text(
                        text = feedback,
                        style = MaterialTheme.typography.labelLarge,
                        color = AstroEmerald,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = {
                        onOpenFreeChest { reward -> lastReward = reward }
                    },
                    enabled = freeAvailable,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AstroEmerald,
                        contentColor = CosmicAbyss
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_free_chest_button")
                ) {
                    Icon(
                        imageVector = if (freeAvailable) Icons.Rounded.CardGiftcard else Icons.Rounded.CheckCircle,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = if (freeAvailable) "Open Free Daily Chest!" else "Daily Chest Claimed Today",
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Button(
                    onClick = {
                        onWatchAdForChest { reward -> lastReward = reward }
                    },
                    enabled = isRewardedAdReady,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AstroMagenta,
                        contentColor = StarlightWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_ad_chest_button")
                ) {
                    Icon(Icons.Rounded.PlayCircle, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = if (isRewardedAdReady) "Watch Ad: Open Bonus Chest" else "Loading Bonus Chest Ad...",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp).testTag("close_mystery_chest_button")
            ) {
                Text("Close", color = AstroCyan)
            }
        }
    )
}

@Composable
fun MissionLoadoutDialog(
    launch: PendingMissionLaunch,
    profile: PlayerProfile?,
    isRewardedAdReady: Boolean,
    onWatchAdForStarterBomb: () -> Unit,
    onLaunch: (BoosterType?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedBooster by remember { mutableStateOf<BoosterType?>(null) }
    val bombs = profile?.bombBoosters ?: 0L
    val prisms = profile?.prismBoosters ?: 0L
    val lasers = profile?.laserBoosters ?: 0L

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
                Icon(Icons.Rounded.RocketLaunch, contentDescription = null, tint = AstroCyan)
                Text(launch.title, style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = launch.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = StarlightMuted
                )
                Text(
                    text = "Pre-Flight Booster Loadout (Optional):",
                    style = MaterialTheme.typography.labelLarge,
                    color = AstroGold
                )

                val options = listOf(
                    Triple(BoosterType.BOMB, bombs, AstroMagenta to Icons.Rounded.Whatshot),
                    Triple(BoosterType.PRISM, prisms, AstroGold to Icons.Rounded.AutoAwesome),
                    Triple(BoosterType.LASER, lasers, AstroCyan to Icons.Rounded.Bolt)
                )
                options.forEach { (booster, owned, stylePair) ->
                    val (accent, icon) = stylePair
                    val isSelected = selectedBooster == booster
                    val canUse = owned > 0L
                    Surface(
                        color = if (isSelected) accent.copy(alpha = 0.22f) else NebulaElevated,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = canUse) {
                                selectedBooster = if (isSelected) null else booster
                            }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) accent else if (canUse) StardustOutline.copy(alpha = 0.6f) else StardustOutline.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .testTag("loadout_option_${booster.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(icon, contentDescription = null, tint = if (canUse) accent else StarlightMuted)
                                Column {
                                    Text(
                                        text = "Start with ${booster.label}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (canUse) StarlightWhite else StarlightMuted
                                    )
                                    Text(
                                        text = "Owned: x$owned",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (canUse) accent else StarlightMuted
                                    )
                                }
                            }
                            if (!canUse) {
                                Icon(Icons.Rounded.Lock, contentDescription = "None owned", tint = StarlightMuted, modifier = Modifier.size(18.dp))
                            } else if (isSelected) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = "Equipped", tint = accent, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onWatchAdForStarterBomb,
                    enabled = isRewardedAdReady,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AstroGold),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("loadout_ad_bomb_button")
                ) {
                    Icon(Icons.Rounded.PlayCircle, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = if (isRewardedAdReady) "Watch Ad: Free Nova Bomb Start!" else "Loading Free Bomb Ad...",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = { onLaunch(selectedBooster) },
                    colors = ButtonDefaults.buttonColors(containerColor = AstroCyan, contentColor = CosmicAbyss),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("launch_mission_confirm_button")
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = if (selectedBooster != null) "Launch + ${selectedBooster!!.label}!" else "Launch Mission!",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp).testTag("close_loadout_button")
            ) {
                Text("Cancel", color = StarlightMuted)
            }
        }
    )
}
