package com.local.adshield.data.repository

import android.content.Context
import android.util.Log
import com.local.adshield.data.local.AppDatabase
import com.local.adshield.data.model.BlockedDomain
import com.local.adshield.data.model.BlocklistSource
import com.local.adshield.vpn.AdShieldVpnService
import com.local.adshield.vpn.DomainFilterEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class BlocklistRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val sourceDao = db.blocklistSourceDao()
    private val blockedDomainDao = db.blockedDomainDao()

    fun getAllSources(): Flow<List<BlocklistSource>> = sourceDao.getAllSources()

    suspend fun addSource(name: String, url: String): Long {
        return sourceDao.insert(
            BlocklistSource(
                name = name,
                url = url,
                enabled = true
            )
        )
    }

    suspend fun updateSource(source: BlocklistSource) {
        sourceDao.update(source)
        if (!source.enabled) {
            blockedDomainDao.deleteBySourceId(source.id)
            AdShieldVpnService.reload(context)
        }
    }

    suspend fun deleteSource(source: BlocklistSource) {
        blockedDomainDao.deleteBySourceId(source.id)
        sourceDao.delete(source)
        AdShieldVpnService.reload(context)
    }

    /**
     * Downloads and parses an external hosts/domain list feed, then batch inserts
     * into Room DB and reloads the VPN engine.
     */
    suspend fun syncSource(sourceId: Long, onProgress: (String) -> Unit = {}): Result<Int> = withContext(Dispatchers.IO) {
        val source = sourceDao.getById(sourceId)
            ?: return@withContext Result.failure(IllegalArgumentException("Source not found: $sourceId"))

        try {
            onProgress("Connecting to ${source.name}...")
            val url = URL(source.url)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "AdShield-Android/1.0")
            }

            if (connection.responseCode !in 200..299) {
                return@withContext Result.failure(Exception("HTTP ${connection.responseCode}: ${connection.responseMessage}"))
            }

            onProgress("Downloading list...")
            val domains = mutableSetOf<String>()
            val reader = BufferedReader(InputStreamReader(connection.inputStream))

            reader.useLines { lines ->
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                        continue
                    }

                    val withoutComment = trimmed.substringBefore('#').trim()
                    if (withoutComment.isEmpty()) continue

                    val tokens = withoutComment.split("\\s+".toRegex())
                    val domainCandidate = when {
                        tokens.size >= 2 && (tokens[0] == "0.0.0.0" || tokens[0] == "127.0.0.1") -> tokens[1]
                        tokens.size == 1 && !tokens[0].contains(" ") -> tokens[0]
                        else -> null
                    }

                    if (domainCandidate != null) {
                        val clean = DomainFilterEngine.normalizeDomain(domainCandidate)
                        if (isValidDomain(clean)) {
                            domains.add(clean)
                        }
                    }
                }
            }

            onProgress("Saving ${domains.size} domains to database...")
            blockedDomainDao.deleteBySourceId(source.id)

            val batchSize = 1000
            val domainList = domains.map { BlockedDomain(domain = it, sourceId = source.id) }
            domainList.chunked(batchSize).forEach { chunk ->
                blockedDomainDao.insertAll(chunk)
            }

            val updatedSource = source.copy(
                last_synced = System.currentTimeMillis(),
                ruleCount = domains.size
            )
            sourceDao.update(updatedSource)

            AdShieldVpnService.reload(context)

            Log.i("BlocklistRepo", "Successfully synced ${source.name}: ${domains.size} rules")
            onProgress("Synced ${domains.size} domains successfully!")
            Result.success(domains.size)
        } catch (e: Exception) {
            Log.e("BlocklistRepo", "Failed to sync ${source.name}", e)
            Result.failure(e)
        }
    }

    suspend fun syncAllEnabledSources(onProgress: (String) -> Unit = {}): Int = withContext(Dispatchers.IO) {
        val enabled = sourceDao.getEnabledSources()
        var total = 0
        for (source in enabled) {
            syncSource(source.id, onProgress).onSuccess { count ->
                total += count
            }
        }
        total
    }

    private fun isValidDomain(domain: String): Boolean {
        if (domain.isEmpty() || domain == "localhost" || domain.length > 253) return false
        if (!domain.contains(".")) return false
        return domain.all { it.isLetterOrDigit() || it == '.' || it == '-' || it == '_' }
    }
}
