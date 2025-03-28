package com.example.capstone.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone.R
import com.example.capstone.data.Group
import com.example.capstone.databinding.FragmentGroupListBinding
import com.example.capstone.databinding.ItemGroupCardBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.AndroidEntryPoint
import com.google.firebase.auth.FirebaseAuth

@AndroidEntryPoint
class GroupListFragment : Fragment() {

    private val TAG = "GroupListFragment"
    private var _binding: FragmentGroupListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private lateinit var groupAdapter: GroupAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        
        // Firebase 인증 상태 확인
        checkAuthStatus()

        // 그룹 목록 관찰
        viewModel.groups.observe(viewLifecycleOwner) { groups ->
            groupAdapter.submitList(groups)
            binding.emptyView.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
        }

        // 그룹 생성 상태 관찰
        viewModel.groupCreationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MainViewModel.GroupCreationState.Loading -> {
                    // 로딩 표시
                    Toast.makeText(requireContext(), "그룹을 생성 중입니다...", Toast.LENGTH_SHORT).show()
                }
                is MainViewModel.GroupCreationState.Success -> {
                    // 성공 메시지
                    Toast.makeText(requireContext(), "그룹이 성공적으로 생성되었습니다!", Toast.LENGTH_SHORT).show()
                    
                    // Firebase 데이터베이스 동기화 지연을 고려해 잠시 대기 후 그룹 목록 새로고침
                    view.postDelayed({
                        viewModel.loadGroups()
                    }, 1000)
                }
                is MainViewModel.GroupCreationState.Error -> {
                    // 오류 메시지
                    val errorMsg = state.message
                    Toast.makeText(requireContext(), "오류 발생: $errorMsg", Toast.LENGTH_LONG).show()
                    
                    // 오류 종류에 따른 처리
                    when {
                        // 인증 오류인 경우 로그인 화면으로 이동
                        errorMsg.contains("인증") || errorMsg.contains("로그인") || 
                        errorMsg.contains("auth") || errorMsg.contains("login") -> {
                            showReLoginDialog()
                        }
                        // 권한 오류인 경우 추가 설명
                        errorMsg.contains("권한") || errorMsg.contains("permission") || errorMsg.contains("denied") -> {
                            Toast.makeText(requireContext(), 
                                "Firebase 보안 규칙 설정을 확인해주세요. 데이터 쓰기 권한이 필요합니다.", 
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }
                else -> { /* Idle 상태는 무시 */ }
            }
        }

        // 그룹 생성 버튼 클릭 리스너
        binding.createGroupButton.setOnClickListener {
            showCreateGroupDialog()
        }
    }

