package com.vinplay.m3u.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vinplay.m3u.data.model.ChannelKind
import com.vinplay.m3u.data.model.TestStatus

@Entity(
    tableName = "channels",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlist_id", "order_index"]),
        Index(value = ["playlist_id", "deleted_at"])
    ]
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "playlist_id") val playlistId: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "group_title") val groupTitle: String = "",
    @ColumnInfo(name = "tvg_id") val tvgId: String = "",
    @ColumnInfo(name = "tvg_logo") val tvgLogo: String = "",
    @ColumnInfo(name = "tvg_name") val tvgName: String = "",
    @ColumnInfo(name = "kind") val kind: ChannelKind = ChannelKind.UNKNOWN,
    @ColumnInfo(name = "order_index") val orderIndex: Int = 0,
    @ColumnInfo(name = "test_status") val testStatus: TestStatus = TestStatus.UNCHECKED,
    @ColumnInfo(name = "test_status_code") val testStatusCode: Int? = null,
    @ColumnInfo(name = "test_checked_at") val testCheckedAt: Long? = null,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null
)
