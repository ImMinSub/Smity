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
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.capstone.R
import com.example.capstone.data.User
import com.example.capstone.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private val TAG = "ProfileFragment"
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            Log.d(TAG, "onCreateView 시작")
            _binding = FragmentProfileBinding.inflate(inflater, container, false)
            return binding.root
        } catch (e: Exception) {
            Log.e(TAG, "onCreateView 오류: ${e.message}", e)
            return View(context)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            Log.d(TAG, "onViewCreated 시작")
            
            // 사용자 정보 새로고침
            viewModel.refreshUserProfile()
            
            // 현재 사용자 정보 로드
            viewModel.currentUser.observe(viewLifecycleOwner) { user ->
                if (user != null) {
                    Log.d(TAG, "사용자 정보 업데이트: ${user.email}")
                    updateProfileUI(user)
                } else {
                    Log.e(TAG, "사용자 정보가 null입니다.")
                    Toast.makeText(context, "사용자 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
                }
            }
            
            // 버튼 클릭 이벤트 설정
            setupClickListeners()
            
            Log.d(TAG, "onViewCreated 완료")
        } catch (e: Exception) {
            Log.e(TAG, "onViewCreated 오류: ${e.message}", e)
            Toast.makeText(context, "프로필 화면 초기화 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateProfileUI(user: User) {
        try {
            // 사용자 이름과 이메일 표시
            binding.userNameText.text = user.username
            binding.userEmailText.text = user.email
            
            // 나이와 MBTI 정보 표시
            // 나이가 null이거나 0 이하면 "미설정"으로 표시
            val ageText = if (user.age != null && user.age > 0) user.age.toString() else "미설정"
            
            // MBTI가 비어있거나 형식에 맞지 않으면 "미설정"으로 표시
            val mbtiPattern = "^(INTJ|INTP|ENTJ|ENTP|INFJ|INFP|ENFJ|ENFP|ISTJ|ISFJ|ESTJ|ESFJ|ISTP|ISFP|ESTP|ESFP)$"
            val mbtiText = if (user.mbti.isNotEmpty() && user.mbti.matches(mbtiPattern.toRegex())) 
                user.mbti 
            else 
                "미설정"
            
            // 프로필 정보 표시용 TextView 업데이트
            binding.userAgeText.text = "나이: $ageText"
            binding.userMbtiText.text = "MBTI: $mbtiText"
            
            Log.d(TAG, "프로필 UI 업데이트 완료: ${user.username}, 이메일: ${user.email}, 나이: $ageText, MBTI: $mbtiText")
        } catch (e: Exception) {
            Log.e(TAG, "updateProfileUI 오류: ${e.message}", e)
        }
    }
    
    private fun setupClickListeners() {
        try {
            // 프로필 이미지 클릭 이벤트
            binding.profileImage.setOnClickListener {
                try {
                    Toast.makeText(context, "프로필 이미지 클릭됨", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "프로필 이미지 클릭 오류: ${e.message}", e)
                }
            }
            
            // 로그아웃 버튼 클릭 이벤트
            binding.logoutButton.setOnClickListener {
                try {
                    // 확인 대화상자 표시
                    AlertDialog.Builder(requireContext())
                        .setTitle("로그아웃")
                        .setMessage("정말 로그아웃하시겠습니까?")
                        .setPositiveButton("예") { _, _ ->
                            try {
                                // 로그아웃 처리
                                viewModel.logout()
                                
                                // AuthActivity로 이동
                                val intent = Intent(requireActivity(), Class.forName("com.example.capstone.ui.auth.AuthActivity"))
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                requireActivity().finish()
                            } catch (e: Exception) {
                                Log.e(TAG, "로그아웃 처리 중 오류 발생: ${e.message}", e)
                                Toast.makeText(context, "로그아웃 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("아니오", null)
                        .show()
                } catch (e: Exception) {
                    Log.e(TAG, "로그아웃 버튼 클릭 오류: ${e.message}", e)
                    Toast.makeText(context, "로그아웃 처리 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                }
            }
            
            // 프로필 편집 클릭 이벤트
            binding.editProfileLayout.setOnClickListener {
                try {
                    showEditProfileDialog()
                } catch (e: Exception) {
                    Log.e(TAG, "프로필 편집 클릭 오류: ${e.message}", e)
                    Toast.makeText(context, "프로필 편집 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                }
            }
            
            // 알림 설정 클릭 이벤트
            binding.notificationSettingsLayout.setOnClickListener {
                try {
                    Toast.makeText(context, "알림 설정 클릭됨", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "알림 설정 클릭 오류: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "setupClickListeners 오류: ${e.message}", e)
        }
    }
    
    private fun showEditProfileDialog() {
        try {
            val currentUser = viewModel.currentUser.value ?: run {
                Log.e(TAG, "프로필 편집 실패: 현재 사용자 정보가 없습니다")
                Toast.makeText(context, "사용자 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
                return
            }
            
            // 레이아웃 인플레이트 시 예외 처리 강화
            val dialogView = try {
                LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)
            } catch (e: Exception) {
                Log.e(TAG, "대화상자 레이아웃 인플레이트 오류: ${e.message}", e)
                Toast.makeText(context, "대화상자를 표시할 수 없습니다", Toast.LENGTH_SHORT).show()
                return
            }
            
            // UI 요소 찾기 시 예외 처리 추가
            val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText) ?: run {
                Log.e(TAG, "nameEditText를 찾을 수 없습니다")
                return
            }
            val ageEditText = dialogView.findViewById<EditText>(R.id.ageEditText) ?: run {
                Log.e(TAG, "ageEditText를 찾을 수 없습니다")
                return
            }
            val mbtiEditText = dialogView.findViewById<EditText>(R.id.mbtiEditText) ?: run {
                Log.e(TAG, "mbtiEditText를 찾을 수 없습니다")
                return
            }
            
            // 현재 값 설정
            nameEditText.setText(currentUser.username)
            currentUser.age?.let { ageEditText.setText(it.toString()) }
            mbtiEditText.setText(currentUser.mbti)
            
            // 대화상자 생성 및 표시
            try {
                AlertDialog.Builder(requireContext())
                    .setTitle("프로필 편집")
                    .setView(dialogView)
                    .setPositiveButton("저장") { _, _ ->
                        try {
                            val updatedName = nameEditText.text.toString().trim()
                            val updatedAge = ageEditText.text.toString().trim()
                            val updatedMbti = mbtiEditText.text.toString().trim().uppercase()
                            
                            // 입력값 검증
                            if (updatedName.isEmpty()) {
                                Toast.makeText(context, "이름은 비워둘 수 없습니다", Toast.LENGTH_SHORT).show()
                                return@setPositiveButton
                            }
                            
                            // 나이는 숫자로 변환 가능한지 확인
                            val ageValue = if (updatedAge.isNotEmpty()) {
                                try {
                                    updatedAge.toInt()
                                } catch (e: NumberFormatException) {
                                    Toast.makeText(context, "나이는 숫자만 입력 가능합니다", Toast.LENGTH_SHORT).show()
                                    return@setPositiveButton
                                }
                            } else null
                            
                            // MBTI 형식 검증
                            val mbtiPattern = "^(INTJ|INTP|ENTJ|ENTP|INFJ|INFP|ENFJ|ENFP|ISTJ|ISFJ|ESTJ|ESFJ|ISTP|ISFP|ESTP|ESFP|)$"
                            if (updatedMbti.isNotEmpty() && !updatedMbti.matches(mbtiPattern.toRegex())) {
                                // 형식이 맞지 않으면 경고만 하고 계속 진행
                                Log.w(TAG, "MBTI 형식이 올바르지 않습니다: $updatedMbti")
                            }
                            
                            // 업데이트할 사용자 객체 생성
                            val updatedUser = currentUser.copy(
                                username = updatedName,
                                age = ageValue,
                                mbti = updatedMbti
                            )
                            
                            // 사용자 정보 업데이트
                            viewModel.updateUserProfile(updatedUser)
                            
                            // 성공 메시지 표시
                            Toast.makeText(context, "프로필이 업데이트되었습니다", Toast.LENGTH_SHORT).show()
                            
                            // 업데이트된 정보로 UI 갱신
                            updateProfileUI(updatedUser)
                        } catch (e: Exception) {
                            Log.e(TAG, "프로필 정보 저장 중 오류 발생: ${e.message}", e)
                            Toast.makeText(context, "프로필 저장 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("취소", null)
                    .show()
            } catch (e: Exception) {
                Log.e(TAG, "대화상자 표시 중 오류 발생: ${e.message}", e)
                Toast.makeText(context, "대화상자를 표시할 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "showEditProfileDialog 오류: ${e.message}", e)
            Toast.makeText(context, "프로필 편집을 시작할 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView 시작")
        _binding = null
        super.onDestroyView()
    }
}

