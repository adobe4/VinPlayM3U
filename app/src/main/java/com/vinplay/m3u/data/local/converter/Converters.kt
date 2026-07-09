package com.vinplay.m3u.data.local.converter

import androidx.room.TypeConverter
import com.vinplay.m3u.data.model.ChannelKind
import com.vinplay.m3u.data.model.TestStatus

class Converters {
    @TypeConverter
    fun fromChannelKind(kind: ChannelKind): String = kind.name

    @TypeConverter
    fun toChannelKind(value: String): ChannelKind = ChannelKind.valueOf(value)

    @TypeConverter
    fun fromTestStatus(status: TestStatus): String = status.name

    @TypeConverter
    fun toTestStatus(value: String): TestStatus = TestStatus.valueOf(value)
}
