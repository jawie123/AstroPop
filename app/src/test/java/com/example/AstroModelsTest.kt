package com.example

import com.example.data.DailySupplyCatalog
import com.example.data.MysteryChestReward
import com.example.data.PlayerProfile
import com.example.data.SeasonPassCatalog
import com.example.data.WheelPrize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AstroModelsTest {

    @Test
    fun dailySupplyCatalog_cyclesFromDay1ToDay7AndWraps() {
        assertEquals(1, DailySupplyCatalog.nextClaimDay(0L))
        assertEquals(2, DailySupplyCatalog.nextClaimDay(1L))
        assertEquals(7, DailySupplyCatalog.nextClaimDay(6L))
        assertEquals(1, DailySupplyCatalog.nextClaimDay(7L))

        val day7 = DailySupplyCatalog.tierForDay(7)
        assertEquals(250L, day7.coins)
        assertEquals(1L, day7.bombs)
        assertEquals(1L, day7.prisms)
        assertEquals(1L, day7.lasers)
    }

    @Test
    fun pilotRank_scalesAcrossTiersAndCapsAtMaxRank() {
        val rookie = PlayerProfile().pilotRank()
        assertEquals(1, rookie.rankNumber)
        assertEquals("Star Cadet", rookie.title)
        assertEquals(500L, rookie.nextRankXp)

        val legend = PlayerProfile(
            totalBubblesPopped = 5000L,
            totalStars = 40L,
            endlessHighScore = 5000L
        ).pilotRank()
        assertEquals(6, legend.rankNumber)
        assertEquals("Galactic Legend", legend.title)
        assertNull(legend.nextRankXp)
        assertEquals(1f, legend.progress, 0.001f)
    }

    @Test
    fun pilotRank_includesBlitzHighScoreInXpCalculation() {
        val blitzPilot = PlayerProfile(blitzHighScore = 7500L).pilotRank()
        assertEquals(500L, blitzPilot.xp)
        assertEquals(2, blitzPilot.rankNumber)
        assertEquals("Orbit Striker", blitzPilot.title)
    }

    @Test
    fun wheelPrizes_hasEightNonEmptyPrizes() {
        assertEquals(8, WheelPrize.entries.size)
        assertTrue(WheelPrize.entries.all { it.coins > 0L || it.booster != null })
    }

    @Test
    fun seasonPassCatalog_has12AscendingTiersWithUniqueKeys() {
        val tiers = SeasonPassCatalog.tiers
        assertEquals(12, tiers.size)
        assertEquals((1..12).toList(), tiers.map { it.tier })
        assertTrue(tiers.zipWithNext().all { (a, b) -> b.requiredXp > a.requiredXp })
        val allKeys = tiers.flatMap { listOf(it.freeKey, it.bonusKey) }
        assertEquals(24, allKeys.distinct().size)
    }

    @Test
    fun mysteryChestRewards_allGrantPositiveValueAndRollRandomReturnsValidEntry() {
        assertEquals(6, MysteryChestReward.entries.size)
        assertTrue(MysteryChestReward.entries.all { it.coins > 0L || it.bombs > 0L || it.prisms > 0L || it.lasers > 0L })
        repeat(20) {
            val rolled = MysteryChestReward.rollRandom()
            assertTrue(rolled in MysteryChestReward.entries)
        }
    }
}
