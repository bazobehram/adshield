package com.local.adshield.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VpnStateHolder {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _activeRulesCount = MutableStateFlow(0)
    val activeRulesCount: StateFlow<Int> = _activeRulesCount.asStateFlow()

    private val _upstreamDnsIp = MutableStateFlow("1.1.1.1")
    val upstreamDnsIp: StateFlow<String> = _upstreamDnsIp.asStateFlow()

    fun setRunning(running: Boolean) {
        _isRunning.value = running
    }

    fun setActiveRulesCount(count: Int) {
        _activeRulesCount.value = count
    }

    fun setUpstreamDnsIp(ip: String) {
        _upstreamDnsIp.value = ip
    }
}
