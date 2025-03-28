package com.example.capstone.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import androidx.room.Ignore

/**
 * 채팅 그룹 정보를 나타내는 데이터 클래스
 * Room DB에 저장되며 Parcelable로 구현되어 화면 간 전달 가능
 * 그룹에 포함된 채널 목록과 구성원 정보를 관리
 */
@Entity(tableName = "group_table")      // Room 데이터베이스 엔티티로 지정
@Parcelize                              // 안드로이드 컴포넌트 간 전달 가능하도록 Parcelable 구현
data class Group(
    @PrimaryKey
    var groupId: String = "",           // 그룹의 고유 식별자 (기본 키)
    var groupName: String = "",         // 그룹 이름
    var groupIcon: String = "",         // 그룹 아이콘 이미지 URL
    var groupTag: String = "",          // 그룹 태그 (동일 이름 구분용: #001, #002 등)
    var channels: MutableList<String> = ArrayList(),       // 모든 채널 ID 목록
    var textChannels: MutableList<String> = ArrayList(),   // 텍스트 채널 ID 목록
    var voiceChannels: MutableList<String> = ArrayList(),  // 음성 채널 ID 목록
    var members: MutableList<String> = ArrayList(),        // 그룹 멤버 사용자 ID 목록
    var ownerId: String = ""            // 그룹 소유자 ID
): Parcelable {
    // 기본 생성자 (Room ORM 사용을 위해)
    @Ignore
    constructor() : this(
        "", "", "", "", 
        ArrayList(), ArrayList(), ArrayList(), ArrayList(), ""
    )
    
    // 채널 추가 메서드
    fun addChannel(channelId: String, channelType: String) {
        if (!channels.contains(channelId)) {
            channels.add(channelId)
            // 채널 타입에 따라 분류
            if (channelType.equals("text", ignoreCase = true)) {
                if (!textChannels.contains(channelId)) {
                    textChannels.add(channelId)
                }
            } else if (channelType.equals("voice", ignoreCase = true)) {
                if (!voiceChannels.contains(channelId)) {
                    voiceChannels.add(channelId)
                }
            }
        }
    }
    
    // 멤버 추가 메서드
    fun addMember(userId: String) {
        if (!members.contains(userId)) {
            members.add(userId)
        }
    }
    
    // 기본 생성용 컴패니언 객체
    companion object {
        fun createDefault(ownerId: String, groupName: String): Group {
            val groupId = "group_${System.currentTimeMillis()}"
            val group = Group(
                groupId = groupId,
                groupName = groupName,
                groupIcon = "https://via.placeholder.com/150",
                groupTag = "#${(1000..9999).random()}",
                ownerId = ownerId
            )
            // 소유자를 멤버로 자동 추가
            group.addMember(ownerId)
            return group
        }
    }
}
