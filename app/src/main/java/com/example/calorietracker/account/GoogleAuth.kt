package com.example.calorietracker.account

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

sealed interface SignInResult {
    data class Success(val name: String, val email: String) : SignInResult
    data object Cancelled : SignInResult
    data class Error(val message: String) : SignInResult
}

/**
 * Sign in with Google through Android's Credential Manager. The app has no server, so the
 * ID token is only used to read the user's name and email on the device.
 */
class GoogleAuth(private val webClientId: String) {

    val isConfigured: Boolean get() = webClientId.isNotBlank()

    /** Shows Google's account picker. [activityContext] must be an Activity. */
    suspend fun signIn(activityContext: Context): SignInResult {
        if (!isConfigured) {
            return SignInResult.Error("Google sign-in isn't set up in this build. You can continue without an account.")
        }
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId = webClientId).build())
            .build()
        return try {
            val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                val email: String? = google.email
                if (email.isNullOrBlank()) {
                    SignInResult.Error("Google didn't share an email address. Please try again.")
                } else {
                    SignInResult.Success(name = google.displayName ?: email.substringBefore('@'), email = email)
                }
            } else {
                SignInResult.Error("Unexpected sign-in response. Please try again.")
            }
        } catch (e: GetCredentialCancellationException) {
            SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            SignInResult.Error("No Google account is available on this phone. Add one in Settings, or continue without an account.")
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Sign-in failed", e)
            SignInResult.Error("Google sign-in failed. Check your connection and try again.")
        } catch (e: GoogleIdTokenParsingException) {
            Log.w(TAG, "Bad ID token", e)
            SignInResult.Error("Google sign-in failed. Please try again.")
        }
    }

    /** Forgets the account choice so the picker shows again next time. */
    suspend fun signOut(context: Context) {
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "Could not clear credential state", e)
        }
    }

    private companion object {
        const val TAG = "GoogleAuth"
    }
}
