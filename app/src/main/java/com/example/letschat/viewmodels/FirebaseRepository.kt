package com.example.letschat.viewmodels

import androidx.lifecycle.MutableLiveData
import com.example.letschat.models.UserModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class FirebaseRepository {

    val usersLiveData = MutableLiveData<MutableMap<String, UserModel>>()

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