package com.veyra.app.access

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

sealed interface AccessResult {
    data object Allowed : AccessResult
    data class Denied(val message: String) : AccessResult
}

/**
 * Invite-only access for tester builds ([enabled] = BuildConfig.INVITE_ONLY). The allowlist is the
 * Remote Config parameter [PARAM], edited in the Firebase console. Fails closed: if the list has
 * never been downloaded, or Firebase isn't configured, nobody gets in.
 */
class InviteChecker(context: Context, val enabled: Boolean) {

    private val remoteConfig: FirebaseRemoteConfig? =
        if (FirebaseApp.getApps(context).isEmpty()) null
        else FirebaseRemoteConfig.getInstance().apply {
            // Re-download at most every 5 minutes so changes in the console apply quickly.
            setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(300).build())
            setDefaultsAsync(mapOf(PARAM to ""))
        }

    /** Instant check against the last downloaded list (no network). */
    fun checkCached(email: String): AccessResult = evaluate(email)

    /**
     * Downloads the latest list (up to ~8 s), then checks. Falls back to the cached list when offline.
     * A refusal triggers one more download that skips the 5-minute minimum, so someone you've just
     * invited isn't kept out by a list downloaded a few minutes earlier.
     */
    suspend fun checkLatest(email: String): AccessResult {
        val config = remoteConfig ?: return evaluate(email)
        val fetched = withTimeoutOrNull(FETCH_TIMEOUT_MS) { config.fetchAndActivate().awaitSuccess() } == true
        if (!fetched) Log.w(TAG, "Allowlist fetch failed or timed out; using cached list")
        val result = evaluate(email)
        if (result is AccessResult.Allowed) return result

        val refreshed = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
            config.fetch(0).awaitSuccess() && config.activate().awaitSuccess()
        } == true
        if (!refreshed) Log.w(TAG, "Forced allowlist refresh failed (offline or throttled)")
        return evaluate(email)
    }

    private fun evaluate(email: String): AccessResult {
        if (!enabled) return AccessResult.Allowed
        val config = remoteConfig
            ?: return AccessResult.Denied("This test build isn't connected to its invite list. Contact the app owner.")
        // fetchTimeMillis is -1 until a list has been downloaded at least once.
        if (config.info.fetchTimeMillis <= 0) {
            return AccessResult.Denied("Connect to the internet so we can check your invite, then try again.")
        }
        return if (InvitePolicy.isAllowed(email, config.getString(PARAM))) {
            AccessResult.Allowed
        } else {
            AccessResult.Denied("$email isn't on the tester list. Ask the app owner to invite you.")
        }
    }

    /** True if the task succeeded. (Some tasks succeed with a null result, so the result itself isn't used.) */
    private suspend fun Task<*>.awaitSuccess(): Boolean = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task -> cont.resume(task.isSuccessful) }
    }

    companion object {
        const val PARAM = "allowed_emails"
        private const val FETCH_TIMEOUT_MS = 8_000L
        private const val TAG = "InviteChecker"
    }
}
