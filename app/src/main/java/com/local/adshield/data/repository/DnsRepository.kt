package com.local.adshield.data.repository

import android.content.Context
import com.local.adshield.data.local.AppDatabase
import com.local.adshield.data.model.QueryLogEntity
import com.local.adshield.data.model.QueryStatus
import com.local.adshield.data.model.VpnStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

class DnsRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val queryLogDao = db.queryLogDao()

    fun getRecentLogs(limit: Int = 150): Flow<List<QueryLogEntity>> = queryLogDao.getRecentLogs(limit)

    fun getFilteredLogs(
        query: String?,
        status: QueryStatus?,
        limit: Int = 150
    ): Flow<List<QueryLogEntity>> {
        val sanitizedQuery = query?.takeIf { it.isNotBlank() }
        return queryLogDao.getFilteredLogs(sanitizedQuery, status, limit)
    }

    fun getTodayStats(): Flow<VpnStats> {
        val startOfDay = getStartOfDayTimestamp()
        val totalFlow = queryLogDao.getTotalCountSince(startOfDay)
        val blockedFlow = queryLogDao.getBlockedCountSince(startOfDay)

        return combine(totalFlow, blockedFlow) { total, blocked ->
            VpnStats(
                totalQueriesToday = total,
                totalBlockedToday = blocked
            )
        }
    }

    suspend fun clearLogs() {
        queryLogDao.clearAll()
    }

    private fun getStartOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
