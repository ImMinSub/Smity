package com.example.capstone.firebase  // Firebase 관련 기능을 담당하는 패키지

import android.app.Activity  // 안드로이드 액티비티를 사용하기 위한 임포트
import android.net.Uri  // URI 처리를 위한 임포트
import android.util.Log  // 로깅을 위한 임포트
import android.widget.Toast  // 토스트 메시지를 표시하기 위한 임포트
import androidx.lifecycle.MutableLiveData  // 관찰 가능한 데이터를 위한 임포트
import com.example.capstone.data.*  // 애플리케이션 데이터 모델 임포트
import com.example.capstone.db.ServerDao  // 서버 DAO 인터페이스 임포트
import com.example.capstone.db.UserDao  // 사용자 DAO 인터페이스 임포트
import com.example.capstone.ui.auth.LoginFragment  // 로그인 프래그먼트 임포트
import com.example.capstone.ui.auth.RegisterFragment  // 회원가입 프래그먼트 임포트
import com.example.capstone.util.Constants  // 상수 유틸리티 임포트
import com.google.firebase.auth.FirebaseAuth  // Firebase 인증 기능 사용을 위한 임포트
import com.google.firebase.firestore.FirebaseFirestore  // Firebase Firestore 데이터베이스 사용을 위한 임포트
import com.google.firebase.firestore.ktx.firestore  // KTX 확장 기능을 통한 Firestore 접근
import com.google.firebase.ktx.Firebase  // KTX 확장 기능을 통한 Firebase 접근
import com.google.firebase.storage.FirebaseStorage  // Firebase Storage 사용을 위한 임포트
import kotlinx.coroutines.tasks.await  // Firebase 작업을 코루틴으로 처리하기 위한 임포트
import javax.inject.Inject  // 의존성 주입을 위한 임포트
import javax.inject.Singleton  // 싱글톤 스코프 어노테이션 임포트

