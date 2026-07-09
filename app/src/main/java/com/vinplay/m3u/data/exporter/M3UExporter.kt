package com.vinplay.m3u.data.exporter

import com.vinplay.m3u.data.local.entity.ChannelEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class M3UExporter @Inject constructor() {

    /**
     * Serializes channels back to M3U format.
     */
    fun exportToM3U(playlistName: String, channels: List<ChannelEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("#EXTM3U")
        sb.appendLine("#PLAYLIST: $playlistName")

        channels.forEach { ch ->
            sb.appendLine(
                "#EXTINF:-1 " +
                "tvg-id=\"${escapeAttr(ch.tvgId)}\" " +
                "tvg-name=\"${escapeAttr(ch.tvgName)}\" " +
                "tvg-logo=\"${escapeAttr(ch.tvgLogo)}\" " +
                "group-title=\"${escapeAttr(ch.groupTitle)}\"," +
                ch.name
            )
            sb.appendLine(ch.url)
        }

        return sb.toString()
    }

    /**
     * Serializes to M3U8 (UTF-8) — same format header.
     */
    fun exportToM3U8(playlistName: String, channels: List<ChannelEntity>): String {
        return exportToM3U(playlistName, channels)
    }

    private fun escapeAttr(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }
}
