package com.example.capstone.ui.main

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.capstone.data.Channel
import com.example.capstone.data.Message
import com.example.capstone.data.Server
import com.example.capstone.data.User
import com.example.capstone.firebase.FirebaseSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val firebaseSource: FirebaseSource
) : ViewModel() {

    private val TAG = "MainViewModel"
    
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser
    
    val currentServer = MutableLiveData<Server>()
    val currentChannel = MutableLiveData<Channel>()
    val messages = MutableLiveData<List<Message>>()
    val servers = MutableLiveData<List<Server>>()
    val channels = MutableLiveData<List<Channel>>()
    
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "코루틴 에러 발생: ${exception.localizedMessage}", exception)
    }

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch(exceptionHandler) {
            try {
                val userId = firebaseSource.getCurrentUser()?.uid
                if (userId != null) {
                    // Firebase에서 최신 사용자 정보 가져오기
                    val user = firebaseSource.getUserById(userId)
                    if (user != null) {
                        _currentUser.postValue(user)  // postValue 사용하여 UI 스레드에서 설정
                        Log.d(TAG, "사용자 정보 로드 성공: ${user.username}, 이메일: ${user.email}")
                    } else {
                        Log.e(TAG, "사용자 정보를 찾을 수 없습니다: $userId")
                        // 사용자가 존재하지 않으면 기본 정보로 생성
                        val currentFirebaseUser = firebaseSource.getCurrentUser()
                        if (currentFirebaseUser != null) {
                            val newUser = User(
                                id = userId,
                                username = currentFirebaseUser.displayName ?: "사용자",
                                email = currentFirebaseUser.email ?: "",
                                profileImageUrl = "https://via.placeholder.com/150",
                                status = "온라인"
                            )
                            // 새 사용자 프로필 생성
                            firebaseSource.createUserProfile(newUser)
                            _currentUser.postValue(newUser)
                            Log.d(TAG, "새 사용자 프로필 생성: ${newUser.email}")
                        }
                    }
                } else {
                    Log.e(TAG, "현재 로그인된 사용자 ID를 가져올 수 없습니다.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "사용자 정보 로드 중 오류 발생", e)
            }
        }
    }

    fun updateUserProfile(user: User) {
        viewModelScope.launch(exceptionHandler) {
            try {
                firebaseSource.updateUserProfile(user)
                _currentUser.value = user
                Log.d(TAG, "사용자 프로필 업데이트 성공: ${user.id}")
            } catch (e: Exception) {
                Log.e(TAG, "사용자 프로필 업데이트 중 오류 발생", e)
            }
        }
    }

    fun logout() {
        try {
            firebaseSource.logoutUser()
            _currentUser.value = null
            Log.d(TAG, "로그아웃 성공")
        } catch (e: Exception) {
            Log.e(TAG, "로그아웃 중 오류 발생", e)
        }
    }

    fun createServer(serverName: String, imageUri: Uri?) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val userId = firebaseSource.getCurrentUser()?.uid ?: return@launch
                val serverId = "server_${System.currentTimeMillis()}"

                var serverIconUrl = "https://via.placeholder.com/150"
                if (imageUri != null) {
                    serverIconUrl = firebaseSource.uploadImage(imageUri, "servers/$serverId/icon")
                }

                val server = Server(
                    serverId = serverId,
                    serverName = serverName,
                    serverIcon = serverIconUrl,
                    ownerId = userId
                )

                server.members.add(userId)
                firebaseSource.createServer(server)

                // 기본 채널 생성
                createChannel(serverId, "일반", "text")
            } catch (e: Exception) {
                Log.e("MainViewModel", "서버 생성 중 에러 발생", e)
            }
        }
    }

    fun createChannel(serverId: String, channelName: String, channelType: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val channelId = "channel_${System.currentTimeMillis()}"
                val channel = Channel(
                    channelId = channelId,
                    channelName = channelName,
                    channelType = channelType,
                    createdAt = System.currentTimeMillis(),
                    serverId = serverId
                )
                firebaseSource.createChannel(channel)
            } catch (e: Exception) {
                Log.e("MainViewModel", "채널 생성 중 에러 발생", e)
            }
        }
    }

    fun sendMessage(channelId: String, messageText: String, imageUri: Uri?) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val userId = firebaseSource.getCurrentUser()?.uid ?: return@launch
                val user = firebaseSource.getUserById(userId) ?: return@launch

                val messageId = "message_${System.currentTimeMillis()}"

                var imageUrl = ""
                if (imageUri != null) {
                    imageUrl = firebaseSource.uploadImage(imageUri, "messages/$messageId/image")
                }

                val message = Message(
                    messageId = messageId,
                    senderId = userId,
                    senderUsername = user.username,
                    senderProfileUrl = user.profileImageUrl,
                    message = messageText,
                    imageUrl = imageUrl,
                    sentAt = System.currentTimeMillis()
                )

                firebaseSource.sendMessage(channelId, message)
            } catch (e: Exception) {
                Log.e("MainViewModel", "메시지 전송 중 에러 발생", e)
            }
        }
    }

    fun updateUserStatus(status: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val userId = firebaseSource.getCurrentUser()?.uid ?: return@launch
                val user = firebaseSource.getUserById(userId) ?: return@launch
                user.status = status
                firebaseSource.updateUserProfile(user)
            } catch (e: Exception) {
                Log.e("MainViewModel", "사용자 상태 업데이트 중 에러 발생", e)
            }
        }
    }

    // 사용자 정보 새로고침 함수 추가
    fun refreshUserProfile() {
        loadCurrentUser()
    }

    // Firebase 인증에서 직접 현재 로그인된 사용자 정보를 가져옴
    fun getCurrentUserDirectly(): User? {
        try {
            val firebaseUser = firebaseSource.getCurrentUser() ?: return null
            
            // 기존 사용자 정보가 있으면 반환
            val existingUser = currentUser.value
            if (existingUser != null) {
                return existingUser
            }
            
            // 기존 정보가 없으면 기본 사용자 정보 생성
            return User(
                id = firebaseUser.uid,
                username = firebaseUser.displayName ?: "사용자",
                email = firebaseUser.email ?: "",
                profileImageUrl = "https://via.placeholder.com/150",
                status = "온라인"
            )
        } catch (e: Exception) {
            Log.e(TAG, "현재 사용자 정보 직접 가져오기 실패: ${e.message}", e)
            return null
        }
    }
}
