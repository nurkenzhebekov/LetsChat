package com.example.letschat.viewmodels

import androidx.lifecycle.MutableLiveData
import com.example.letschat.fcmapi.NotificationBody
import com.example.letschat.fcmapi.RetrofitInstance
import com.example.letschat.models.UserModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.ResponseBody
import retrofit2.Response

class FirebaseRepository {

    val mResponse = MutableLiveData<Response<ResponseBody>>()

    val usersLiveData = MutableLiveData<MutableMap<String, UserModel>>()

    suspend fun sendNotification(notification: NotificationBody) {
        mResponse.value = RetrofitInstance.api.sendNotification(notification)
    }

    fun getAndSavePushTokenToServer(currentUserUid: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (it.isSuccessful) {
                val newToken = it.result
                savePushTokenToServer(newToken, currentUserUid)
            }
        }
    }

    fun savePushTokenToServer(newToken: String, currentUserUid: String) {
        val mRef = Firebase.database.reference
            .child("users/$currentUserUid")

        mRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val oldToken = snapshot.getValue<UserModel>()!!.pushToken
                    if (newToken != oldToken) {
                        val tokenMap = mutableMapOf<String, String>()
                        tokenMap["pushToken"] = newToken

                        mRef.updateChildren(tokenMap as Map<String, Any>)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    fun fetchUsers() {

        Firebase.database.reference
            .child("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        usersLiveData.value = snapshot.getValue<MutableMap<String, UserModel>>()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })

    }

}