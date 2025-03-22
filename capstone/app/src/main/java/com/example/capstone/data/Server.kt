package com.example.capstone.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * 채팅 서버 정보를 나타내는 데이터 클래스
 * Room DB에 저장되며 Parcelable로 구현되어 화면 간 전달 가능
 * 서버에 포함된 채널 목록과 구성원 정보를 관리
 */
@Entity(tableName = "server_table")     // Room 데이터베이스 엔티티로 지정
@Parcelize                              // 안드로이드 컴포넌트 간 전달 가능하도록 Parcelable 구현
data class Server(
    @PrimaryKey
    var serverId: String = "",          // 서버의 고유 식별자 (기본 키)
    val serverName: String = "",        // 서버 이름
    val serverIcon: String = "",        // 서버 아이콘 이미지 URL
    val channels: MutableList<String> = ArrayList(),       // 모든 채널 ID 목록
    val textChannels: MutableList<String> = ArrayList(),   // 텍스트 채널 ID 목록
    val voiceChannels: MutableList<String> = ArrayList(),  // 음성 채널 ID 목록
    val members: MutableList<String> = ArrayList(),        // 서버 멤버 사용자 ID 목록
    val ownerId: String = ""            // 서버 소유자 ID
): Parcelable