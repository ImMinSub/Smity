package com.example.capstone.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.capstone.data.Group
import com.example.capstone.data.User
import com.example.capstone.data.UserPreferences

@Database(
    entities = [User::class, UserPreferences::class, Group::class],  // 데이터베이스에 포함될 엔티티 클래스들
    version = 1,                             // 데이터베이스 버전
    exportSchema = false                     // 스키마 내보내기 비활성화
)
@TypeConverters(Converters::class)           // 커스텀 타입 변환기 적용
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao          // 사용자 데이터 접근을 위한 DAO 인터페이스
    abstract fun groupDao(): GroupDao      // 그룹 데이터 접근을 위한 DAO 인터페이스
}
