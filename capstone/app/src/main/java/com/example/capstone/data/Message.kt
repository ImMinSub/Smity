package com.example.capstone.data

/**
 * 채널 내의 개별 메시지를 나타내는 데이터 클래스
 * 메시지 내용, 발신자 정보 및 첨부 이미지 정보를 포함
 */
data class Message(
    val senderId: String = "",          // 메시지 발신자의 사용자 ID
    val message: String = "",           // 메시지 텍스트 내용
    var messageId: String = "",         // 메시지의 고유 식별자
    val imageUrl: String = "",          // 메시지에 첨부된 이미지의 URL (있는 경우)
    val senderUsername: String = "",    // 발신자 사용자 이름
    val senderProfileUrl: String = "",  // 발신자 프로필 이미지 URL
    val sentAt: Long = 0L,              // 메시지 전송 시간(타임스탬프)
    var edited: Boolean = false         // 메시지가 편집되었는지 여부
)