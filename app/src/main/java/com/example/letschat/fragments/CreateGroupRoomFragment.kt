package com.example.letschat.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.letschat.R
import com.example.letschat.databinding.FragmentCreateGroupRoomBinding
import com.example.letschat.databinding.ItemCreateGroupBinding
import com.example.letschat.models.ChatRoomModel
import com.example.letschat.models.UserModel
import com.example.letschat.utils.toast
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class CreateGroupRoomFragment : BaseFragment<FragmentCreateGroupRoomBinding>() {

    private val args by navArgs<CreateGroupRoomFragmentArgs>()

    private lateinit var currentUserUid: String

    private var roomUsersSelected = mutableListOf<String>()
    private var chatRoomKey: String? = null

    private var newChatRoom = ChatRoomModel()

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCreateGroupRoomBinding {
        return FragmentCreateGroupRoomBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserUid = Firebase.auth.uid!!

        roomUsersSelected = args.roomUsersSelected.toMutableList()
        chatRoomKey = args.currentRoomKey

        binding.selectFriendRecyclerview.apply {
            adapter = RecyclerviewAdapter()
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }

        binding.imageBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnCreateGroup.setOnClickListener {
            if (newChatRoom.roomUsers.isEmpty()) {
                toast(requireContext(), "Please select new friends!")
                return@setOnClickListener
            }

            checkOldChatRooms()
        }
    }

    override fun onBackPressed() {
        findNavController().navigate(R.id.action_createGroupRoomFragment_to_chatLogFragment)
    }

    private fun checkOldChatRooms() {
        val addedRoomUsers = mutableListOf<String>()
        addedRoomUsers.addAll(roomUsersSelected)
        addedRoomUsers.addAll(newChatRoom.roomUsers.keys)

        Firebase.database.reference
            .child("chatRooms")
            .orderByChild("roomUsers/$currentUserUid").equalTo(true)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val mapList = snapshot.getValue<MutableMap<String, ChatRoomModel>>()

                        val filteredList = mapList!!.toList().filter {
                            val roomUserList = it.second.roomUsers.keys.toList()
                            roomUserList.size == addedRoomUsers.size && roomUserList.containsAll(addedRoomUsers)
                        }

                        if (filteredList.isEmpty()) {
                            if (roomUsersSelected.size == 2) makeNewGroupChatRoom()
                            else updateChatRoom()
                        } else
                            navigateToChatLogFragment(filteredList[0].first)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    private fun makeNewGroupChatRoom() {
        roomUsersSelected.map { newChatRoom.roomUsers[it] = true }

        val mRef = Firebase.database.reference.child("chatRooms")
        val newChatRoomKey = mRef.push().key

        mRef.child(newChatRoomKey!!)
            .setValue(newChatRoom)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    toast(requireContext(), "Successfully created!")
                    navigateToChatLogFragment(newChatRoomKey)
                }
            }
    }

    private fun updateChatRoom() {
        Firebase.database.reference
            .child("chatRooms/$chatRoomKey/roomUsers")
            .updateChildren(newChatRoom.roomUsers as Map<String, Any>)
            .addOnSuccessListener {
                toast(requireContext(), "Successfully added!")
                navigateToChatLogFragment(chatRoomKey!!)
            }
    }

    private fun navigateToChatLogFragment(roomKey: String) {
        val action =
            CreateGroupRoomFragmentDirections.actionCreateGroupRoomFragmentToChatLogFragment(
                roomKey
            )
        findNavController().navigate(action)
    }

    inner class RecyclerviewAdapter : RecyclerView.Adapter<RecyclerviewAdapter.CustomHolder>() {

        private var userList = listOf<UserModel>()

        init {
            fetchUsers()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemCreateGroupBinding.inflate(inflater, parent, false)
            return CustomHolder(binding)
        }

        override fun getItemCount(): Int = userList.size

        override fun onBindViewHolder(holder: CustomHolder, position: Int) {
            holder.bind(userList[position])
        }

        inner class CustomHolder(private val binding: ItemCreateGroupBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(user: UserModel) {
                binding.apply {
                    user.also {
                        itemUsername.text = it.userName
                        if (it.userImageUri != "") {
                            itemImageview.load(it.userImageUri) {
                                transformations(CircleCropTransformation())
                            }
                        }

                        if (roomUsersSelected.contains(it.userUid)) {
                            itemCheckbox.isChecked = true
                            itemCheckbox.isEnabled = false
                        }

                        itemCheckbox.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
                            override fun onCheckedChanged(
                                buttonView: CompoundButton?,
                                isChecked: Boolean
                            ) {
                                if (isChecked) {
                                    newChatRoom.roomUsers[it.userUid] = true
                                } else {
                                    newChatRoom.roomUsers.remove(it.userUid)
                                }
                            }

                        })
                    }
                }
            }
        }

        private fun fetchUsers() {
            Firebase.database.reference
                .child("users")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val mapList = snapshot.getValue<MutableMap<String, UserModel>>()
                            userList = mapList!!.values.toList().filter { it.userUid != currentUserUid }

                            notifyDataSetChanged()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }

                })
        }
    }

}