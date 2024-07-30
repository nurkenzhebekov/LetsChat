package com.example.letschat.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.letschat.fragments.ChatRoomFragment
import com.example.letschat.R
import com.example.letschat.databinding.ItemChatRoomBinding
import com.example.letschat.models.ChatRoomModel
import com.example.letschat.models.UserModel
import com.example.letschat.utils.DateTimeUtil
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.Collections
import java.util.TreeMap

class ChatRoomRecyclerAdapter : RecyclerView.Adapter<ChatRoomRecyclerAdapter.CustomViewHolder>() {

    private var chatRoomList = mutableListOf<ChatRoomModel>()
    private var chatRoomKeyList = mutableListOf<String>()

    private lateinit var mContext: Context

    var onItemClickListener: ((String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        mContext = parent.context
        val inflater = LayoutInflater.from(mContext)
        val binding = ItemChatRoomBinding.inflate(inflater, parent, false)
        return CustomViewHolder(binding)
    }

    override fun getItemCount(): Int = chatRoomList.size

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        holder.bind(chatRoomList[position], position)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(chatRooms: List<ChatRoomModel>, chatRoomKeys: List<String>) {
        chatRoomList.clear()
        chatRoomKeyList.clear()
        chatRoomList.addAll(chatRooms)
        chatRoomKeyList.addAll(chatRoomKeys)
        notifyDataSetChanged()
    }

    inner class CustomViewHolder(private val binding: ItemChatRoomBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chatRoom: ChatRoomModel, position: Int) {
            binding.apply {
                var friendUid: String? = null
                var chatRoomTitle = ""
                var counter = 0

                chatRoom.roomUsers.keys.forEach {
                    if (it == Firebase.auth.uid) return@forEach
                    friendUid = it
                    chatRoomTitle += ChatRoomFragment.mapOfUserName[it]
                    if (counter < chatRoom.roomUsers.size - 2) chatRoomTitle += ", "
                    counter++
                }

                itemTitle.text = chatRoomTitle

                if (chatRoom.roomUsers.size > 2) {
                    itemCounterRoomusers.text = chatRoom.roomUsers.size.toString()
                }

                setProfileImage(friendUid!!, itemImageview)

                var lastMessage = ""
                var lastTimeStamp = 0L
                var counterUnread = 0

                val chatMap =
                    TreeMap<String, ChatRoomModel.Companion.ChatModel> (Collections.reverseOrder())
                chatMap.putAll(chatRoom.chats)

                if (chatMap.keys.toTypedArray().isNotEmpty()) {
                    val lastMessageKey = chatMap.keys.toTypedArray()[0]
                    lastMessage = chatRoom.chats[lastMessageKey]!!.message
                    lastTimeStamp = chatRoom.chats[lastMessageKey]!!.timeStamp

                    chatMap.values.forEach {
                        if (it.readUsers.containsKey(Firebase.auth.uid)) return@forEach
                        counterUnread += 1
                    }
                }

                itemLastMessage.text = lastMessage
                itemTimestamp.text = DateTimeUtil.getFormattedTime(lastTimeStamp)

                itemCounterUnread.text = counterUnread.toString()
                itemCounterUnread.visibility = if (counterUnread > 0) View.VISIBLE else View.GONE

                root.setOnClickListener {
                    onItemClickListener?.invoke(chatRoomKeyList[position])
                }
            }
        }
    }

    private fun setProfileImage(friendUid: String, imageView: ImageView) {
        Firebase.database.reference
            .child("/users/$friendUid")
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