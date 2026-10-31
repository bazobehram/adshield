package com.local.adshield.ui.screens.inspector

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.local.adshield.data.model.QueryLogEntity
import com.local.adshield.data.model.QueryStatus
import com.local.adshield.data.model.RuleType
import com.local.adshield.data.repository.DnsRepository
import com.local.adshield.data.repository.RuleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StatusFilter {
    ALL,
    BLOCKED,
    ALLOWED
}

@OptIn(ExperimentalCoroutinesApi::class)
class InspectorViewModel(application: Application) : AndroidViewModel(application) {

    private val dnsRepository = DnsRepository(application)
    private val ruleRepository = RuleRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    val statusFilter: StateFlow<StatusFilter> = _statusFilter.asStateFlow()

    val filteredLogs: StateFlow<List<QueryLogEntity>> = combine(_searchQuery, _statusFilter) { query, filter ->
        Pair(query, filter)
    }.flatMapLatest { (query, filter) ->
        val queryStatus = when (filter) {
            StatusFilter.ALL -> null
            StatusFilter.BLOCKED -> QueryStatus.BLOCKED
            StatusFilter.ALLOWED -> QueryStatus.ALLOWED
        }
        dnsRepository.getFilteredLogs(query, queryStatus)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: StatusFilter) {
        _statusFilter.value = filter
    }

    fun clearLogs() {
        viewModelScope.launch {
            dnsRepository.clearLogs()
        }
    }

    fun addToWhitelist(domain: String) {
        viewModelScope.launch {
            ruleRepository.addRule(domain, RuleType.WHITELIST, "Added from Logs")
        }
    }

    fun blockDomain(domain: String) {
        viewModelScope.launch {
            ruleRepository.addRule(domain, RuleType.BLACKLIST, "Added from Logs")
        }
    }
}
