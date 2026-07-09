package com.vinplay.m3u.network

import com.vinplay.m3u.data.model.TestStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class LinkTestResult(
    val channelId: Long,
    val status: TestStatus,
    val statusCode: Int? = null
)

@Singleton
class LinkTester @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val concurrencyLimit = Semaphore(10)

    suspend fun testLinks(urls: List<Pair<Long, String>>): List<LinkTestResult> = coroutineScope {
        urls.map { (id, url) ->
            async(Dispatchers.IO) {
                concurrencyLimit.withPermit {
                    testSingleLink(id, url)
                }
            }
        }.awaitAll()
    }

    private fun testSingleLink(channelId: Long, url: String): LinkTestResult {
        return try {
            // First try HEAD request
            val headRequest = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "VinPlayM3U/1.0")
                .build()

            val headResponse = client.newCall(headRequest).execute()
            val code = headResponse.code
            headResponse.close()

            if (code in 200..399) {
                LinkTestResult(channelId, TestStatus.ONLINE, code)
            } else if (code in 400..499) {
                tryRangeGet(url, channelId, code)
            } else {
                LinkTestResult(channelId, TestStatus.OFFLINE, code)
            }
        } catch (e: java.net.SocketTimeoutException) {
            LinkTestResult(channelId, TestStatus.TIMEOUT)
        } catch (_: Exception) {
            LinkTestResult(channelId, TestStatus.ERROR)
        }
    }

    private fun tryRangeGet(url: String, channelId: Long, headCode: Int): LinkTestResult {
        return try {
            val rangeRequest = Request.Builder()
                .url(url)
                .header("User-Agent", "VinPlayM3U/1.0")
                .header("Range", "bytes=0-0")
                .build()

            val rangeResponse = client.newCall(rangeRequest).execute()
            val code = rangeResponse.code
            rangeResponse.close()

            if (code in 200..399 || code == 416) {
                LinkTestResult(channelId, TestStatus.ONLINE, code)
            } else {
                LinkTestResult(channelId, TestStatus.OFFLINE, code)
            }
        } catch (_: Exception) {
            LinkTestResult(channelId, TestStatus.OFFLINE, headCode)
        }
    }
}
