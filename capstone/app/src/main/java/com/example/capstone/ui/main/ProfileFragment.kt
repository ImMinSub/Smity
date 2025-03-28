package com.example.capstone.ui.main

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.capstone.R
import com.example.capstone.data.User
import com.example.capstone.databinding.FragmentProfileBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private val TAG = "ProfileFragment"
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated 시작")
        
        try {
            // 프로필 정보 로드 및 표시
            setupProfileInfo()
            
            // 로그아웃 버튼 설정
            setupLogoutButton()
            
            // 프로필 편집 기능 설정
            setupEditProfileButton()
            
            Log.d(TAG, "onViewCreated 완료")
        } catch (e: Exception) {
            Log.e(TAG, "onViewCreated 오류: ${e.message}", e)
            Toast.makeText(requireContext(), "프로필 화면 초기화 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupProfileInfo() {
        Log.d(TAG, "setupProfileInfo 시작")
        
        try {
            // 기존 사용자 정보를 Firebase에서 직접 가져와 표시
            viewModel.getCurrentUserDirectly()?.let { user ->
                updateProfileUI(user)
                Log.d(TAG, "초기 사용자 정보 설정: ${user.email}")
            }
            
            // DB에서 최신 정보 가져오기
            viewModel.refreshUserProfile()
            
            // LiveData 관찰하여 UI 업데이트
            viewModel.currentUser.observe(viewLifecycleOwner) { user ->
                if (user != null) {
                    updateProfileUI(user)
                    Log.d(TAG, "사용자 정보 업데이트됨")
                } else {
                    Log.w(TAG, "사용자 정보가 null입니다")
                }
            }
            
            Log.d(TAG, "setupProfileInfo 완료")
        } catch (e: Exception) {
            Log.e(TAG, "setupProfileInfo 오류: ${e.message}", e)
        }
    }
    
    private fun updateProfileUI(user: User) {
        try {
            // 사용자 이름과 이메일 표시
            binding.userNameText.text = user.username
            binding.userEmailText.text = user.email
            
            // 나이와 MBTI 정보 표시
            val ageText = if (user.age != null && user.age!! > 0) user.age.toString() else "미설정"
            val mbtiText = if (user.mbti.isNotEmpty()) user.mbti else "미설정"
            
            binding.userAgeText.text = "나이: $ageText"
            binding.userMbtiText.text = "MBTI: $mbtiText"
            
            // 프로필 이미지 로드
            if (user.profileImageUrl.isNotEmpty()) {
                Glide.with(requireContext())
                    .load(user.profileImageUrl)
                    .placeholder(R.drawable.ic_error)
                    .error(R.drawable.ic_error)
                    .apply(RequestOptions.circleCropTransform())
                    .into(binding.profileImage)
            }
            
            Log.d(TAG, "updateProfileUI 완료: ${user.username}, 이메일: ${user.email}")
        } catch (e: Exception) {
            Log.e(TAG, "updateProfileUI 오류: ${e.message}", e)
        }
    }
    
    private fun setupLogoutButton() {
        binding.logoutButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("로그아웃")
                .setMessage("정말 로그아웃하시겠습니까?")
                .setPositiveButton("예") { _, _ ->
                    try {
                        viewModel.logout()
                        navigateToAuthActivity()
                    } catch (e: Exception) {
                        Log.e(TAG, "로그아웃 처리 중 오류: ${e.message}", e)
                        Toast.makeText(requireContext(), "로그아웃 처리 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("아니오", null)
                .show()
        }
    }
    
    private fun setupEditProfileButton() {
        // 프로필 편집 버튼 또는 카드 클릭 이벤트 설정
        binding.editProfileLayout.setOnClickListener {
            showEditProfileDialog()
        }
    }
    
    private fun showEditProfileDialog() {
        Log.d(TAG, "showEditProfileDialog 시작")
        
        try {
            // 현재 사용자 정보 가져오기
            val currentUser = viewModel.currentUser.value ?: viewModel.getCurrentUserDirectly()
            
            if (currentUser == null) {
                Log.e(TAG, "사용자 정보를 찾을 수 없습니다")
                Toast.makeText(requireContext(), "사용자 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
                return
            }
            
            // 다이얼로그 뷰 생성
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)
            
            // 다이얼로그 UI 요소
            val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)
            val ageEditText = dialogView.findViewById<EditText>(R.id.ageEditText)
            val mbtiEditText = dialogView.findViewById<EditText>(R.id.mbtiEditText)
            
            // 현재 값으로 초기화
            nameEditText.setText(currentUser.username)
            currentUser.age?.let { ageEditText.setText(it.toString()) }
            mbtiEditText.setText(currentUser.mbti)
            
            // 다이얼로그 생성 및 표시
            AlertDialog.Builder(requireContext())
                .setTitle("프로필 편집")
                .setView(dialogView)
                .setPositiveButton("저장") { _, _ ->
                    try {
                        val updatedName = nameEditText.text.toString().trim()
                        val updatedAgeText = ageEditText.text.toString().trim()
                        val updatedMbti = mbtiEditText.text.toString().trim().uppercase()
                        
                        // 입력값 검증
                        if (updatedName.isEmpty()) {
                            Toast.makeText(requireContext(), "이름은 비워둘 수 없습니다", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        
                        // 나이는 숫자로 변환 가능한지 확인
                        val updatedAge = if (updatedAgeText.isNotEmpty()) {
                            try {
                                updatedAgeText.toInt()
                            } catch (e: NumberFormatException) {
                                Toast.makeText(requireContext(), "나이는 숫자만 입력 가능합니다", Toast.LENGTH_SHORT).show()
                                return@setPositiveButton
                            }
                        } else null
                        
                        // 업데이트할 사용자 객체 생성 (깊은 복사)
                        val updatedUser = currentUser.copy(
                            username = updatedName,
                            age = updatedAge,
                            mbti = updatedMbti
                        )
                        
                        // 사용자 프로필 업데이트
                        viewModel.updateUserProfile(updatedUser)
                        
                        // 성공 메시지 표시
                        Toast.makeText(requireContext(), "프로필이 업데이트되었습니다", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "프로필 업데이트 성공")
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "프로필 업데이트 중 오류: ${e.message}", e)
                        Toast.makeText(requireContext(), "프로필 업데이트 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("취소", null)
                .show()
            
            Log.d(TAG, "showEditProfileDialog 완료")
        } catch (e: Exception) {
            Log.e(TAG, "showEditProfileDialog 오류: ${e.message}", e)
            Toast.makeText(requireContext(), "프로필 편집 다이얼로그를 표시할 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToAuthActivity() {
        val intent = Intent(requireContext(), com.example.capstone.ui.auth.AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView 완료")
    }
}

