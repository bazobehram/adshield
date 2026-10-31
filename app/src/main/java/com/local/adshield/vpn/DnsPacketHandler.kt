package com.local.adshield.vpn

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

data class ParsedDnsQuery(
    val id: Int,
    val domain: String,
    val qType: Int,
    val qClass: Int,
    val qTypeName: String,
    val rawDnsPayload: ByteArray,
    val questionEndOffset: Int,
    val clientIp: String,
    val clientPort: Int,
    val serverIp: String,
    val serverPort: Int
)

object DnsPacketHandler {

    private const val IP_HEADER_MIN_LEN = 20
    private const val UDP_HEADER_LEN = 8
    private const val DNS_HEADER_LEN = 12
    private const val PROTOCOL_UDP = 17
    private const val DNS_PORT = 53

    const val TYPE_A = 1
    const val TYPE_NS = 2
    const val TYPE_CNAME = 5
    const val TYPE_SOA = 6
    const val TYPE_PTR = 12
    const val TYPE_MX = 15
    const val TYPE_TXT = 16
    const val TYPE_AAAA = 28
    const val TYPE_SRV = 33
    const val TYPE_HTTPS = 65

    fun parseUdpDnsPacket(packet: ByteArray, length: Int): ParsedDnsQuery? {
        if (length < IP_HEADER_MIN_LEN + UDP_HEADER_LEN + DNS_HEADER_LEN) {
            return null
        }

        // 1. Parse IPv4 Header
        val version = (packet[0].toInt() ushr 4) and 0x0F
        if (version != 4) return null

        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (ihl < IP_HEADER_MIN_LEN || length < ihl + UDP_HEADER_LEN + DNS_HEADER_LEN) {
            return null
        }

        val protocol = packet[9].toInt() and 0xFF
        if (protocol != PROTOCOL_UDP) return null

        val srcIp = "${packet[12].toInt() and 0xFF}.${packet[13].toInt() and 0xFF}.${packet[14].toInt() and 0xFF}.${packet[15].toInt() and 0xFF}"
        val dstIp = "${packet[16].toInt() and 0xFF}.${packet[17].toInt() and 0xFF}.${packet[18].toInt() and 0xFF}.${packet[19].toInt() and 0xFF}"

        // 2. Parse UDP Header
        val srcPort = ((packet[ihl].toInt() and 0xFF) shl 8) or (packet[ihl + 1].toInt() and 0xFF)
        val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
        val udpLen = ((packet[ihl + 4].toInt() and 0xFF) shl 8) or (packet[ihl + 5].toInt() and 0xFF)

        if (dstPort != DNS_PORT || udpLen < UDP_HEADER_LEN + DNS_HEADER_LEN) {
            return null
        }

        val dnsOffset = ihl + UDP_HEADER_LEN
        val dnsLength = minOf(udpLen - UDP_HEADER_LEN, length - dnsOffset)
        if (dnsLength < DNS_HEADER_LEN) return null

        val rawDnsPayload = ByteArray(dnsLength)
        System.arraycopy(packet, dnsOffset, rawDnsPayload, 0, dnsLength)

        // 3. Parse DNS Header
        val id = ((rawDnsPayload[0].toInt() and 0xFF) shl 8) or (rawDnsPayload[1].toInt() and 0xFF)
        val flags = ((rawDnsPayload[2].toInt() and 0xFF) shl 8) or (rawDnsPayload[3].toInt() and 0xFF)
        val qr = (flags ushr 15) and 0x01
        val qdCount = ((rawDnsPayload[4].toInt() and 0xFF) shl 8) or (rawDnsPayload[5].toInt() and 0xFF)

        // Only handle queries with at least one question
        if (qr != 0 || qdCount < 1) return null

        // 4. Parse Question QNAME
        var pos = DNS_HEADER_LEN
        val domainParts = mutableListOf<String>()

        while (pos < dnsLength) {
            val labelLen = rawDnsPayload[pos].toInt() and 0xFF
            if (labelLen == 0) {
                pos++ // End of domain
                break
            }
            if ((labelLen and 0xC0) == 0xC0) {
                pos += 2
                break
            }
            pos++
            if (pos + labelLen > dnsLength) return null

            val label = String(rawDnsPayload, pos, labelLen, Charsets.US_ASCII)
            domainParts.add(label)
            pos += labelLen
        }

        if (pos + 4 > dnsLength) return null
        val qType = ((rawDnsPayload[pos].toInt() and 0xFF) shl 8) or (rawDnsPayload[pos + 1].toInt() and 0xFF)
        val qClass = ((rawDnsPayload[pos + 2].toInt() and 0xFF) shl 8) or (rawDnsPayload[pos + 3].toInt() and 0xFF)
        val questionEndOffset = pos + 4

        val domain = domainParts.joinToString(".")

        return ParsedDnsQuery(
            id = id,
            domain = domain,
            qType = qType,
            qClass = qClass,
            qTypeName = getTypeName(qType),
            rawDnsPayload = rawDnsPayload,
            questionEndOffset = questionEndOffset,
            clientIp = srcIp,
            clientPort = srcPort,
            serverIp = dstIp,
            serverPort = dstPort
        )
    }

