package com.example.capstone.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 사용자 앱 설정 정보를 나타내는 데이터 클래스
 * Room DB에 저장되어 앱 재시작 시에도 사용자의 마지막 상태 유지
 */
@Entity(tableName = "user_preferences")  // Room 데이터베이스 엔티티로 지정
data class UserPreferences(
    @PrimaryKey
    var userId: String = "",            // 사용자 고유 식별자 (기본 키)
    var currentServerId: String = "",   // 사용자가 마지막으로 접속한 서버 ID
    var currentChannelId: String = ""   // 사용자가 마지막으로 접속한 채널 ID
)