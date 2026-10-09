package com.example.calorietracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalorieCalculatorTest {

    @Test
    fun bmr_male_usesMifflinStJeor() {
        // 10*80 + 6.25*180 - 5*30 + 5 = 1780
        assertEquals(1780.0, CalorieCalculator.bmr(30, Sex.MALE, 180.0, 80.0), 0.001)
    }

    @Test
    fun bmr_female_usesMifflinStJeor() {
        // 10*60 + 6.25*165 - 5*25 - 161 = 1345.25
        assertEquals(1345.25, CalorieCalculator.bmr(25, Sex.FEMALE, 165.0, 60.0), 0.001)
    }

    @Test
    fun dailyTarget_maintain_isBmrTimesActivity() {
        // 1780 * 1.2 = 2136
        val target = CalorieCalculator.dailyTarget(30, Sex.MALE, 180.0, 80.0, ActivityLevel.SEDENTARY, Goal.MAINTAIN)
        assertEquals(2136, target)
    }

    @Test
    fun dailyTarget_lose_subtracts500() {
        // 1345.25 * 1.55 - 500 = 1585.14
        val target = CalorieCalculator.dailyTarget(25, Sex.FEMALE, 165.0, 60.0, ActivityLevel.MODERATE, Goal.LOSE)
        assertEquals(1585, target)
    }

    @Test
    fun dailyTarget_gain_adds300() {
        // 1780 * 1.55 + 300 = 3059
        val target = CalorieCalculator.dailyTarget(30, Sex.MALE, 180.0, 80.0, ActivityLevel.MODERATE, Goal.GAIN)
        assertEquals(3059, target)
    }

    @Test
    fun dailyTarget_neverBelowFloor() {
        // 826.5 * 1.2 - 500 = 491.8, raised to the 1200 floor
        val target = CalorieCalculator.dailyTarget(70, Sex.FEMALE, 150.0, 40.0, ActivityLevel.SEDENTARY, Goal.LOSE)
        assertEquals(CalorieCalculator.MIN_DAILY_KCAL, target)
    }

    @Test
    fun proteinTarget_scalesWithWeightAndActivity() {
        assertEquals(160, CalorieCalculator.proteinTarget(100.0, ActivityLevel.MODERATE))
        assertEquals(60, CalorieCalculator.proteinTarget(60.0, ActivityLevel.SEDENTARY))
        assertEquals(126, CalorieCalculator.proteinTarget(70.0, ActivityLevel.VERY_ACTIVE))
    }

    @Test
    fun macroTargets_splitsCaloriesIntoProteinFatAndCarbs() {
        // Protein 80 kg × 1.6 = 128 g (512 kcal), fat 2136 × 0.25 / 9 = 59 g (531 kcal),
        // carbs (2136 − 512 − 531) / 4 = 273 g
        assertEquals(MacroTargets(proteinG = 128, carbsG = 273, fatG = 59), CalorieCalculator.macroTargets(2136, 80.0, ActivityLevel.MODERATE))
    }

    @Test
    fun macroTargets_carbsNeverNegative() {
        // Protein alone (270 g = 1080 kcal) plus fat exceeds 1200 kcal.
        assertEquals(0, CalorieCalculator.macroTargets(1200, 150.0, ActivityLevel.VERY_ACTIVE).carbsG)
    }

    @Test
    fun waterTarget_is35mlPerKgWithinLimits() {
        assertEquals(3500, CalorieCalculator.waterTargetMl(100.0))
        assertEquals(2450, CalorieCalculator.waterTargetMl(70.0))
        assertEquals(2000, CalorieCalculator.waterTargetMl(45.0)) // 1575 ml raised to the 2 L minimum
        assertEquals(4000, CalorieCalculator.waterTargetMl(150.0)) // 5250 ml capped at 4 L
    }

    @Test
    fun validation_checksRanges() {
        assertTrue(CalorieCalculator.isValidAge(13))
        assertTrue(CalorieCalculator.isValidAge(100))
        assertFalse(CalorieCalculator.isValidAge(12))
        assertFalse(CalorieCalculator.isValidAge(null))
        assertTrue(CalorieCalculator.isValidHeight(250.0))
        assertFalse(CalorieCalculator.isValidHeight(99.9))
        assertTrue(CalorieCalculator.isValidWeight(30.0))
        assertFalse(CalorieCalculator.isValidWeight(300.5))
    }
}
