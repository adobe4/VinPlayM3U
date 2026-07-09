package com.vinplay.m3u.data.repository

import com.vinplay.m3u.data.local.dao.ChannelDao
import com.vinplay.m3u.data.local.entity.ChannelEntity
import com.vinplay.m3u.data.model.ChannelKind
import com.vinplay.m3u.data.model.TestStatus
import com.vinplay.m3u.data.parser.M3UParser
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepository @Inject constructor(
    private val channelDao: ChannelDao
) {
    fun getChannelsByPlaylist(playlistId: Long): Flow<List<ChannelEntity>> =
        channelDao.getChannelsByPlaylist(playlistId)

    suspend fun getChannelsPaginated(playlistId: Long, limit: Int, offset: Int): List<ChannelEntity> =
        channelDao.getChannelsPaginated(playlistId, limit, offset)

    suspend fun searchChannels(
        playlistId: Long,
        query: String? = null,
        groupFilter: String? = null,
        kindFilter: String? = null,
        limit: Int = 200,
        offset: Int = 0
    ): List<ChannelEntity> = channelDao.searchChannels(
        playlistId, query, groupFilter, kindFilter, limit, offset
    )

    suspend fun countChannels(
        playlistId: Long,
        query: String? = null,
        groupFilter: String? = null,
        kindFilter: String? = null
    ): Int = channelDao.countChannels(playlistId, query, groupFilter, kindFilter)

    fun getGroups(playlistId: Long): Flow<List<String>> = channelDao.getGroups(playlistId)

    fun getChannelCount(playlistId: Long): Flow<Int> = channelDao.getChannelCount(playlistId)

    suspend fun insertAll(channels: List<ChannelEntity>) = channelDao.insertAll(channels)

    suspend fun updateChannel(channel: ChannelEntity) = channelDao.update(channel)

    suspend fun softDelete(id: Long) = channelDao.softDelete(id)

    suspend fun softDeleteBatch(playlistId: Long, ids: List<Long>) =
        channelDao.softDeleteBatch(playlistId, ids)

    suspend fun restore(id: Long) = channelDao.restore(id)

    suspend fun renameGroup(playlistId: Long, oldGroup: String?, newGroup: String) =
        channelDao.renameGroup(playlistId, oldGroup, newGroup)

    fun getTrashedChannels(playlistId: Long): Flow<List<ChannelEntity>> =
        channelDao.getTrashedChannels(playlistId)

    suspend fun emptyTrash(playlistId: Long) = channelDao.emptyTrash(playlistId)

    suspend fun updateTestStatus(id: Long, status: TestStatus, statusCode: Int? = null) =
        channelDao.updateTestStatus(id, status, statusCode)

    suspend fun getAllActiveChannels(playlistId: Long): List<ChannelEntity> =
        channelDao.getAllActiveChannels(playlistId)

    suspend fun getMaxOrderIndex(playlistId: Long): Int =
        channelDao.getMaxOrderIndex(playlistId) ?: 0

    suspend fun getChannelById(id: Long): ChannelEntity? = channelDao.getChannelById(id)

    /**
     * Insert parsed channels in a batch transaction.
     * Returns the max order index after insertion.
     */
    suspend fun insertParsedBatch(
        channels: List<M3UParser.ParsedChannel>,
        playlistId: Long,
        startOrderIndex: Int
    ): Int {
        val entities = M3UParser().convertToEntities(channels, playlistId, startOrderIndex)
        insertAll(entities)
        return startOrderIndex + entities.size
    }
}
