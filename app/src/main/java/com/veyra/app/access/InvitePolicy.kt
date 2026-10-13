package com.veyra.app.access

/**
 * Matches an email against the allowlist text from Remote Config. Entries are separated by
 * commas, spaces or new lines and are case-insensitive:
 * - `name@gmail.com`  one person
 * - `@company.com`    everyone with that email domain
 * - `*`               everyone (turns the allowlist off without a new build)
 */
object InvitePolicy {

    fun parse(list: String): Set<String> =
        list.split(',', ' ', '\n', '\r', '\t', ';').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    fun isAllowed(email: String, list: String): Boolean {
        val address = email.trim().lowercase()
        if (address.isEmpty() || '@' !in address) return false
        val entries = parse(list)
        return "*" in entries || address in entries || ("@" + address.substringAfter('@')) in entries
    }
}
