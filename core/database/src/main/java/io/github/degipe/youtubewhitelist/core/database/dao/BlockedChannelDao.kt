package io.github.degipe.youtubewhitelist.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.degipe.youtubewhitelist.core.database.entity.BlockedChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedChannelDao {

    @Query("SELECT * FROM blocked_channels WHERE kidProfileId = :profileId ORDER BY blockedAt DESC")
    fun getAllByProfile(profileId: String): Flow<List<BlockedChannelEntity>>

    @Query("SELECT channelId FROM blocked_channels WHERE kidProfileId = :profileId")
    fun getBlockedChannelIdsFlow(profileId: String): Flow<List<String>>

    @Query("SELECT channelId FROM blocked_channels WHERE kidProfileId = :profileId")
    suspend fun getBlockedChannelIds(profileId: String): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_channels WHERE kidProfileId = :profileId AND channelId = :channelId)")
    suspend fun isBlocked(profileId: String, channelId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BlockedChannelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<BlockedChannelEntity>)

    @Query("DELETE FROM blocked_channels WHERE kidProfileId = :profileId AND channelId = :channelId")
    suspend fun unblockChannel(profileId: String, channelId: String)

    @Query("DELETE FROM blocked_channels WHERE kidProfileId = :profileId")
    suspend fun deleteAllByProfile(profileId: String)
}
