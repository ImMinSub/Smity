package com.example.capstone.data

/**
 * 그룹 내의 채널 정보를 나타내는 데이터 클래스
 * 텍스트 또는 음성 채널 유형을 포함하며 메시지 목록을 저장함
 */
data class Channel(
    val channelId: String = "",         // 채널의 고유 식별자
    val channelName: String = "",       // 채널 이름
    val channelType: String = "text",   // 채널 유형: "text" 또는 "voice"
    val createdAt: Long = 0L,           // 채널 생성 시간(타임스탬프)
    val messages: MutableList<String> = ArrayList(),  // 채널에 포함된 메시지 ID 목록
    val groupId: String = ""           // 이 채널이 속한 그룹의 ID
) {
    // 클래스 내부에 정적 default() 함수 추가
    companion object {
        // 기본 채널 인스턴스를 생성하는 함수
        fun default(groupId: String = "", channelName: String = "기본 채널"): Channel {
            return Channel(
                channelId = "default_${System.currentTimeMillis()}",
                channelName = channelName,
                channelType = "text",
                createdAt = System.currentTimeMillis(),
                messages = ArrayList(),
                groupId = groupId
            )
        }
    }
}
