package io.github.degipe.youtubewhitelist.core.data.repository

import io.github.degipe.youtubewhitelist.core.data.model.BlockedChannel
import kotlinx.coroutines.flow.Flow

interface BlockedChannelRepository {
    fun getAll(): Flow<List<BlockedChannel>>
    fun getAllIds(): Flow<Set<String>>
    suspend fun block(channelId: String, channelName: String)
    suspend fun unblock(channelId: String)
}
