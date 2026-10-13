package com.example.calorietracker.account

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Who is using the app. [email] is null for a guest (no account). */
data class Account(
    val name: String,
    val email: String?,
) {
    val isGuest: Boolean get() = email == null

    /** "Abhinav" from "Abhinav Sinha"; empty for a guest without a name. */
    val firstName: String get() = name.trim().substringBefore(' ')

    /** Up to two initials for the avatar, e.g. "AS". */
    val initials: String
        get() = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
}

/**
 * The signed-in account, kept in private SharedPreferences (not the Room database).
 * Nothing here is sent anywhere.
 */
class AccountStore(context: Context) {
    private val prefs = context.getSharedPreferences("account", Context.MODE_PRIVATE)
    private val _account = MutableStateFlow(read())
    val account: StateFlow<Account?> = _account.asStateFlow()

    fun signIn(name: String, email: String?) {
        prefs.edit().putBoolean("signed_in", true).putString("name", name).putString("email", email).apply()
        _account.value = Account(name, email)
    }

    fun signOut() {
        prefs.edit().clear().apply()
        _account.value = null
    }

    private fun read(): Account? =
        if (!prefs.getBoolean("signed_in", false)) null
        else Account(prefs.getString("name", "").orEmpty(), prefs.getString("email", null))
}
