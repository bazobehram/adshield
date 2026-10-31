package com.local.adshield.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.local.adshield.data.model.BlockedDomain
import com.local.adshield.data.model.BlocklistSource
import com.local.adshield.data.model.QueryLogEntity
import com.local.adshield.data.model.RuleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        QueryLogEntity::class,
        RuleEntity::class,
        BlocklistSource::class,
        BlockedDomain::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun queryLogDao(): QueryLogDao
    abstract fun ruleDao(): RuleDao
    abstract fun blocklistSourceDao(): BlocklistSourceDao
    abstract fun blockedDomainDao(): BlockedDomainDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "adshield.db"
                ).fallbackToDestructiveMigration()
                 .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val instance = getInstance(context)
                            seedDefaultSourcesAndBaseline(instance)
                        }
                    }
                }).build().also { INSTANCE = it }
            }
        }

        private suspend fun seedDefaultSourcesAndBaseline(db: AppDatabase) {
            // 1. Pre-configure primary security feeds
            val sourceId1 = db.blocklistSourceDao().insert(
                BlocklistSource(
                    name = "StevenBlack Unified (Ads & Trackers)",
                    url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
                    enabled = true
                )
            )

            val sourceId2 = db.blocklistSourceDao().insert(
                BlocklistSource(
                    name = "URLhaus Malware & Ransomware",
                    url = "https://malware-filter.gitlab.io/urlhaus-filter/urlhaus-filter-hosts.txt",
                    enabled = true
                )
            )

            val sourceId3 = db.blocklistSourceDao().insert(
                BlocklistSource(
                    name = "Phishing Army (Scams & Fraud)",
                    url = "https://phishing.army/download/phishing_army_blocklist_extended.txt",
                    enabled = true
                )
            )

            val sourceId4 = db.blocklistSourceDao().insert(
                BlocklistSource(
                    name = "AdGuard Mobile Filter",
                    url = "https://adguardteam.github.io/HostlistsRegistry/assets/filter_1.txt",
                    enabled = false
                )
            )

            // 2. Pre-seed baseline protection for immediate zero-day coverage out of the box
            val baselineTrackers = listOf(
                "doubleclick.net",
                "googleads.g.doubleclick.net",
                "pagead2.googlesyndication.com",
                "adservice.google.com",
                "analytics.google.com",
                "app-measurement.com",
                "appsflyer.com",
                "adjust.com",
                "branch.io",
                "criteo.com",
                "criteo.net",
                "taboola.com",
                "outbrain.com",
                "scorecardresearch.com",
                "quantserve.com",
                "popads.net",
                "propellerads.com",
                "adcolony.com",
                "unityads.unity3d.com",
                "vungle.com",
                "chartboost.com",
                "ironsrc.com",
                "flurry.com",
                "crashlytics.com",
                "adnxs.com",
                "rubiconproject.com",
                "pubmatic.com",
                "openx.net",
                "adroll.com",
                "zemanta.com"
            )

            val baselineEntities = baselineTrackers.map { domain ->
                BlockedDomain(domain = domain, sourceId = sourceId1)
            }
            db.blockedDomainDao().insertAll(baselineEntities)
            db.blocklistSourceDao().update(
                BlocklistSource(
                    id = sourceId1,
                    name = "StevenBlack Unified (Ads & Trackers)",
                    url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
                    enabled = true,
                    ruleCount = baselineTrackers.size,
                    last_synced = System.currentTimeMillis()
                )
            )
        }
    }
}
