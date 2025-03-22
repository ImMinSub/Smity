package com.example.capstone.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.capstone.data.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM user_table WHERE id = :userId")
    fun getUser(userId: String): Flow<User>

    @Query("UPDATE user_table SET profileImageUrl = :profileUrl WHERE id = :userId")
    fun updateProfileImage(userId: String, profileUrl: String)

    @Query("UPDATE user_table SET status = :status WHERE id = :userId")
    fun updateStatus(userId: String, status: String)
}