package io.github.degipe.youtubewhitelist.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.degipe.youtubewhitelist.core.database.entity.BlockedChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedChannelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BlockedChannelEntity)

    @Query("DELETE FROM blocked_channels WHERE channelId = :channelId")
    suspend fun delete(channelId: String)

    @Query("SELECT * FROM blocked_channels ORDER BY addedAt DESC")
    fun getAll(): Flow<List<BlockedChannelEntity>>

    @Query("SELECT channelId FROM blocked_channels")
    fun getAllIds(): Flow<List<String>>
}
