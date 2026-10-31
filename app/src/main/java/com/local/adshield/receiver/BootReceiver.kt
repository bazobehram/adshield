package com.local.adshield.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.local.adshield.data.local.AppPreferences
import com.local.adshield.vpn.AdShieldVpnService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.i("BootReceiver", "Boot or update event received: $action")
            val prefs = AppPreferences(context)
            if (prefs.isProtectionEnabled && prefs.isAutoStartOnBoot) {
                val prepareIntent = VpnService.prepare(context)
                if (prepareIntent == null) {
                    Log.i("BootReceiver", "VPN permission already granted. Auto-starting AdShield...")
                    AdShieldVpnService.start(context, prefs.savedUpstreamDns)
                } else {
                    Log.w("BootReceiver", "VPN permission not yet granted by user. Waiting for manual launch.")
                }
            } else {
                Log.i("BootReceiver", "Auto-start or protection is disabled in settings.")
            }
        }
    }
}
