package io.github.degipe.youtubewhitelist.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_channels")
data class BlockedChannelEntity(
    @PrimaryKey val channelId: String,
    val channelName: String,
    val addedAt: Long = System.currentTimeMillis()
)
