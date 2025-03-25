package com.example.capstone.ui.auth

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.capstone.R
import com.example.capstone.databinding.FragmentRegisterBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {
    private val TAG = "RegisterFragment"
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            _binding = FragmentRegisterBinding.inflate(inflater, container, false)
            return binding.root
        } catch (e: Exception) {
            Log.e(TAG, "onCreateView 오류: ${e.message}", e)
            return View(context)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            // 로딩 상태 관찰
            viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.registerButton.isEnabled = !isLoading
            }

            // 로그인 상태 관찰
            viewModel.isLoggedIn.observe(viewLifecycleOwner) { isLoggedIn ->
                if (isLoggedIn) {
                    // 이미 로그인 상태라면 메인 화면으로 이동
                    viewModel.navigateToMainActivity(requireActivity())
                }
            }

            // 회원가입 버튼 클릭 리스너
            binding.registerButton.setOnClickListener {
                try {
                    val username = binding.usernameEditText.text.toString().trim()
                    val email = binding.emailEditText.text.toString().trim()
                    val password = binding.passwordEditText.text.toString()
                    val confirmPassword = binding.confirmPasswordEditText.text.toString()

                    // 입력값 유효성 검사
                    when {
                        TextUtils.isEmpty(username) -> {
                            binding.usernameEditText.error = "사용자 이름을 입력해주세요"
                            binding.usernameEditText.requestFocus()
                            return@setOnClickListener
                        }
                        TextUtils.isEmpty(email) -> {
                            binding.emailEditText.error = "이메일을 입력해주세요"
                            binding.emailEditText.requestFocus()
                            return@setOnClickListener
                        }
                        !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                            binding.emailEditText.error = "유효한 이메일 주소를 입력해주세요"
                            binding.emailEditText.requestFocus()
                            return@setOnClickListener
                        }
                        TextUtils.isEmpty(password) -> {
                            binding.passwordEditText.error = "비밀번호를 입력해주세요"
                            binding.passwordEditText.requestFocus()
                            return@setOnClickListener
                        }
                        password.length < 6 -> {
                            binding.passwordEditText.error = "비밀번호는 최소 6자 이상이어야 합니다"
                            binding.passwordEditText.requestFocus()
                            return@setOnClickListener
                        }
                        TextUtils.isEmpty(confirmPassword) -> {
                            binding.confirmPasswordEditText.error = "비밀번호 확인을 입력해주세요"
                            binding.confirmPasswordEditText.requestFocus()
                            return@setOnClickListener
                        }
                        password != confirmPassword -> {
                            binding.confirmPasswordEditText.error = "비밀번호가 일치하지 않습니다"
                            binding.confirmPasswordEditText.requestFocus()
                            return@setOnClickListener
                        }
                        else -> {
                            // 모든 유효성 검사 통과 시 회원가입 시도
                            viewModel.register(email, password, username, requireActivity())
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "회원가입 버튼 클릭 처리 중 오류: ${e.message}", e)
                    Toast.makeText(context, "회원가입 처리 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                }
            }

            // 로그인 화면으로 돌아가기 링크 클릭 리스너
            binding.loginLink.setOnClickListener {
                try {
                    findNavController().navigateUp()
                } catch (e: Exception) {
                    Log.e(TAG, "로그인 링크 클릭 처리 중 오류: ${e.message}", e)
                    Toast.makeText(context, "로그인 화면으로 이동할 수 없습니다", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onViewCreated 오류: ${e.message}", e)
            Toast.makeText(context, "화면 초기화 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
