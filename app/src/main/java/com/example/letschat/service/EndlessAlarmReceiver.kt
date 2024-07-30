package com.example.letschat.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class EndlessAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "ALARM_OFF") {
            FirebaseNotificationService.isNotifying = false
        }
    }
}