package com.veyra.app.network

import com.veyra.app.data.DayDraft
import com.veyra.app.data.ExerciseDraft
import com.veyra.app.data.PlanDraft
import com.veyra.app.domain.ActivityLevel
import com.veyra.app.domain.CalorieCalculator
import com.veyra.app.domain.Goal
import com.veyra.app.domain.Sex
import org.json.JSONException
import org.json.JSONObject
import java.time.DayOfWeek
import kotlin.math.roundToInt

/** What Gemini estimated for a meal description. */
data class MealEstimate(
    val name: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val note: String,
    /** False when the model decided the text wasn't about food. */
    val isFood: Boolean = true,
)

/** Profile details Gemini found in the user's description; null means "not mentioned". */
data class ProfileExtraction(
    val age: Int? = null,
    val sex: Sex? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val activity: ActivityLevel? = null,
    val goal: Goal? = null,
    val note: String = "",
    /** False when the model decided the text wasn't a description of the user. */
    val isProfile: Boolean = true,
) {
    val isEmpty: Boolean
        get() = age == null && sex == null && heightCm == null && weightKg == null && activity == null && goal == null
}

/** A parsed training-plan answer; [isFitness] is false when the request wasn't about training. */
data class GeneratedPlan(val isFitness: Boolean, val draft: PlanDraft)

/** A parsed single-day edit; [day] is null when [isFitness] is false. */
data class GeneratedDay(val isFitness: Boolean, val day: DayDraft?)

/** Defensive parsing of Gemini responses. Never throws. */
object GeminiParser {

    const val DEFAULT_NAME = "Meal"

    // Upper bounds for one meal; anything larger is treated as a bad answer and clamped.
    const val MAX_MEAL_KCAL = 5000
    const val MAX_MACRO_G = 500.0
    const val MAX_NAME_CHARS = 60
    const val MAX_NOTE_CHARS = 300

    // Limits for generated training plans.
    const val MAX_EXERCISES_PER_DAY = 10
    const val MAX_SETS = 20
    private const val MAX_TITLE_CHARS = 60
    private const val MAX_TARGET_CHARS = 40
    private const val MAX_EXERCISE_NOTE_CHARS = 200
    private const val MAX_PLAN_NOTES_CHARS = 800

