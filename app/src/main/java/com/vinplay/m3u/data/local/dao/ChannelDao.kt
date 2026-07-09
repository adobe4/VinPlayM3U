package com.vinplay.m3u.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vinplay.m3u.data.local.entity.ChannelEntity
import com.vinplay.m3u.data.model.TestStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("""
        SELECT * FROM channels 
        WHERE playlist_id = :playlistId AND deleted_at IS NULL 
        ORDER BY order_index ASC
    """)
    fun getChannelsByPlaylist(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("""
        SELECT * FROM channels 
        WHERE playlist_id = :playlistId AND deleted_at IS NULL 
        ORDER BY order_index ASC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getChannelsPaginated(playlistId: Long, limit: Int, offset: Int): List<ChannelEntity>

    @Query("""
        SELECT * FROM channels 
        WHERE playlist_id = :playlistId AND deleted_at IS NULL 
        AND (:query IS NULL OR name LIKE '%' || :query || '%')
        AND (:groupFilter IS NULL OR group_title = :groupFilter)
        AND (:kindFilter IS NULL OR kind = :kindFilter)
        ORDER BY order_index ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchChannels(
        playlistId: Long,
        query: String?,
        groupFilter: String?,
        kindFilter: String?,
        limit: Int,
        offset: Int
    ): List<ChannelEntity>

    @Query("""
        SELECT COUNT(*) FROM channels 
        WHERE playlist_id = :playlistId AND deleted_at IS NULL
        AND (:query IS NULL OR name LIKE '%' || :query || '%')
        AND (:groupFilter IS NULL OR group_title = :groupFilter)
        AND (:kindFilter IS NULL OR kind = :kindFilter)
    """)
    suspend fun countChannels(
        playlistId: Long,
        query: String? = null,
        groupFilter: String? = null,
        kindFilter: String? = null
    ): Int

    @Query("SELECT DISTINCT group_title FROM channels WHERE playlist_id = :playlistId AND deleted_at IS NULL AND group_title != '' ORDER BY group_title ASC")
    fun getGroups(playlistId: Long): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getChannelById(id: Long): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(channel: ChannelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<ChannelEntity>)

    @Update
    suspend fun update(channel: ChannelEntity)

    @Query("UPDATE channels SET order_index = :orderIndex WHERE id = :id")
    suspend fun updateOrder(id: Long, orderIndex: Int)

    @Query("UPDATE channels SET deleted_at = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE channels SET deleted_at = :now WHERE playlist_id = :playlistId AND id IN (:ids)")
    suspend fun softDeleteBatch(playlistId: Long, ids: List<Long>, now: Long = System.currentTimeMillis())

    @Query("UPDATE channels SET deleted_at = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("""
        UPDATE channels SET group_title = :newGroup 
        WHERE playlist_id = :playlistId AND deleted_at IS NULL 
        AND (:oldGroup IS NULL OR group_title = :oldGroup)
    """)
    suspend fun renameGroup(playlistId: Long, oldGroup: String?, newGroup: String)

    @Query("SELECT * FROM channels WHERE playlist_id = :playlistId AND deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    fun getTrashedChannels(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("DELETE FROM channels WHERE playlist_id = :playlistId AND deleted_at IS NOT NULL")
    suspend fun emptyTrash(playlistId: Long)

    @Query("""
        UPDATE channels SET test_status = :status, test_status_code = :statusCode, test_checked_at = :checkedAt 
        WHERE id = :id
    """)
    suspend fun updateTestStatus(id: Long, status: TestStatus, statusCode: Int? = null, checkedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM channels WHERE playlist_id = :playlistId AND deleted_at IS NULL")
    suspend fun getAllActiveChannels(playlistId: Long): List<ChannelEntity>

    @Query("SELECT COUNT(*) FROM channels WHERE playlist_id = :playlistId AND deleted_at IS NULL")
    fun getChannelCount(playlistId: Long): Flow<Int>

    @Query("SELECT MAX(order_index) FROM channels WHERE playlist_id = :playlistId")
    suspend fun getMaxOrderIndex(playlistId: Long): Int?
}
