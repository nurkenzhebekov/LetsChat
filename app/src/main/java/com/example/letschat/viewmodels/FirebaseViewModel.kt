package com.example.letschat.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.letschat.fcmapi.NotificationBody
import kotlinx.coroutines.launch

class FirebaseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FirebaseRepository = FirebaseRepository()

    val mResponse = repository.mResponse

    val usersLiveData = repository.usersLiveData

    fun sendNotification(notification: NotificationBody) {
        viewModelScope.launch {
            repository.sendNotification(notification)
        }
    }

    fun getAndSavePushTokenToServer(currentUserUid: String) {
        repository.getAndSavePushTokenToServer(currentUserUid)
    }

    fun fetchUsers() {
        repository.fetchUsers()
    }

}