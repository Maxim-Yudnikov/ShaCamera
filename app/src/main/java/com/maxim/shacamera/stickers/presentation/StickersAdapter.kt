package com.maxim.shacamera.stickers.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.maxim.shacamera.databinding.StickerLayoutBinding

class StickersAdapter(
    private val listener: Listener
) : RecyclerView.Adapter<StickersAdapter.ViewHolder>() {
    private val list = mutableListOf<StickerUi>()

    class ViewHolder(private val binding: StickerLayoutBinding, private val listener: Listener) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StickerUi) {
            item.showPreview(binding.stickerPreview)
            binding.root.setOnClickListener {
                item.onClick(listener)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            StickerLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ), listener
        )
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    fun update(newList: List<StickerUi>) {
        val diff = StickerDiffUtil(list, newList)
        val result = DiffUtil.calculateDiff(diff)
        list.clear()
        list.addAll(newList)
        result.dispatchUpdatesTo(this)
    }

    interface Listener {
        fun onClick(drawableId: Int)
    }
}

class StickerDiffUtil(
    private val oldList: List<StickerUi>,
    private val newList: List<StickerUi>,
): DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition] == newList[newItemPosition]

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = true
}