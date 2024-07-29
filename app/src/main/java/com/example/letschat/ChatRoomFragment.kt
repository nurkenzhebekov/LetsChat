package com.example.letschat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letschat.adapters.ChatRoomRecyclerAdapter
import com.example.letschat.databinding.FragmentChatRoomBinding
import com.example.letschat.models.ChatRoomModel
import com.example.letschat.models.UserModel
import com.example.letschat.viewmodels.FirebaseViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.TreeMap

class ChatRoomFragment : BaseFragment<FragmentChatRoomBinding>() {

    private val firebaseViewModel: FirebaseViewModel by viewModels()

    private val chatRoomRecyclerAdapter: ChatRoomRecyclerAdapter by lazy {
        ChatRoomRecyclerAdapter()
    }

    private lateinit var currentUserUid: String

    companion object {
        var mapOfUserName = mutableMapOf<String, String>()
    }

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentChatRoomBinding {
        return FragmentChatRoomBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerviewChatRoom.apply {
            adapter = chatRoomRecyclerAdapter
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.VERTICAL, false
            )
        }

        chatRoomRecyclerAdapter.onItemClickListener = { chatRoomKey ->
            val action = ChatRoomFragmentDirections.actionChatRoomFragmentToChatLogFragment(chatRoomKey)
            findNavController().navigate(action)
        }
    }

    override fun onResume() {
        super.onResume()

        if (Firebase.auth.currentUser == null)
            findNavController().navigate(R.id.action_chatRoomFragment_to_loginFragment)
        else {
            currentUserUid = Firebase.auth.uid!!

            fetchChatRooms()
        }
    }

    override fun onBackPressed() {
        activity?.finish()
    }

    private fun fetchChatRooms() {
        binding.progressBar.visibility = View.VISIBLE

        Firebase.database.reference
            .child("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val mapList = snapshot.getValue<MutableMap<String, UserModel>>()
                        mapList!!.values.map {
                            mapOfUserName[it.userUid] = it.userName
                        }

                        Firebase.database.reference
                            .child("chatRooms")
                            .orderByChild("roomUsers/$currentUserUid").equalTo(true)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        val mapListOfChatRoom =
                                            snapshot.getValue<MutableMap<String, ChatRoomModel>>()

                                        val sortedMapList = TreeMap<String, ChatRoomModel>()
                                        sortedMapList.putAll(mapListOfChatRoom!!)

                                        val chatRoomList = sortedMapList.values.toTypedArray().toList()
                                        val chatRoomKeys = sortedMapList.keys.toTypedArray().toList()

                                        chatRoomRecyclerAdapter.setData(chatRoomList, chatRoomKeys)
                                        binding.progressBar.visibility = View.GONE
                                    } else {
                                        binding.progressBar.visibility = View.GONE
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {

                                }

                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }
}