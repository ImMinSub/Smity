package com.example.capstone

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CapstoneApplication : Application() {
    
    companion object {
        private const val TAG = "CapstoneApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 성능 측정 시작
        val startTime = System.currentTimeMillis()
        
        // Firebase 초기화
        initializeFirebase()
        
        // UI 모드 설정 (다크 모드 사용자 설정에 따름)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        // 초기화 시간 로깅
        val initTime = System.currentTimeMillis() - startTime
        Log.d(TAG, "애플리케이션 초기화 완료: ${initTime}ms 소요")
    }
    
    private fun initializeFirebase() {
        try {
            // Firebase 초기화
            FirebaseApp.initializeApp(this)
            
            // Firestore 설정 최적화
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)  // 오프라인 캐싱 활성화
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)  // 캐시 크기 제한 없음
                .build()
            
            FirebaseFirestore.getInstance().firestoreSettings = settings
            
            Log.d(TAG, "Firebase 초기화 완료")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase 초기화 실패", e)
        }
    }
}
