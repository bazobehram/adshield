package com.local.adshield.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.local.adshield.MainActivity
import com.local.adshield.R
import com.local.adshield.data.local.AppDatabase
import com.local.adshield.data.model.QueryLogEntity
import com.local.adshield.data.model.QueryStatus
import com.local.adshield.data.model.RuleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class AdShieldVpnService : VpnService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var vpnInterface: ParcelFileDescriptor? = null
    private val filterEngine = DomainFilterEngine()
    private val logChannel = Channel<QueryLogEntity>(capacity = 500)

    private var upstreamDns = "1.1.1.1"
    private var isVpnActive = false

    override fun onCreate() {
        super.onCreate()
        Log.v(TAG, "AdShieldVpnService created")
        Log.v(TAG_COMMON, "AdShield local DNS engine initializing")
        createNotificationChannel()
        startLogProcessor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_STOP -> {
                Log.v(TAG, "Action STOP received")
                stopVpn()
                stopSelf()
            }
            ACTION_RELOAD_RULES -> {
                Log.v(TAG, "Action RELOAD_RULES received")
                reloadRules()
            }
            ACTION_START -> {
                val chosenDns = intent?.getStringExtra(EXTRA_UPSTREAM_DNS) ?: "1.1.1.1"
                upstreamDns = chosenDns
                VpnStateHolder.setUpstreamDnsIp(upstreamDns)
                Log.v(TAG, "Action START with upstream DNS: $upstreamDns")
                startVpn()
            }
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (isVpnActive) return

        startForegroundNotification()

        serviceScope.launch {
            loadRulesFromDb()
            startVpnTunnel()
        }
    }

    private fun startForegroundNotification() {
        val stopIntent = Intent(this, AdShieldVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.vpn_service_title))
            .setContentText(getString(R.string.vpn_service_running))
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.action_stop), stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private suspend fun loadRulesFromDb() = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(applicationContext)
            val customRules = db.ruleDao().getAllEnabledRules()
            val whitelisted = customRules.filter { it.type == RuleType.WHITELIST }.map { it.domain }
            val customBlacklist = customRules.filter { it.type == RuleType.BLACKLIST }.map { it.domain }
            val feedBlockedDomains = db.blockedDomainDao().getAllDomains()

            val combinedBlacklist = HashSet<String>(customBlacklist.size + feedBlockedDomains.size).apply {
                addAll(customBlacklist)
                addAll(feedBlockedDomains)
            }

            filterEngine.updateRules(
                whitelistDomains = whitelisted,
                blacklistDomains = combinedBlacklist
            )

            val totalActive = combinedBlacklist.size
            VpnStateHolder.setActiveRulesCount(totalActive)
            Log.v(TAG, "Rules loaded successfully: ${whitelisted.size} whitelist, $totalActive blacklist")
            Log.v(TAG_COMMON, "Active blacklist rules count: $totalActive")
        } catch (e: Exception) {
            Log.e(TAG, "Failed loading rules from DB", e)
        }
    }

    private fun reloadRules() {
        serviceScope.launch {
            loadRulesFromDb()
        }
    }

    private fun startVpnTunnel() {
        try {
            val builder = Builder()
                .setSession("AdShield DNS Protection")
                .setMtu(1500)
                .addAddress("10.0.0.2", 32)
                .addDnsServer("10.0.0.1")
                // Route SOLELY the local virtual DNS server 10.0.0.1/32
                // Regular data traffic (TCP/UDP to Internet) bypasses the VPN completely
                .addRoute("10.0.0.1", 32)
                .setBlocking(true)

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface (null descriptor)")
                stopSelf()
                return
            }

            isVpnActive = true
            VpnStateHolder.setRunning(true)
            Log.v(TAG, "AdShield TUN interface established on 10.0.0.2 -> 10.0.0.1/32")
            Log.v(TAG_COMMON, "AdShield DNS sinkhole is ACTIVE and intercepting port 53")

            runPacketLoop()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN tunnel", e)
            stopVpn()
            stopSelf()
        }
    }

    private fun runPacketLoop() {
        val pfd = vpnInterface ?: return
        val inputStream = FileInputStream(pfd.fileDescriptor)
        val outputStream = FileOutputStream(pfd.fileDescriptor)

        val packetBuffer = ByteArray(4096)

        while (serviceScope.isActive && isVpnActive) {
            try {
                val length = inputStream.read(packetBuffer)
                if (length <= 0) continue

                val query = DnsPacketHandler.parseUdpDnsPacket(packetBuffer, length)
                if (query == null) continue

                handleDnsQuery(query, outputStream)
            } catch (e: Exception) {
                if (isVpnActive) {
                    Log.w(TAG, "Packet loop read error: ${e.message}")
                }
                break
            }
        }
    }

    private fun handleDnsQuery(query: ParsedDnsQuery, outputStream: FileOutputStream) {
        val startTime = System.currentTimeMillis()
        val matchResult = filterEngine.checkDomain(query.domain)

        when (matchResult) {
            DomainMatchResult.BLACKLISTED -> {
                // Immediate local sinkhole response (0.0.0.0 or ::)
                val sinkholeDns = DnsPacketHandler.buildSinkholeDnsResponse(query)
                val responsePacket = DnsPacketHandler.buildIpUdpPacket(
                    dnsPayload = sinkholeDns,
                    srcIp = query.serverIp,
                    srcPort = query.serverPort,
                    dstIp = query.clientIp,
                    dstPort = query.clientPort
                )

                synchronized(outputStream) {
                    try {
                        outputStream.write(responsePacket)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed writing sinkhole packet", e)
                    }
                }

                Log.v(TAG, "BLOCKED domain: ${query.domain} (${query.qTypeName}) -> 0.0.0.0")
                Log.v(TAG_COMMON, "BLOCKED: ${query.domain}")

                logChannel.trySend(
                    QueryLogEntity(
                        domain = query.domain,
                        timestamp = startTime,
                        status = QueryStatus.BLOCKED,
                        queryType = query.qTypeName,
                        latencyMs = System.currentTimeMillis() - startTime,
                        clientIp = query.clientIp,
                        ruleMatched = "Blacklist"
                    )
                )
            }

            DomainMatchResult.WHITELISTED,
            DomainMatchResult.NOT_MATCHED -> {
                // Forward query to upstream DNS resolver
                serviceScope.launch {
                    forwardToUpstream(query, outputStream, startTime, matchResult)
                }
            }
        }
    }

    private fun forwardToUpstream(
        query: ParsedDnsQuery,
        outputStream: FileOutputStream,
        startTime: Long,
        matchResult: DomainMatchResult
    ) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            protect(socket) // Critical: socket bypasses VPN tunnel
            socket.soTimeout = 2500

            val serverAddr = InetAddress.getByName(upstreamDns)
            val sendPacket = DatagramPacket(
                query.rawDnsPayload,
                query.rawDnsPayload.size,
                serverAddr,
                53
            )
            socket.send(sendPacket)

            val receiveBuf = ByteArray(4096)
            val receivePacket = DatagramPacket(receiveBuf, receiveBuf.size)
            socket.receive(receivePacket)

            val responseDnsPayload = ByteArray(receivePacket.length)
            System.arraycopy(receiveBuf, 0, responseDnsPayload, 0, receivePacket.length)

            val responsePacket = DnsPacketHandler.buildIpUdpPacket(
                dnsPayload = responseDnsPayload,
                srcIp = query.serverIp,
                srcPort = query.serverPort,
                dstIp = query.clientIp,
                dstPort = query.clientPort
            )

            synchronized(outputStream) {
                outputStream.write(responsePacket)
            }

            val latency = System.currentTimeMillis() - startTime
            val ruleTag = if (matchResult == DomainMatchResult.WHITELISTED) "Whitelist" else "Allowed"

            Log.v(TAG, "ALLOWED domain: ${query.domain} (${query.qTypeName}) via $upstreamDns in ${latency}ms")
            Log.v(TAG_COMMON, "ALLOWED: ${query.domain} (${latency}ms)")

            logChannel.trySend(
                QueryLogEntity(
                    domain = query.domain,
                    timestamp = startTime,
                    status = QueryStatus.ALLOWED,
                    queryType = query.qTypeName,
                    latencyMs = latency,
                    clientIp = query.clientIp,
                    ruleMatched = ruleTag
                )
            )
        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "DNS query timed out for: ${query.domain}")
        } catch (e: Exception) {
            Log.w(TAG, "DNS forward error for ${query.domain}: ${e.message}")
        } finally {
            socket?.close()
        }
    }

    private fun startLogProcessor() {
        serviceScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            for (log in logChannel) {
                try {
                    db.queryLogDao().insert(log)
                } catch (e: Exception) {
                    Log.e(TAG, "Error inserting log into DB", e)
                }
            }
        }
    }

    private fun stopVpn() {
        isVpnActive = false
        VpnStateHolder.setRunning(false)
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN descriptor", e)
        }
        Log.v(TAG, "AdShield VPN service stopped")
        Log.v(TAG_COMMON, "AdShield protection PAUSED")
    }

    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        logChannel.close()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows AdShield active protection status"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val TAG = "AdShieldVpnService"
        const val TAG_COMMON = "AdShield"
        const val ACTION_START = "com.local.adshield.START_VPN"
        const val ACTION_STOP = "com.local.adshield.STOP_VPN"
        const val ACTION_RELOAD_RULES = "com.local.adshield.RELOAD_RULES"
        const val EXTRA_UPSTREAM_DNS = "extra_upstream_dns"
        private const val CHANNEL_ID = "adshield_vpn_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context, upstreamDns: String = "1.1.1.1") {
            val intent = Intent(context, AdShieldVpnService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_UPSTREAM_DNS, upstreamDns)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AdShieldVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun reload(context: Context) {
            val intent = Intent(context, AdShieldVpnService::class.java).apply {
                action = ACTION_RELOAD_RULES
            }
            context.startService(intent)
        }
    }
}
