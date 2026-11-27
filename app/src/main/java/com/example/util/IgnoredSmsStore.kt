package com.example.util

import android.content.Context

object IgnoredSmsStore {
    private const val KEY_IGNORED_SMS_HASHES = "key_ignored_sms_hashes"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    fun getIgnoredHashes(context: Context): Set<String> {
        return prefs(context).getStringSet(KEY_IGNORED_SMS_HASHES, emptySet())?.toSet() ?: emptySet()
    }

    fun addIgnoredHash(context: Context, smsHash: String) {
        if (smsHash.isBlank()) return
        val current = HashSet(getIgnoredHashes(context))
        if (current.add(smsHash)) {
            prefs(context).edit().putStringSet(KEY_IGNORED_SMS_HASHES, current).apply()
        }
    }

    fun removeIgnoredHash(context: Context, smsHash: String) {
        if (smsHash.isBlank()) return
        val current = HashSet(getIgnoredHashes(context))
        if (current.remove(smsHash)) {
            prefs(context).edit().putStringSet(KEY_IGNORED_SMS_HASHES, current).apply()
        }
    }

    fun clearAll(context: Context) {
        prefs(context).edit().remove(KEY_IGNORED_SMS_HASHES).apply()
    }

    fun count(context: Context): Int = getIgnoredHashes(context).size
}
