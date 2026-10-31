package com.local.adshield.data.repository

import android.content.Context
import com.local.adshield.data.local.AppDatabase
import com.local.adshield.data.model.RuleEntity
import com.local.adshield.data.model.RuleType
import com.local.adshield.vpn.AdShieldVpnService
import com.local.adshield.vpn.DomainFilterEngine
import kotlinx.coroutines.flow.Flow

class RuleRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val ruleDao = db.ruleDao()

    fun getAllRules(): Flow<List<RuleEntity>> = ruleDao.getAllRules()

    fun getRulesByType(type: RuleType): Flow<List<RuleEntity>> = ruleDao.getRulesByType(type)

    fun getEnabledCount(): Flow<Int> = ruleDao.getEnabledCount()

    suspend fun addRule(domain: String, type: RuleType, note: String? = null): Long {
        val clean = DomainFilterEngine.normalizeDomain(domain)
        if (clean.isEmpty()) return -1L

        val existing = ruleDao.findByDomain(clean)
        val id = if (existing != null) {
            ruleDao.update(existing.copy(type = type, enabled = true, note = note))
            existing.id
        } else {
            ruleDao.insert(
                RuleEntity(
                    domain = clean,
                    type = type,
                    enabled = true,
                    note = note
                )
            )
        }
        AdShieldVpnService.reload(context)
        return id
    }

    suspend fun toggleRule(rule: RuleEntity) {
        ruleDao.update(rule.copy(enabled = !rule.enabled))
        AdShieldVpnService.reload(context)
    }

    suspend fun deleteRule(rule: RuleEntity) {
        ruleDao.delete(rule)
        AdShieldVpnService.reload(context)
    }

    suspend fun deleteByDomain(domain: String) {
        val clean = DomainFilterEngine.normalizeDomain(domain)
        ruleDao.deleteByDomain(clean)
        AdShieldVpnService.reload(context)
    }
}
