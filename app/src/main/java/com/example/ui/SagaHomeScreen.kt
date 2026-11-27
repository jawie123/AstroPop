package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Toll
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ads.AstroBannerAd
import com.example.data.BoosterType
import com.example.data.CannonSkin
import com.example.data.CosmicQuest
import com.example.data.DailySupplyCatalog
import com.example.data.DailySupplyTier
import com.example.data.GameMode
import com.example.data.LeaderboardEntry
import com.example.data.LevelProgress
import com.example.data.MysteryChestReward
import com.example.data.PilotRankInfo
import com.example.data.PlayerProfile
import com.example.data.SeasonPassCatalog
import com.example.data.SeasonPassTier
import com.example.data.UiState
import com.example.data.WheelPrize
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.example.game.LevelCatalog
import com.example.game.LevelGoal
import com.example.game.SagaLevelSpec
import com.example.ui.theme.AstroCyan
import com.example.ui.theme.AstroEmerald
import com.example.ui.theme.AstroGold
import com.example.ui.theme.AstroMagenta
import com.example.ui.theme.AstroViolet
import com.example.ui.theme.CosmicAbyss
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.NebulaElevated
import com.example.ui.theme.NebulaSurface
import com.example.ui.theme.StarlightMuted
import com.example.ui.theme.StarlightWhite
import com.example.ui.theme.StardustOutline

