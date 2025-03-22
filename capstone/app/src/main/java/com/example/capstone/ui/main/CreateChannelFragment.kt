package com.example.capstone.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.capstone.R
import com.example.capstone.databinding.FragmentCreateChannelBinding
import com.example.capstone.util.Constants
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateChannelFragment : Fragment() {

    private var _binding: FragmentCreateChannelBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateChannelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 채널 타입 스피너 설정
        val channelTypes = listOf(Constants.TEXT_CHANNEL, Constants.VOICE_CHANNEL)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, channelTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.channelTypeSpinner.adapter = adapter

        // 채널 생성 버튼 클릭 리스너
        binding.createChannelButton.setOnClickListener {
            val channelName = binding.channelNameEditText.text.toString()
            val channelType = binding.channelTypeSpinner.selectedItem.toString()
            
            if (channelName.isNotEmpty()) {
                viewModel.currentServer.value?.serverId?.let { serverId ->
                    viewModel.createChannel(serverId, channelName, channelType)
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

