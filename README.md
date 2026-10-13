# Veyra

Veyra is an Android app for tracking food, water, training and progress (v2.0). Kotlin + Jetpack Compose,
all data stored on the phone with Room, and optional AI help (Gemini free tier) for estimating meals,
filling in the profile and planning training. No backend: there is nothing to deploy.

## Features

Five tabs:

- **Today:** daily hub with today's calories and macros, your next workout, water and weight.
- **Nutrition:** calorie ring split by macro (tap it for the kcal split), macro targets, water, and the
  day's meals (edit, delete with undo). Arrows switch days.
- **Training:** a repeating weekly plan with a daily checklist. Build it by hand or let AI draft it, then
  edit anything: add, edit, reorder, move, duplicate and delete exercises; rename, copy or clear days;
  "Edit day with AI" and "Swap with AI" for targeted changes, with Undo.
- **Progress:** this week's average intake, days within target and days logged; 7-day calorie chart;
  weight history with "Log weight".
- **Profile:** account, daily targets (calories, protein, carbs, fat, water), body & goal (with Edit),
  notification settings and AI usage.

Also:

- **Targets:** Mifflin-St Jeor BMR × activity ± goal (min 1,200 kcal); protein by g/kg for the activity
  level, fat 25% of calories, carbs the rest; water 35 ml/kg (2–4 L).
- **Sign-in:** optional Google Sign-In, or use the app without an account (no limits). Without a
  Google client ID in the build, the sign-in screen just shows "Get started".
- **Reminders:** water (every 2 h when behind), meal logging (10 AM / 2 PM / 9 PM if not logged) and
  workout (6 PM if unfinished). Checked hourly by WorkManager, never before 8 AM or after 10 PM.

## Setup

1. Install Android Studio, or the Android command-line tools (SDK platform 37, build-tools 36).
2. Create `local.properties` in the project root (git-ignored, never commit it):

   ```properties
   sdk.dir=/path/to/Android/sdk
   GEMINI_API_KEY=your_key_here
   # Optional, for Sign in with Google (see below):
   GOOGLE_WEB_CLIENT_ID=
   ```

3. Debug build on a connected phone: `./gradlew installDebug`. Tests: `./gradlew testDebugUnitTest`.

The app works without a Gemini key; the AI buttons then explain that AI isn't set up.
To change the model, edit `GEMINI_MODEL` in `network/GeminiClient.kt`. If it is overloaded (503s or
timeouts, common on the free tier at peak times) the app falls back to `GEMINI_FALLBACK_MODELS`. Stick to Flash / Flash-Lite
models, which are the free-tier ones.

### Release build (for testers)

Release builds are shrunk with R8 (about 3 MB) and signed with the key in `release.jks`, configured in
`keystore.properties`. Both files are git-ignored: **back them up somewhere safe**. Without them you
can't ship an update to anyone who installed a release build.

```properties
# keystore.properties
storeFile=release.jks
storePassword=...
keyAlias=calorietracker
keyPassword=...
```

```sh
./gradlew assembleRelease   # → app/build/outputs/apk/release/app-release.apk
```

Raise `versionCode` in `app/build.gradle.kts` for every build you hand out, so testers can update in place.

### Sign in with Google (optional)

In the [Google Cloud console](https://console.cloud.google.com), in the project that owns your Gemini key:

1. **Google Auth Platform → Branding:** app name and support email; audience **External**; add testers
   under **Test users** (while the app is in testing).
2. **Clients → Create client → Android:** package `com.veyra.app` and the SHA-1 of every
   key that signs builds you install: debug (`keytool -list -v -keystore ~/.android/debug.keystore
   -alias androiddebugkey -storepass android`) and release (`keytool -list -v -keystore release.jks
   -alias calorietracker`).
3. **Create client → Web application** (no redirect URIs). Put its client ID in `GOOGLE_WEB_CLIENT_ID`.

Sign-in only identifies the user on that phone; there is no sync between devices.

## AI guardrails

Every AI request goes through one place, `GeminiClient`, in this order:

| Layer | Where | What it does |
|---|---|---|
| Input checks | `AiGuardrails` | Blocks text that is too short or long (300 characters; 500 for training goals), links, code, prompt-injection phrases ("ignore previous instructions", "act as"…) and keyboard mashing. Meal and profile text also can't be general-assistant requests ("write…", "explain…"); the profile assist and training goals must look like what they are. Blocked text costs **no** request. |
| Cache | `GeminiClient` | The same request again returns the previous answer without a new request. |
| Rate limits | `AiRateLimiter` | 3 s cooldown, at most 5 requests a minute and 50 a day per phone (the daily count survives restarts). Change them in `AiRateLimiter`'s companion object. |
| Prompt | `GeminiClient` | The user's text is wrapped in `<user_input>` tags and treated as data only. Responses must use a fixed JSON schema and are capped (400 output tokens; 1,024 for a day edit; 4,096 for a weekly plan). |
| Topic check | model + `GeminiClient` | The model must answer `is_food` / `is_profile` / `is_fitness`; if false, the app shows a "doesn't look like…" message instead of filling anything in. |
| Output checks | `GeminiParser` | Values are clamped (≤ 5,000 kcal and ≤ 500 g per macro per meal; age, height and weight within the app's ranges; ≤ 10 exercises a day, 1–20 sets), text is length-limited, and bad JSON never crashes the app. |

Training answers that mention pain or injury stay conservative and add "stop if it hurts / check with a
doctor or physiotherapist" to the affected exercises. The UI never names the AI provider; technical error
details go to Logcat (`adb logcat -s AiClient`).

## Security and privacy

- **API key:** read from `local.properties` into `BuildConfig` at build time and sent only in the
  `x-goog-api-key` header over HTTPS. It is never committed, but **it is inside every APK you hand out**:
  anyone with the APK could extract it, and all testers share its free-tier quota. Restrict it (below),
  consider a separate key for test builds, and move the AI call to a small backend before a public release.
- **Restrict the key:** in the Google Cloud console open the key (*APIs & Services → Credentials*), choose
  *Restrict key* under **API restrictions**, and select only **Generative Language API**.
- **No billing:** keep billing disabled on the key's project. On the free tier, going over a limit only
  returns "limit reached" errors; you can't be charged.
- **Data:** profile, meals, weights, water and training stay on the phone (Room database). Account name,
  email and reminder settings are in private app preferences. Only text typed into an AI box (plus, for
  training plans, basic profile details) is sent to the AI service, and the app says so where it happens.
- **Permissions:** internet and notifications only.

## Licences

The Inter typeface is bundled under the SIL Open Font License (`licenses/Inter-OFL.txt`).
