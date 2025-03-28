package com.example.capstone.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.capstone.databinding.FragmentGroupChannelsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GroupChannelsFragment : Fragment() {
    private val TAG = "GroupChannelsFragment"
    private var _binding: FragmentGroupChannelsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            Log.d(TAG, "onCreateView 시작")
            _binding = FragmentGroupChannelsBinding.inflate(inflater, container, false)
            return binding.root
        } catch (e: Exception) {
            Log.e(TAG, "onCreateView 오류: ${e.message}", e)
            // 오류 발생 시 빈 뷰 반환
            return View(context)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            Log.d(TAG, "onViewCreated 시작")
            
            setupToolbar()
            setupChannelsList()
            setupViewPager()
            
            Log.d(TAG, "onViewCreated 완료")
        } catch (e: Exception) {
            Log.e(TAG, "onViewCreated 오류: ${e.message}", e)
        }
    }

    private fun setupToolbar() {
        try {
            // 그룹 이름 설정
            binding.groupNameText.text = "Group #001"
            
            // 드롭다운 아이콘 클릭 이벤트
            binding.dropdownIcon.setOnClickListener {
                Log.d(TAG, "드롭다운 아이콘 클릭됨")
                // 그룹 메뉴 표시 로직
            }
        } catch (e: Exception) {
            Log.e(TAG, "setupToolbar 오류: ${e.message}", e)
        }
    }

    private fun setupChannelsList() {
        try {
            binding.channelsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                // 채널 어댑터 설정
                // adapter = ChannelsAdapter(channels)
            }
        } catch (e: Exception) {
            Log.e(TAG, "setupChannelsList 오류: ${e.message}", e)
        }
    }

    private fun setupViewPager() {
        try {
            binding.horizontalPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    Log.d(TAG, "페이지 선택됨: $position")
                    // 페이지 전환 시 UI 업데이트
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "setupViewPager 오류: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView 시작")
        _binding = null
        super.onDestroyView()
    }
} 
