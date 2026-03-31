package io.github.degipe.youtubewhitelist.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocked_channels",
    foreignKeys = [
        ForeignKey(
            entity = KidProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["kidProfileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("kidProfileId"),
        Index(value = ["kidProfileId", "channelId"], unique = true)
    ]
)
data class BlockedChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kidProfileId: String,
    val channelId: String,
    val channelTitle: String,
    val channelThumbnailUrl: String?,
    val blockedAt: Long = System.currentTimeMillis()
)
