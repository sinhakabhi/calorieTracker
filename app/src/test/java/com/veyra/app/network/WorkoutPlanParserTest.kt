package com.veyra.app.network

import com.veyra.app.data.ExerciseDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class WorkoutPlanParserTest {

    @Test
    fun parsesAWeek() {
        val plan = GeminiParser.parseWorkoutPlan(
            """
            {"is_fitness": true, "title": "Lean 4-day split", "notes": "Add weight each week.",
             "days": [
               {"day": "TUE", "title": "Lower", "is_rest": false, "exercises": [
                 {"name": "Back squat", "sets": 4, "target": "6-8 reps", "notes": "2 min rest"}]},
               {"day": "MON", "title": "Upper", "is_rest": false, "exercises": [
                 {"name": "Bench press", "sets": 4, "target": "8-10 reps", "notes": ""},
                 {"name": "Easy run", "sets": null, "target": "5 km", "notes": ""}]},
               {"day": "SUN", "title": "Rest", "is_rest": true, "exercises": []}
             ]}
            """.trimIndent()
        )!!

        assertTrue(plan.isFitness)
        assertEquals("Lean 4-day split", plan.draft.title)
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.SUNDAY), plan.draft.days.map { it.dayOfWeek })
        assertEquals(ExerciseDraft("Easy run", null, "5 km", ""), plan.draft.days[0].exercises[1])
        assertTrue(plan.draft.days[2].isRest)
    }

    @Test
    fun clampsAndCleansBadValues() {
        val exercises = (1..15).joinToString(",") { """{"name": "Move $it", "sets": 3, "target": "10 reps"}""" }
        val plan = GeminiParser.parseWorkoutPlan(
            """
            {"is_fitness": true, "title": "x", "notes": "",
             "days": [
               {"day": "MON", "title": "Big day", "exercises": [$exercises]},
               {"day": "MON", "title": "Duplicate Monday", "exercises": []},
               {"day": "FUNDAY", "title": "Not a day", "exercises": []},
               {"day": "WED", "title": "", "exercises": [
                 {"name": "  ", "sets": 3, "target": "blank name"},
                 {"name": "Plank", "sets": 999, "target": "45 s"}]}
             ]}
            """.trimIndent()
        )!!

        assertEquals(2, plan.draft.days.size)
        assertEquals(GeminiParser.MAX_EXERCISES_PER_DAY, plan.draft.days[0].exercises.size)
        assertEquals("Big day", plan.draft.days[0].title)
        val wednesday = plan.draft.days[1]
        assertEquals(listOf("Plank"), wednesday.exercises.map { it.name })
        assertNull(wednesday.exercises[0].sets) // 999 sets is out of range
        assertEquals("Training", wednesday.title)
    }

    @Test
    fun readsNotFitnessFlag() {
        val plan = GeminiParser.parseWorkoutPlan("""{"is_fitness": false, "title": "", "notes": "Geography question.", "days": []}""")!!
        assertFalse(plan.isFitness)
    }

    @Test
    fun fitnessAnswerWithoutDaysOrGarbageIsNull() {
        assertNull(GeminiParser.parseWorkoutPlan("""{"is_fitness": true, "title": "Empty", "notes": "", "days": []}"""))
        assertNull(GeminiParser.parseWorkoutPlan("Sorry, I can't do that."))
    }

    @Test
    fun parsesADayEdit() {
        val edit = GeminiParser.parseDayEdit(
            """{"is_fitness": true, "title": "Hotel workout", "is_rest": false, "exercises": [
                {"name": "DB goblet squat", "sets": 3, "target": "12 reps", "notes": ""},
                {"name": "", "sets": 3, "target": "dropped"},
                {"name": "Push-ups", "sets": 50, "target": "max reps", "notes": "Stop 2 reps short of failure"}]}""",
            DayOfWeek.THURSDAY,
        )!!

        assertTrue(edit.isFitness)
        val day = edit.day!!
        assertEquals(DayOfWeek.THURSDAY, day.dayOfWeek)
        assertEquals("Hotel workout", day.title)
        assertEquals(listOf("DB goblet squat", "Push-ups"), day.exercises.map { it.name })
        assertNull(day.exercises[1].sets) // 50 sets is out of range
    }

    @Test
    fun dayEditNotFitnessOrGarbage() {
        val edit = GeminiParser.parseDayEdit("""{"is_fitness": false, "title": "", "is_rest": false, "exercises": []}""", DayOfWeek.MONDAY)!!
        assertFalse(edit.isFitness)
        assertNull(edit.day)
        assertNull(GeminiParser.parseDayEdit("no json here", DayOfWeek.MONDAY))
    }
}
