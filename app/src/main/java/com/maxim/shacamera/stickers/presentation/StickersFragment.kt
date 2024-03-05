package com.maxim.shacamera.stickers.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.maxim.shacamera.core.sl.ProvideViewModel
import com.maxim.shacamera.databinding.FragmentStickersBinding

class StickersFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentStickersBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: StickersViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStickersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = (requireActivity() as ProvideViewModel).viewModel(StickersViewModel::class.java)

        val adapter = StickersAdapter(object : StickersAdapter.Listener {
            override fun onClick(drawableId: Int) {
                viewModel.create(drawableId)
            }
        })
        binding.stickersRecyclerView.adapter = adapter
        binding.stickersRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        viewModel.observe(this) {
            it.show(adapter)
        }

        viewModel.init()
    }

    override fun onDestroyView() {
        _binding = null
        viewModel.clear()
        super.onDestroyView()
    }
}