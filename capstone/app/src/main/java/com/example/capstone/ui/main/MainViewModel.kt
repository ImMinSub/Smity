package com.example.capstone.ui.main

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.capstone.data.Channel
import com.example.capstone.data.Group
import com.example.capstone.data.Message
import com.example.capstone.data.User
import com.example.capstone.firebase.FirebaseSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val firebaseSource: FirebaseSource
) : ViewModel() {

    private val TAG = "MainViewModel"
    
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser
    
    val currentGroup = MutableLiveData<Group>()
    val currentChannel = MutableLiveData<Channel>()
    val messages = MutableLiveData<List<Message>>()
    val groups = MutableLiveData<List<Group>>()
    val channels = MutableLiveData<List<Channel>>()
    
    // 그룹 생성 상태를 추적하는 LiveData 추가
    private val _groupCreationState = MutableLiveData<GroupCreationState>()
    val groupCreationState: LiveData<GroupCreationState> = _groupCreationState

    // 그룹 생성 상태를 나타내는 봉인 클래스
    sealed class GroupCreationState {
        object Idle : GroupCreationState()
        object Loading : GroupCreationState()
        object Success : GroupCreationState()
        data class Error(val message: String) : GroupCreationState()
    }
    
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "코루틴 에러 발생: ${exception.localizedMessage}", exception)
    }

    init {
        loadCurrentUser()
        loadGroups()
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

    fun createGroup(groupName: String) {
        // 이미 로딩 중이면 중복 요청 방지
        if (_groupCreationState.value is GroupCreationState.Loading) return
        
        _groupCreationState.value = GroupCreationState.Loading
        
        viewModelScope.launch(exceptionHandler) {
            try {
                Log.d(TAG, "그룹 생성 시작: $groupName")
                
                val currentUser = firebaseSource.getCurrentUser()
                if (currentUser == null) {
                    Log.e(TAG, "그룹 생성 실패: 현재 로그인된 사용자 없음")
                    _groupCreationState.postValue(GroupCreationState.Error("로그인된 사용자 정보를 찾을 수 없습니다"))
                    return@launch
                }
                
                Log.d(TAG, "현재 사용자 ID: ${currentUser.uid}")
                
                // Firebase 토큰 리프레시 시도
                try {
                    Log.d(TAG, "Firebase 토큰 리프레시 시도")
                    currentUser.getIdToken(true).await()
                    Log.d(TAG, "Firebase 토큰 리프레시 성공")
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase 토큰 리프레시 실패", e)
                    _groupCreationState.postValue(GroupCreationState.Error("인증 토큰 갱신 실패: ${e.message}"))
                    return@launch
                }
                
                // 네트워크 연결 확인
                try {
                    val testTask = firebaseSource.getFirestoreInstance().collection("__DUMMY__").document("__TEST__").get()
                    testTask.addOnFailureListener { e -> 
                        Log.e(TAG, "Firestore 연결 테스트 실패", e)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "연결 테스트 중 예외 발생", e)
                }
                
                // 그룹 객체 생성 (헬퍼 메서드 사용)
                val group = Group.createDefault(currentUser.uid, groupName)
                Log.d(TAG, "그룹 객체 생성 완료: $group")

                // 기본 채널 생성
                val generalChannelId = firebaseSource.generateDocumentId("channels")
                val generalChannel = Channel(
                    channelId = generalChannelId,
                    channelName = "일반",
                    channelType = "text",
                    groupId = group.groupId,
                    createdAt = System.currentTimeMillis()
                )
                Log.d(TAG, "기본 채널 객체 생성 완료: $generalChannel")
                
                // 그룹에 채널 추가 (헬퍼 메서드 사용)
                group.addChannel(generalChannelId, "text")
                
                try {
                    // Firestore에 그룹 저장
                    Log.d(TAG, "Firestore에 그룹 저장 시작")
                    firebaseSource.createGroup(group)
                    Log.d(TAG, "Firestore에 그룹 저장 완료")
                    
                    // Firestore에 채널 저장
                    Log.d(TAG, "Firestore에 채널 저장 시작")
                    firebaseSource.createChannel(generalChannel)
                    Log.d(TAG, "Firestore에 채널 저장 완료")
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore에 데이터 저장 중 오류 발생", e)
                    
                    // 더 자세한 오류 메시지 생성
                    val errorMsg = when {
                        e.message?.contains("permission") == true || e.message?.contains("denied") == true -> 
                            "데이터베이스 접근 권한 오류: ${e.message}"
                        e.message?.contains("network") == true -> 
                            "네트워크 연결 오류: ${e.message}"
                        e.message?.contains("cancelled") == true -> 
                            "작업이 취소되었습니다: ${e.message}"
                        e.message?.contains("offline") == true -> 
                            "오프라인 상태입니다: ${e.message}"
                        else -> "데이터베이스 저장 중 오류: ${e.message}"
                    }
                    
                    _groupCreationState.postValue(GroupCreationState.Error(errorMsg))
                    return@launch
                }
                
                try {
                    // 사용자 정보 업데이트 (그룹 추가)
                    Log.d(TAG, "사용자 정보 조회 시작")
                    val user = firebaseSource.getUserById(currentUser.uid)
                    if (user == null) {
                        Log.e(TAG, "사용자 정보를 찾을 수 없음: ${currentUser.uid}")
                        // 사용자 정보가 없어도 그룹은 생성되었으므로 성공으로 처리
                        _groupCreationState.postValue(GroupCreationState.Success)
                        loadGroups()
                        return@launch
                    }
                    
                    Log.d(TAG, "사용자 정보 업데이트 시작")
                    // 그룹 ID 추가
                    if (!user.groups.contains(group.groupId)) {
                        user.groups.add(group.groupId)
                        firebaseSource.updateUserProfile(user)
                        Log.d(TAG, "사용자 정보 업데이트 완료")
                    } else {
                        Log.d(TAG, "이미 그룹에 포함되어 있음")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "사용자 정보 업데이트 중 오류", e)
                    // 사용자 정보 업데이트에 실패해도 그룹은 생성되었으므로 성공으로 처리
                    _groupCreationState.postValue(GroupCreationState.Success)
                    loadGroups()
                    return@launch
                }
                
                // 그룹 목록 다시 로드
                Log.d(TAG, "그룹 목록 새로고침 시작")
                loadGroups()
                Log.d(TAG, "그룹 생성 프로세스 완료")
                
                // 성공 상태 업데이트
                _groupCreationState.postValue(GroupCreationState.Success)
            } catch (e: Exception) {
                Log.e(TAG, "그룹 생성 중 예외 발생", e)
                _groupCreationState.postValue(GroupCreationState.Error("그룹 생성 중 오류: ${e.message}"))
            }
        }
    }

    fun loadGroups() {
        viewModelScope.launch(exceptionHandler) {
            try {
                val currentUser = firebaseSource.getCurrentUser()
                if (currentUser != null) {
                    val userGroups = mutableListOf<Group>()
                    
                    // 현재 사용자가 속한 그룹 목록 가져오기
                    val user = firebaseSource.getUserById(currentUser.uid)
                    user?.groups?.forEach { groupId ->
                        val group = firebaseSource.getGroupById(groupId)
                        group?.let { userGroups.add(it) }
                    }
                    
                    groups.value = userGroups
                }
            } catch (e: Exception) {
                Log.e(TAG, "그룹 목록 로드 중 오류", e)
            }
        }
    }
    
    fun selectGroup(group: Group) {
        try {
            Log.d(TAG, "그룹 선택: ${group.groupName} (ID: ${group.groupId})")
            
            // 현재 선택된 그룹 설정 전에 필수 필드 확인
            if (group.groupId.isBlank()) {
                Log.e(TAG, "그룹 ID가 비어있습니다!")
                return
            }
            
            // 깊은 복사본 생성하여 독립적으로 사용
            val safeGroup = Group(
                groupId = group.groupId,
                groupName = group.groupName,
                groupIcon = group.groupIcon,
                groupTag = group.groupTag,
                channels = ArrayList(group.channels),
                textChannels = ArrayList(group.textChannels),
                voiceChannels = ArrayList(group.voiceChannels),
                members = ArrayList(group.members),
                ownerId = group.ownerId
            )
            
            // 안전하게 현재 그룹 업데이트
            currentGroup.value = safeGroup
            
            // 그룹에 기본 채널이 없으면 기본 채널 생성 여부 확인
            if (safeGroup.channels.isEmpty()) {
                Log.w(TAG, "선택된 그룹에 채널이 없습니다. 기본 채널 생성 고려")
                // 기본 채널이 없는 경우 빈 리스트 설정
                channels.value = emptyList()
                currentChannel.value = null
            } else {
                // 채널 목록 로드 시도
                loadChannels(safeGroup.groupId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "그룹 선택 처리 중 오류 발생", e)
            // 오류 시 빈 채널 목록 설정
            channels.value = emptyList()
            currentChannel.value = null
        }
    }
    
    fun loadChannels(groupId: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                if (groupId.isBlank()) {
                    Log.e(TAG, "채널 로드 실패: 그룹 ID가 비어있습니다.")
                    channels.value = emptyList()
                    return@launch
                }
                
                Log.d(TAG, "채널 로드 시작: 그룹 ID = $groupId")
                
                // 채널 목록 초기화
                val groupChannels = mutableListOf<Channel>()
                
                // 그룹 정보 확인
                val group = firebaseSource.getGroupById(groupId)
                if (group == null) {
                    Log.e(TAG, "그룹을 찾을 수 없음: $groupId")
                    channels.value = emptyList()
                    
                    // 기본 채널 생성 옵션 추가
                    currentChannel.value = Channel.default(groupId, "채널 없음")
                    return@launch
                }
                
                Log.d(TAG, "그룹 정보 불러옴: ${group.groupName}, 채널 수: ${group.channels.size}")
                
                // 채널 목록이 비어있는 경우
                if (group.channels.isEmpty()) {
                    Log.d(TAG, "그룹의 채널 목록이 비어있음")
                    channels.value = emptyList()
                    
                    // 기본 채널 생성 옵션 추가
                    currentChannel.value = Channel.default(groupId, "기본 채널")
                    return@launch
                }
                
                // 그룹에 속한 각 채널 정보 가져오기
                var successfullyLoadedChannel = false
                
                for (channelId in group.channels) {
                    try {
                        if (channelId.isBlank()) {
                            Log.w(TAG, "채널 ID가 비어있습니다. 건너뜁니다.")
                            continue
                        }
                        
                        Log.d(TAG, "채널 정보 조회 시도: $channelId")
                        val channelRef = firebaseSource.getFirestoreInstance().collection("channels").document(channelId)
                        val channelDoc = channelRef.get().await()
                        
                        if (!channelDoc.exists()) {
                            Log.w(TAG, "채널 문서가 존재하지 않음: $channelId")
                            continue
                        }
                        
                        val channel = channelDoc.toObject<Channel>()
                        if (channel != null) {
                            // 채널 객체 검증
                            if (channel.channelId.isBlank() || channel.channelName.isBlank()) {
                                Log.w(TAG, "불완전한 채널 정보: ${channel.channelId}, 이름: ${channel.channelName}")
                                // 필수 정보가 비어있어도 일단 추가
                            }
                            
                            Log.d(TAG, "채널 로드 성공: ${channel.channelName} (ID: ${channel.channelId})")
                            groupChannels.add(channel)
                            successfullyLoadedChannel = true
                        } else {
                            Log.w(TAG, "채널 객체 변환 실패: $channelId")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "채널 정보 로드 중 오류 발생: $channelId", e)
                    }
                }
                
                // 채널 목록 설정
                Log.d(TAG, "총 ${groupChannels.size}개의 채널 로드 완료")
                channels.value = groupChannels
                
                // 첫 번째 채널을 선택 (있는 경우)
                if (groupChannels.isNotEmpty()) {
                    Log.d(TAG, "첫 번째 채널 선택: ${groupChannels[0].channelName}")
                    currentChannel.value = groupChannels[0]
                } else {
                    Log.w(TAG, "로드된 채널이 없음, 기본 채널 생성")
                    
                    // 모든 채널 로드에 실패한 경우 임시 채널 객체를 생성하여 UI가 깨지지 않도록 함
                    if (!successfullyLoadedChannel) {
                        currentChannel.value = Channel.default(groupId, "기본 채널")
                    } else {
                        currentChannel.value = null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "채널 목록 로드 중 오류", e)
                channels.value = emptyList()
                
                // 오류 발생 시 기본 채널 제공
                currentChannel.value = Channel.default(groupId, "오류 발생")
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

    // 채널 생성 메서드 추가
    fun createChannel(groupId: String, channelName: String, channelType: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                // 채널 ID 생성
                val channelId = firebaseSource.generateDocumentId("channels")
                val channel = Channel(
                    channelId = channelId,
                    channelName = channelName,
                    channelType = channelType,
                    createdAt = System.currentTimeMillis(),
                    groupId = groupId
                )
                firebaseSource.createChannel(channel)
                
                // 채널 목록 새로고침
                loadChannels(groupId)
            } catch (e: Exception) {
                Log.e(TAG, "채널 생성 중 오류 발생", e)
            }
        }
    }

    fun loadMessages(channelId: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                Log.d(TAG, "메시지 로드 시작: 채널 ID = $channelId")
                
                // 메시지 목록을 가져오는 함수 호출
                val messagesList = firebaseSource.getMessages(channelId)
                messages.value = messagesList
                
                Log.d(TAG, "메시지 로드 완료: ${messagesList.size}개의 메시지")
                
                // 메시지 실시간 업데이트 구독
                subscribeToMessageUpdates(channelId)
            } catch (e: Exception) {
                Log.e(TAG, "메시지 로드 중 오류", e)
                messages.value = emptyList()
            }
        }
    }
    
    private var messageListener: ListenerRegistration? = null
    
    private fun subscribeToMessageUpdates(channelId: String) {
        try {
            // 기존 리스너가 있다면 제거
            messageListener?.remove()
            
            // 메시지 콜렉션에 대한 실시간 리스너 설정
            messageListener = firebaseSource.getFirestoreInstance()
                .collection("channels")
                .document(channelId)
                .collection("messages")
                .orderBy("sentAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e(TAG, "메시지 구독 중 오류", e)
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val messageList = snapshot.toObjects(Message::class.java)
                        Log.d(TAG, "메시지 업데이트 감지: ${messageList.size}개")
                        messages.value = messageList
                    }
                }
            
            Log.d(TAG, "메시지 업데이트 구독 완료: 채널 ID = $channelId")
        } catch (e: Exception) {
            Log.e(TAG, "메시지 업데이트 구독 중 오류", e)
        }
    }
    
    // 채널 리스너 해제
    fun unsubscribeFromMessages() {
        messageListener?.remove()
        messageListener = null
        Log.d(TAG, "메시지 업데이트 구독 해제")
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel이 소멸될 때 리스너 해제
        unsubscribeFromMessages()
    }
}
