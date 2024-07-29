package com.example.letschat.utils

import android.content.Context
import android.widget.Toast

const val CHATROOM_KEY = "chatroom_key"

enum class MessageType {
    TEXT,
    IMAGE
}

fun toast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}