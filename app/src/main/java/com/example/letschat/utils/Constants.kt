package com.example.letschat.utils

import android.content.Context
import android.widget.Toast

/*const val CHATROOM_KEY = "chatroom_key"*/

enum class MessageType {
    TEXT,
    IMAGE
}

const val FCM_SERVER_KEY = ""
const val FCM_BASE_URL = "https://fcm.googleapis.com"
const val FCM_CONTENT_TYPE = "application/json"

fun toast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}