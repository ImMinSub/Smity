package com.example.capstone.firebase

import android.app.Activity
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.example.capstone.data.*
import com.example.capstone.db.ServerDao
import com.example.capstone.db.UserDao
import com.example.capstone.ui.auth.LoginFragment
import com.example.capstone.ui.auth.RegisterFragment
import com.example.capstone.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSource @Inject constructor(
    private val userDao: UserDao,
    private val serverDao: ServerDao
) {
    private val firebaseAuth: FirebaseAuth by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseAuth 초기화 오류", e)
            throw e
        }
    }

    private val firestore: FirebaseFirestore by lazy {
        try {
            Firebase.firestore
        } catch (e: Exception) {
            Log.e(TAG, "Firestore 초기화 오류", e)
            throw e
        }
    }

    private val storage: FirebaseStorage by lazy {
        try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseStorage 초기화 오류", e)
            throw e
        }
    }

    private val userCollection by lazy { firestore.collection("users") }
    private val serverCollection by lazy { firestore.collection("servers") }
    private val channelCollection by lazy { firestore.collection("channels") }
    private val messageCollection by lazy { firestore.collection("messages") }

    val isLoading = MutableLiveData(false)
    val isLoggedIn = MutableLiveData(false)

    init {
        // 현재 사용자 상태 확인
        try {
            isLoggedIn.value = firebaseAuth.currentUser != null
            Log.d(TAG, "Firebase 초기화 성공. 로그인 상태: ${isLoggedIn.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase 초기화 과정에서 오류 발생", e)
        }
    }

    // 인증 관련 함수
    fun registerUser(email: String, password: String, activity: Activity, callback: (Boolean) -> Unit = {}) {
        isLoading.value = true
        try {
            Log.d(TAG, "Firebase 인증 인스턴스: ${firebaseAuth}")
            Log.d(TAG, "회원가입 시도: $email")
            
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    isLoading.value = false
                    if (task.isSuccessful) {
                        isLoggedIn.value = true
                        Toast.makeText(activity, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                        callback(true)
                    } else {
                        val exception = task.exception
                        val errorMessage = when {
                            exception?.message?.contains("CONFIGURATION") == true -> 
                                "Firebase 설정 오류: Authentication 서비스가 활성화되어 있는지 확인하세요."
                            exception?.message?.contains("email address is already") == true ->
                                "이미 사용 중인 이메일 주소입니다."
                            exception?.message?.contains("password is invalid") == true ->
                                "비밀번호가 유효하지 않습니다. 6자 이상이어야 합니다."
                            else -> exception?.message ?: "알 수 없는 오류"
                        }
                        Log.e(TAG, "회원가입 실패: $errorMessage", task.exception)
                        Log.e(TAG, "상세 오류: ${task.exception}")
                        Toast.makeText(activity, "회원가입 실패: $errorMessage", Toast.LENGTH_LONG).show()
                        callback(false)
                    }
                }
        } catch (e: Exception) {
            isLoading.value = false
            Log.e(TAG, "registerUser 호출 중 예외 발생", e)
            Log.e(TAG, "상세 오류 정보: ${e.stackTraceToString()}")
            Toast.makeText(activity, "오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
            callback(false)
        }
    }

    fun loginUser(email: String, password: String, activity: Activity, callback: (Boolean) -> Unit = {}) {
        isLoading.value = true
        try {
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    isLoading.value = false
                    if (task.isSuccessful) {
                        isLoggedIn.value = true
                        Toast.makeText(activity, "로그인 성공!", Toast.LENGTH_SHORT).show()
                        callback(true)
                    } else {
                        val errorMessage = task.exception?.message ?: "알 수 없는 오류"
                        Log.e(TAG, "로그인 실패: $errorMessage", task.exception)
                        Toast.makeText(activity, "로그인 실패: $errorMessage", Toast.LENGTH_SHORT).show()
                        callback(false)
                    }
                }
        } catch (e: Exception) {
            isLoading.value = false
            Log.e(TAG, "loginUser 호출 중 예외 발생", e)
            Toast.makeText(activity, "오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
            callback(false)
        }
    }

    fun logoutUser() {
        try {
            firebaseAuth.signOut()
            isLoggedIn.value = false
            Log.d(TAG, "로그아웃 성공")
        } catch (e: Exception) {
            Log.e(TAG, "로그아웃 중 오류 발생", e)
        }
    }

    // 사용자 관련 함수
    suspend fun createUserProfile(user: User) {
        try {
            userCollection.document(user.id).set(user).await()
            Log.d(TAG, "사용자 프로필 생성 성공: ${user.id}")
        } catch (e: Exception) {
            Log.e(TAG, "사용자 프로필 생성 중 오류", e)
            throw e
        }
    }

    suspend fun getUserById(userId: String): User? {
        return try {
            val result = userCollection.document(userId).get().await()
            val user = result.toObject(User::class.java)
            Log.d(TAG, "사용자 조회 결과: $user")
            user
        } catch (e: Exception) {
            Log.e(TAG, "사용자 조회 중 오류: $userId", e)
            null
        }
    }

    suspend fun updateUserProfile(user: User) {
        try {
            userCollection.document(user.id).set(user).await()
            Log.d(TAG, "사용자 프로필 업데이트 성공: ${user.id}")
        } catch (e: Exception) {
            Log.e(TAG, "사용자 프로필 업데이트 중 오류", e)
            throw e
        }
    }

    // 서버 관련 함수
    suspend fun createServer(server: Server) {
        try {
            serverCollection.document(server.serverId).set(server).await()
            Log.d(TAG, "서버 생성 성공: ${server.serverId}")
        } catch (e: Exception) {
            Log.e(TAG, "서버 생성 중 오류", e)
            throw e
        }
    }

    suspend fun getServerById(serverId: String): Server? {
        return try {
            val result = serverCollection.document(serverId).get().await()
            val server = result.toObject(Server::class.java)
            Log.d(TAG, "서버 조회 결과: $server")
            server
        } catch (e: Exception) {
            Log.e(TAG, "서버 조회 중 오류: $serverId", e)
            null
        }
    }

    // 채널 관련 함수
    suspend fun createChannel(channel: Channel) {
        try {
            channelCollection.document(channel.channelId).set(channel).await()
            Log.d(TAG, "채널 생성 성공: ${channel.channelId}")

            // 서버에 채널 추가
            val server = getServerById(channel.serverId)
            server?.let {
                it.channels.add(channel.channelId)
                if (channel.channelType == "text") {
                    it.textChannels.add(channel.channelId)
                } else {
                    it.voiceChannels.add(channel.channelId)
                }
                serverCollection.document(channel.serverId).set(it).await()
                Log.d(TAG, "서버에 채널 추가 성공: ${channel.serverId}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "채널 생성 중 오류", e)
            throw e
        }
    }

    // 메시지 관련 함수
    suspend fun sendMessage(channelId: String, message: Message) {
        try {
            messageCollection.document(message.messageId).set(message).await()
            Log.d(TAG, "메시지 전송 성공: ${message.messageId}")

            // 채널에 메시지 추가
            channelCollection.document(channelId).update(
                "messages", com.google.firebase.firestore.FieldValue.arrayUnion(message.messageId)
            ).await()
            Log.d(TAG, "채널에 메시지 추가 성공: $channelId")
        } catch (e: Exception) {
            Log.e(TAG, "메시지 전송 중 오류", e)
            throw e
        }
    }

    // 이미지 업로드 함수
    suspend fun uploadImage(imageUri: Uri, path: String): String {
        try {
            val ref = storage.reference.child(path)
            ref.putFile(imageUri).await()
            val url = ref.downloadUrl.await().toString()
            Log.d(TAG, "이미지 업로드 성공: $path, URL: $url")
            return url
        } catch (e: Exception) {
            Log.e(TAG, "이미지 업로드 중 오류", e)
            throw e
        }
    }

    fun getCurrentUser() = firebaseAuth.currentUser
    
    companion object {
        private const val TAG = "FirebaseSource"
    }
}
