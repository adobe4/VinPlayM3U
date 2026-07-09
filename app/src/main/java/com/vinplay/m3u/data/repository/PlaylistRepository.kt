package com.vinplay.m3u.data.repository

import com.vinplay.m3u.data.local.dao.PlaylistDao
import com.vinplay.m3u.data.local.dao.PlaylistWithCount
import com.vinplay.m3u.data.local.entity.PlaylistEntity
import com.vinplay.m3u.data.model.PlaylistSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao
) {
    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    fun getPlaylistSummaries(): Flow<List<PlaylistSummary>> {
        return playlistDao.getPlaylistsWithCount().map { list ->
            list.map { pw ->
                PlaylistSummary(
                    id = pw.id,
                    name = pw.name,
                    channelCount = pw.channel_count,
                    createdAt = pw.created_at,
                    updatedAt = pw.updated_at
                )
            }
        }
    }

    suspend fun getPlaylistById(id: Long): PlaylistEntity? = playlistDao.getPlaylistById(id)

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insert(PlaylistEntity(name = name))
    }

    suspend fun renamePlaylist(id: Long, name: String) = playlistDao.rename(id, name)

    suspend fun deletePlaylist(id: Long) = playlistDao.deleteById(id)

    suspend fun touchPlaylist(id: Long) = playlistDao.touch(id)
}
