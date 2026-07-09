package com.vinplay.m3u.data.parser

import com.vinplay.m3u.data.local.entity.ChannelEntity
import com.vinplay.m3u.data.model.ChannelKind
import com.vinplay.m3u.data.model.ImportProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Streaming M3U/M3U8 parser.
 * Parses line-by-line to avoid OOM on huge playlists (100k+ channels).
 * Emits progress updates via Flow.
 */
class M3UParser {

    data class ParsedChannel(
        val name: String,
        val url: String,
        val tvgId: String = "",
        val tvgName: String = "",
        val tvgLogo: String = "",
        val groupTitle: String = ""
    )

    fun parseStream(
        inputStream: InputStream,
        playlistId: Long,
        batchSize: Int = 500
    ): Flow<ParsedBatch> = flow {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        var currentExtinf: M3UExtinf? = null
        var lineNumber = 0
        var parsedCount = 0
        val batch = mutableListOf<ParsedChannel>()
        val seenUrls = mutableSetOf<String>()

        while (reader.readLine().also { line = it } != null) {
            val l = line?.trim() ?: continue
            lineNumber++

            // Skip empty lines, comments (except EXTINF), and #EXTM3U header
            if (l.isEmpty() || l.startsWith("#EXTM3U") || l.startsWith("#EXT-X-")) continue

            if (l.startsWith("#EXTINF:")) {
                currentExtinf = parseExtinf(l)
                continue
            }

            // Regular comment lines that aren't EXTINF
            if (l.startsWith("#")) continue

            // This line is a URL
            val channel = currentExtinf
            if (channel != null && l.isNotBlank()) {
                // Deduplicate by URL
                if (seenUrls.add(l)) {
                    batch.add(
                        ParsedChannel(
                            name = channel.name.ifBlank { extractNameFromUrl(l) },
                            url = l,
                            tvgId = channel.tvgId,
                            tvgName = channel.tvgName,
                            tvgLogo = channel.tvgLogo,
                            groupTitle = channel.groupTitle
                        )
                    )
                    parsedCount++
                }
            }
            currentExtinf = null

            if (batch.size >= batchSize) {
                emit(ParsedBatch(batch.toList(), parsedCount))
                batch.clear()
            }
        }

        // Emit remaining
        if (batch.isNotEmpty()) {
            emit(ParsedBatch(batch.toList(), parsedCount))
        }

        reader.close()
    }

    private fun parseExtinf(line: String): M3UExtinf? {
        try {
            // Format: #EXTINF:-1 tvg-id="..." tvg-name="..." tvg-logo="..." group-title="...",Channel Name
            val attrs = mutableMapOf<String, String>()

            // Extract the display name after the last comma
            val commaIndex = line.lastIndexOf(',')
            val name = if (commaIndex >= 0) {
                line.substring(commaIndex + 1).trim()
            } else ""

            if (commaIndex < 0) return null

            val attrPart = line.substring(8, commaIndex).trim()

            // Parse key="value" pairs
            val regex = """(\w[\w-]*)\s*=\s*"([^"]*)"""".toRegex()
            regex.findAll(attrPart).forEach { match ->
                attrs[match.groupValues[1]] = match.groupValues[2]
            }

            return M3UExtinf(
                name = name,
                tvgId = attrs["tvg-id"] ?: "",
                tvgName = attrs["tvg-name"] ?: "",
                tvgLogo = attrs["tvg-logo"] ?: "",
                groupTitle = attrs["group-title"] ?: ""
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun extractNameFromUrl(url: String): String {
        return try {
            val path = url.substringAfter("://").substringAfter("/")
            val filename = path.substringAfterLast("/").substringBefore("?")
            filename.substringBeforeLast(".").replace("_", " ").replace("-", " ").trim()
                .ifBlank { url.take(50) }
        } catch (_: Exception) {
            url.take(50)
        }
    }

    fun detectKind(url: String): ChannelKind {
        val lower = url.lowercase()
        return when {
            lower.contains("/series/") || lower.endsWith(".ts") -> ChannelKind.SERIES
            lower.contains("/movie/") || lower.endsWith(".mp4") || lower.endsWith(".mkv") || 
                lower.endsWith(".avi") || lower.endsWith(".mov") || lower.endsWith(".webm") -> ChannelKind.VOD
            lower.endsWith(".m3u8") || lower.endsWith(".m3u") || lower.contains(".m3u8/") -> ChannelKind.LIVE
            lower.startsWith("rtmp://") || lower.startsWith("rtsp://") -> ChannelKind.LIVE
            else -> ChannelKind.UNKNOWN
        }
    }

    fun convertToEntities(
        channels: List<ParsedChannel>,
        playlistId: Long,
        startOrderIndex: Int = 0
    ): List<ChannelEntity> {
        return channels.mapIndexed { index, ch ->
            ChannelEntity(
                playlistId = playlistId,
                name = ch.name,
                url = ch.url,
                groupTitle = ch.groupTitle,
                tvgId = ch.tvgId,
                tvgLogo = ch.tvgLogo,
                tvgName = ch.tvgName,
                kind = detectKind(ch.url),
                orderIndex = startOrderIndex + index
            )
        }
    }

    data class ParsedBatch(
        val channels: List<ParsedChannel>,
        val totalSoFar: Int
    )

    private data class M3UExtinf(
        val name: String,
        val tvgId: String,
        val tvgName: String,
        val tvgLogo: String,
        val groupTitle: String
    )
}
