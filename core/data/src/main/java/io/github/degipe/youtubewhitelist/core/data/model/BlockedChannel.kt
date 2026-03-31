package io.github.degipe.youtubewhitelist.core.data.model

data class BlockedChannel(
    val id: Long = 0,
    val kidProfileId: String,
    val channelId: String,
    val channelTitle: String,
    val channelThumbnailUrl: String?,
    val blockedAt: Long = System.currentTimeMillis()
)
