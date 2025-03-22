package com.example.capstone.ui.main

import android.net.Uri
import android.util.Log
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
    private val firebaseSource: FirebaseSource
) : ViewModel() {

    val currentUser = MutableLiveData<User>()
    val currentServer = MutableLiveData<Server>()
    val currentChannel = MutableLiveData<Channel>()
    val messages = MutableLiveData<List<Message>>()
    val servers = MutableLiveData<List<Server>>()
    val channels = MutableLiveData<List<Channel>>()
    
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e("MainViewModel", "코루틴 에러 발생: ${exception.localizedMessage}", exception)
    }

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch(exceptionHandler) {
            try {
                val currentFirebaseUser = firebaseSource.getCurrentUser()
                if (currentFirebaseUser != null) {
                    val userId = currentFirebaseUser.uid
                    val user = firebaseSource.getUserById(userId)
                    if (user != null) {
                        currentUser.postValue(user)
                    } else {
                        Log.w("MainViewModel", "사용자 데이터를 Firestore에서 찾을 수 없습니다: $userId")
                    }
                } else {
                    Log.w("MainViewModel", "Firebase 사용자가 널입니다. 로그인이 필요합니다.")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "사용자 로드 중 에러 발생", e)
            }
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

    fun logout() {
        firebaseSource.logoutUser()
    }
}