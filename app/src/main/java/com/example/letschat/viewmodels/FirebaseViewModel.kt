package com.example.letschat.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class FirebaseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FirebaseRepository = FirebaseRepository()

    val usersLiveData = repository.usersLiveData

    fun fetchUsers() {
        repository.fetchUsers()
    }

}