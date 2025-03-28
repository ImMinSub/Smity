package com.example.capstone.ui.main

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone.R
import com.example.capstone.data.Message
import com.example.capstone.databinding.ItemMessageReceivedBinding
import com.example.capstone.databinding.ItemMessageSentBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 채팅 메시지를 표시하는 RecyclerView 어댑터
 */
class MessageAdapter(
    private var messages: List<Message> = emptyList()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TAG = "MessageAdapter"
    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2
    private val dateFormat = SimpleDateFormat("a h:mm", Locale.KOREA)
    
    // 현재 사용자 ID (임시 값, 실제로는 사용자 인증에서 가져와야 함)
    private var currentUserId: String = ""
    
    /**
     * 메시지 목록을 업데이트하고 UI에 반영
     */
    fun updateMessages(newMessages: List<Message>) {
        val diffCallback = MessageDiffCallback(this.messages, newMessages)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        this.messages = newMessages
        diffResult.dispatchUpdatesTo(this)
    }
    
    /**
     * 현재 사용자 ID 설정
     */
    fun setCurrentUserId(userId: String) {
        this.currentUserId = userId
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                SentMessageViewHolder(binding)
            }
            VIEW_TYPE_RECEIVED -> {
                val binding = ItemMessageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ReceivedMessageViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    /**
     * 보낸 메시지용 ViewHolder
     */
    inner class SentMessageViewHolder(
        private val binding: ItemMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(message: Message) {
            try {
                binding.textMessageBody.text = message.message
                
                // 시간 표시
                val timestamp = Date(message.sentAt)
                binding.textMessageTime.text = dateFormat.format(timestamp)
                
                // 메시지 상태 표시 (전송 중, 전송 완료, 읽음 등)
                // 실제 status 필드가 없으므로 항상 GONE으로 처리하거나, edited 값으로 대체
                binding.textMessageStatus.visibility = 
                    if (message.edited) View.VISIBLE else View.GONE
                
                if (message.edited) {
                    binding.textMessageStatus.text = "수정됨"
                } else {
                    binding.textMessageStatus.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e(TAG, "보낸 메시지 바인딩 중 오류: ${e.message}", e)
            }
        }
    }

    /**
     * 받은 메시지용 ViewHolder
     */
    inner class ReceivedMessageViewHolder(
        private val binding: ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(message: Message) {
            try {
                binding.textMessageBody.text = message.message
                
                // 시간 표시
                val timestamp = Date(message.sentAt)
                binding.textMessageTime.text = dateFormat.format(timestamp)
                
                // 발신자 이름 표시
                binding.textSenderName.text = message.senderUsername
                
                // 발신자 프로필 이미지 설정 (Glide 사용)
                if (message.senderProfileUrl.isNotEmpty()) {
                    Glide.with(binding.root.context)
                        .load(message.senderProfileUrl)
                        .placeholder(R.drawable.ic_error)
                        .error(R.drawable.ic_error)
                        .apply(RequestOptions.circleCropTransform())
                        .into(binding.imageSenderProfile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "받은 메시지 바인딩 중 오류: ${e.message}", e)
            }
        }
    }
    
    /**
     * 메시지 목록 비교를 위한 DiffUtil 콜백
     */
    private class MessageDiffCallback(
        private val oldList: List<Message>,
        private val newList: List<Message>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize(): Int = oldList.size
        
        override fun getNewListSize(): Int = newList.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].messageId == newList[newItemPosition].messageId
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            
            return oldItem.message == newItem.message &&
                    oldItem.sentAt == newItem.sentAt &&
                    oldItem.edited == newItem.edited
        }
    }
} 
