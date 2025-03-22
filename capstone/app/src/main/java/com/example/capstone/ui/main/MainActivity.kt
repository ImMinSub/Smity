package com.example.capstone.ui.main

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.capstone.R
import com.example.capstone.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            setupViewPager()
            setupBottomNavigation()
            
        } catch (e: Exception) {
            Log.e(TAG, "onCreate 오류: ${e.message}", e)
        }
    }

    private fun setupViewPager() {
        val pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.apply {
            adapter = pagerAdapter
            offscreenPageLimit = 4  // 페이지 미리 로드
            isUserInputEnabled = false  // 스와이프로 페이지 변경 비활성화
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_servers -> binding.viewPager.currentItem = 0
                R.id.navigation_friends -> binding.viewPager.currentItem = 1
                R.id.navigation_search -> binding.viewPager.currentItem = 2
                R.id.navigation_notifications -> binding.viewPager.currentItem = 3
                R.id.navigation_profile -> binding.viewPager.currentItem = 4
                else -> return@setOnItemSelectedListener false
            }
            true
        }

        // ViewPager 페이지 변경 시 바텀 네비게이션 아이템 업데이트
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val menuItem = when (position) {
                    0 -> R.id.navigation_servers
                    1 -> R.id.navigation_friends
                    2 -> R.id.navigation_search
                    3 -> R.id.navigation_notifications
                    4 -> R.id.navigation_profile
                    else -> null
                }
                menuItem?.let { binding.bottomNav.selectedItemId = it }
            }
        })
    }

    private inner class MainPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount() = 5

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ServerChannelsFragment()
                1 -> FriendsFragment()
                2 -> SearchFragment()
                3 -> NotificationsFragment()
                4 -> ProfileFragment()
                else -> ServerChannelsFragment()
            }
        }
    }
}