    /**
     * Pulls the model's text out of a generateContent response body
     * (candidates[0].content.parts[*].text, skipping "thought" parts).
     * Returns null if there is no text.
     */
    fun extractText(responseBody: String): String? = try {
        val parts = JSONObject(responseBody)
            .optJSONArray("candidates")?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
        if (parts == null) {
            null
        } else {
            val text = buildString {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue
                    if (part.optBoolean("thought", false)) continue
                    append(part.optString("text", ""))
                }
            }
            text.ifBlank { null }
        }
    } catch (e: JSONException) {
        null
    }

    /**
     * Parses the JSON object the model returned. Strips ``` fences and surrounding text,
     * fills defaults for missing fields, and returns null if no JSON object can be read.
     */
    fun parseEstimate(text: String): MealEstimate? {
        val json = extractJsonObject(text) ?: return null
        return try {
            val obj = JSONObject(json)
            MealEstimate(
                name = obj.optString("name", "").trim().take(MAX_NAME_CHARS).ifEmpty { DEFAULT_NAME },
                calories = obj.nonNegativeDouble("calories").roundToInt().coerceAtMost(MAX_MEAL_KCAL),
                proteinG = obj.nonNegativeDouble("protein_g").coerceAtMost(MAX_MACRO_G),
                carbsG = obj.nonNegativeDouble("carbs_g").coerceAtMost(MAX_MACRO_G),
                fatG = obj.nonNegativeDouble("fat_g").coerceAtMost(MAX_MACRO_G),
                note = obj.optString("note", "").trim().take(MAX_NOTE_CHARS),
                isFood = obj.optBoolean("is_food", true),
            )
        } catch (e: JSONException) {
            null
        }
    }

    /**
     * Parses the profile JSON. Missing, null, unknown or out-of-range values (see [CalorieCalculator]'s
     * validation) become null so the form keeps what the user already entered.
     * Returns null if no JSON object can be read.
     */
    fun parseProfile(text: String): ProfileExtraction? {
        val json = extractJsonObject(text) ?: return null
        return try {
            val obj = JSONObject(json)
            ProfileExtraction(
                age = obj.positiveDoubleOrNull("age")?.roundToInt()?.takeIf { CalorieCalculator.isValidAge(it) },
                sex = obj.enumOrNull<Sex>("sex"),
                heightCm = obj.positiveDoubleOrNull("height_cm")?.takeIf { CalorieCalculator.isValidHeight(it) },
                weightKg = obj.positiveDoubleOrNull("weight_kg")?.takeIf { CalorieCalculator.isValidWeight(it) },
                activity = obj.enumOrNull<ActivityLevel>("activity_level"),
                goal = obj.enumOrNull<Goal>("goal"),
                note = obj.optString("note", "").trim().takeUnless { it == "null" }.orEmpty().take(MAX_NOTE_CHARS),
                isProfile = obj.optBoolean("is_profile", true),
            )
        } catch (e: JSONException) {
            null
        }
    }

    /**
     * Parses a weekly plan. Unknown or repeated days are skipped, blank exercises dropped, and
     * every value clamped. Returns null if there's no JSON object, or a "fitness" answer has no days.
     */
    fun parseWorkoutPlan(text: String): GeneratedPlan? {
        val json = extractJsonObject(text) ?: return null
        return try {
            val obj = JSONObject(json)
            val isFitness = obj.optBoolean("is_fitness", true)
            val days = mutableListOf<DayDraft>()
            val daysArray = obj.optJSONArray("days")
            for (i in 0 until (daysArray?.length() ?: 0)) {
                val dayObj = daysArray!!.optJSONObject(i) ?: continue
                val dayOfWeek = dayOfWeekOrNull(dayObj.optString("day")) ?: continue
                if (days.any { it.dayOfWeek == dayOfWeek }) continue
                days += parseDayObject(dayObj, dayOfWeek)
            }
            if (isFitness && days.isEmpty()) return null
            GeneratedPlan(
                isFitness = isFitness,
                draft = PlanDraft(
                    title = obj.cleanString("title", MAX_TITLE_CHARS).ifEmpty { "My training week" },
                    notes = obj.cleanString("notes", MAX_PLAN_NOTES_CHARS),
                    days = days.sortedBy { it.dayOfWeek },
                ),
            )
        } catch (e: JSONException) {
            null
        }
    }

    /**
     * Parses an AI edit of one day (same fields as a day in a weekly plan, without "day").
     * Returns null if there's no JSON object.
     */
    fun parseDayEdit(text: String, dayOfWeek: DayOfWeek): GeneratedDay? {
        val json = extractJsonObject(text) ?: return null
        return try {
            val obj = JSONObject(json)
            if (!obj.optBoolean("is_fitness", true)) GeneratedDay(false, null)
            else GeneratedDay(true, parseDayObject(obj, dayOfWeek))
        } catch (e: JSONException) {
            null
        }
    }

    /** One day: blank exercises dropped, at most [MAX_EXERCISES_PER_DAY], every value clamped. */
    private fun parseDayObject(dayObj: JSONObject, dayOfWeek: DayOfWeek): DayDraft {
        val exercises = mutableListOf<ExerciseDraft>()
        val exArray = dayObj.optJSONArray("exercises")
        for (j in 0 until (exArray?.length() ?: 0)) {
            if (exercises.size >= MAX_EXERCISES_PER_DAY) break
            val ex = exArray!!.optJSONObject(j) ?: continue
            val name = ex.cleanString("name", MAX_TITLE_CHARS)
            if (name.isEmpty()) continue
            exercises += ExerciseDraft(
                name = name,
                sets = ex.positiveDoubleOrNull("sets")?.roundToInt()?.takeIf { it in 1..MAX_SETS },
                target = ex.cleanString("target", MAX_TARGET_CHARS),
                notes = ex.cleanString("notes", MAX_EXERCISE_NOTE_CHARS),
            )
        }
        return DayDraft(
            dayOfWeek = dayOfWeek,
            title = dayObj.cleanString("title", MAX_TITLE_CHARS).ifEmpty { if (exercises.isEmpty()) "Rest" else "Training" },
            isRest = dayObj.optBoolean("is_rest", exercises.isEmpty()),
            exercises = exercises,
        )
    }

    private fun dayOfWeekOrNull(value: String): DayOfWeek? {
        val key = value.trim().uppercase().take(3)
        return DayOfWeek.entries.firstOrNull { it.name.startsWith(key) && key.length == 3 }
    }

    /** A trimmed string field cut to [maxChars]; missing or JSON null becomes "". */
    private fun JSONObject.cleanString(key: String, maxChars: Int): String =
        if (isNull(key)) "" else optString(key, "").trim().take(maxChars)

    private fun extractJsonObject(text: String): String? {
        val withoutFences = text.replace("```json", "").replace("```", "")
        val start = withoutFences.indexOf('{')
        val end = withoutFences.lastIndexOf('}')
        return if (start >= 0 && end > start) withoutFences.substring(start, end + 1) else null
    }

    /** Reads a number (or numeric string); missing, invalid or negative values become 0. */
    private fun JSONObject.nonNegativeDouble(key: String): Double {
        val value = optDouble(key, 0.0)
        return if (value.isNaN() || value.isInfinite() || value < 0) 0.0 else value
    }

    private fun JSONObject.positiveDoubleOrNull(key: String): Double? {
        if (isNull(key)) return null
        val value = optDouble(key)
        return if (value.isNaN() || value.isInfinite() || value <= 0) null else value
    }

    private inline fun <reified E : Enum<E>> JSONObject.enumOrNull(key: String): E? {
        if (isNull(key)) return null
        val name = optString(key).trim().uppercase().replace(' ', '_')
        return enumValues<E>().firstOrNull { it.name == name }
    }
}
