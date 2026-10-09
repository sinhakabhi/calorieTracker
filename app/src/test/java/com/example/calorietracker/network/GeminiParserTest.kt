package com.example.calorietracker.network

import com.example.calorietracker.domain.ActivityLevel
import com.example.calorietracker.domain.Goal
import com.example.calorietracker.domain.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiParserTest {

    @Test
    fun parseEstimate_validJson() {
        val text = """
            {"name": "Roti, dal and salad", "calories": 420, "protein_g": 16.5,
             "carbs_g": 62, "fat_g": 11.2, "note": "2 rotis (30 g atta each), 1 bowl dal (150 g)."}
        """.trimIndent()

        val estimate = GeminiParser.parseEstimate(text)

        assertEquals(
            MealEstimate("Roti, dal and salad", 420, 16.5, 62.0, 11.2, "2 rotis (30 g atta each), 1 bowl dal (150 g)."),
            estimate,
        )
    }

    @Test
    fun parseEstimate_fencedJson() {
        val text = """
            ```json
            {"name": "Poha", "calories": 250, "protein_g": 5, "carbs_g": 45, "fat_g": 6, "note": "1 plate"}
            ```
        """.trimIndent()

        val estimate = GeminiParser.parseEstimate(text)

        assertNotNull(estimate)
        assertEquals("Poha", estimate!!.name)
        assertEquals(250, estimate.calories)
        assertEquals(45.0, estimate.carbsG, 0.001)
    }

    @Test
    fun parseEstimate_missingFieldsUseDefaults() {
        val estimate = GeminiParser.parseEstimate("""{"calories": "310.6"}""")

        assertEquals(MealEstimate(GeminiParser.DEFAULT_NAME, 311, 0.0, 0.0, 0.0, ""), estimate)
    }

    @Test
    fun parseEstimate_negativeAndInvalidNumbersBecomeZero() {
        val estimate = GeminiParser.parseEstimate("""{"name": "Tea", "calories": -5, "protein_g": "lots"}""")

        assertEquals(0, estimate!!.calories)
        assertEquals(0.0, estimate.proteinG, 0.001)
    }

    @Test
    fun parseEstimate_garbageReturnsNull() {
        assertNull(GeminiParser.parseEstimate("Sorry, I can't help with that."))
        assertNull(GeminiParser.parseEstimate("{not json at all"))
        assertNull(GeminiParser.parseEstimate(""))
    }

    @Test
    fun extractText_readsCandidateTextAndSkipsThoughts() {
        val body = """
            {"candidates": [{"content": {"role": "model", "parts": [
                {"text": "thinking...", "thought": true},
                {"text": "{\"name\": \"Idli\", \"calories\": 160}"}
            ]}}]}
        """.trimIndent()

        assertEquals("""{"name": "Idli", "calories": 160}""", GeminiParser.extractText(body))
    }

    @Test
    fun extractText_missingCandidatesReturnsNull() {
        assertNull(GeminiParser.extractText("""{"promptFeedback": {"blockReason": "SAFETY"}}"""))
        assertNull(GeminiParser.extractText("not json"))
    }

    @Test
    fun parseProfile_fullDescription() {
        val text = """
            {"age": 27, "sex": "MALE", "height_cm": 182, "weight_kg": 100, "activity_level": "MODERATE",
             "goal": "LOSE", "note": "Target weight 90 kg, so the goal is to lose."}
        """.trimIndent()

        assertEquals(
            ProfileExtraction(27, Sex.MALE, 182.0, 100.0, ActivityLevel.MODERATE, Goal.LOSE, "Target weight 90 kg, so the goal is to lose."),
            GeminiParser.parseProfile(text),
        )
    }

    @Test
    fun parseProfile_nullsAndMissingFieldsStayNull() {
        val profile = GeminiParser.parseProfile("""```json
            {"age": null, "sex": "female", "weight_kg": 62.5, "activity_level": null, "note": null}
            ```""")!!

        assertEquals(ProfileExtraction(sex = Sex.FEMALE, weightKg = 62.5), profile)
    }

    @Test
    fun parseProfile_invalidValuesBecomeNull() {
        val profile = GeminiParser.parseProfile("""{"age": -3, "height_cm": "tall", "sex": "OTHER", "goal": "BULK"}""")!!

        assertTrue(profile.isEmpty)
    }

    @Test
    fun parseProfile_garbageReturnsNull() {
        assertNull(GeminiParser.parseProfile("I couldn't find anything."))
    }

    @Test
    fun parseEstimate_readsNotFoodFlag() {
        val estimate = GeminiParser.parseEstimate("""{"is_food": false, "name": "", "calories": 0, "note": "A poem request."}""")!!

        assertEquals(false, estimate.isFood)
    }

    @Test
    fun parseEstimate_clampsAbsurdValues() {
        val estimate = GeminiParser.parseEstimate(
            """{"is_food": true, "name": "${"x".repeat(200)}", "calories": 99999, "protein_g": 5000, "carbs_g": 10, "fat_g": 1e9}"""
        )!!

        assertEquals(GeminiParser.MAX_NAME_CHARS, estimate.name.length)
        assertEquals(GeminiParser.MAX_MEAL_KCAL, estimate.calories)
        assertEquals(GeminiParser.MAX_MACRO_G, estimate.proteinG, 0.001)
        assertEquals(GeminiParser.MAX_MACRO_G, estimate.fatG, 0.001)
    }

    @Test
    fun parseProfile_dropsOutOfRangeValuesAndReadsFlag() {
        val profile = GeminiParser.parseProfile("""{"is_profile": true, "age": 7, "height_cm": 900, "weight_kg": 80}""")!!
        assertEquals(ProfileExtraction(weightKg = 80.0), profile)

        assertEquals(false, GeminiParser.parseProfile("""{"is_profile": false, "note": "Not about a person."}""")!!.isProfile)
    }
}
