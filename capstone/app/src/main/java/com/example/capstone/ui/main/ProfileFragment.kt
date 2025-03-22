package com.example.capstone.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.capstone.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private val TAG = "ProfileFragment"
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

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
            
            // 프로필 정보 설정
            setupProfileInfo()
            
            // 버튼 클릭 이벤트 설정
            setupClickListeners()
            
            Log.d(TAG, "onViewCreated 완료")
        } catch (e: Exception) {
            Log.e(TAG, "onViewCreated 오류: ${e.message}", e)
            Toast.makeText(context, "프로필 화면 초기화 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupProfileInfo() {
        try {
            // 사용자 정보 표시
            binding.userNameText.text = "사용자 이름"
            binding.userEmailText.text = "user@example.com"
        } catch (e: Exception) {
            Log.e(TAG, "setupProfileInfo 오류: ${e.message}", e)
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
                    Toast.makeText(context, "로그아웃 클릭됨", Toast.LENGTH_SHORT).show()
                    // 로그아웃 로직 구현
                } catch (e: Exception) {
                    Log.e(TAG, "로그아웃 버튼 클릭 오류: ${e.message}", e)
                }
            }
            
            // 프로필 편집 클릭 이벤트
            binding.editProfileLayout.setOnClickListener {
                try {
                    Toast.makeText(context, "프로필 편집 클릭됨", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "프로필 편집 클릭 오류: ${e.message}", e)
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

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView 시작")
        _binding = null
        super.onDestroyView()
    }
}

