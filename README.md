# Calorie Tracker

A personal Android calorie tracker (v1.0). Kotlin + Jetpack Compose, data stored on the phone with Room,
and optional AI help (Gemini free tier) for estimating meals and filling in the profile.

## Features

- **Profile:** age, sex, height, weight, activity level and goal give a daily calorie target
  (Mifflin-St Jeor BMR × activity ± goal, min 1,200 kcal), plus protein / carbs / fat and water targets.
  Weight is logged per day; targets always use the latest weight.
- **Dashboard:** calorie ring split by macro (tap it for the kcal split), macro tiles (eaten / target),
  water tracker, 7-day chart and the day's meals (edit, delete with undo). Arrows switch days.
- **Add meal:** describe the meal and let AI estimate it, or type everything in yourself.

## Setup

1. Install Android Studio, or the Android command-line tools (SDK platform 37, build-tools 36).
2. Create `local.properties` in the project root (it is git-ignored, never commit it):

   ```properties
   sdk.dir=/path/to/Android/sdk
   GEMINI_API_KEY=your_key_here
   ```

3. Build and install: `./gradlew installDebug` (phone connected with USB debugging on).
4. Run the tests: `./gradlew testDebugUnitTest`.

The app works without a key; the AI buttons then explain that AI isn't set up.
To change the model, edit `GEMINI_MODEL` in `network/GeminiClient.kt`. Stick to Flash / Flash-Lite
models, which are the free-tier ones.

## AI guardrails

Every AI request goes through one place, `GeminiClient`, in this order:

| Layer | Where | What it does |
|---|---|---|
| Input checks | `AiGuardrails` | Blocks text under 3 or over 300 characters, links, code, prompt-injection phrases ("ignore previous instructions", "act as"…), general-assistant requests ("write…", "explain…", "who is…") and keyboard mashing. The profile assist also needs something profile-like (numbers, height, weight, gym…). Blocked text costs **no** request. |
| Cache | `GeminiClient` | The same text asked again returns the previous answer without a new request. |
| Rate limits | `AiRateLimiter` | 3 s cooldown, at most 5 requests a minute and 50 a day (the daily count survives restarts). Change the limits in `AiRateLimiter`'s companion object. |
| Prompt | `GeminiClient` | The user's text is wrapped in `<user_input>` tags and the model is told to treat it as data only, never as instructions. Responses must use a fixed JSON schema and are capped at 400 output tokens. |
| Topic check | model + `GeminiClient` | The model must answer `is_food` / `is_profile`; if false, the app shows "That doesn't look like a food description" instead of filling fields. |
| Output checks | `GeminiParser` | Values are clamped (≤ 5,000 kcal and ≤ 500 g per macro per meal; age, height and weight must be in the app's valid ranges), text is length-limited, and bad JSON never crashes the app. |

The UI never names the AI provider. Technical error details go to Logcat (`adb logcat -s AiClient`).

## Security and privacy

- **API key:** read from `local.properties` into `BuildConfig` at build time and sent only in the
  `x-goog-api-key` header over HTTPS. It is never hard-coded or committed. Anyone who has the APK could
  extract it, so don't share the APK. For apps used by other people, move the AI call to a small backend
  that holds the key.
- **Restrict the key** (recommended): in [Google AI Studio](https://aistudio.google.com) open your key in
  the Google Cloud console (*APIs & Services → Credentials → your key*), then under **API restrictions**
  choose *Restrict key* and select only **Generative Language API**. A leaked key then can't be used
  for any other Google API.
- **No billing:** keep billing disabled on the key's Google Cloud project. On the free tier, going over a
  limit only returns "limit reached" errors; you can't be charged.
- **Data:** profile, meals, weights and water stay on the phone (Room database). Only the text typed into
  an AI box is sent to the AI service, and the app says so under each AI button.
- **Permissions:** internet only. No health, location, contacts or storage access.
- **Network:** HTTPS only (Android blocks cleartext traffic by default for this target SDK).
