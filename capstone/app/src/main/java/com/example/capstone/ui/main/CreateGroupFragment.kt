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
import com.example.capstone.databinding.FragmentCreateGroupBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateGroupFragment : Fragment() {

    private var _binding: FragmentCreateGroupBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private var selectedImageUri: Uri? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.groupIconImageView.setImageURI(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 이미지 선택 버튼 클릭 리스너
        binding.selectImageButton.setOnClickListener {
            getContent.launch("image/*")
        }

        // 그룹 생성 버튼 클릭 리스너
        binding.createGroupButton.setOnClickListener {
            val groupName = binding.groupNameEditText.text.toString()
            if (groupName.isNotEmpty()) {
                viewModel.createGroup(groupName)
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
