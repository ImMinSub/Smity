package com.example.capstone.db

import androidx.room.*
import com.example.capstone.data.Group
import com.example.capstone.data.UserPreferences
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {  // 그룹 데이터 접근을 위한 DAO 인터페이스
    @Insert(onConflict = OnConflictStrategy.REPLACE)  // 사용자 설정 정보 삽입 (중복 시 교체)
    suspend fun insertPreferences(preferences: UserPreferences): Long

    @Query("SELECT * FROM user_preferences WHERE userId = :userId")  // 특정 사용자의 설정 정보 조회하는 쿼리
    fun getUserPreferences(userId: String): Flow<UserPreferences>

    @Query("UPDATE user_preferences SET currentGroupId = :groupId WHERE userId = :userId")  // 현재 그룹 ID 업데이트 쿼리
    suspend fun updateCurrentGroup(userId: String, groupId: String): Int

    @Query("UPDATE user_preferences SET currentChannelId = :channelId WHERE userId = :userId")  // 현재 채널 ID 업데이트 쿼리
    suspend fun updateCurrentChannel(userId: String, channelId: String): Int

    @Query("SELECT * FROM group_table")  // 모든 그룹 정보를 조회하는 쿼리
    fun getAllGroups(): Flow<List<Group>>

    @Query("SELECT * FROM group_table WHERE groupId = :groupId")  // 특정 그룹 정보를 조회하는 쿼리
    suspend fun getGroupById(groupId: String): Group?

    @Insert(onConflict = OnConflictStrategy.REPLACE)  // 그룹 정보 삽입 (중복 시 교체)
    suspend fun insertGroup(group: Group): Long

    @Update  // 그룹 정보 업데이트
    suspend fun updateGroup(group: Group): Int

    @Delete  // 그룹 정보 삭제
    suspend fun deleteGroup(group: Group): Int
} 
