package com.vinplay.m3u.data.model

data class PlaylistSummary(
    val id: Long,
    val name: String,
    val channelCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)
