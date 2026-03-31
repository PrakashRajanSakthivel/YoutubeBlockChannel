package io.github.degipe.youtubewhitelist.core.data.repository

import io.github.degipe.youtubewhitelist.core.data.model.BlockedChannel
import kotlinx.coroutines.flow.Flow

interface BlocklistRepository {
    fun getAllBlockedChannels(profileId: String): Flow<List<BlockedChannel>>
    fun getBlockedChannelIdsFlow(profileId: String): Flow<List<String>>
    suspend fun isBlocked(profileId: String, channelId: String): Boolean
    suspend fun blockChannel(
        profileId: String,
        channelId: String,
        channelTitle: String,
        channelThumbnailUrl: String?
    )
    suspend fun unblockChannel(profileId: String, channelId: String)
}
