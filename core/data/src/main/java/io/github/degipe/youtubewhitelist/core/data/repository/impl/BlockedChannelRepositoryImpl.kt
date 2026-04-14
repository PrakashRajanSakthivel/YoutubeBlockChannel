package io.github.degipe.youtubewhitelist.core.data.repository.impl

import io.github.degipe.youtubewhitelist.core.data.model.BlockedChannel
import io.github.degipe.youtubewhitelist.core.data.repository.BlockedChannelRepository
import io.github.degipe.youtubewhitelist.core.database.dao.BlockedChannelDao
import io.github.degipe.youtubewhitelist.core.database.entity.BlockedChannelEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BlockedChannelRepositoryImpl @Inject constructor(
    private val dao: BlockedChannelDao
) : BlockedChannelRepository {

    override fun getAll(): Flow<List<BlockedChannel>> =
        dao.getAll().map { list ->
            list.map { BlockedChannel(it.channelId, it.channelName, it.addedAt) }
        }

    override fun getAllIds(): Flow<Set<String>> = dao.getAllIds().map { it.toSet() }

    override suspend fun block(channelId: String, channelName: String) {
        dao.insert(BlockedChannelEntity(channelId = channelId, channelName = channelName))
    }

    override suspend fun unblock(channelId: String) {
        dao.delete(channelId)
    }
}
