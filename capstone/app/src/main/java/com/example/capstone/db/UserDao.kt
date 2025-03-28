package com.example.capstone.db

import androidx.room.*
import com.example.capstone.data.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_table")  // 모든 사용자 정보를 조회하는 쿼리
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM user_table WHERE id = :userId")  // 특정 사용자 정보를 조회하는 쿼리
    suspend fun getUserById(userId: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)  // 사용자 정보 삽입 (중복 시 교체)
    suspend fun insertUser(user: User): Long

    @Update  // 사용자 정보 업데이트
    suspend fun updateUser(user: User): Int

    @Delete  // 사용자 정보 삭제
    suspend fun deleteUser(user: User): Int

    @Query("SELECT * FROM user_table WHERE id = :userId")
    fun getUser(userId: String): Flow<User>

    @Query("UPDATE user_table SET profileImageUrl = :profileUrl WHERE id = :userId")
    suspend fun updateProfileImage(userId: String, profileUrl: String): Int

    @Query("UPDATE user_table SET status = :status WHERE id = :userId")
    suspend fun updateStatus(userId: String, status: String): Int
}
