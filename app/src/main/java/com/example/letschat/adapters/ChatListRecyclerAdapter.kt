package com.example.letschat.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.load
import coil.transform.CircleCropTransformation
import coil.transform.RoundedCornersTransformation
import com.example.letschat.fragments.ChatRoomFragment
import com.example.letschat.R
import com.example.letschat.databinding.ItemChatLeftBinding
import com.example.letschat.databinding.ItemChatRightBinding
import com.example.letschat.databinding.ItemImageLeftBinding
import com.example.letschat.databinding.ItemImageRightBinding
import com.example.letschat.models.ChatRoomModel
import com.example.letschat.models.UserModel
import com.example.letschat.utils.DateTimeUtil
import com.example.letschat.utils.MessageType
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

private const val TEXT_LEFT = 1
private const val TEXT_RIGHT = 2
private const val IMAGE_LEFT = 3
private const val IMAGE_RIGHT = 4

class ChatListRecyclerAdapter : RecyclerView.Adapter<ViewHolder>() {

    private var chatModelList = mutableListOf<ChatRoomModel.Companion.ChatModel>()
    private var sizeOfCurrentRoomUsers = 0

    var onMessageLongClickListener: ((Int) -> Unit)? = null
    var onImageClickListener: ((Int) -> Unit)? = null
    var onImageLongClickListener: ((Int) -> Unit)? = null
    var onUnreadCounterClickListener: ((ChatRoomModel.Companion.ChatModel) -> Unit)? = null

    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<ChatRoomModel.Companion.ChatModel>, size: Int) {
        sizeOfCurrentRoomUsers = size

        val diffCallback = ChatDiffUtil(chatModelList, list)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        chatModelList.clear()
        chatModelList.addAll(list)

        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            TEXT_LEFT -> {
                val binding = ItemChatLeftBinding.inflate(inflater, parent, false)
                TextLeftViewHolder(binding)
            }
            TEXT_RIGHT -> {
                val binding = ItemChatRightBinding.inflate(inflater, parent, false)
                TextRightViewHolder(binding)
            }
            IMAGE_LEFT -> {
                val binding = ItemImageLeftBinding.inflate(inflater, parent, false)
                ImageLeftViewHolder(binding)
            }
            else -> {
                val binding = ItemImageRightBinding.inflate(inflater, parent, false)
                ImageRightViewHolder(binding)
            }
        }

    }

    override fun getItemCount(): Int = chatModelList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        if (chatModelList[position].fromUid == Firebase.auth.uid) {
            if (chatModelList[position].msgType == MessageType.TEXT.name)
                (holder as TextRightViewHolder).bind(chatModelList[position], position)
            else
                (holder as ImageRightViewHolder).bind(chatModelList[position], position)
        } else {
            if (chatModelList[position].msgType == MessageType.TEXT.name)
                (holder as TextLeftViewHolder).bind(chatModelList[position], position)
            else
                (holder as ImageLeftViewHolder).bind(chatModelList[position], position)
        }

    }

    override fun getItemViewType(position: Int): Int {
        return if (chatModelList[position].fromUid == Firebase.auth.uid) {
            if (chatModelList[position].msgType == MessageType.TEXT.name) TEXT_RIGHT else IMAGE_RIGHT
        } else {
            if (chatModelList[position].msgType == MessageType.TEXT.name) TEXT_LEFT else IMAGE_LEFT
        }
    }

    inner class TextLeftViewHolder(private val binding: ItemChatLeftBinding) : ViewHolder(binding.root) {
        fun bind(chat: ChatRoomModel.Companion.ChatModel, position: Int) {
            binding.apply {
                chat.also {
                    itemUsername.text = ChatRoomFragment.mapOfUserName[it.fromUid]
                    itemMessage.text = it.message
                    itemTimestamp.text = DateTimeUtil.getFormattedTime(it.timeStamp)

                    showProfileImage(it.fromUid, itemProfileImage)
                    showCounterUnread(it, itemCountUnread)
                }

                if (sizeOfCurrentRoomUsers > 2 && sizeOfCurrentRoomUsers != chat.readUsers.size) {
                    root.setOnClickListener {
                        onUnreadCounterClickListener?.invoke(chat)
                    }
                }
            }
        }
    }

    inner class TextRightViewHolder(private val binding: ItemChatRightBinding) : ViewHolder(binding.root) {
        fun bind(chat: ChatRoomModel.Companion.ChatModel, position: Int) {
            binding.apply {
                chat.also {
                    itemMessage.text = it.message
                    itemTimestamp.text = DateTimeUtil.getFormattedTime(it.timeStamp)

                    showCounterUnread(it, itemCountUnread)
                }

                itemMessage.setOnLongClickListener {
                    onMessageLongClickListener?.invoke(position)
                    true
                }

                if (sizeOfCurrentRoomUsers > 2 && sizeOfCurrentRoomUsers != chat.readUsers.size) {
                    root.setOnClickListener {
                        onUnreadCounterClickListener?.invoke(chat)
                    }
                }
            }
        }
    }

    inner class ImageLeftViewHolder(private val itemBinding: ItemImageLeftBinding) : ViewHolder(itemBinding.root) {
        fun bind(chat: ChatRoomModel.Companion.ChatModel, position: Int) {
            itemBinding.apply {
                chat.also {
                    itemUsername.text = ChatRoomFragment.mapOfUserName[it.fromUid]
                    itemTimestamp.text = DateTimeUtil.getFormattedTime(it.timeStamp)
                    itemImage.load(it.message) {
                        transformations(RoundedCornersTransformation(40f, 40f, 40f, 40f))
                    }

                    showProfileImage(it.fromUid, itemProfileImage)
                    showCounterUnread(it, itemCountUnread)
                }

                itemImage.setOnClickListener {
                    onImageClickListener?.invoke(position)
                }

                if (sizeOfCurrentRoomUsers > 2 && sizeOfCurrentRoomUsers != chat.readUsers.size) {
                    root.setOnClickListener {
                        onUnreadCounterClickListener?.invoke(chat)
                    }
                }
            }
        }
    }

    inner class ImageRightViewHolder(private val itemBinding: ItemImageRightBinding) : ViewHolder(itemBinding.root) {
        fun bind(chat: ChatRoomModel.Companion.ChatModel, position: Int) {
            itemBinding.apply {
                chat.also {
                    itemTimestamp.text = DateTimeUtil.getFormattedTime(it.timeStamp)
                    itemImage.load(it.message) {
                        transformations(RoundedCornersTransformation(40f, 40f, 40f, 40f))
                    }

                    showCounterUnread(it, itemCountUnread)
                }

                itemImage.setOnClickListener {
                    onImageClickListener?.invoke(position)
                }

                itemImage.setOnLongClickListener {
                    onImageLongClickListener?.invoke(position)
                    true
                }

                if (sizeOfCurrentRoomUsers > 2 && sizeOfCurrentRoomUsers != chat.readUsers.size) {
                    root.setOnClickListener {
                        onUnreadCounterClickListener?.invoke(chat)
                    }
                }
            }
        }
    }

    private fun showCounterUnread(chat: ChatRoomModel.Companion.ChatModel, textView: TextView) {
        val counterUnread = sizeOfCurrentRoomUsers - chat.readUsers.size

        textView.text = if (counterUnread > 0) counterUnread.toString() else ""
    }

    private fun showProfileImage(userUid: String, imageView: ImageView) {
        Firebase.database.reference
            .child("users/$userUid")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user = snapshot.getValue<UserModel>()

                        if (user!!.userImageUri == "") {
                            imageView.load(R.drawable.baseline_account_circle_24)
                        } else {
                            imageView.load(user.userImageUri) {
                                transformations(CircleCropTransformation())
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }
}