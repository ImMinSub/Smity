package com.example.capstone.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * 사용자 정보를 나타내는 데이터 클래스
 * Room DB에 저장되며 Parcelable로 구현되어 화면 간 전달 가능
 * 사용자 프로필, 상태 및 관계 정보 포함
 */
@Entity(tableName = "user_table")      // Room 데이터베이스 엔티티로 지정
@Parcelize                             // 안드로이드 컴포넌트 간 전달 가능하도록 Parcelable 구현
data class User(
    @PrimaryKey
    var id: String = "",               // 사용자 고유 식별자 (기본 키)
    var username: String = "",         // 사용자 이름
    var email: String = "",            // 사용자 이메일
    var profileImageUrl: String = "",  // 프로필 이미지 URL
    var status: String = "온라인",       // 사용자 상태 메시지
    var age: Int? = null,              // 사용자 나이
    var mbti: String = "",             // 사용자 MBTI
    @Ignore                            // Room DB에 저장하지 않음
    var isOnline: Boolean = false,     // 온라인 상태 여부
    @Ignore                            // Room DB에 저장하지 않음
    var groups: MutableList<String> = ArrayList(),  // 사용자가 속한 그룹 ID 목록
    @Ignore                            // Room DB에 저장하지 않음
    val friends: MutableList<String> = ArrayList()   // 사용자의 친구 ID 목록
): Parcelable
