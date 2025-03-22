package com.example.capstone.ui.main

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.capstone.databinding.FragmentCreateServerBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateServerFragment : Fragment() {

    private var _binding: FragmentCreateServerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private var selectedImageUri: Uri? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.serverIconImageView.setImageURI(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateServerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 이미지 선택 버튼 클릭 리스너
        binding.selectImageButton.setOnClickListener {
            getContent.launch("image/*")
        }

        // 서버 생성 버튼 클릭 리스너
        binding.createServerButton.setOnClickListener {
            val serverName = binding.serverNameEditText.text.toString()
            if (serverName.isNotEmpty()) {
                viewModel.createServer(serverName, selectedImageUri)
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

