package com.vinplay.m3u.data.model

data class ImportProgress(
    val totalChannels: Int = 0,
    val parsedChannels: Int = 0,
    val currentLine: String = ""
)
