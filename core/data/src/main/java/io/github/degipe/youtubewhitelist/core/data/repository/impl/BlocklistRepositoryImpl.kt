package io.github.degipe.youtubewhitelist.core.data.repository.impl

import io.github.degipe.youtubewhitelist.core.common.di.IoDispatcher
import io.github.degipe.youtubewhitelist.core.data.model.BlockedChannel
import io.github.degipe.youtubewhitelist.core.data.repository.BlocklistRepository
import io.github.degipe.youtubewhitelist.core.database.dao.BlockedChannelDao
import io.github.degipe.youtubewhitelist.core.database.entity.BlockedChannelEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BlocklistRepositoryImpl @Inject constructor(
    private val blockedChannelDao: BlockedChannelDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BlocklistRepository {

    override fun getAllBlockedChannels(profileId: String): Flow<List<BlockedChannel>> =
        blockedChannelDao.getAllByProfile(profileId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getBlockedChannelIdsFlow(profileId: String): Flow<List<String>> =
        blockedChannelDao.getBlockedChannelIdsFlow(profileId)

    override suspend fun isBlocked(profileId: String, channelId: String): Boolean =
        withContext(ioDispatcher) {
            blockedChannelDao.isBlocked(profileId, channelId)
        }

    override suspend fun blockChannel(
        profileId: String,
        channelId: String,
        channelTitle: String,
        channelThumbnailUrl: String?
    ) = withContext(ioDispatcher) {
        blockedChannelDao.insert(
            BlockedChannelEntity(
                kidProfileId = profileId,
                channelId = channelId,
                channelTitle = channelTitle,
                channelThumbnailUrl = channelThumbnailUrl
            )
        )
    }

    override suspend fun unblockChannel(profileId: String, channelId: String) =
        withContext(ioDispatcher) {
            blockedChannelDao.unblockChannel(profileId, channelId)
        }

    companion object {
        private fun BlockedChannelEntity.toDomain(): BlockedChannel = BlockedChannel(
            id = id,
            kidProfileId = kidProfileId,
            channelId = channelId,
            channelTitle = channelTitle,
            channelThumbnailUrl = channelThumbnailUrl,
            blockedAt = blockedAt
        )
    }
}
