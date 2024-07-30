package com.example.letschat.fragments

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.letschat.activities.MainActivity
import com.example.letschat.adapters.ChatListRecyclerAdapter
import com.example.letschat.databinding.DialogDeleteChatBinding
import com.example.letschat.databinding.DialogUsernameUnreadBinding
import com.example.letschat.databinding.FragmentChatLogBinding
import com.example.letschat.fcmapi.NotificationBody
import com.example.letschat.models.ChatRoomModel
import com.example.letschat.models.UserModel
import com.example.letschat.utils.DateTimeUtil
import com.example.letschat.utils.MessageType
import com.example.letschat.utils.SaveToMediaStore
import com.example.letschat.utils.toast
import com.example.letschat.viewmodels.FirebaseViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.TreeMap
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChatLogFragment : BaseFragment<FragmentChatLogBinding>() {

    private val chatListRecyclerAdapter: ChatListRecyclerAdapter by lazy {
        ChatListRecyclerAdapter()
    }

    private val args: ChatLogFragmentArgs by navArgs()

    private val firebaseViewModel: FirebaseViewModel by viewModels()

    private var chatRoomKey: String? = null
    private lateinit var currentUserUid: String
    private var roomUsers = mutableMapOf<String, Boolean>()
    private var sizeOfCurrentRoomUsers = 0

    private var imageUriSelected: String? = null

    private lateinit var mDatabaseReference: DatabaseReference
    private var mValureEventListener: ValueEventListener? = null

    private var chatModelList = listOf<ChatRoomModel.Companion.ChatModel>()
    private var chatKeyList = listOf<String>()

    private var notificationManager: NotificationManager? = null

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentChatLogBinding {
        return FragmentChatLogBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
            val action =
                ChatLogFragmentDirections.actionChatLogFragmentToCreateGroupRoomFragment(
                    roomUsers.keys.toTypedArray(), chatRoomKey!!
                )
            findNavController().navigate(action)
        }

        binding.btnSelectImage.setOnClickListener {
            cropImage(null)
        }

        itemClickListener()

        includeZoomClickListener()
    }

    private fun itemClickListener() {
        chatListRecyclerAdapter.apply {
            onMessageLongClickListener = { position ->
                val chat = chatModelList[position]
                showDialogForDelete(chat.msgType, chat.message, chatKeyList[position])
            }

            onImageLongClickListener = { position ->
                val chat = chatModelList[position]
                showDialogForDelete(chat.msgType, chat.message, chatKeyList[position])
            }

            onUnreadCounterClickListener = { chat ->
                val userUidUnread = roomUsers.keys.toSet().minus(chat.readUsers.keys.toSet())
                showDialogForUnread(userUidUnread)
            }

            onImageClickListener = { position ->
                val chat = chatModelList[position]
                binding.includeZoomImage.apply {
                    layoutZoomPart.visibility = View.VISIBLE
                    imageUriSelected = chat.message
                    zoomImage.load(chat.message) {
                        transformations(RoundedCornersTransformation(40f, 40f, 40f, 40f))
                    }
                }
            }
        }
    }

    private fun includeZoomClickListener() {
        binding.includeZoomImage.apply {
            btnReturn.setOnClickListener {
                layoutZoomPart.visibility = View.GONE
            }

            btnStore.setOnClickListener {
                if (imageUriSelected == null) return@setOnClickListener
                progressBar.visibility = View.VISIBLE
                val mRef = Firebase.storage.getReferenceFromUrl(imageUriSelected!!)
                val localFile = File.createTempFile("tempImage", "jpg")

                mRef.getFile(localFile)
                    .addOnSuccessListener {
                        val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                        val storeUri = SaveToMediaStore.getImageUri(requireContext(), bitmap)
                        progressBar.visibility = View.GONE
                        if (storeUri != null) {
                            toast(requireContext(), "Successfully stored!")
                        } else {
                            toast(requireContext(), "Failed to store, Please try again!")
                        }
                    }
                    .addOnFailureListener {
                        toast(requireContext(), "Failed to store, Please try again!")
                    }
            }
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

                        var chatRoomTitle = ""
                        var counter = 0
                        roomUsers.keys.forEach {
                            if (it == currentUserUid) return@forEach
                            chatRoomTitle += ChatRoomFragment.mapOfUserName[it]
                            if (counter < roomUsers.size -2) chatRoomTitle += ", "
                            counter++
                        }
                        binding.titleName.text = chatRoomTitle

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

                        val notifyId = DateTimeUtil.createNotifyId(chat.timeStamp)
                        notificationManager?.cancel(notifyId)

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
        val title = ChatRoomFragment.mapOfUserName[currentUserUid]
        val data = NotificationBody.NotificationData(
            title!!, message, currentUserUid, timeStamp
        )

        lifecycleScope.launch {
            delay(300)
            fetchPushToken(toUserUid)?.let {
                val body = NotificationBody(it, data, "high")

                firebaseViewModel.sendNotification(body)
            }
        }
    }

    private suspend fun fetchPushToken(toUserUid: String) = suspendCoroutine { continuation ->
        Firebase.database.reference
            .child("users/$toUserUid")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val pushToken = snapshot.getValue<UserModel>()!!.pushToken
                        continuation.resume(pushToken)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(null)
                }

            })
    }

    private fun cropImage(uri: Uri?) {
        context?.let {
            CropImage.activity(uri)
                .setActivityTitle("Crop Image")
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .setCropMenuCropButtonTitle("OK")
                .start(it, this)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val result = CropImage.getActivityResult(data)

        when (requestCode) {
            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    result.uri?.let { uri ->
                        saveImageToFirebaseStorage(uri)
                    }
                }
            }

            CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE -> {
                Log.e("TAG", result.error.toString())
            }
        }
    }

    private fun saveImageToFirebaseStorage(imageUri: Uri) {
        binding.progressBar.visibility = View.VISIBLE

        val ref = Firebase.storage.reference
            .child("/messageImages/$currentUserUid")
            .child(UUID.randomUUID().toString())

        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener {
                    val readUsers = mutableMapOf<String, Boolean>()
                    readUsers[currentUserUid] = true

                    val chat = ChatRoomModel.Companion.ChatModel(
                        currentUserUid,
                        it.toString(),
                        MessageType.IMAGE.name,
                        System.currentTimeMillis(),
                        readUsers
                    )

                    val message = "Image has sent"

                    saveChatAndSendFcm(chat, message)
                }
            }
    }

    private fun showDialogForDelete(msgType: String, msg: String, chatKey: String) {
        val dialogBinding = DialogDeleteChatBinding.inflate(layoutInflater)

        dialogBinding.apply {
            when (msgType) {
                MessageType.TEXT.name -> {
                    chatImage.visibility = View.GONE
                    chatText.text = msg
                }

                MessageType.IMAGE.name -> {
                    chatText.visibility = View.GONE
                    chatImage.load(msg) {
                        transformations(RoundedCornersTransformation(40f, 40f, 40f, 40f))
                    }
                }
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.btnDelete.setOnClickListener {
            when (msgType) {
                MessageType.TEXT.name -> deleteTextSelected(chatKey)
                MessageType.IMAGE.name -> deleteImageSelected(msg, chatKey)
            }

            dialog.dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
            binding.messageRecyclerview.scrollToPosition(chatModelList.size - 1)
        }
    }

    private fun deleteTextSelected(chatKey: String) {
        val mRef = Firebase.database.reference
            .child("chatRooms/$chatRoomKey/chats")
            .child(chatKey)
        mRef.removeValue()
            .addOnSuccessListener {
                toast(requireContext(), "Successfully deleted")
            }
            .addOnFailureListener {
                toast(requireContext(), "Failed to delete, Please try again")
            }
    }

    private fun deleteImageSelected(imageUri: String, chatKey: String) {
        val mRef = Firebase.storage.getReferenceFromUrl(imageUri)
        mRef.delete()
            .addOnSuccessListener {
                deleteTextSelected(chatKey)
            }
            .addOnFailureListener {
                toast(requireContext(), "Failed to delete, Please try again")
            }
    }

    private fun showDialogForUnread(userUidsUnread: Set<String>) {
        var namesUnread = ""
        userUidsUnread.forEach {
            namesUnread += ChatRoomFragment.mapOfUserName[it] + "\n"
        }

        val dialogBinding = DialogUsernameUnreadBinding.inflate(layoutInflater)
        dialogBinding.apply {
            usernameUnread.text = namesUnread
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.btnReturn.setOnClickListener {
            dialog.dismiss()
        }
    }

}