    /**
     * Synthesizes an immediate DNS sinkhole response (0.0.0.0 or ::) for blocked domains.
     */
    fun buildSinkholeDnsResponse(query: ParsedDnsQuery): ByteArray {
        val out = ByteArrayOutputStream()

        // 1. DNS Header
        out.write((query.id ushr 8) and 0xFF)
        out.write(query.id and 0xFF)

        // Flags: QR=1 (Response), Opcode=0, AA=1 (Authoritative), TC=0, RD=1, RA=1, Z=0, RCODE=0 (NoError)
        val flags = 0x8180
        out.write((flags ushr 8) and 0xFF)
        out.write(flags and 0xFF)

        // QDCOUNT = 1
        out.write(0x00)
        out.write(0x01)

        val isTypeA = query.qType == TYPE_A
        val isTypeAAAA = query.qType == TYPE_AAAA

        if (isTypeA || isTypeAAAA) {
            // ANCOUNT = 1
            out.write(0x00)
            out.write(0x01)
        } else {
            // ANCOUNT = 0
            out.write(0x00)
            out.write(0x00)
        }

        // NSCOUNT = 0
        out.write(0x00)
        out.write(0x00)

        // ARCOUNT = 0
        out.write(0x00)
        out.write(0x00)

        // 2. Question section (copied verbatim from request)
        val questionLen = query.questionEndOffset - DNS_HEADER_LEN
        out.write(query.rawDnsPayload, DNS_HEADER_LEN, questionLen)

        // 3. Answer section
        if (isTypeA) {
            // Pointer to QNAME at byte 12 (0xC00C)
            out.write(0xC0)
            out.write(0x0C)

            // Type A (1)
            out.write(0x00)
            out.write(0x01)

            // Class IN (1)
            out.write(0x00)
            out.write(0x01)

            // TTL (60s)
            out.write(0x00)
            out.write(0x00)
            out.write(0x00)
            out.write(0x3C)

            // RDLENGTH = 4
            out.write(0x00)
            out.write(0x04)

            // RDATA = 0.0.0.0
            out.write(0x00)
            out.write(0x00)
            out.write(0x00)
            out.write(0x00)
        } else if (isTypeAAAA) {
            // Pointer to QNAME at byte 12 (0xC00C)
            out.write(0xC0)
            out.write(0x0C)

            // Type AAAA (28)
            out.write(0x00)
            out.write(0x1C)

            // Class IN (1)
            out.write(0x00)
            out.write(0x01)

            // TTL (60s)
            out.write(0x00)
            out.write(0x00)
            out.write(0x00)
            out.write(0x3C)

            // RDLENGTH = 16
            out.write(0x00)
            out.write(0x10)

            // RDATA = :: (16 zeroes)
            for (i in 0 until 16) {
                out.write(0x00)
            }
        }

        return out.toByteArray()
    }

    /**
     * Wraps raw DNS payload into a valid IPv4 UDP packet ready to write to TUN interface.
     */
    fun buildIpUdpPacket(
        dnsPayload: ByteArray,
        srcIp: String,
        srcPort: Int,
        dstIp: String,
        dstPort: Int
    ): ByteArray {
        val totalLength = IP_HEADER_MIN_LEN + UDP_HEADER_LEN + dnsPayload.size
        val packet = ByteArray(totalLength)
        val buffer = ByteBuffer.wrap(packet)

        // 1. IPv4 Header
        buffer.put(0x45.toByte()) // Version 4, IHL 5 (20 bytes)
        buffer.put(0x00.toByte()) // DSCP/ECN
        buffer.putShort(totalLength.toShort()) // Total Length
        buffer.putShort(0.toShort()) // Identification
        buffer.putShort(0x4000.toShort()) // Flags: Don't Fragment
        buffer.put(64.toByte()) // TTL
        buffer.put(PROTOCOL_UDP.toByte()) // Protocol
        buffer.putShort(0.toShort()) // Checksum placeholder

        // Source IP
        parseIpAddress(srcIp, packet, 12)
        // Destination IP
        parseIpAddress(dstIp, packet, 16)

        // Compute IPv4 Checksum
        val ipChecksum = computeIpChecksum(packet, 0, IP_HEADER_MIN_LEN)
        packet[10] = ((ipChecksum ushr 8) and 0xFF).toByte()
        packet[11] = (ipChecksum and 0xFF).toByte()

        // 2. UDP Header
        buffer.position(IP_HEADER_MIN_LEN)
        buffer.putShort(srcPort.toShort())
        buffer.putShort(dstPort.toShort())
        val udpLen = UDP_HEADER_LEN + dnsPayload.size
        buffer.putShort(udpLen.toShort())
        buffer.putShort(0.toShort()) // Checksum 0 = disabled/optional in IPv4 UDP

        // 3. DNS Payload
        System.arraycopy(dnsPayload, 0, packet, IP_HEADER_MIN_LEN + UDP_HEADER_LEN, dnsPayload.size)

        return packet
    }

    private fun parseIpAddress(ipStr: String, dest: ByteArray, offset: Int) {
        val parts = ipStr.split(".")
        if (parts.size == 4) {
            for (i in 0..3) {
                dest[offset + i] = (parts[i].toIntOrNull() ?: 0).toByte()
            }
        }
    }

    private fun computeIpChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length) {
            val high = data[i].toInt() and 0xFF
            val low = data[i + 1].toInt() and 0xFF
            sum += (high shl 8) or low
            i += 2
        }
        while ((sum ushr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        return sum.inv() and 0xFFFF
    }

    fun getTypeName(qType: Int): String {
        return when (qType) {
            TYPE_A -> "A"
            TYPE_NS -> "NS"
            TYPE_CNAME -> "CNAME"
            TYPE_SOA -> "SOA"
            TYPE_PTR -> "PTR"
            TYPE_MX -> "MX"
            TYPE_TXT -> "TXT"
            TYPE_AAAA -> "AAAA"
            TYPE_SRV -> "SRV"
            TYPE_HTTPS -> "HTTPS"
            else -> "TYPE-$qType"
        }
    }
}
