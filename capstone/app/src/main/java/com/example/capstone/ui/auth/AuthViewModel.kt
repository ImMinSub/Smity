package com.example.capstone.ui.auth

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.capstone.data.User
import com.example.capstone.firebase.FirebaseSource
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
                createUserProfile(email, username)
            }
        }
    }
    
    private fun createUserProfile(email: String, username: String) {
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
                } else {
                    Log.e("AuthViewModel", "사용자 ID가 null입니다.")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "사용자 프로필 생성 중 오류 발생", e)
            }
        }
    }

    fun login(email: String, password: String, activity: Activity) {
        firebaseSource.loginUser(email, password, activity)
    }
}