@Singleton  // 애플리케이션 생명주기 동안 단일 인스턴스로 유지
class FirebaseSource @Inject constructor(  // Firebase 데이터 소스 클래스 (의존성 주입 가능)
    private val userDao: UserDao,  // 로컬 데이터베이스의 사용자 DAO
    private val serverDao: ServerDao  // 로컬 데이터베이스의 서버 DAO
) {
    private val firebaseAuth: FirebaseAuth by lazy {  // 지연 초기화된 Firebase 인증 객체
        try {
            FirebaseAuth.getInstance()  // Firebase 인증 인스턴스 획득
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseAuth 초기화 오류", e)  // 초기화 오류 로깅
            throw e  // 오류 전파
        }
    }

    private val firestore: FirebaseFirestore by lazy {  // 지연 초기화된 Firestore 데이터베이스 객체
        try {
            Firebase.firestore  // KTX를 통한 Firestore 인스턴스 획득
        } catch (e: Exception) {
            Log.e(TAG, "Firestore 초기화 오류", e)  // 초기화 오류 로깅
            throw e  // 오류 전파
        }
    }

    private val storage: FirebaseStorage by lazy {  // 지연 초기화된 Firebase Storage 객체
        try {
            FirebaseStorage.getInstance()  // Firebase Storage 인스턴스 획득
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseStorage 초기화 오류", e)  // 초기화 오류 로깅
            throw e  // 오류 전파
        }
    }

    private val userCollection by lazy { firestore.collection("users") }  // 사용자 정보를 저장하는 Firestore 컬렉션
    private val serverCollection by lazy { firestore.collection("servers") }  // 서버 정보를 저장하는 Firestore 컬렉션
    private val channelCollection by lazy { firestore.collection("channels") }  // 채널 정보를 저장하는 Firestore 컬렉션
    private val messageCollection by lazy { firestore.collection("messages") }  // 메시지 정보를 저장하는 Firestore 컬렉션

    val isLoading = MutableLiveData(false)  // 로딩 상태를 나타내는 관찰 가능한 데이터
    val isLoggedIn = MutableLiveData(false)  // 로그인 상태를 나타내는 관찰 가능한 데이터

    init {  // 클래스 초기화 블록
        // 현재 사용자 상태 확인
        try {
            isLoggedIn.value = firebaseAuth.currentUser != null  // 현재 로그인된 사용자 여부 확인
            Log.d(TAG, "Firebase 초기화 성공. 로그인 상태: ${isLoggedIn.value}")  // 초기화 성공 로깅
        } catch (e: Exception) {
            Log.e(TAG, "Firebase 초기화 과정에서 오류 발생", e)  // 초기화 오류 로깅
        }
    }

    // 인증 관련 함수
    fun registerUser(email: String, password: String, activity: Activity, callback: (Boolean) -> Unit = {}) {  // 사용자 회원가입 함수
        isLoading.value = true  // 로딩 상태 활성화
        try {
            Log.d(TAG, "Firebase 인증 인스턴스: ${firebaseAuth}")  // Firebase 인증 인스턴스 로깅
            Log.d(TAG, "회원가입 시도: $email")  // 회원가입 시도 로깅
            
            firebaseAuth.createUserWithEmailAndPassword(email, password)  // Firebase에 사용자 계정 생성 요청
                .addOnCompleteListener { task ->  // 작업 완료 리스너
                    isLoading.value = false  // 로딩 상태 비활성화
                    if (task.isSuccessful) {  // 작업 성공 시
                        isLoggedIn.value = true  // 로그인 상태 활성화
                        Toast.makeText(activity, "회원가입 성공!", Toast.LENGTH_SHORT).show()  // 성공 메시지 표시
                        callback(true)  // 성공 콜백 호출
                    } else {  // 작업 실패 시
                        val exception = task.exception  // 예외 정보 가져오기
                        val errorMessage = when {  // 오류 메시지 생성
                            exception?.message?.contains("CONFIGURATION") == true -> 
                                "Firebase 설정 오류: Authentication 서비스가 활성화되어 있는지 확인하세요."  // 설정 오류 메시지
                            exception?.message?.contains("email address is already") == true ->
                                "이미 사용 중인 이메일 주소입니다."  // 이메일 중복 오류 메시지
                            exception?.message?.contains("password is invalid") == true ->
                                "비밀번호가 유효하지 않습니다. 6자 이상이어야 합니다."  // 비밀번호 유효성 오류 메시지
                            else -> exception?.message ?: "알 수 없는 오류"  // 기타 오류 메시지
                        }
                        Log.e(TAG, "회원가입 실패: $errorMessage", task.exception)  // 회원가입 실패 로깅
                        Log.e(TAG, "상세 오류: ${task.exception}")  // 상세 오류 로깅
                        Toast.makeText(activity, "회원가입 실패: $errorMessage", Toast.LENGTH_LONG).show()  // 실패 메시지 표시
                        callback(false)  // 실패 콜백 호출
                    }
                }
        } catch (e: Exception) {  // 예외 발생 시
            isLoading.value = false  // 로딩 상태 비활성화
            Log.e(TAG, "registerUser 호출 중 예외 발생", e)  // 예외 로깅
            Log.e(TAG, "상세 오류 정보: ${e.stackTraceToString()}")  // 상세 스택트레이스 로깅
            Toast.makeText(activity, "오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()  // 오류 메시지 표시
            callback(false)  // 실패 콜백 호출
        }
    }

    fun loginUser(email: String, password: String, activity: Activity, callback: (Boolean) -> Unit = {}) {  // 사용자 로그인 함수
        isLoading.value = true  // 로딩 상태 활성화
        try {
            firebaseAuth.signInWithEmailAndPassword(email, password)  // Firebase 로그인 요청
                .addOnCompleteListener { task ->  // 작업 완료 리스너
                    isLoading.value = false  // 로딩 상태 비활성화
                    if (task.isSuccessful) {  // 작업 성공 시
                        isLoggedIn.value = true  // 로그인 상태 활성화
                        Toast.makeText(activity, "로그인 성공!", Toast.LENGTH_SHORT).show()  // 성공 메시지 표시
                        callback(true)  // 성공 콜백 호출
                    } else {  // 작업 실패 시
                        val errorMessage = task.exception?.message ?: "알 수 없는 오류"  // 오류 메시지 가져오기
                        Log.e(TAG, "로그인 실패: $errorMessage", task.exception)  // 로그인 실패 로깅
                        Toast.makeText(activity, "로그인 실패: $errorMessage", Toast.LENGTH_SHORT).show()  // 실패 메시지 표시
                        callback(false)  // 실패 콜백 호출
                    }
                }
        } catch (e: Exception) {  // 예외 발생 시
            isLoading.value = false  // 로딩 상태 비활성화
            Log.e(TAG, "loginUser 호출 중 예외 발생", e)  // 예외 로깅
            Toast.makeText(activity, "오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()  // 오류 메시지 표시
            callback(false)  // 실패 콜백 호출
        }
    }

    fun logoutUser() {  // 사용자 로그아웃 함수
        try {
            firebaseAuth.signOut()  // Firebase 로그아웃 요청
            isLoggedIn.value = false  // 로그인 상태 비활성화
            Log.d(TAG, "로그아웃 성공")  // 로그아웃 성공 로깅
        } catch (e: Exception) {  // 예외 발생 시
            Log.e(TAG, "로그아웃 중 오류 발생", e)  // 로그아웃 오류 로깅
        }
    }

    // 사용자 관련 함수
    suspend fun createUserProfile(user: User) {  // 사용자 프로필 생성 함수
        try {
            userCollection.document(user.id).set(user).await()  // Firestore에 사용자 문서 생성
            Log.d(TAG, "사용자 프로필 생성 성공: ${user.id}")  // 성공 로깅
        } catch (e: Exception) {  // 예외 발생 시
            Log.e(TAG, "사용자 프로필 생성 중 오류", e)  // 오류 로깅
            throw e  // 오류 전파
        }
    }

    suspend fun getUserById(userId: String): User? {  // 사용자 ID로 사용자 정보 조회 함수
        return try {
            val result = userCollection.document(userId).get().await()  // Firestore에서 사용자 문서 가져오기
            val user = result.toObject(User::class.java)  // 문서를 User 객체로 변환
            Log.d(TAG, "사용자 조회 결과: $user")  // 조회 결과 로깅
            user  // 사용자 객체 반환
        } catch (e: Exception) {  // 예외 발생 시
            Log.e(TAG, "사용자 조회 중 오류: $userId", e)  // 오류 로깅
            null  // null 반환
        }
    }

    suspend fun updateUserProfile(user: User) {  // 사용자 프로필 업데이트 함수
        try {
            userCollection.document(user.id).set(user).await()  // Firestore에 사용자 문서 업데이트
            Log.d(TAG, "사용자 프로필 업데이트 성공: ${user.id}")  // 성공 로깅
        } catch (e: Exception) {  // 예외 발생 시
            Log.e(TAG, "사용자 프로필 업데이트 중 오류", e)  // 오류 로깅
            throw e  // 오류 전파
        }
    }

    // 서버 관련 함수
    suspend fun createServer(server: Server) {  // 서버 생성 함수
        try {
            serverCollection.document(server.serverId).set(server).await()  // Firestore에 서버 문서 생성
            Log.d(TAG, "서버 생성 성공: ${server.serverId}")  // 성공 로깅
        } catch (e: Exception) {  // 예외 발생 시
            Log.e(TAG, "서버 생성 중 오류", e)  // 오류 로깅
            throw e  // 오류 전파
        }
    }

    suspend fun getServerById(serverId: String): Server? {  // 서버 ID로 서버 정보 조회 함수
        return try {
            val result = serverCollection.document(serverId).get().await()  // Firestore에서 서버 문서 가져오기
            val server = result.toObject(Server::class.java)  // 문서를 Server 객체로 변환
            Log.d(TAG, "서버 조회 결과: $server")  // 조회 결과 로깅
            server  // 서버 객체 반환
        } catch (e: Exception) {  // 예외 발생 시
            Log.e(TAG, "서버 조회 중 오류: $serverId", e)  // 오류 로깅
            null  // null 반환
        }
    }

    // 채널 관련 함수
    suspend fun createChannel(channel: Channel) {  // 채널 생성 함수
        try {
            channelCollection.document(channel.channelId).set(channel).await()  // Firestore에 채널 문서 생성
            Log.d(TAG, "채널 생성 성공: ${channel.channelId}")  // 성공 로깅

            // 서버에 채널 추가
            val server = getServerById(channel.serverId)  // 채널이 속한 서버 정보 조회
            server?.let {  // 서버 정보가 있으면
                it.channels.add(channel.channelId)  // 서버의 채널 목록에 채널 ID 추가
                if (channel.channelType == "text") {  // 텍스트 채널인 경우
                    it.textChannels.add(channel.channelId)  // 텍스트 채널 목록에 추가
                } else {  // 음성 채널인 경우
                    it.voiceChannels.add(channel.channelId)  // 음성 채널 목록에 추가
                }
                serverCollection.document(channel.serverId).set(it).await()  // 서버 정보 업데이트
                Log.d(TAG, "서버에 채널 추가 성공: ${channel.serverId}")  // 성공 로깅
            }
        } catch (e: Exception) {  // 예외 발생 시
            Log.e(TAG, "채널 생성 중 오류", e)  // 오류 로깅
            throw e  // 오류 전파
        }
    }

    // 메시지 관련 함수
    suspend fun sendMessage(channelId: String, message: Message) {  // 메시지 전송 함수
        try {
            messageCollection.document(message.messageId).set(message).await()  // Firestore에 메시지 문서 생성
            Log.d(TAG, "메시지 전송 성공: ${message.messageId}")  // 성공 로깅

            // 채널에 메시지 추가
            channelCollection.document(channelId).update(  // 채널 문서 업데이트
                "messages", com.google.firebase.firestore.FieldValue.arrayUnion(message.messageId)  // 메시지 배열에 메시지 ID 추가
            ).await()
            Log.d(TAG, "채널에 메시지 추가 성공: $channelId")  // 성공 로깅
        } catch (e: Exception) {  // 예외 발생 시
            Log.e(TAG, "메시지 전송 중 오류", e)  // 오류 로깅
            throw e  // 오류 전파
        }
    }

    // 이미지 업로드 함수
    suspend fun uploadImage(imageUri: Uri, path: String): String {  // 이미지 업로드 함수
        try {
            val ref = storage.reference.child(path)  // 스토리지 경로에 대한 참조 생성
            ref.putFile(imageUri).await()  // 파일 업로드
            val url = ref.downloadUrl.await().toString()  // 다운로드 URL 가져오기
            Log.d(TAG, "이미지 업로드 성공: $url")  // 성공 로깅
            return url  // 이미지 URL 반환
        } catch (e: Exception) {  // 예외 발생 시
            Log.e(TAG, "이미지 업로드 중 오류", e)  // 오류 로깅
            throw e  // 오류 전파
        }
    }

    fun getCurrentUser() = firebaseAuth.currentUser //
    
    companion object { 
        private const val TAG = "FirebaseSource"
    }
}
