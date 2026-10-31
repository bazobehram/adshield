package com.local.adshield.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.local.adshield.data.local.AppPreferences
import com.local.adshield.data.model.DnsServer
import com.local.adshield.data.model.QueryLogEntity
import com.local.adshield.data.model.VpnStats
import com.local.adshield.data.repository.DnsRepository
import com.local.adshield.vpn.AdShieldVpnService
import com.local.adshield.vpn.VpnStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val dnsRepository = DnsRepository(application)
    private val prefs = AppPreferences(application)

    val isVpnActive: StateFlow<Boolean> = VpnStateHolder.isRunning

    val stats: StateFlow<VpnStats> = dnsRepository.getTodayStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VpnStats())

    val activeRulesCount: StateFlow<Int> = VpnStateHolder.activeRulesCount

    private val _upstreamDns = MutableStateFlow(DnsServer.findOrDefault(prefs.savedUpstreamDns))
    val upstreamDns: StateFlow<DnsServer> = _upstreamDns.asStateFlow()

    val recentQueries: StateFlow<List<QueryLogEntity>> = dnsRepository.getRecentLogs(limit = 6)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setUpstreamDns(server: DnsServer) {
        _upstreamDns.value = server
        prefs.savedUpstreamDns = server.ip
        VpnStateHolder.setUpstreamDnsIp(server.ip)
        if (isVpnActive.value) {
            AdShieldVpnService.start(getApplication(), server.ip)
        }
    }

    fun startVpn() {
        prefs.isProtectionEnabled = true
        AdShieldVpnService.start(getApplication(), _upstreamDns.value.ip)
    }

    fun stopVpn() {
        prefs.isProtectionEnabled = false
        AdShieldVpnService.stop(getApplication())
    }
}
