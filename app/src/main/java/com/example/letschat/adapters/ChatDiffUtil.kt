package com.example.letschat.adapters

import androidx.recyclerview.widget.DiffUtil
import com.example.letschat.models.ChatRoomModel

class ChatDiffUtil(
    private val oldItems: List<ChatRoomModel.Companion.ChatModel>,
    private val newItems: List<ChatRoomModel.Companion.ChatModel>
) : DiffUtil.Callback() {



    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = newItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]

        return oldItem.timeStamp == newItem.timeStamp
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]

        return oldItem == newItem
    }
}