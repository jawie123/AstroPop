package com.example.ui

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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Toll
import androidx.compose.material.icons.rounded.Whatshot
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BoosterType
import com.example.data.CannonSkin
import com.example.data.CosmicQuest
import com.example.data.LevelProgress
import com.example.data.PlayerProfile
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
    isGuest: Boolean,
    errorMessage: String?,
    shopFeedback: String?,
    onPlaySagaLevel: (Int) -> Unit,
    onPlayDaily: () -> Unit,
    onPlayEndless: () -> Unit,
    onPurchaseBooster: (BoosterType) -> Unit,
    onUnlockOrEquipSkin: (CannonSkin) -> Unit,
    onClaimQuest: (CosmicQuest) -> Unit,
    onClearShopFeedback: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val unlockedLevel = (profile?.highestUnlockedLevel ?: 1L).toInt().coerceIn(1, LevelCatalog.sagaLevels.size)
    val levelMap = levels.associateBy { it.levelNumber.toInt() }
    val todayKey = remember { LevelCatalog.todayKey() }
    val dailyCompletedToday = profile?.lastDailyDay == todayKey
    var showArmoryDialog by remember { mutableStateOf(false) }
    var showQuestsDialog by remember { mutableStateOf(false) }
    var showCodexDialog by remember { mutableStateOf(false) }
    val claimableQuestCount = remember(profile) {
        if (profile == null) 0
        else CosmicQuest.entries.count { it.isCompleted(profile) && it.id !in profile.claimedQuests }
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
                                    onClick = onPlayDaily,
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
                                    onClick = onPlayEndless,
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
                                text = "18 Constellation Stages across 3 Sectors",
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
                        onClick = { if (isUnlocked) onPlaySagaLevel(spec.levelNumber) }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }

        if (showArmoryDialog) {
            CosmicArmoryDialog(
                profile = profile,
                feedback = shopFeedback,
                onPurchaseBooster = onPurchaseBooster,
                onUnlockOrEquipSkin = onUnlockOrEquipSkin,
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
    }
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
    onPurchaseBooster: (BoosterType) -> Unit,
    onUnlockOrEquipSkin: (CannonSkin) -> Unit,
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
        Triple("Boss Guardians (Stages 6, 12, 18)", "Every pop & avalanche damages the Sector Boss. Watch its countdown — it casts Frost curses if unchallenged!", DangerCrimson),
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
