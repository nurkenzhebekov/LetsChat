package com.example.letschat.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.example.letschat.R
import com.example.letschat.activities.MainActivity
import com.example.letschat.models.UserModel
import com.example.letschat.utils.DateTimeUtil
import com.example.letschat.utils.ENDLESS_ALARM
import com.example.letschat.utils.getBoolean
import com.example.letschat.viewmodels.FirebaseRepository
import com.google.android.play.core.integrity.ap
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseNotificationService : FirebaseMessagingService() {

    private val repository: FirebaseRepository = FirebaseRepository()

    companion object {
        var isNotifying = false
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        repository.savePushTokenToServer(token, Firebase.auth.uid!!)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"]
            val msg = remoteMessage.data["message"]
            val fromUserUid = remoteMessage.data["fromUserUid"]
            val timeStamp = remoteMessage.data["timeStamp"]

            Firebase.database.reference
                .child("users/$fromUserUid")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val profileImageUrl = snapshot.getValue<UserModel>()!!.userImageUri

                            isNotifying = true

                            CoroutineScope(Dispatchers.Main).launch {
                                while (isNotifying) {
                                    getIconCompat(
                                        applicationContext,
                                        title!!,
                                        msg!!,
                                        timeStamp!!.toLong(),
                                        profileImageUrl!!
                                    )

                                    if (!getBoolean(
                                            applicationContext,
                                            ENDLESS_ALARM
                                        )
                                    ) isNotifying = false

                                    delay(3 * 1000)
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }

                })
        }
    }

    private fun getIconCompat(
        context: Context,
        title: String,
        msg: String,
        timeStamp: Long,
        profileImageUrl: String
    ) {
        var iconCompat: IconCompat

        if (profileImageUrl == "") {
            iconCompat = IconCompat.createWithResource(
                context,
                R.drawable.baseline_account_circle_24
            )
            showNotification(context, title, msg, timeStamp, iconCompat)
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                urlToBitmap(profileImageUrl)?.let {
                    iconCompat = IconCompat.createWithBitmap(it)
                    showNotification(
                        context,
                        title,
                        msg,
                        timeStamp,
                        iconCompat
                    )
                }
            }
        }
    }

    private suspend fun urlToBitmap(imageUrl: String) = suspendCoroutine<Bitmap?> { continuation ->
        val mRef = Firebase.storage.getReferenceFromUrl(imageUrl)
        val localFile = File.createTempFile("tempImage", "jpg")

        mRef.getFile(localFile)
            .addOnSuccessListener {
                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                continuation.resume(bitmap)
            }
            .addOnFailureListener {
                continuation.resume(null)
            }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun showNotification(
        context: Context,
        title: String,
        msg: String,
        timeStamp: Long,
        iconCompat: IconCompat
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "from_chat"

        val notifyId = DateTimeUtil.createNotifyId(timeStamp)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelId,
                NotificationManager.IMPORTANCE_HIGH
            )

            manager.createNotificationChannel(channel)
        }

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE
                    else 0

        val alarmOfPendingIntent = PendingIntent.getBroadcast(
            context,
            335765,
            Intent(context, EndlessAlarmReceiver::class.java).setAction("ALARM_OFF"),
            flag
        )

        val person = Person.Builder()
            .setIcon(iconCompat)
            .setName(title)
            .build()
        val notificationStyle = NotificationCompat.MessagingStyle(person)
            .addMessage(msg, System.currentTimeMillis(), person)

        val newMessageNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_chat)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(notificationStyle)
            .setContentIntent(getMainActivityPendingIntent(context))
            .addAction(R.drawable.baseline_notifications_off_24, "Alarm Off", alarmOfPendingIntent)
            .build()

        manager.notify(notifyId, newMessageNotification)
    }

    private fun getMainActivityPendingIntent(context: Context) = PendingIntent.getActivity(
        context,
        234764,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}