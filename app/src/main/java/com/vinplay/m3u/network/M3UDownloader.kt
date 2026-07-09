package com.vinplay.m3u.network

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class M3UDownloader @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Downloads an M3U file and returns the input stream for line-by-line parsing.
     * Uses streaming so large files don't OOM.
     */
    fun downloadStream(url: String): InputStream {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "VinPlayM3U/1.0")
            .header("Accept", "audio/x-mpegurl, application/vnd.apple.mpegurl, */*")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw M3UDownloadException("HTTP ${response.code}: ${response.message}")
        }
        return response.body?.byteStream()
            ?: throw M3UDownloadException("Empty response body")
    }
}

class M3UDownloadException(message: String) : Exception(message)
