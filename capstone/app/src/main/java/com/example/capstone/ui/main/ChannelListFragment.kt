package com.example.capstone.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.capstone.R
import com.example.capstone.databinding.FragmentChannelListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChannelListFragment : Fragment() {

    private var _binding: FragmentChannelListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChannelListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 채널 목록 관찰
        viewModel.channels.observe(viewLifecycleOwner) { channels ->
            // 채널 어댑터 업데이트
        }

        // 채널 생성 버튼 클릭 리스너
        binding.createChannelButton.setOnClickListener {
            findNavController().navigate(R.id.action_channelListFragment_to_createChannelFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

