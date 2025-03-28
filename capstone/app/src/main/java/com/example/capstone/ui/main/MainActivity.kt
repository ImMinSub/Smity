package com.example.capstone.ui.main

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.capstone.R
import com.example.capstone.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.material.snackbar.Snackbar

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {
    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private var navController: NavController? = null
    private var lastNavigationTime: Long = 0
    private val NAVIGATION_THROTTLE_TIME = 500 // 0.5초

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            setupViewPager()
            setupBottomNavigation()
            setupNavigation()
            
            // 기본적으로 그룹 탭을 보여줌
            binding.bottomNav.selectedItemId = R.id.navigation_groups
            
        } catch (e: Exception) {
            Log.e(TAG, "onCreate 오류: ${e.message}", e)
            Snackbar.make(
                findViewById(android.R.id.content),
                "앱 초기화 중 오류가 발생했습니다: ${e.message}",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }
    
    private fun setupNavigation() {
        try {
            // NavHostFragment 찾기
            val navHost = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                
            if (navHost != null) {
                navController = navHost.navController
                navController?.addOnDestinationChangedListener(this)
                
                Log.d(TAG, "NavController 설정 완료")
            } else {
                Log.w(TAG, "NavHostFragment를 찾을 수 없음")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Navigation 설정 중 오류", e)
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
                R.id.navigation_groups -> binding.viewPager.currentItem = 0
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
                    0 -> R.id.navigation_groups
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

    // 네비게이션 변경 리스너
    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        try {
            val destinationId = destination.id
            val currentTime = System.currentTimeMillis()
            
            // 너무 빠른 네비게이션 방지 (중복 클릭 등)
            if (currentTime - lastNavigationTime < NAVIGATION_THROTTLE_TIME) {
                Log.w(TAG, "빠른 네비게이션 시도 무시: ${destination.label}")
                return
            }
            
            lastNavigationTime = currentTime
            
            // 채팅 화면으로 이동하면 바텀 네비게이션 숨기기
            if (destinationId == R.id.chatFragment) {
                binding.bottomNav.visibility = android.view.View.GONE
            } else if (binding.bottomNav.visibility != android.view.View.VISIBLE) {
                binding.bottomNav.visibility = android.view.View.VISIBLE
            }
            
            Log.d(TAG, "Navigation 변경: ${destination.label}")
        } catch (e: Exception) {
            Log.e(TAG, "Navigation 변경 처리 중 오류", e)
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController?.navigateUp() ?: super.onSupportNavigateUp()
    }
    
    override fun onDestroy() {
        try {
            navController?.removeOnDestinationChangedListener(this)
            binding.viewPager.unregisterOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {})
        } finally {
            super.onDestroy()
        }
    }

    private inner class MainPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount() = 5

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> GroupListFragment()
                1 -> FriendsFragment()
                2 -> SearchFragment()
                3 -> NotificationsFragment()
                4 -> ProfileFragment()
                else -> GroupListFragment()
            }
        }
    }
}
