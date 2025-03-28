package com.example.capstone.ui.main

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.capstone.R
import com.example.capstone.data.Channel
import com.example.capstone.data.Group
import com.example.capstone.databinding.FragmentChatBinding
import com.example.capstone.ui.util.ItemSpacingDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatFragment : Fragment() {
    private val TAG = "ChatFragment"
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    
    // 액티비티와 공유되는 ViewModel 사용
    private val viewModel: MainViewModel by activityViewModels()
    
    // 메시지 어댑터
    private lateinit var messageAdapter: MessageAdapter
    private var messageUpdateJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            // 이전 바인딩이 있으면 정리
            if (_binding != null) {
                _binding = null
            }
            
            _binding = FragmentChatBinding.inflate(inflater, container, false)
            Log.d(TAG, "onCreateView: View 생성 완료")
            return binding.root
        } catch (e: Exception) {
            Log.e(TAG, "onCreateView 오류: ${e.message}", e)
            
            // 오류 시 빈 레이아웃 반환
            val errorView = inflater.inflate(R.layout.layout_error_fallback, container, false)
            val errorTitle = errorView.findViewById<TextView>(R.id.error_title)
            val errorMessage = errorView.findViewById<TextView>(R.id.error_message)
            val retryButton = errorView.findViewById<Button>(R.id.btn_retry)
            
            errorTitle.text = "채팅 화면을 로드할 수 없습니다"
            errorMessage.text = "뒤로 가기를 눌러 그룹 목록으로 돌아가세요"
            
            retryButton.setOnClickListener {
                // 다시 시도 로직 (이전 화면으로 돌아가기)
                findNavController().navigateUp()
            }
            
            return errorView
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated 시작")
        
        try {
            // Fragment 상태 확인
            if (!isAdded || isDetached || isRemoving) {
                Log.w(TAG, "onViewCreated: Fragment가 유효하지 않음")
                return
            }
            
            // 로딩 상태 표시
            safeShowLoading(true)
            
            // 오류 숨기기
            safeHideError()
            
            // 현재 선택된 그룹 가져오기
            val currentGroup = viewModel.currentGroup.value
            
            if (currentGroup == null) {
                Log.e(TAG, "선택된 그룹이 없습니다")
                safeShowLoading(false)
                safeShowError("선택된 그룹을 찾을 수 없습니다. 이전 화면으로 돌아가세요")
                return
            }
            
            // UI 설정
            safeSetupUI(currentGroup)
            
            // 로딩 상태 숨기기
            safeShowLoading(false)
            
            Log.d(TAG, "onViewCreated 완료: ${currentGroup.groupName}")
        } catch (e: Exception) {
            Log.e(TAG, "onViewCreated 오류: ${e.message}", e)
            safeShowLoading(false)
            safeShowError("채팅 화면 로드 중 오류가 발생했습니다: ${e.message}")
        }
    }
    
    // 안전한 UI 설정 메서드
    private fun safeSetupUI(group: Group) {
        if (!isAdded || isDetached || isRemoving || _binding == null) {
            Log.w(TAG, "safeSetupUI: Fragment 또는 바인딩이 유효하지 않음")
            return
        }
        
        try {
            // 툴바 설정
            binding.chatToolbar.title = group.groupName
            binding.chatToolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
            
            // 첫 번째 채널 가져오기 (있다면)
            val firstChannelId = group.channels.firstOrNull()
            
            if (firstChannelId != null) {
                // 채널 ID로 채널 객체 생성
                val channel = Channel(
                    channelId = firstChannelId,
                    channelName = "일반",
                    channelType = "text",
                    groupId = group.groupId
                )
                safeSetupChannelView(channel)
            } else {
                Log.w(TAG, "채널이 없습니다: ${group.groupId}")
                safeShowError("이 그룹에는 활성화된 채널이 없습니다")
            }
        } catch (e: Exception) {
            Log.e(TAG, "UI 설정 중 오류: ${e.message}", e)
            safeShowError("UI 설정 중 오류: ${e.message}")
        }
    }
    
    // 안전한 채널 뷰 설정 메서드
    private fun safeSetupChannelView(channel: Channel) {
        if (!isAdded || isDetached || isRemoving || _binding == null) {
            Log.w(TAG, "safeSetupChannelView: Fragment 또는 바인딩이 유효하지 않음")
            return
        }
        
        try {
            // 현재 채널 설정
            viewModel.currentChannel.value = channel
            
            // 채널 이름 설정
            binding.chatHeaderTitle.text = "#${channel.channelName}"
            
            // 메시지 목록 설정
            safeSetupMessageList(channel)
            
            // 메시지 전송 설정
            safeSetupMessageSending(channel)
            
            Log.d(TAG, "채널 뷰 설정 완료: ${channel.channelId}")
        } catch (e: Exception) {
            Log.e(TAG, "채널 뷰 설정 중 오류: ${e.message}", e)
        }
    }
    
    // 안전한 메시지 목록 설정 메서드
    private fun safeSetupMessageList(channel: Channel) {
        if (!isAdded || isDetached || isRemoving || _binding == null) {
            Log.w(TAG, "safeSetupMessageList: Fragment 또는 바인딩이 유효하지 않음")
            return
        }
        
        try {
            // 메시지 어댑터 초기화 (비어있는 리스트로 시작)
            messageAdapter = MessageAdapter(emptyList())
            
            // RecyclerView 설정
            binding.messagesRecyclerView.apply {
                adapter = messageAdapter
                layoutManager = LinearLayoutManager(context).apply {
                    stackFromEnd = true // 최신 메시지가 아래에 표시되도록
                }
                addItemDecoration(
                    ItemSpacingDecoration(
                        resources.getDimensionPixelSize(R.dimen.message_item_spacing)
                    )
                )
            }
            
            // 메시지 업데이트 리스너 설정
            safeLoadChannelMessages(channel)
            
            Log.d(TAG, "메시지 목록 설정 완료: ${channel.channelId}")
        } catch (e: Exception) {
            Log.e(TAG, "메시지 목록 설정 중 오류: ${e.message}", e)
        }
    }
    
    // 안전한 채널 메시지 로딩 메서드
    private fun safeLoadChannelMessages(channel: Channel) {
        if (!isAdded || isDetached || isRemoving) {
            Log.w(TAG, "safeLoadChannelMessages: Fragment가 유효하지 않음")
            return
        }
        
        try {
            // 이전 메시지 업데이트 구독 취소
            messageUpdateJob?.cancel()
            
            // 메시지 로드 시작
            viewModel.loadMessages(channel.channelId)
            
            // 채널 메시지 구독
            messageUpdateJob = lifecycleScope.launch {
                try {
                    // 메시지 실시간 업데이트를 위해 LiveData 관찰
                    viewModel.messages.observe(viewLifecycleOwner) { messages ->
                        if (isAdded && !isDetached && !isRemoving && _binding != null) {
                            messageAdapter.updateMessages(messages)
                            if (messages.isNotEmpty()) {
                                binding.messagesRecyclerView.scrollToPosition(messages.size - 1)
                            }
                            Log.d(TAG, "메시지 업데이트: ${messages.size}개")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "메시지 업데이트 중 오류: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "채널 메시지 로딩 중 오류: ${e.message}", e)
        }
    }
    
    // 안전한 메시지 전송 설정 메서드
    private fun safeSetupMessageSending(channel: Channel) {
        if (!isAdded || isDetached || isRemoving || _binding == null) {
            Log.w(TAG, "safeSetupMessageSending: Fragment 또는 바인딩이 유효하지 않음")
            return
        }
        
        try {
            // 메시지 입력 리스너
            binding.sendButton.setOnClickListener {
                try {
                    val messageText = binding.messageEditText.text.toString().trim()
                    if (messageText.isNotEmpty()) {
                        viewModel.sendMessage(channel.channelId, messageText, null)
                        binding.messageEditText.text.clear()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "메시지 전송 중 오류: ${e.message}", e)
                    Toast.makeText(
                        requireContext(),
                        "메시지 전송 실패: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "메시지 전송 설정 중 오류: ${e.message}", e)
        }
    }
    
    // 안전한 로딩 표시 메서드
    private fun safeShowLoading(show: Boolean) {
        if (!isAdded || isDetached || isRemoving || _binding == null) return
        
        try {
            binding.loadingProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "로딩 표시 설정 중 오류: ${e.message}", e)
        }
    }
    
    // 안전한 오류 표시 메서드
    private fun safeShowError(errorMessage: String) {
        if (!isAdded || isDetached || isRemoving || _binding == null) return
        
        try {
            binding.errorMessageText.text = errorMessage
            binding.errorMessageText.visibility = View.VISIBLE
            binding.messagesRecyclerView.visibility = View.GONE
            binding.messageInputLayout.visibility = View.GONE
            
            Log.e(TAG, "오류 표시: $errorMessage")
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "오류 표시 중 오류: ${e.message}", e)
        }
    }
    
    // 안전한 오류 숨김 메서드
    private fun safeHideError() {
        if (!isAdded || isDetached || isRemoving || _binding == null) return
        
        try {
            binding.errorMessageText.visibility = View.GONE
            binding.messagesRecyclerView.visibility = View.VISIBLE
            binding.messageInputLayout.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e(TAG, "오류 숨김 중 오류: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        try {
            // 메시지 업데이트 구독 취소
            messageUpdateJob?.cancel()
            messageUpdateJob = null
            
            // 메시지 리스너 해제
            viewModel.unsubscribeFromMessages()
            
            // 바인딩 정리
            _binding = null
        } finally {
            super.onDestroyView()
            Log.d(TAG, "onDestroyView 완료")
        }
    }
}


