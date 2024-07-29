package com.example.letschat

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letschat.activities.MainActivity
import com.example.letschat.adapters.ChatListRecyclerAdapter
import com.example.letschat.databinding.FragmentChatLogBinding
import com.example.letschat.models.ChatRoomModel
import com.example.letschat.utils.MessageType
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.TreeMap

class ChatLogFragment : BaseFragment<FragmentChatLogBinding>() {

    private val chatListRecyclerAdapter: ChatListRecyclerAdapter by lazy {
        ChatListRecyclerAdapter()
    }

    private val args: ChatLogFragmentArgs by navArgs()

    private var chatRoomKey: String? = null
    private lateinit var currentUserUid: String
    private var roomUsers = mutableMapOf<String, Boolean>()
    private var sizeOfCurrentRoomUsers = 0

    private lateinit var mDatabaseReference: DatabaseReference
    private var mValureEventListener: ValueEventListener? = null

    private var chatModelList = listOf<ChatRoomModel.Companion.ChatModel>()
    private var chatKeyList = listOf<String>()

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentChatLogBinding {
        return FragmentChatLogBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatRoomKey = args.roomKeySelected

        currentUserUid = Firebase.auth.uid!!

        if (chatRoomKey == null) onBackPressed()

        fetchRoomUsers()

        binding.messageRecyclerview.apply {
            adapter = chatListRecyclerAdapter
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

            addOnLayoutChangeListener(object : OnLayoutChangeListener {
                override fun onLayoutChange(v: View?,
                                            left: Int, top: Int, right: Int, bottom: Int,
                                            oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
                ) {
                    if (bottom < oldBottom) {
                        binding.messageRecyclerview.postDelayed({
                            binding.messageRecyclerview.scrollToPosition(chatModelList.size - 1)
                        }, 100)
                    }
                }
            })
        }

        binding.imageBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnSendMessage.setOnClickListener {
            sendMessage()
        }

        binding.btnPlusFriend.setOnClickListener {
            //
        }
    }

    override fun onResume() {
        super.onResume()

        binding.etxMessage.clearFocus()

        if (mValureEventListener != null)
            mDatabaseReference.addValueEventListener(mValureEventListener!!)
    }

    override fun onPause() {
        super.onPause()

        if (mValureEventListener != null)
            mDatabaseReference.removeEventListener(mValureEventListener!!)
    }

    override fun onBackPressed() {
        if (mValureEventListener != null)
            mDatabaseReference.removeEventListener(mValureEventListener!!)

        Intent(requireContext(), MainActivity::class.java).also {
            startActivity(it)
            requireActivity().finish()
        }
    }

    private fun fetchRoomUsers() {
        if (chatRoomKey == null) return
        binding.progressBar.visibility = View.VISIBLE

        Firebase.database.reference
            .child("chatRooms/$chatRoomKey/roomUsers")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        roomUsers = snapshot.getValue<MutableMap<String, Boolean>>()!!
                        sizeOfCurrentRoomUsers = roomUsers.size

                        fetchMessages()
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun fetchMessages() {
        mDatabaseReference = Firebase.database.reference
            .child("chatRooms/$chatRoomKey/chats")

        mValureEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val mapList = snapshot.getValue<MutableMap<String,ChatRoomModel.Companion.ChatModel>>()
                    val sortedMapList = TreeMap<String, ChatRoomModel.Companion.ChatModel>()
                    sortedMapList.putAll(mapList!!)

                    val mapForUpdate = mutableMapOf<String, Any>()

                    sortedMapList.forEach {
                        val chat = it.value

                        if (!chat.readUsers.containsKey(currentUserUid)) {
                            chat.readUsers[currentUserUid] = true
                            mapForUpdate[it.key] = chat
                        }
                    }

                    chatKeyList = sortedMapList.keys.toTypedArray().toList()
                    chatModelList = sortedMapList.values.toTypedArray().toList()

                    mDatabaseReference.updateChildren(mapForUpdate)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                chatListRecyclerAdapter.setData(chatModelList, sizeOfCurrentRoomUsers)

                                binding.messageRecyclerview.scrollToPosition(chatModelList.size - 1)
                                binding.progressBar.visibility = View.GONE
                            }
                        }
                } else {
                    binding.progressBar.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        }
        mDatabaseReference.addValueEventListener(mValureEventListener as ValueEventListener)
    }

    private fun sendMessage() {
        val message = binding.etxMessage.text.toString()
        if (message.isEmpty()) return

        val readUsers = mutableMapOf<String, Boolean>()
        readUsers[currentUserUid] = true

        val chat = ChatRoomModel.Companion.ChatModel(
            currentUserUid, message, MessageType.TEXT.name, System.currentTimeMillis(), readUsers
        )

        saveChatAndSendFcm(chat, message)
    }

    private fun saveChatAndSendFcm(chat: ChatRoomModel.Companion.ChatModel, message: String) {
        Firebase.database.reference
            .child("chatRooms/$chatRoomKey/chats")
            .push()
            .setValue(chat)
            .addOnCompleteListener {
                binding.etxMessage.setText("")
                roomUsers.keys.forEach {
                    if (it == currentUserUid) return@forEach
                    sendFcm(message, it, chat.timeStamp)
                }
            }
    }

    private fun sendFcm(message: String, toUserUid: String, timeStamp: Long) {
        //
    }
}