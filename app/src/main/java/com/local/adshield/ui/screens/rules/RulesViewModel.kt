package com.local.adshield.ui.screens.rules

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.local.adshield.data.model.BlocklistSource
import com.local.adshield.data.model.RuleEntity
import com.local.adshield.data.model.RuleType
import com.local.adshield.data.repository.BlocklistRepository
import com.local.adshield.data.repository.RuleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RulesSubTab {
    CUSTOM_RULES,
    SUBSCRIPTIONS
}

class RulesViewModel(application: Application) : AndroidViewModel(application) {

    private val ruleRepository = RuleRepository(application)
    private val blocklistRepository = BlocklistRepository(application)

    private val _currentSubTab = MutableStateFlow(RulesSubTab.CUSTOM_RULES)
    val currentSubTab: StateFlow<RulesSubTab> = _currentSubTab.asStateFlow()

    private val _customRuleTypeFilter = MutableStateFlow(RuleType.WHITELIST)
    val customRuleTypeFilter: StateFlow<RuleType> = _customRuleTypeFilter.asStateFlow()

    val customRules: StateFlow<List<RuleEntity>> = ruleRepository.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subscriptions: StateFlow<List<BlocklistSource>> = blocklistRepository.getAllSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    val syncStatusMessage: StateFlow<String?> = _syncStatusMessage.asStateFlow()

    fun setSubTab(tab: RulesSubTab) {
        _currentSubTab.value = tab
    }

    fun setCustomRuleTypeFilter(type: RuleType) {
        _customRuleTypeFilter.value = type
    }

    fun addCustomRule(domain: String, type: RuleType, note: String? = null) {
        viewModelScope.launch {
            ruleRepository.addRule(domain, type, note)
        }
    }

    fun toggleCustomRule(rule: RuleEntity) {
        viewModelScope.launch {
            ruleRepository.toggleRule(rule)
        }
    }

    fun deleteCustomRule(rule: RuleEntity) {
        viewModelScope.launch {
            ruleRepository.deleteRule(rule)
        }
    }

    fun addSubscription(name: String, url: String) {
        viewModelScope.launch {
            blocklistRepository.addSource(name, url)
        }
    }

    fun toggleSubscription(source: BlocklistSource) {
        viewModelScope.launch {
            blocklistRepository.updateSource(source.copy(enabled = !source.enabled))
        }
    }

    fun deleteSubscription(source: BlocklistSource) {
        viewModelScope.launch {
            blocklistRepository.deleteSource(source)
        }
    }

    fun syncAllSubscriptions() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatusMessage.value = "Starting blocklist synchronization..."
            try {
                val totalCount = blocklistRepository.syncAllEnabledSources { progress ->
                    _syncStatusMessage.value = progress
                }
                _syncStatusMessage.value = "Synced $totalCount domains successfully!"
            } catch (e: Exception) {
                _syncStatusMessage.value = "Sync failed: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun syncSingleSubscription(sourceId: Long) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                blocklistRepository.syncSource(sourceId) { progress ->
                    _syncStatusMessage.value = progress
                }
            } catch (e: Exception) {
                _syncStatusMessage.value = "Sync failed: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
