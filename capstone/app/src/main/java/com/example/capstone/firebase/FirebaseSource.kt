package com.example.capstone.firebase  // Firebase 관련 기능을 담당하는 패키지

import android.app.Activity  // 안드로이드 액티비티를 사용하기 위한 임포트
import android.net.Uri  // URI 처리를 위한 임포트
import android.util.Log  // 로깅을 위한 임포트
import android.widget.Toast  // 토스트 메시지를 표시하기 위한 임포트
import androidx.lifecycle.MutableLiveData  // 관찰 가능한 데이터를 위한 임포트
import com.example.capstone.data.*  // 애플리케이션 데이터 모델 임포트
import com.example.capstone.db.UserDao  // 사용자 DAO 인터페이스 임포트
import com.example.capstone.db.GroupDao  // 그룹 DAO 인터페이스 임포트
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
    private val groupDao: GroupDao  // 로컬 데이터베이스의 그룹 DAO
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
    private val groupCollection by lazy { firestore.collection("groups") }  // 그룹 정보를 저장하는 Firestore 컬렉션
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
            Log.d(TAG, "updateUserProfile 호출됨: userId=${user.id}, username=${user.username}")
            
            // 문서가 이미 존재하는지 확인
            val documentRef = userCollection.document(user.id)
            val existingDoc = documentRef.get().await()
            
            if (existingDoc.exists()) {
                Log.d(TAG, "기존 사용자 문서 발견: ${user.id}")
                // 필드별 업데이트 (null이 아닌 필드만)
                val updates = mutableMapOf<String, Any>()
                
                // 필수 필드 추가
                updates["id"] = user.id
                updates["username"] = user.username
                
                // 선택적 필드 추가 (null이 아닌 경우)
                if (user.email.isNotEmpty()) updates["email"] = user.email
                if (user.profileImageUrl.isNotEmpty()) updates["profileImageUrl"] = user.profileImageUrl
                if (user.status.isNotEmpty()) updates["status"] = user.status
                if (user.age != null) updates["age"] = user.age!!
                if (user.mbti.isNotEmpty()) updates["mbti"] = user.mbti
                updates["groups"] = user.groups
                updates["friends"] = user.friends
                
                // 문서 업데이트
                documentRef.update(updates).await()
                Log.d(TAG, "사용자 프로필 업데이트 성공: ${user.id}")
            } else {
                Log.d(TAG, "사용자 문서가 존재하지 않음, 새로 생성: ${user.id}")
                // 새 문서 생성
                documentRef.set(user).await()
                Log.d(TAG, "새 사용자 프로필 생성 성공: ${user.id}")
            }
        } catch (e: Exception) {  // 예외 발생 시
            Log.e(TAG, "사용자 프로필 업데이트 중 오류 발생: ${e.message}", e)  // 오류 로깅
            throw e  // 오류 전파
        }
    }

    // 그룹 관련 함수
    suspend fun createGroup(group: Group) {  // 그룹 생성 함수
        try {
            Log.d(TAG, "createGroup 호출됨: groupId=${group.groupId}, name=${group.groupName}")
            Log.d(TAG, "그룹 데이터: $group")
            
            // 현재 인증된 사용자 확인
            val currentUser = getCurrentUser()
            if (currentUser == null) {
                Log.e(TAG, "그룹 생성 실패: 인증된 사용자 없음")
                throw Exception("인증된 사용자 없음. 로그인이 필요합니다.")
            }
            
            Log.d(TAG, "인증된 사용자 확인: ${currentUser.uid}")
            
            // 토큰 갱신 시도
            try {
                currentUser.getIdToken(true).await()
                Log.d(TAG, "사용자 토큰 리프레시 성공")
            } catch (e: Exception) {
                Log.e(TAG, "사용자 토큰 리프레시 실패", e)
                throw Exception("인증 토큰 갱신 실패: ${e.message}")
            }
            
            // Firestore 트랜잭션 사용하여 원자적 쓰기 작업
            try {
                // 직접 데이터 쓰기 시도 (트랜잭션 사용하지 않음)
                Log.d(TAG, "그룹 문서 직접 저장 시도: ${group.groupId}")
                groupCollection.document(group.groupId).set(group).await()
                Log.d(TAG, "그룹 문서 저장 성공: ${group.groupId}")
            } catch (e: Exception) {
                val errorMsg = e.message ?: "알 수 없는 오류"
                Log.e(TAG, "Firestore 데이터 저장 실패: $errorMsg", e)
                
                // 자세한 에러 정보 로깅
                Log.e(TAG, "자세한 오류 정보: ${e.stackTraceToString()}")
                
                // 오류 종류에 따른 구체적인 메시지
                when {
                    errorMsg.contains("permission_denied") || errorMsg.contains("PERMISSION_DENIED") -> {
                        Log.e(TAG, "Firebase 권한 오류 감지: 보안 규칙을 확인하세요")
                        throw Exception("데이터베이스 접근 권한이 없습니다. Firebase 보안 규칙을 확인하세요.")
                    }
                    errorMsg.contains("network") || errorMsg.contains("UNAVAILABLE") -> {
                        Log.e(TAG, "네트워크 오류 감지")
                        throw Exception("네트워크 연결 오류: ${e.message}")
                    }
                    errorMsg.contains("unauthenticated") || errorMsg.contains("UNAUTHENTICATED") -> {
                        Log.e(TAG, "인증 오류 감지: 사용자가 로그인되어 있지 않거나 토큰이 만료됨")
                        throw Exception("인증 오류: 다시 로그인이 필요합니다.")
                    }
                    else -> {
                        throw Exception("그룹 생성 실패: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "그룹 생성 최종 실패: ${e.message}", e)
            throw e
        }
    }

    suspend fun getGroupById(groupId: String): Group? {  // 그룹 ID로 그룹 정보 조회 함수
        return try {
            val result = groupCollection.document(groupId).get().await()  // Firestore에서 그룹 문서 가져오기
            val group = result.toObject(Group::class.java)  // 문서를 Group 객체로 변환
            Log.d(TAG, "그룹 조회 결과: $group")  // 조회 결과 로깅
            group  // 그룹 객체 반환
        } catch (e: Exception) {  // 예외 발생 시
            Log.e(TAG, "그룹 조회 중 오류: $groupId", e)  // 오류 로깅
            null  // null 반환
        }
    }

    // 동일한 이름의 그룹에 대해 태그 생성 함수 추가
    suspend fun generateGroupTag(groupName: String): String {
        try {
            val querySnapshot = groupCollection
                .whereEqualTo("groupName", groupName)
                .get()
                .await()
            
            val count = querySnapshot.size()
            return "#${String.format("%03d", count + 1)}"
        } catch (e: Exception) {
            Log.e(TAG, "그룹 태그 생성 중 오류", e)
            return "#001"  // 오류 발생 시 기본 태그 반환
        }
    }

    // 채널 관련 함수
    suspend fun createChannel(channel: Channel) {  // 채널 생성 함수
        try {
            Log.d(TAG, "createChannel 호출됨: channelId=${channel.channelId}, name=${channel.channelName}")
            Log.d(TAG, "채널 데이터: $channel")
            
            // 인증 상태 확인
            val currentUser = getCurrentUser()
            if (currentUser == null) {
                Log.e(TAG, "채널 생성 실패: 인증된 사용자 없음")
                throw Exception("인증된 사용자 없음. 로그인이 필요합니다.")
            }
            
            // 트랜잭션 시작
            try {
                // 먼저 채널 문서 저장
                Log.d(TAG, "채널 문서 저장 시작: ${channel.channelId}")
                channelCollection.document(channel.channelId).set(channel).await()
                Log.d(TAG, "채널 문서 저장 완료: ${channel.channelId}")
                
                // 그룹 정보 가져오기
                Log.d(TAG, "그룹 정보 조회 시작: ${channel.groupId}")
                val groupDocRef = groupCollection.document(channel.groupId)
                val groupSnapshot = groupDocRef.get().await()
                
                if (!groupSnapshot.exists()) {
                    Log.e(TAG, "채널 추가 실패: 그룹이 존재하지 않음 - ${channel.groupId}")
                    throw Exception("그룹이 존재하지 않습니다")
                }
                
                val group = groupSnapshot.toObject(Group::class.java)
                if (group == null) {
                    Log.e(TAG, "채널 추가 실패: 그룹 데이터 변환 실패 - ${channel.groupId}")
                    throw Exception("그룹 데이터를 읽을 수 없습니다")
                }
                
                Log.d(TAG, "그룹 정보 조회 완료: ${group.groupName}")
                
                // 중복 추가 확인
                var updated = false
                if (!group.channels.contains(channel.channelId)) {
                    group.channels.add(channel.channelId)  // 그룹의 채널 목록에 채널 ID 추가
                    updated = true
                    
                    if (channel.channelType == "text") {  // 텍스트 채널인 경우
                        if (!group.textChannels.contains(channel.channelId)) {
                            group.textChannels.add(channel.channelId)  // 텍스트 채널 목록에 추가
                        }
                    } else {  // 음성 채널인 경우
                        if (!group.voiceChannels.contains(channel.channelId)) {
                            group.voiceChannels.add(channel.channelId)  // 음성 채널 목록에 추가
                        }
                    }
                } else {
                    Log.d(TAG, "채널이 이미 그룹에 존재함: ${channel.channelId}")
                }
                
                // 그룹 정보가 업데이트된 경우에만 저장
                if (updated) {
                    Log.d(TAG, "그룹 정보 업데이트 시작: ${channel.groupId}")
                    groupDocRef.set(group).await()
                    Log.d(TAG, "그룹 정보 업데이트 완료: ${channel.groupId}")
                }
                
                Log.d(TAG, "채널 생성 및 그룹 연결 완료: ${channel.channelId}")
            } catch (e: Exception) {
                val errorMsg = e.message ?: "알 수 없는 오류"
                Log.e(TAG, "채널 생성 처리 중 오류: $errorMsg", e)
                
                // 오류 종류 확인
                if (errorMsg.contains("permission") || errorMsg.contains("denied")) {
                    throw Exception("데이터베이스 접근 권한이 없습니다: ${e.message}")
                } else if (errorMsg.contains("network")) {
                    throw Exception("네트워크 연결 오류: ${e.message}")
                } else {
                    throw Exception("채널 생성 실패: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "채널 생성 최종 실패: ${e.message}", e)
            throw e
        }
    }

    // 메시지 관련 함수
    suspend fun getMessages(channelId: String): List<Message> {
        try {
            Log.d(TAG, "getMessages 호출됨: channelId=$channelId")
            
            // channelId 유효성 검사
            if (channelId.isBlank()) {
                Log.e(TAG, "getMessages 실패: 채널 ID가 비어있음")
                return emptyList()
            }
            
            // 메시지 컬렉션 참조
            val messagesRef = firestore.collection("channels")
                .document(channelId)
                .collection("messages")
                .orderBy("sentAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
            
            // 메시지 가져오기
            val messagesSnapshot = messagesRef.get().await()
            val messages = mutableListOf<Message>()
            
            for (doc in messagesSnapshot.documents) {
                val message = doc.toObject(Message::class.java)
                if (message != null) {
                    messages.add(message)
                }
            }
            
            Log.d(TAG, "메시지 로드 완료: ${messages.size}개")
            return messages
        } catch (e: Exception) {
            Log.e(TAG, "메시지 로드 실패: ${e.message}", e)
            throw e
        }
    }

    suspend fun sendMessage(channelId: String, message: Message) {
        try {
            Log.d(TAG, "sendMessage 호출됨: channelId=$channelId, messageId=${message.messageId}")
            
            // 현재 인증된 사용자 확인
            val currentUser = getCurrentUser()
            if (currentUser == null) {
                Log.e(TAG, "메시지 전송 실패: 인증된 사용자 없음")
                throw Exception("인증된 사용자 없음. 로그인이 필요합니다.")
            }
            
            // 메시지 검증
            if (message.messageId.isBlank()) {
                message.messageId = "message_${System.currentTimeMillis()}_${currentUser.uid.take(5)}"
                Log.d(TAG, "메시지 ID 자동 생성: ${message.messageId}")
            }
            
            // 채널 문서 참조
            val channelRef = firestore.collection("channels").document(channelId)
            
            // 메시지 문서 참조
            val messageRef = channelRef.collection("messages").document(message.messageId)
            
            // 메시지 저장
            messageRef.set(message).await()
            Log.d(TAG, "메시지 저장 성공: ${message.messageId}")
            
            // 채널 문서 업데이트 (최근 메시지 및 메시지 목록)
            firestore.runTransaction { transaction ->
                val channelDoc = transaction.get(channelRef)
                val channel = channelDoc.toObject(Channel::class.java) ?: throw Exception("채널 정보가 없습니다")
                
                // 메시지 ID 추가 (중복 방지)
                if (!channel.messages.contains(message.messageId)) {
                    channel.messages.add(message.messageId)
                }
                
                // 채널 문서 업데이트
                transaction.set(channelRef, channel)
                
                null
            }.await()
            
            Log.d(TAG, "채널 정보 업데이트 완료")
        } catch (e: Exception) {
            Log.e(TAG, "메시지 전송 실패: ${e.message}", e)
            throw e
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
    
    // FirebaseSource 클래스에 getFirestore 메서드 추가
    fun getFirestoreInstance(): FirebaseFirestore {
        return firestore
    }

    // 문서 ID 생성 도우미 함수 추가
    fun generateDocumentId(collectionPath: String): String {
        return firestore.collection(collectionPath).document().id
    }

    companion object { 
        private const val TAG = "FirebaseSource"
    }
}
