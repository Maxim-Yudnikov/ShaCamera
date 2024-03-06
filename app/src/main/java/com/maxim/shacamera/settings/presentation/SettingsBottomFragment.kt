package com.maxim.shacamera.settings.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
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

        var byUser = false
        val rtxListener = object :AdapterView.OnItemSelectedListener,View.OnTouchListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (byUser)
                    viewModel.setRtx(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                byUser = false
            }

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                byUser = true
                return false
            }

        }
        binding.rtxSpinner.onItemSelectedListener = rtxListener
        binding.rtxSpinner.setOnTouchListener(rtxListener)

        val dlssListener = object :AdapterView.OnItemSelectedListener,View.OnTouchListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (byUser)
                    viewModel.setDlss(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                byUser = false
            }

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                byUser = true
                return false
            }

        }
        binding.dlssSpinner.onItemSelectedListener = dlssListener
        binding.dlssSpinner.setOnTouchListener(dlssListener)

        val fsrListener = object :AdapterView.OnItemSelectedListener,View.OnTouchListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (byUser)
                    viewModel.setFsr(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                byUser = false
            }

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                byUser = true
                return false
            }

        }
        binding.fsrSpinner.onItemSelectedListener = fsrListener
        binding.fsrSpinner.setOnTouchListener(fsrListener)



        viewModel.observe(this) {
            it.show(binding.radioGroup, binding.rtxSpinner, binding.dlssSpinner, binding.fsrSpinner)
        }

        viewModel.init()
    }


    override fun onDestroyView() {
        _binding = null
        viewModel.clear()
        super.onDestroyView()
    }
}