package com.example.capstone.ui.auth

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.capstone.data.User
import com.example.capstone.firebase.FirebaseSource
import com.example.capstone.ui.main.MainActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseSource: FirebaseSource
) : ViewModel() {

    val isLoading = firebaseSource.isLoading
    val isLoggedIn = firebaseSource.isLoggedIn
    
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e("AuthViewModel", "코루틴 에러 발생: ${exception.localizedMessage}", exception)
    }

    fun register(email: String, password: String, username: String, activity: Activity) {
        // 사용자 등록 후 프로필 생성 로직
        firebaseSource.registerUser(email, password, activity) { success ->
            if (success) {
                createUserProfile(email, username) {
                    // 회원가입 및 프로필 생성 완료 후 MainActivity로 이동
                    navigateToMainActivity(activity)
                }
            }
        }
    }
    
    private fun createUserProfile(email: String, username: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val userId = firebaseSource.getCurrentUser()?.uid
                if (userId != null) {
                    val user = User(
                        id = userId,
                        username = username,
                        email = email,
                        profileImageUrl = "https://via.placeholder.com/150",
                        status = "온라인"
                    )
                    firebaseSource.createUserProfile(user)
                    onComplete()
                } else {
                    Log.e("AuthViewModel", "사용자 ID가 null입니다.")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "사용자 프로필 생성 중 오류 발생", e)
            }
        }
    }

    fun login(email: String, password: String, activity: Activity) {
        firebaseSource.loginUser(email, password, activity) { success ->
            if (success) {
                // 로그인 성공 시 MainActivity로 이동
                navigateToMainActivity(activity)
            }
        }
    }
    
    fun navigateToMainActivity(activity: Activity) {
        val intent = Intent(activity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
        activity.finish()
    }
}
