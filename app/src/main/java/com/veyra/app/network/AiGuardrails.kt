package com.veyra.app.network

/**
 * Cheap on-device checks that run before any AI request, so obviously off-topic,
 * malicious or junk text never costs a request. Returns an error message to show,
 * or null if the text may be sent.
 *
 * These checks are deliberately loose about food words (dish names vary too much,
 * e.g. "poha", "rajma chawal"); the model makes the final "is this food?" call.
 */
object AiGuardrails {

    const val MAX_INPUT_CHARS = 300

    /** Training goals need more room (schedule, equipment, injuries). */
    const val MAX_WORKOUT_CHARS = 500
    private const val MIN_INPUT_CHARS = 3

    const val NOT_FOOD_MESSAGE =
        "That doesn't look like a food description. Describe what you ate, e.g. \"2 rotis and a bowl of dal\"."
    const val NOT_FITNESS_MESSAGE =
        "That doesn't look like a training goal. Describe what you train for, e.g. \"Gym 4 days a week to get lean\"."
    const val NOT_PROFILE_MESSAGE =
        "That doesn't look like a description of you. Mention things like your age, height, weight and how often you exercise."

    private val ignoreCase = setOf(RegexOption.IGNORE_CASE)

    private val links = Regex("""https?://|www\.|\b[\w-]+\.(com|net|org|io|in|ai|dev)\b""", ignoreCase)

    private val code = Regex("""[{}<>`\\]|=>|\b(function|class|import|select|def|return)\b""", ignoreCase)

    // Attempts to change the AI's instructions ("prompt injection").
    private val injection = Regex(
        """ignore (all |any |the )?(previous|above|prior|earlier)|disregard|system prompt|""" +
            """you are now|act as|pretend (to be|you)|jailbreak|developer mode|new instructions""",
        ignoreCase,
    )

    // Requests for a general-purpose assistant rather than a food description.
    private val offTopicRequest = Regex(
        """^(please |can you |could you )?(write|explain|translate|summari[sz]e|solve|generate|create|""" +
            """compose|draft|tell me a|who (is|was)|define|code|debug)\b""",
        ignoreCase,
    )

    private val repeatedChar = Regex("""(.)\1{4,}""")

    private val profileHints = Regex(
        """\d|\b(age|years?|yrs?|old|kg|kgs|kilos?|lbs?|pounds?|cm|feet|foot|ft|inch(es)?|tall|height|weigh(t|s)?|""" +
            """male|female|man|woman|boy|girl|gym|work ?out|exercise|walk(ing)?|run(ning)?|active|sedentary|desk|""" +
            """lose|gain|maintain|fat|muscle|bulk|cut)\b""",
        ignoreCase,
    )

    private val fitnessHints = Regex(
        """\b(gym|work ?outs?|train(ing)?|exercis(e|es|ing)|fitness|fit|strength|strong|muscle|lean|bulk|""" +
            """cut(ting)?|tone|toning|fat|weight|lift(ing)?|powerlifting|bodybuilding|hypertrophy|cardio|run(ning)?|""" +
            """jog(ging)?|marathon|half[- ]marathon|5k|10k|sprint|hyrox|crossfit|calisthenics|yoga|pilates|mobility|""" +
            """stretch(ing)?|swim(ming)?|cycl(e|ing)|row(ing)?|hike|hiking|walk(ing)?|sports?|football|cricket|""" +
            """badminton|tennis|basketball|boxing|mma|martial|abs|core|legs|arms|chest|endurance|stamina|reps?|""" +
            """dumbbells?|kettlebells?|barbell|resistance bands?|athlete|race)\b""",
        ignoreCase,
    )

    /** Checks a "what do you want to train for?" goal. Plan requests ("Create a 4-day plan…") are fine here. */
    fun checkWorkout(text: String): String? =
        checkCommon(text, NOT_FITNESS_MESSAGE, maxChars = MAX_WORKOUT_CHARS, blockAssistantRequests = false)
            ?: if (fitnessHints.containsMatchIn(text)) null else NOT_FITNESS_MESSAGE

    /**
     * Checks an instruction for changing one day of an existing plan ("make it 30 minutes",
     * "swap squats for something knee-friendly"). The topic is already training, so no
     * keyword is required; junk, links, code and prompt injection are still blocked.
     */
    fun checkPlanEdit(text: String): String? =
        checkCommon(text, NOT_FITNESS_MESSAGE, blockAssistantRequests = false)

    /** Checks a "what did you eat?" description. */
    fun checkMeal(text: String): String? = checkCommon(text, NOT_FOOD_MESSAGE)

    /** Checks a "describe yourself" text; it must also contain something profile-like. */
    fun checkProfile(text: String): String? =
        checkCommon(text, NOT_PROFILE_MESSAGE)
            ?: if (profileHints.containsMatchIn(text)) null else NOT_PROFILE_MESSAGE

    private fun checkCommon(
        raw: String,
        offTopicMessage: String,
        maxChars: Int = MAX_INPUT_CHARS,
        blockAssistantRequests: Boolean = true,
    ): String? {
        val text = raw.trim()
        return when {
            text.length < MIN_INPUT_CHARS -> "Describe it in a few words first."
            text.length > maxChars -> "Keep it under $maxChars characters."
            links.containsMatchIn(text) || code.containsMatchIn(text) || injection.containsMatchIn(text) ||
                (blockAssistantRequests && offTopicRequest.containsMatchIn(text)) -> offTopicMessage
            looksLikeGibberish(text) -> offTopicMessage
            else -> null
        }
    }

    private fun looksLikeGibberish(text: String): Boolean {
        val letters = text.filter { it.isLetter() }
        if (letters.length < 2) return true
        if (repeatedChar.containsMatchIn(text)) return true
        if (text.split(Regex("""\s+""")).any { it.length > 25 }) return true
        // Keyboard mashing like "asdfghjkl": Latin letters with almost no vowels.
        // Real food words (English or Hinglish, e.g. "rajma chawal") are well above 20% vowels.
        val latinOnly = letters.all { it in 'a'..'z' || it in 'A'..'Z' }
        val vowelShare = letters.count { it.lowercaseChar() in "aeiouy" }.toDouble() / letters.length
        return latinOnly && letters.length > 5 && vowelShare < 0.2
    }
}
