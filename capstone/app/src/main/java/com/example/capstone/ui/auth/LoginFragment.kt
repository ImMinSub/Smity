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
import com.example.capstone.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private val TAG = "LoginFragment"
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            _binding = FragmentLoginBinding.inflate(inflater, container, false)
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
                binding.loginButton.isEnabled = !isLoading
            }

            // 로그인 여부 관찰
            viewModel.isLoggedIn.observe(viewLifecycleOwner) { isLoggedIn ->
                if (isLoggedIn) {
                    // 이미 로그인된 상태라면 메인으로 이동
                    viewModel.navigateToMainActivity(requireActivity())
                }
            }

            // 로그인 버튼 클릭 리스너
            binding.loginButton.setOnClickListener {
                try {
                    val email = binding.emailEditText.text.toString().trim()
                    val password = binding.passwordEditText.text.toString()

                    // 입력값 유효성 검사
                    when {
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
                        else -> {
                            // 모든 유효성 검사 통과 시 로그인 시도
                            viewModel.login(email, password, requireActivity())
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "로그인 버튼 클릭 처리 중 오류: ${e.message}", e)
                    Toast.makeText(context, "로그인 처리 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                }
            }

            // 회원가입 링크 클릭 리스너
            binding.registerLink.setOnClickListener {
                try {
                    findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
                } catch (e: Exception) {
                    Log.e(TAG, "회원가입 링크 클릭 처리 중 오류: ${e.message}", e)
                    Toast.makeText(context, "회원가입 화면으로 이동할 수 없습니다", Toast.LENGTH_SHORT).show()
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