@Composable
fun SagaHomeScreen(
    profile: PlayerProfile?,
    levels: List<LevelProgress>,
    leaderboardState: UiState<List<LeaderboardEntry>>,
    isGuest: Boolean,
    errorMessage: String?,
    shopFeedback: String?,
    isRewardedAdReady: Boolean,
    canRequestAds: Boolean = true,
    onPlaySagaLevel: (Int, BoosterType?, Boolean) -> Unit,
    onPlayDaily: (BoosterType?, Boolean) -> Unit,
    onPlayEndless: (BoosterType?, Boolean) -> Unit,
    onPlayBlitz: (BoosterType?, Boolean) -> Unit,
    onPurchaseBooster: (BoosterType) -> Unit,
    onUnlockOrEquipSkin: (CannonSkin) -> Unit,
    onClaimQuest: (CosmicQuest) -> Unit,
    onClaimDailySupply: (String) -> Unit,
    onClaimWheelPrize: (WheelPrize, String, Boolean) -> Unit,
    onClaimPassReward: (SeasonPassTier, Boolean) -> Unit,
    onOpenMysteryChest: (String, Boolean, (MysteryChestReward) -> Unit) -> Unit,
    onWatchAdForCoins: () -> Unit,
    onWatchAdForSpin: (() -> Unit) -> Unit,
    onWatchAdForChest: (() -> Unit) -> Unit,
    onWatchAdForPassBonus: (() -> Unit) -> Unit,
    onWatchAdForStarterBomb: (() -> Unit) -> Unit,
    onClearShopFeedback: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val unlockedLevel = (profile?.highestUnlockedLevel ?: 1L).toInt().coerceIn(1, LevelCatalog.sagaLevels.size)
    val levelMap = levels.associateBy { it.levelNumber.toInt() }
    val todayKey = remember { LevelCatalog.todayKey() }
    val dailyCompletedToday = profile?.lastDailyDay == todayKey
    val supplyAvailableToday = profile?.lastLoginRewardDate != todayKey
    val freeSpinAvailableToday = profile?.lastWheelSpinDate != todayKey
    val freeChestAvailableToday = profile?.lastMysteryChestDate != todayKey
    val pilotRank = remember(profile) { (profile ?: PlayerProfile()).pilotRank() }
    var showArmoryDialog by remember { mutableStateOf(false) }
    var showQuestsDialog by remember { mutableStateOf(false) }
    var showCodexDialog by remember { mutableStateOf(false) }
    var showDailySupplyDialog by remember { mutableStateOf(false) }
    var showLuckyWheelDialog by remember { mutableStateOf(false) }
    var showLeaderboardDialog by remember { mutableStateOf(false) }
    var showSeasonPassDialog by remember { mutableStateOf(false) }
    var showMysteryChestDialog by remember { mutableStateOf(false) }
    var pendingLaunch by remember { mutableStateOf<PendingMissionLaunch?>(null) }

    val claimableQuestCount = remember(profile) {
        if (profile == null) 0
        else CosmicQuest.entries.count { it.isCompleted(profile) && it.id !in profile.claimedQuests }
    }
    val claimablePassCount = remember(profile, pilotRank) {
        if (profile == null) 0
        else SeasonPassCatalog.tiers.count { tier ->
            pilotRank.xp >= tier.requiredXp && tier.freeKey !in profile.claimedPassTiers
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_cosmic_nebula_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            CosmicAbyss.copy(alpha = 0.50f),
                            CosmicAbyss.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
        ) {
            // Top Captain Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_astro_mascot),
                        contentDescription = "Captain avatar",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .border(2.dp, AstroCyan, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = profile?.displayName ?: "StarCaptain",
                                style = MaterialTheme.typography.titleLarge,
                                color = StarlightWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isGuest) {
                                Surface(
                                    color = AstroGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "GUEST",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AstroGold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BadgePill(
                                icon = Icons.Rounded.Star,
                                tint = AstroGold,
                                text = "${profile?.totalStars ?: 0}"
                            )
                            BadgePill(
                                icon = Icons.Rounded.Toll,
                                tint = AstroGold,
                                text = "${profile?.starCoins ?: 150}"
                            )
                            BadgePill(
                                icon = Icons.Rounded.Whatshot,
                                tint = AstroMagenta,
                                text = "${profile?.bombBoosters ?: 0}/${profile?.prismBoosters ?: 0}/${profile?.laserBoosters ?: 0}"
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box {
                        IconButton(
                            onClick = {
                                onClearShopFeedback()
                                showQuestsDialog = true
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(NebulaElevated)
                                .border(
                                    1.dp,
                                    if (claimableQuestCount > 0) AstroEmerald else AstroCyan.copy(alpha = 0.5f),
                                    CircleShape
                                )
                                .testTag("open_quests_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = "Cosmic Quests",
                                tint = if (claimableQuestCount > 0) AstroEmerald else AstroCyan
                            )
                        }
                        if (claimableQuestCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(AstroMagenta),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$claimableQuestCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StarlightWhite
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            onClearShopFeedback()
                            showArmoryDialog = true
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(NebulaElevated)
                            .border(1.dp, AstroGold.copy(alpha = 0.6f), CircleShape)
                            .testTag("open_armory_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Storefront,
                            contentDescription = "Cosmic Armory and Hangar",
                            tint = AstroGold
                        )
                    }
                    IconButton(
                        onClick = { showCodexDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(NebulaSurface.copy(alpha = 0.85f))
                            .testTag("open_codex_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                            contentDescription = "Cosmic Codex guide",
                            tint = AstroCyan
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(NebulaSurface.copy(alpha = 0.85f))
                            .testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings and stats",
                            tint = StarlightWhite
                        )
                    }
                }
            }

            PilotRankStrip(rank = pilotRank)

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = StarlightWhite,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Quick Action Row: 7-Day Daily Supply & Cosmic Lucky Wheel
                item {
                    val nextSupplyDay = DailySupplyCatalog.nextClaimDay(profile?.loginRewardDay ?: 0L)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickPerkCard(
                            title = "7-Day Supply",
                            subtitle = if (supplyAvailableToday) "Day $nextSupplyDay Ready!" else "Claimed Today",
                            badgeText = if (supplyAvailableToday) "CLAIM!" else "DONE",
                            accent = if (supplyAvailableToday) AstroEmerald else AstroCyan,
                            icon = Icons.Rounded.CardGiftcard,
                            highlighted = supplyAvailableToday,
                            onClick = {
                                onClearShopFeedback()
                                showDailySupplyDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_daily_supply_button")
                        )
                        QuickPerkCard(
                            title = "Lucky Wheel",
                            subtitle = if (freeSpinAvailableToday) "Free Daily Spin" else "Bonus Ad Spin",
                            badgeText = if (freeSpinAvailableToday) "FREE SPIN" else "AD SPIN",
                            accent = if (freeSpinAvailableToday) AstroGold else AstroMagenta,
                            icon = Icons.Rounded.Casino,
                            highlighted = freeSpinAvailableToday,
                            onClick = {
                                onClearShopFeedback()
                                showLuckyWheelDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_lucky_wheel_button")
                        )
                    }
                }

                // Quick Action Row 2: Mystery Star Chest & Cosmic Season Pass
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickPerkCard(
                            title = "Mystery Chest",
                            subtitle = if (freeChestAvailableToday) "Free Vault Ready!" else "Bonus Ad Chest",
                            badgeText = if (freeChestAvailableToday) "OPEN FREE" else "JACKPOT",
                            accent = if (freeChestAvailableToday) AstroMagenta else AstroViolet,
                            icon = Icons.Rounded.CardGiftcard,
                            highlighted = freeChestAvailableToday,
                            onClick = {
                                onClearShopFeedback()
                                showMysteryChestDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_mystery_chest_button")
                        )
                        QuickPerkCard(
                            title = "Season Pass",
                            subtitle = if (claimablePassCount > 0) "$claimablePassCount Tier(s) Ready!" else "12 Milestone Tiers",
                            badgeText = if (claimablePassCount > 0) "CLAIM $claimablePassCount!" else "XP TRACK",
                            accent = if (claimablePassCount > 0) AstroEmerald else AstroCyan,
                            icon = Icons.Rounded.WorkspacePremium,
                            highlighted = claimablePassCount > 0,
                            onClick = {
                                onClearShopFeedback()
                                showSeasonPassDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_season_pass_button")
                        )
                    }
                }

                // Galactic Leaderboard Strip
                item {
                    Surface(
                        color = NebulaSurface.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { showLeaderboardDialog = true }
                            .border(1.dp, AstroGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                            .testTag("open_leaderboard_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.Leaderboard, contentDescription = "Global Leaderboard", tint = AstroGold)
                                Column {
                                    Text(
                                        text = "Galactic Leaderboard",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = StarlightWhite
                                    )
                                    Text(
                                        text = "Global rankings for Endless, Cosmic Blitz & Stars",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StarlightMuted
                                    )
                                }
                            }
                            Surface(
                                color = AstroGold.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "RANKS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AstroGold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Daily Cosmic Challenge Card
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.5.dp,
                                brush = Brush.horizontalGradient(listOf(AstroGold, AstroEmerald)),
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF28204C),
                                            Color(0xFF132D3F)
                                        )
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (dailyCompletedToday) Icons.Rounded.CheckCircle else Icons.Rounded.CalendarToday,
                                            contentDescription = null,
                                            tint = if (dailyCompletedToday) AstroEmerald else AstroGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (dailyCompletedToday) "CLEARED TODAY" else "DAILY EVENT • +120 COINS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (dailyCompletedToday) AstroEmerald else AstroGold
                                        )
                                    }
                                    Text(
                                        text = "Daily Nebula",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = StarlightWhite
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = AstroMagenta,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Streak: ${profile?.dailyStreak ?: 0} days",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = StarlightMuted
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        pendingLaunch = PendingMissionLaunch(
                                            mode = GameMode.DAILY,
                                            title = "Daily Nebula",
                                            subtitle = "Rescue the 3 Daily Star Cores for +120 Star Coins!"
                                        )
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (dailyCompletedToday) AstroEmerald else AstroGold,
                                        contentColor = CosmicAbyss
                                    ),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("play_daily_button")
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text(
                                        text = if (dailyCompletedToday) "Replay" else "Launch",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // Cosmic Blitz (2-Minute Time Attack) Banner
                item {
                    Card(
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.5.dp,
                                brush = Brush.horizontalGradient(listOf(AstroCyan, AstroGold)),
                                shape = RoundedCornerShape(26.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF0E294B),
                                            Color(0xFF1F1D4C)
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Timer,
                                            contentDescription = null,
                                            tint = AstroCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "2:00 TIME ATTACK",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AstroGold
                                        )
                                    }
                                    Text(
                                        text = "Cosmic Blitz",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = StarlightWhite
                                    )
                                    Text(
                                        text = "Blitz Best: ${profile?.blitzHighScore ?: 0}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AstroCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = {
                                        pendingLaunch = PendingMissionLaunch(
                                            mode = GameMode.BLITZ,
                                            title = "Cosmic Blitz (2:00)",
                                            subtitle = "Unlimited shots, auto waves — chase Gold Ace Medal (8,000+ pts)!"
                                        )
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AstroCyan,
                                        contentColor = CosmicAbyss
                                    ),
                                    modifier = Modifier
                                        .height(50.dp)
                                        .testTag("play_blitz_button")
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text("Blitz", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }

                // Endless Panic Hero Banner
                item {
                    Card(
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.5.dp,
                                brush = Brush.horizontalGradient(listOf(AstroMagenta, AstroCyan)),
                                shape = RoundedCornerShape(26.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF34145E),
                                            Color(0xFF192454)
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Bolt,
                                            contentDescription = null,
                                            tint = AstroMagenta,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "ARCADE SURVIVAL",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AstroCyan
                                        )
                                    }
                                    Text(
                                        text = "Endless Panic",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = StarlightWhite
                                    )
                                    Text(
                                        text = "High Score: ${profile?.endlessHighScore ?: 0}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AstroGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = {
                                        pendingLaunch = PendingMissionLaunch(
                                            mode = GameMode.ENDLESS,
                                            title = "Endless Panic",
                                            subtitle = "Survive escalating cosmic waves and set a Global Leaderboard record!"
                                        )
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AstroMagenta,
                                        contentColor = StarlightWhite
                                    ),
                                    modifier = Modifier
                                        .height(50.dp)
                                        .testTag("play_endless_button")
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text("Play", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }

                // Section Header for Starlight Saga
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Starlight Saga",
                                style = MaterialTheme.typography.headlineMedium,
                                color = StarlightWhite
                            )
                            Text(
                                text = "${LevelCatalog.sagaLevels.size} Constellation Stages across 4 Sectors",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StarlightMuted
                            )
                        }
                        Surface(
                            color = NebulaSurface,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "Stage $unlockedLevel / ${LevelCatalog.sagaLevels.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = AstroCyan,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Saga Level Cards with Chapter Headers
                itemsIndexed(LevelCatalog.sagaLevels) { index, spec ->
                    val prevChapter = if (index > 0) LevelCatalog.sagaLevels[index - 1].chapter else null
                    if (spec.chapter != prevChapter) {
                        ChapterBanner(chapter = spec.chapter)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    val progress = levelMap[spec.levelNumber]
                    val isUnlocked = spec.levelNumber <= unlockedLevel
                    val isCurrent = spec.levelNumber == unlockedLevel
                    SagaLevelCard(
                        spec = spec,
                        progress = progress,
                        isUnlocked = isUnlocked,
                        isCurrent = isCurrent,
                        onClick = {
                            if (isUnlocked) {
                                pendingLaunch = PendingMissionLaunch(
                                    mode = GameMode.SAGA,
                                    levelNumber = spec.levelNumber,
                                    title = "Stage ${spec.levelNumber}: ${spec.title}",
                                    subtitle = spec.subtitle
                                )
                            }
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            AstroBannerAd(
                canRequestAds = canRequestAds,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        if (showArmoryDialog) {
            CosmicArmoryDialog(
                profile = profile,
                feedback = shopFeedback,
                isRewardedAdReady = isRewardedAdReady,
                onPurchaseBooster = onPurchaseBooster,
                onUnlockOrEquipSkin = onUnlockOrEquipSkin,
                onWatchAdForCoins = onWatchAdForCoins,
                onDismiss = {
                    onClearShopFeedback()
                    showArmoryDialog = false
                }
            )
        }

        if (showQuestsDialog) {
            CosmicQuestsDialog(
                profile = profile,
                feedback = shopFeedback,
                onClaimQuest = onClaimQuest,
                onDismiss = {
                    onClearShopFeedback()
                    showQuestsDialog = false
                }
            )
        }

        if (showCodexDialog) {
            CosmicCodexDialog(onDismiss = { showCodexDialog = false })
        }

        if (showDailySupplyDialog) {
            DailySupplyDialog(
                profile = profile,
                todayKey = todayKey,
                feedback = shopFeedback,
                onClaim = { onClaimDailySupply(todayKey) },
                onDismiss = {
                    onClearShopFeedback()
                    showDailySupplyDialog = false
                }
            )
        }

        if (showLuckyWheelDialog) {
            LuckyWheelDialog(
                freeSpinAvailable = freeSpinAvailableToday,
                isRewardedAdReady = isRewardedAdReady,
                feedback = shopFeedback,
                onWatchAdForSpin = onWatchAdForSpin,
                onPrizeWon = { prize, isFree ->
                    onClaimWheelPrize(prize, todayKey, isFree)
                },
                onDismiss = {
                    onClearShopFeedback()
                    showLuckyWheelDialog = false
                }
            )
        }

        if (showLeaderboardDialog) {
            LeaderboardDialog(
                profile = profile,
                leaderboardState = leaderboardState,
                isGuest = isGuest,
                onDismiss = { showLeaderboardDialog = false }
            )
        }

        if (showSeasonPassDialog) {
            SeasonPassDialog(
                profile = profile,
                feedback = shopFeedback,
                isRewardedAdReady = isRewardedAdReady,
                onClaimFree = { tier -> onClaimPassReward(tier, false) },
                onWatchAdForBonus = { tier ->
                    onWatchAdForPassBonus {
                        onClaimPassReward(tier, true)
                    }
                },
                onDismiss = {
                    onClearShopFeedback()
                    showSeasonPassDialog = false
                }
            )
        }

        if (showMysteryChestDialog) {
            MysteryChestDialog(
                profile = profile,
                todayKey = todayKey,
                feedback = shopFeedback,
                isRewardedAdReady = isRewardedAdReady,
                onOpenFreeChest = { onOpened ->
                    onOpenMysteryChest(todayKey, true, onOpened)
                },
                onWatchAdForChest = { onOpened ->
                    onWatchAdForChest {
                        onOpenMysteryChest(todayKey, false, onOpened)
                    }
                },
                onDismiss = {
                    onClearShopFeedback()
                    showMysteryChestDialog = false
                }
            )
        }

        pendingLaunch?.let { launch ->
            fun dispatchLaunch(booster: BoosterType?, adFree: Boolean) {
                pendingLaunch = null
                when (launch.mode) {
                    GameMode.SAGA -> onPlaySagaLevel(launch.levelNumber, booster, adFree)
                    GameMode.DAILY -> onPlayDaily(booster, adFree)
                    GameMode.ENDLESS -> onPlayEndless(booster, adFree)
                    GameMode.BLITZ -> onPlayBlitz(booster, adFree)
                }
            }
            MissionLoadoutDialog(
                launch = launch,
                profile = profile,
                isRewardedAdReady = isRewardedAdReady,
                onWatchAdForStarterBomb = {
                    onWatchAdForStarterBomb {
                        dispatchLaunch(BoosterType.BOMB, true)
                    }
                },
                onLaunch = { chosen -> dispatchLaunch(chosen, false) },
                onDismiss = { pendingLaunch = null }
            )
        }
    }
}

@Composable
private fun PilotRankStrip(rank: PilotRankInfo) {
    Surface(
        color = NebulaSurface.copy(alpha = 0.86f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 2.dp)
            .border(1.dp, AstroCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = "Pilot Rank",
                        tint = AstroGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Rank ${rank.rankNumber} • ${rank.title}",
                        style = MaterialTheme.typography.labelLarge,
                        color = StarlightWhite
                    )
                }
                Text(
                    text = if (rank.nextRankXp != null) "${rank.xp} / ${rank.nextRankXp} XP" else "${rank.xp} XP • MAX",
                    style = MaterialTheme.typography.labelSmall,
                    color = AstroCyan
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(StardustOutline.copy(alpha = 0.35f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(rank.progress.coerceIn(0.04f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(AstroCyan, AstroGold)))
                )
            }
        }
    }
}

@Composable
private fun QuickPerkCard(
    title: String,
    subtitle: String,
    badgeText: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NebulaSurface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (highlighted) 1.5.dp else 1.dp,
                color = if (highlighted) accent else StardustOutline.copy(alpha = 0.55f),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Surface(
                    color = accent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = StarlightWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = StarlightMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DailySupplyDialog(
    profile: PlayerProfile?,
    todayKey: String,
    feedback: String?,
    onClaim: () -> Unit,
    onDismiss: () -> Unit
) {
    val canClaimToday = profile?.lastLoginRewardDate != todayKey
    val rawDay = (profile?.loginRewardDay ?: 0L).toInt().coerceIn(0, 7)
    val claimedCount = if (canClaimToday) rawDay % 7 else rawDay
    val nextDay = DailySupplyCatalog.nextClaimDay(rawDay.toLong())

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
                Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = AstroEmerald)
                Text("7-Day Star Supply", style = MaterialTheme.typography.headlineMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                val firstSix = DailySupplyCatalog.tiers.take(6).chunked(3)
                firstSix.forEach { rowTiers ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTiers.forEach { tier ->
                            val isClaimed = tier.day <= claimedCount
                            val isNext = canClaimToday && tier.day == nextDay
                            SupplyTierTile(
                                tier = tier,
                                isClaimed = isClaimed,
                                isNext = isNext,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                val daySeven = DailySupplyCatalog.tiers.last()
                val daySevenClaimed = daySeven.day <= claimedCount
                val daySevenNext = canClaimToday && daySeven.day == nextDay
                Surface(
                    color = if (daySevenNext) AstroGold.copy(alpha = 0.22f) else NebulaElevated,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (daySevenNext) 2.dp else 1.dp,
                            color = if (daySevenNext) AstroGold else if (daySevenClaimed) AstroEmerald else AstroGold.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DAY 7 • JACKPOT SUPPLY",
                                style = MaterialTheme.typography.labelSmall,
                                color = AstroGold
                            )
                            Text(
                                text = "+250 Coins & All 3 Boosters!",
                                style = MaterialTheme.typography.titleMedium,
                                color = StarlightWhite
                            )
                        }
                        if (daySevenClaimed) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Claimed", tint = AstroEmerald)
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = AstroGold)
                        }
                    }
                }

                Button(
                    onClick = onClaim,
                    enabled = canClaimToday,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AstroEmerald,
                        contentColor = CosmicAbyss
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("claim_daily_supply_button")
                ) {
                    Text(
                        text = if (canClaimToday) "Claim Day $nextDay Supply" else "Come Back Tomorrow!",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp).testTag("close_daily_supply_button")
            ) {
                Text("Close", color = AstroCyan)
            }
        }
    )
}

@Composable
private fun SupplyTierTile(
    tier: DailySupplyTier,
    isClaimed: Boolean,
    isNext: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = when {
            isNext -> AstroEmerald.copy(alpha = 0.2f)
            isClaimed -> NebulaElevated.copy(alpha = 0.65f)
            else -> NebulaElevated
        },
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(
            width = if (isNext) 2.dp else 1.dp,
            color = when {
                isNext -> AstroEmerald
                isClaimed -> AstroEmerald.copy(alpha = 0.45f)
                else -> StardustOutline.copy(alpha = 0.45f)
            },
            shape = RoundedCornerShape(14.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "DAY ${tier.day}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isNext) AstroEmerald else AstroCyan
            )
            if (isClaimed) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Claimed",
                    tint = AstroEmerald,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.Rounded.Toll,
                    contentDescription = null,
                    tint = AstroGold,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = tier.title,
                style = MaterialTheme.typography.labelSmall,
                color = if (isClaimed) StarlightMuted else StarlightWhite,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LuckyWheelDialog(
    freeSpinAvailable: Boolean,
    isRewardedAdReady: Boolean,
    feedback: String?,
    onWatchAdForSpin: (() -> Unit) -> Unit,
    onPrizeWon: (WheelPrize, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val prizes = remember { WheelPrize.entries }
    val sliceColors = remember {
        listOf(
            Color(0xFF1E5F74),
            Color(0xFF7B2CBF),
            Color(0xFF0E7C66),
            Color(0xFFB5179E),
            Color(0xFF2B4C7E),
            Color(0xFF0A8754),
            Color(0xFFD97706),
            Color(0xFF4361EE)
        )
    }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var isSpinning by remember { mutableStateOf(false) }
    var lastWonPrize by remember { mutableStateOf<WheelPrize?>(null) }
    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.RIGHT
            isFakeBoldText = true
            isAntiAlias = true
        }
    }

    fun startSpin(isFreeDaily: Boolean) {
        if (isSpinning) return
        isSpinning = true
        lastWonPrize = null
        val targetIndex = prizes.indices.random()
        val selected = prizes[targetIndex]
        val baseMod = ((rotation.value % 360f) + 360f) % 360f
        val targetMod = (360f - targetIndex * 45f) % 360f
        val delta = ((targetMod - baseMod) + 360f) % 360f
        val jitter = (-10..10).random().toFloat()
        val finalTarget = rotation.value + 5f * 360f + delta + jitter
        scope.launch {
            rotation.animateTo(
                targetValue = finalTarget,
                animationSpec = tween(durationMillis = 3400, easing = FastOutSlowInEasing)
            )
            lastWonPrize = selected
            isSpinning = false
            onPrizeWon(selected, isFreeDaily)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSpinning) onDismiss() },
        containerColor = NebulaSurface,
        titleContentColor = StarlightWhite,
        textContentColor = StarlightWhite,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Casino, contentDescription = null, tint = AstroGold)
                Text("Cosmic Lucky Wheel", style = MaterialTheme.typography.headlineMedium)
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(230.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.minDimension * 0.44f
                        val center = Offset(size.width / 2f, size.height / 2f + 6.dp.toPx())
                        val topLeft = Offset(center.x - radius, center.y - radius)
                        val arcSize = Size(radius * 2f, radius * 2f)

                        drawCircle(
                            color = AstroGold.copy(alpha = 0.25f),
                            radius = radius + 8.dp.toPx(),
                            center = center
                        )

                        rotate(degrees = rotation.value, pivot = center) {
                            prizes.forEachIndexed { idx, prize ->
                                val startAngle = -90f - 22.5f + idx * 45f
                                drawArc(
                                    color = sliceColors[idx % sliceColors.size],
                                    startAngle = startAngle,
                                    sweepAngle = 45f,
                                    useCenter = true,
                                    topLeft = topLeft,
                                    size = arcSize
                                )
                                drawArc(
                                    color = StarlightWhite.copy(alpha = 0.35f),
                                    startAngle = startAngle,
                                    sweepAngle = 45f,
                                    useCenter = true,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                                val midAngle = -90f + idx * 45f
                                val rad = midAngle * PI.toFloat() / 180f
                                textPaint.textSize = 12.dp.toPx()
                                val canvas = drawContext.canvas.nativeCanvas
                                canvas.save()
                                canvas.translate(center.x, center.y)
                                canvas.rotate(midAngle)
                                canvas.drawText(
                                    prize.shortLabel,
                                    radius * 0.86f,
                                    4.dp.toPx(),
                                    textPaint
                                )
                                canvas.restore()
                                // Dot peg at slice boundary
                                val pegRad = (startAngle) * PI.toFloat() / 180f
                                drawCircle(
                                    color = AstroGold,
                                    radius = 3.dp.toPx(),
                                    center = Offset(
                                        center.x + cos(pegRad) * (radius - 2.dp.toPx()),
                                        center.y + sin(pegRad) * (radius - 2.dp.toPx())
                                    )
                                )
                                // Keep rad referenced cleanly
                                if (rad.isNaN()) return@forEachIndexed
                            }
                        }

                        // Outer golden rim
                        drawCircle(
                            color = AstroGold,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 4.dp.toPx())
                        )

                        // Center hub
                        drawCircle(
                            color = CosmicAbyss,
                            radius = 22.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = AstroCyan,
                            radius = 22.dp.toPx(),
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Top pointer triangle
                        val pointerTipY = center.y - radius + 8.dp.toPx()
                        val pointerTopY = center.y - radius - 14.dp.toPx()
                        val path = Path().apply {
                            moveTo(center.x, pointerTipY)
                            lineTo(center.x - 11.dp.toPx(), pointerTopY)
                            lineTo(center.x + 11.dp.toPx(), pointerTopY)
                            close()
                        }
                        drawPath(path = path, color = AstroGold)
                    }
                }

                val banner = lastWonPrize?.let { "You Won: ${it.fullLabel}" } ?: feedback
                if (banner != null) {
                    Surface(
                        color = AstroGold.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AstroGold.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = banner,
                            style = MaterialTheme.typography.titleMedium,
                            color = AstroGold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (freeSpinAvailable) {
                    Button(
                        onClick = { startSpin(isFreeDaily = true) },
                        enabled = !isSpinning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AstroGold,
                            contentColor = CosmicAbyss
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("spin_wheel_free_button")
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = if (isSpinning) "Spinning..." else "Spin Free Daily Wheel!",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            onWatchAdForSpin {
                                startSpin(isFreeDaily = false)
                            }
                        },
                        enabled = isRewardedAdReady && !isSpinning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AstroMagenta,
                            contentColor = StarlightWhite
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("spin_wheel_ad_button")
                    ) {
                        Icon(Icons.Rounded.PlayCircle, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = when {
                                isSpinning -> "Spinning..."
                                isRewardedAdReady -> "Watch Ad for Bonus Spin!"
                                else -> "Loading Ad..."
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (!isSpinning) onDismiss() },
                enabled = !isSpinning,
                modifier = Modifier.height(48.dp).testTag("close_lucky_wheel_button")
            ) {
                Text("Close", color = AstroCyan)
            }
        }
    )
}

@Composable
private fun ChapterBanner(chapter: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = AstroCyan.copy(alpha = 0.16f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.border(1.dp, AstroCyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
        ) {
            Text(
                text = chapter.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = AstroCyan,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(StardustOutline.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun CosmicArmoryDialog(
    profile: PlayerProfile?,
    feedback: String?,
    isRewardedAdReady: Boolean,
    onPurchaseBooster: (BoosterType) -> Unit,
    onUnlockOrEquipSkin: (CannonSkin) -> Unit,
    onWatchAdForCoins: () -> Unit,
    onDismiss: () -> Unit
) {
    val coins = profile?.starCoins ?: 0L
    var selectedTab by remember { mutableIntStateOf(0) }
    val equippedSkin = profile?.resolvedSkin() ?: CannonSkin.CYAN_INTERCEPTOR
    val unlockedSkins = profile?.unlockedSkins ?: listOf(CannonSkin.CYAN_INTERCEPTOR.name)

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
                Text("Cosmic Armory", style = MaterialTheme.typography.headlineMedium)
                BadgePill(icon = Icons.Rounded.Toll, tint = AstroGold, text = "$coins")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = AstroGold.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AstroGold.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Free +75 Star Coins",
                                style = MaterialTheme.typography.titleMedium,
                                color = AstroGold
                            )
                            Text(
                                text = if (isRewardedAdReady) "Watch a short video ad" else "Loading rewarded ad...",
                                style = MaterialTheme.typography.bodySmall,
                                color = StarlightMuted
                            )
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Button(
                            onClick = onWatchAdForCoins,
                            enabled = isRewardedAdReady,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AstroGold,
                                contentColor = CosmicAbyss
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("watch_ad_coins_button")
                        ) {
                            Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(if (isRewardedAdReady) "+75c" else "Wait", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ArmoryTabPill(
                        label = "Boosters",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("armory_tab_boosters")
                    )
                    ArmoryTabPill(
                        label = "Cannon Hangar",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("armory_tab_hangar")
                    )
                }

                if (feedback != null) {
                    Surface(
                        color = AstroCyan.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = feedback,
                            style = MaterialTheme.typography.labelLarge,
                            color = AstroCyan,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (selectedTab == 0) {
                    BoosterType.entries.forEach { booster ->
                        val owned = when (booster) {
                            BoosterType.BOMB -> profile?.bombBoosters ?: 0L
                            BoosterType.PRISM -> profile?.prismBoosters ?: 0L
                            BoosterType.LASER -> profile?.laserBoosters ?: 0L
                        }
                        val desc = when (booster) {
                            BoosterType.BOMB -> "Blasts a wide 2-ring cosmic crater"
                            BoosterType.PRISM -> "Matches any adjacent bubble cluster"
                            BoosterType.LASER -> "Pierces through entire columns"
                        }
                        val accent = when (booster) {
                            BoosterType.BOMB -> AstroMagenta
                            BoosterType.PRISM -> AstroGold
                            BoosterType.LASER -> AstroCyan
                        }
                        Surface(
                            color = NebulaElevated,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${booster.label} (x$owned)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = accent
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StarlightMuted
                                    )
                                }
                                Spacer(modifier = Modifier.size(8.dp))
                                Button(
                                    onClick = { onPurchaseBooster(booster) },
                                    enabled = coins >= booster.coinCost,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accent,
                                        contentColor = CosmicAbyss
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("buy_booster_${booster.name.lowercase()}")
                                ) {
                                    Text("${booster.coinCost}c", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                } else {
                    CannonSkin.entries.forEach { skin ->
                        val isUnlocked = skin == CannonSkin.CYAN_INTERCEPTOR || skin.name in unlockedSkins
                        val isEquipped = skin == equippedSkin
                        val skinColor = skinAccentColor(skin)
                        Surface(
                            color = NebulaElevated,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isEquipped) 1.5.dp else 0.dp,
                                    color = if (isEquipped) skinColor else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
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
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(skinColor.copy(alpha = 0.25f))
                                            .border(2.dp, skinColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.RocketLaunch,
                                            contentDescription = null,
                                            tint = skinColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = skin.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = skinColor
                                        )
                                        Text(
                                            text = skin.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = StarlightMuted
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.size(8.dp))
                                Button(
                                    onClick = { onUnlockOrEquipSkin(skin) },
                                    enabled = !isEquipped && (isUnlocked || coins >= skin.coinCost),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isEquipped) AstroEmerald else skinColor,
                                        contentColor = CosmicAbyss
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("skin_action_${skin.name.lowercase()}")
                                ) {
                                    Text(
                                        text = when {
                                            isEquipped -> "Active"
                                            isUnlocked -> "Equip"
                                            else -> "${skin.coinCost}c"
                                        },
                                        style = MaterialTheme.typography.labelLarge
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
                modifier = Modifier.height(48.dp).testTag("close_armory_button")
            ) {
                Text("Done", color = AstroCyan)
            }
        }
    )
}

@Composable
private fun ArmoryTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) AstroCyan.copy(alpha = 0.22f) else NebulaElevated,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = if (selected) AstroCyan else StardustOutline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) AstroCyan else StarlightMuted
            )
        }
    }
}

private fun skinAccentColor(skin: CannonSkin): Color = when (skin) {
    CannonSkin.CYAN_INTERCEPTOR -> AstroCyan
    CannonSkin.SOLAR_VALKYRIE -> AstroGold
    CannonSkin.PLASMA_NOVA -> AstroMagenta
    CannonSkin.VOID_SINGULARITY -> AstroViolet
}

@Composable
private fun CosmicQuestsDialog(
    profile: PlayerProfile?,
    feedback: String?,
    onClaimQuest: (CosmicQuest) -> Unit,
    onDismiss: () -> Unit
) {
    val claimed = profile?.claimedQuests ?: emptyList()
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
                Text("Cosmic Quests", style = MaterialTheme.typography.headlineMedium)
                BadgePill(icon = Icons.Rounded.EmojiEvents, tint = AstroGold, text = "${claimed.size}/${CosmicQuest.entries.size}")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                CosmicQuest.entries.forEach { quest ->
                    val isClaimed = quest.id in claimed
                    val isDone = profile != null && quest.isCompleted(profile)
                    val boosterStr = quest.rewardBooster?.let { " + 1 ${it.label}" } ?: ""
                    Surface(
                        color = NebulaElevated,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isDone && !isClaimed) 1.5.dp else 0.dp,
                                color = if (isDone && !isClaimed) AstroEmerald else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = quest.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isClaimed) StarlightMuted else StarlightWhite
                                )
                                Text(
                                    text = quest.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StarlightMuted
                                )
                                Text(
                                    text = if (profile != null) "${quest.progressText(profile)} • Reward: +${quest.rewardCoins}c$boosterStr" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDone) AstroEmerald else AstroGold
                                )
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                            Button(
                                onClick = { onClaimQuest(quest) },
                                enabled = isDone && !isClaimed,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDone && !isClaimed) AstroEmerald else NebulaSurface,
                                    contentColor = if (isDone && !isClaimed) CosmicAbyss else StarlightMuted
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("claim_quest_${quest.id}")
                            ) {
                                Text(
                                    text = when {
                                        isClaimed -> "Claimed"
                                        isDone -> "Claim"
                                        else -> "Locked"
                                    },
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp).testTag("close_quests_button")
            ) {
                Text("Close", color = AstroCyan)
            }
        }
    )
}

@Composable
private fun CosmicCodexDialog(onDismiss: () -> Unit) {
    val entries = listOf(
        Triple("Shielded Armor (Stages 19–30)", "Titanium-plated bubbles resist direct matches — pop an adjacent cluster, or blast them with Nova Bombs & Lasers!", AstroCyan),
        Triple("Cosmic Blitz (2:00 Time Attack)", "Race the 2-minute clock with unlimited shots and auto waves. Hit 8,000+ points for the Gold Ace Medal!", AstroGold),
        Triple("Boss Guardians (Stages 6, 12, 18, 24, 30)", "Every pop & avalanche damages the Sector Boss. Watch its countdown — it casts Frost curses if unchallenged!", DangerCrimson),
        Triple("Smart Cluster Aim", "Hovering aim illuminates the exact 3+ bubble cluster you will detonate before you fire.", AstroCyan),
        Triple("x5 Well & Avalanche Bonus", "Land detached bubbles in the center x5 bucket for +3 Star Coins each! Drop 6+ bubbles at once for +1 Shot.", AstroGold),
        Triple("Overdrive 2X Fever", "Chain 5 consecutive popping shots to ignite Overdrive 2X — double points & instant row shockwaves!", AstroMagenta),
        Triple("Warp Portals & Black Holes", "Portals warp your shot across the board. Black Holes absorb 1 bubble, then implode an entire blast ring!", AstroViolet),
        Triple("Lightning, Chameleon & Star Cores", "Lightning zaps an entire row. Chameleons morph every turn. Rescue Star Cores by popping or dropping them!", AstroEmerald)
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
                Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = AstroCyan)
                Text("Cosmic Codex", style = MaterialTheme.typography.headlineMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                entries.forEach { (title, body, accent) ->
                    Surface(
                        color = NebulaElevated,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(title, style = MaterialTheme.typography.titleMedium, color = accent)
                            Text(body, style = MaterialTheme.typography.bodySmall, color = StarlightMuted)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp).testTag("close_codex_button")
            ) {
                Text("Got it", color = AstroCyan)
            }
        }
    )
}

@Composable
private fun BadgePill(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, text: String) {
    Surface(
        color = NebulaSurface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.border(1.dp, StardustOutline.copy(alpha = 0.5f), RoundedCornerShape(50))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall, color = StarlightWhite)
        }
    }
}

@Composable
private fun SagaLevelCard(
    spec: SagaLevelSpec,
    progress: LevelProgress?,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val stars = (progress?.stars ?: 0L).toInt().coerceIn(0, 3)
    val containerColor = when {
        isCurrent -> NebulaElevated
        isUnlocked -> NebulaSurface.copy(alpha = 0.92f)
        else -> NebulaSurface.copy(alpha = 0.45f)
    }
    val borderColor = when {
        isCurrent -> AstroCyan
        stars == 3 -> AstroGold.copy(alpha = 0.6f)
        isUnlocked -> StardustOutline
        else -> StardustOutline.copy(alpha = 0.3f)
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .clickable(enabled = isUnlocked, onClick = onClick)
            .testTag("saga_level_${spec.levelNumber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUnlocked) {
                                Brush.linearGradient(listOf(AstroCyan, AstroMagenta))
                            } else {
                                Brush.linearGradient(listOf(StardustOutline, CosmicAbyss))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUnlocked) {
                        Text(
                            text = "${spec.levelNumber}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = StarlightWhite
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Locked stage",
                            tint = StarlightMuted
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = spec.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isUnlocked) StarlightWhite else StarlightMuted
                        )
                        if (spec.goal == LevelGoal.RESCUE_STARS) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Star Rescue",
                                tint = AstroGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (spec.bossSpec != null) {
                            Surface(
                                color = DangerCrimson.copy(alpha = 0.24f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.border(1.dp, DangerCrimson.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shield,
                                        contentDescription = "Boss Stage",
                                        tint = DangerCrimson,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "BOSS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DangerCrimson
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = spec.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StarlightMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if ((progress?.bestScore ?: 0L) > 0L) {
                        Text(
                            text = "Best: ${progress?.bestScore} • ${spec.movesLimit} shots",
                            style = MaterialTheme.typography.labelSmall,
                            color = AstroEmerald
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (i in 1..3) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = if (i <= stars) AstroGold else StardustOutline.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (isCurrent) {
                    Surface(
                        color = AstroCyan,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "PLAY",
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicAbyss,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
