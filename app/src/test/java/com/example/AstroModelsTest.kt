package com.example

import com.example.data.DailySupplyCatalog
import com.example.data.PlayerProfile
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
    fun wheelPrizes_hasEightNonEmptyPrizes() {
        assertEquals(8, WheelPrize.entries.size)
        assertTrue(WheelPrize.entries.all { it.coins > 0L || it.booster != null })
    }
}
