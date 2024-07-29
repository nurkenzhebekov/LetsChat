package com.example.letschat.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.letschat.databinding.ItemChatLeftBinding
import com.example.letschat.databinding.ItemChatRightBinding
import com.example.letschat.models.ChatRoomModel
import com.example.letschat.utils.DateTimeUtil
import com.example.letschat.utils.MessageType
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

private const val TEXT_LEFT = 1
private const val TEXT_RIGHT = 2
private const val IMAGE_LEFT = 3
private const val IMAGE_RIGHT = 4

class ChatListRecyclerAdapter : RecyclerView.Adapter<ViewHolder>() {

    private var chatModelList = mutableListOf<ChatRoomModel.Companion.ChatModel>()
    private var sizeOfCurrentRoomUsers = 0

    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<ChatRoomModel.Companion.ChatModel>, size: Int) {
        sizeOfCurrentRoomUsers = size
        chatModelList.clear()
        chatModelList.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return when (viewType) {
            TEXT_LEFT -> {
                val inflater = LayoutInflater.from(parent.context)
                val binding = ItemChatLeftBinding.inflate(inflater, parent, false)
                TextLeftViewHolder(binding)
            }
            else -> {
                val inflater = LayoutInflater.from(parent.context)
                val binding = ItemChatRightBinding.inflate(inflater, parent, false)
                TextRightViewHolder(binding)
            }
        }

    }

    override fun getItemCount(): Int = chatModelList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        if (chatModelList[position].fromUid == Firebase.auth.uid)
            (holder as TextRightViewHolder).bind(chatModelList[position])
        else
            (holder as TextLeftViewHolder).bind(chatModelList[position])

    }

    override fun getItemViewType(position: Int): Int {
        return if (chatModelList[position].fromUid == Firebase.auth.uid) {
            if (chatModelList[position].msgType == MessageType.TEXT.name) TEXT_RIGHT else IMAGE_RIGHT
        } else {
            if (chatModelList[position].msgType == MessageType.TEXT.name) TEXT_LEFT else IMAGE_LEFT
        }
    }

    inner class TextLeftViewHolder(private val binding: ItemChatLeftBinding) : ViewHolder(binding.root) {
        fun bind(chat: ChatRoomModel.Companion.ChatModel) {
            binding.apply {
                chat.also {
                    itemUsername.text = it.fromUid
                    itemMessage.text = it.message
                    itemTimestamp.text = DateTimeUtil.getFormattedTime(it.timeStamp)
                }
            }
        }
    }

    inner class TextRightViewHolder(private val binding: ItemChatRightBinding) : ViewHolder(binding.root) {
        fun bind(chat: ChatRoomModel.Companion.ChatModel) {
            binding.apply {
                chat.also {
                    itemMessage.text = it.message
                    itemTimestamp.text = DateTimeUtil.getFormattedTime(it.timeStamp)
                }
            }
        }
    }
}