    private fun setupRecyclerView() {
        try {
            Log.d(TAG, "그룹 RecyclerView 설정 시작")
            
            groupAdapter = GroupAdapter { group ->
                Log.d(TAG, "그룹 카드 클릭: ${group.groupName} (ID: ${group.groupId})")
                try {
                    // Loading Toast 표시
                    val loadingToast = Toast.makeText(requireContext(), "채팅 화면으로 이동 중...", Toast.LENGTH_SHORT)
                    loadingToast.show()
                    
                    // UI 응답성 유지를 위해 약간의 딜레이 추가
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            // 로딩 토스트 취소
                            loadingToast.cancel()
                            
                            // Fragment가 아직 유효한지 확인
                            if (!isAdded || isDetached || isRemoving) {
                                Log.w(TAG, "Fragment가 더 이상 유효하지 않음. 그룹 선택 무시")
                                return@postDelayed
                            }
                            
                            // 그룹 ID 유효성 검사
                            if (group.groupId.isBlank()) {
                                val errorMsg = "유효하지 않은 그룹 ID입니다"
                                Log.e(TAG, errorMsg)
                                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                                return@postDelayed
                            }
                            
                            // 그룹 선택 처리
                            try {
                                viewModel.selectGroup(group)
                                Log.d(TAG, "그룹 선택 성공: ${group.groupName}")
                            } catch (e: Exception) {
                                val errorMsg = "그룹 선택 중 오류: ${e.message}"
                                Log.e(TAG, errorMsg, e)
                                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                                return@postDelayed
                            }
                            
                            // 네비게이션 처리
                            try {
                                // 현재 NavController가 유효한지 확인
                                val navController = findNavController()
                                val currentDestination = navController.currentDestination
                                
                                if (currentDestination?.id == R.id.groupListFragment) {
                                    Log.d(TAG, "ChatFragment로 이동 시작")
                                    navController.navigate(R.id.action_groupListFragment_to_chatFragment)
                                    Log.d(TAG, "ChatFragment로 이동 완료")
                                } else {
                                    Log.w(TAG, "현재 destination이 groupListFragment가 아님: ${currentDestination?.label}")
                                }
                            } catch (e: Exception) {
                                val errorMsg = "그룹으로 이동 중 오류가 발생했습니다: ${e.message}"
                                Log.e(TAG, errorMsg, e)
                                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                            }
                            
                        } catch (e: Exception) {
                            Log.e(TAG, "그룹 클릭 처리 중 오류 발생", e)
                            if (isAdded && context != null) {
                                Toast.makeText(
                                    requireContext(), 
                                    "처리 중 오류 발생: ${e.message}", 
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }, 200) // 200ms 딜레이
                    
                } catch (e: Exception) {
                    Log.e(TAG, "그룹 클릭 핸들러 오류", e)
                }
            }

            binding.recyclerView.apply {
                layoutManager = GridLayoutManager(requireContext(), 2) // 그리드 형태로 표시 (2열)
                adapter = groupAdapter
            }
            
            Log.d(TAG, "그룹 RecyclerView 설정 완료")
        } catch (e: Exception) {
            Log.e(TAG, "RecyclerView 설정 중 오류 발생", e)
            Toast.makeText(
                requireContext(),
                "목록을 불러오는 중 오류가 발생했습니다",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showCreateGroupDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_group, null)
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("새 그룹 만들기")
            .setView(dialogView)
            .setPositiveButton("생성") { dialog, _ ->
                val groupNameEditText = dialogView.findViewById<android.widget.EditText>(R.id.groupNameEditText)
                val groupName = groupNameEditText.text.toString().trim()
                
                if (groupName.isNotEmpty()) {
                    // 네트워크 연결 확인
                    val connectivityManager = requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                    
                    val isConnected = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        val network = connectivityManager.activeNetwork
                        val capabilities = connectivityManager.getNetworkCapabilities(network)
                        capabilities != null && (
                            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        val networkInfo = connectivityManager.activeNetworkInfo
                        @Suppress("DEPRECATION")
                        networkInfo != null && networkInfo.isConnected
                    }
                    
                    if (!isConnected) {
                        Toast.makeText(requireContext(), "인터넷 연결이 필요합니다. 네트워크 연결을 확인해주세요.", Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                    
                    // 로딩 표시
                    val loadingToast = Toast.makeText(requireContext(), "그룹을 생성 중입니다...", Toast.LENGTH_LONG)
                    loadingToast.show()
                    
                    // 그룹 생성 요청
                    viewModel.createGroup(groupName)
                } else {
                    Toast.makeText(requireContext(), "그룹 이름을 입력해주세요", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            
        builder.create().show()
    }

    // 인증 상태 확인 함수
    private fun checkAuthStatus() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        if (currentUser == null) {
            // 로그인되지 않은 상태
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }
        
        // 사용자 토큰 검증
        currentUser.getIdToken(true)
            .addOnSuccessListener { 
                Log.d("GroupListFragment", "토큰 갱신 성공: ${currentUser.uid}")
                // 토큰 갱신 성공 시 그룹 목록 로드
                viewModel.loadGroups()
            }
            .addOnFailureListener { e ->
                Log.e("GroupListFragment", "토큰 갱신 실패", e)
                showReLoginDialog()
            }
    }
    
    // 재로그인 다이얼로그 표시
    private fun showReLoginDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("재로그인 필요")
            .setMessage("세션이 만료되었거나 권한이 부족합니다. 다시 로그인하시겠습니까?")
            .setPositiveButton("로그인") { _, _ ->
                navigateToLogin()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
    
    // 로그인 화면으로 이동
    private fun navigateToLogin() {
        viewModel.logout()
        val intent = Intent(requireContext(), com.example.capstone.ui.auth.AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// 그룹 목록을 표시하는 RecyclerView 어댑터
class GroupAdapter(private val onItemClick: (Group) -> Unit) :
    RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    private var groupList: List<Group> = emptyList()

    fun submitList(groups: List<Group>) {
        this.groupList = groups
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groupList[position])
    }

    override fun getItemCount(): Int = groupList.size

    inner class GroupViewHolder(private val binding: ItemGroupCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(groupList[position])
                }
            }
        }

        fun bind(group: Group) {
            binding.groupNameTextView.text = group.groupName
            binding.groupTagTextView.text = group.groupTag
            
            // 그룹 아이콘 로드 (Glide 사용)
            try {
                // 기본 옵션 설정
                val options = RequestOptions()
                    .placeholder(R.drawable.ic_group_placeholder)
                    .error(R.drawable.ic_group_placeholder)
                
                Glide.with(binding.root.context)
                    .load(group.groupIcon.ifEmpty { R.drawable.ic_group_placeholder })
                    .apply(options)
                    .into(binding.groupIconImageView)
            } catch (e: Exception) {
                Log.e("GroupAdapter", "이미지 로드 실패: ${e.message}")
                binding.groupIconImageView.setImageResource(R.drawable.ic_group_placeholder)
            }
        }
    }
} 
