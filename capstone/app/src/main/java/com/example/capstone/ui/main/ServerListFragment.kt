package com.example.capstone.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.capstone.R
import com.example.capstone.databinding.FragmentServerListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ServerListFragment : Fragment() {

    private var _binding: FragmentServerListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentServerListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 서버 목록 관찰
        viewModel.servers.observe(viewLifecycleOwner) { servers ->
            // 서버 어댑터 업데이트
        }

        // 서버 생성 버튼 클릭 리스너
        binding.createServerButton.setOnClickListener {
            findNavController().navigate(R.id.action_serverListFragment_to_createServerFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

