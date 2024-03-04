package com.maxim.shacamera.settings.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.maxim.shacamera.core.sl.ProvideViewModel
import com.maxim.shacamera.databinding.FragmentSettingsBinding

class SettingsBottomFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SettingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = (requireActivity() as ProvideViewModel).viewModel(SettingsViewModel::class.java)

        binding.radioGroup.children.forEachIndexed { i, v ->
            v.setOnClickListener {
                viewModel.setRatio(i)
            }
        }

        binding.rtxSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setFilter(isChecked, 0)
        }

        viewModel.observe(this) {
            it.show(binding.radioGroup, binding.rtxSwitch)
        }

        viewModel.init()
    }


    override fun onDestroyView() {
        _binding = null
        viewModel.clear()
        super.onDestroyView()
    }
}