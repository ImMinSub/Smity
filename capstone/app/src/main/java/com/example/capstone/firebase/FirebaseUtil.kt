package com.example.capstone.firebase  // Firebase 관련 기능을 담당하는 패키지

import android.content.Context  // 안드로이드 컨텍스트를 사용하기 위한 임포트
import android.widget.Toast  // 토스트 메시지를 표시하기 위한 임포트
import com.google.firebase.auth.FirebaseAuth  // Firebase 인증 기능 사용을 위한 임포트
import com.google.firebase.firestore.FirebaseFirestore  // Firebase Firestore 데이터베이스 사용을 위한 임포트
import com.google.firebase.firestore.ktx.firestore  // KTX 확장 기능을 통한 Firestore 접근
import com.google.firebase.ktx.Firebase  // KTX 확장 기능을 통한 Firebase 접근
import javax.inject.Inject  // 의존성 주입을 위한 임포트

class FirebaseUtil @Inject constructor() {  // Firebase 유틸리티 클래스 (의존성 주입 가능)
    private val firebaseAuth: FirebaseAuth by lazy {  // 지연 초기화된 Firebase 인증 객체
        FirebaseAuth.getInstance()  // Firebase 인증 인스턴스 획득
    }

    private val firestore: FirebaseFirestore by lazy {  // 지연 초기화된 Firestore 데이터베이스 객체
        Firebase.firestore  // KTX를 통한 Firestore 인스턴스 획득
    }

    private val tokenCollection = firestore.collection("tokens")  // 토큰 정보를 저장하는 Firestore 컬렉션

    fun updateToken(token: String) {  // FCM 토큰을 업데이트하는 함수
        val currentUser = firebaseAuth.currentUser ?: return  // 현재 로그인된 사용자 확인 (없으면 함수 종료)

        val map = hashMapOf("token" to token)  // 토큰 정보를 담은 맵 생성
        tokenCollection.document(currentUser.uid).set(map)  // 사용자 UID를 문서 ID로 사용하여 토큰 정보 저장
    }

    fun showToast(context: Context, message: String) {  // 토스트 메시지를 표시하는 유틸리티 함수
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()  // 짧은 길이의 토스트 메시지 표시
    }
}
