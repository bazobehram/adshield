package com.local.adshield.data.local

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isProtectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_PROTECTION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, value).apply()

    var isAutoStartOnBoot: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START_BOOT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START_BOOT, value).apply()

    var savedUpstreamDns: String
        get() = prefs.getString(KEY_UPSTREAM_DNS, "1.1.1.1") ?: "1.1.1.1"
        set(value) = prefs.edit().putString(KEY_UPSTREAM_DNS, value).apply()

    companion object {
        private const val PREFS_NAME = "adshield_preferences"
        private const val KEY_PROTECTION_ENABLED = "key_protection_enabled"
        private const val KEY_AUTO_START_BOOT = "key_auto_start_boot"
        private const val KEY_UPSTREAM_DNS = "key_upstream_dns"
    }
}
