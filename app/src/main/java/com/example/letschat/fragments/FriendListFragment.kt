package com.example.letschat.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letschat.adapters.FriendListRecyclerAdapter
import com.example.letschat.databinding.FragmentFriendListBinding
import com.example.letschat.models.ChatRoomModel
import com.example.letschat.utils.toast
import com.example.letschat.viewmodels.FirebaseViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class FriendListFragment : BaseFragment<FragmentFriendListBinding>() {

    private val firebaseViewModel: FirebaseViewModel by viewModels()

    private val friendListRecyclerAdapter: FriendListRecyclerAdapter by lazy {
        FriendListRecyclerAdapter()
    }

    private lateinit var currentUserUid: String

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentFriendListBinding {
        return FragmentFriendListBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserUid = Firebase.auth.uid!!

        binding.rvFriendList.apply {
            adapter = friendListRecyclerAdapter
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }



        friendListRecyclerAdapter.onItemClickListener = { userUid ->
            checkOldChatRooms(userUid)
        }

        initLiveDataObserver()
    }

    private fun initLiveDataObserver() {
        firebaseViewModel.usersLiveData.observe(viewLifecycleOwner) { mapUsers ->
            val filteredList = mapUsers.values.toList().filter {
                it.userUid != currentUserUid
            }

            friendListRecyclerAdapter.setData(filteredList)
        }
    }

    override fun onResume() {
        super.onResume()

        firebaseViewModel.fetchUsers()
    }

    override fun onBackPressed() {
        requireActivity().finish()
    }

    private fun checkOldChatRooms(selectedUid: String) {

        Firebase.database.reference
            .child("chatRooms")
            .orderByChild("roomsUsers/$currentUserUid").equalTo(true)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val mapList = snapshot.getValue<MutableMap<String, ChatRoomModel>>()

                        val filteredList = mapList!!.toList().filter {
                            it.second.roomUsers.size == 2 && it.second.roomUsers.containsKey(selectedUid)
                        }

                        if (filteredList.isEmpty()) makeNewChatRoom(selectedUid)
                        else navigateToChatLogFragment(filteredList[0].first)
                    } else {
                        makeNewChatRoom(selectedUid)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun makeNewChatRoom(selectedUid: String) {
        val newChatRoom = ChatRoomModel()
        newChatRoom.roomUsers[currentUserUid] = true
        newChatRoom.roomUsers[selectedUid] = true

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

    private fun navigateToChatLogFragment(roomKey: String) {
        val action =
            FriendListFragmentDirections.actionFriendListFragmentToChatLogFragment(
                roomKey
            )
        findNavController().navigate(action)
    }
}