package com.example.capstone.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import com.example.capstone.databinding.ActivityAuthBinding
import com.example.capstone.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 로그인 상태 관찰
        viewModel.isLoggedIn.observe(this) { isLoggedIn ->
            if (isLoggedIn) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}