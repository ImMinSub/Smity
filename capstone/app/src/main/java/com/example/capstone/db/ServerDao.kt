package com.example.capstone.db

import androidx.room.*
import com.example.capstone.data.UserPreferences
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(preferences: UserPreferences)

    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    fun getUserPreferences(userId: String): Flow<UserPreferences>

    @Query("UPDATE user_preferences SET currentServerId = :serverId WHERE userId = :userId")
    suspend fun updateCurrentServer(userId: String, serverId: String)

    @Query("UPDATE user_preferences SET currentChannelId = :channelId WHERE userId = :userId")
    suspend fun updateCurrentChannel(userId: String, channelId: String)
}