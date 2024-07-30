package com.example.letschat.fcmapi

import com.example.letschat.utils.FCM_CONTENT_TYPE
import com.example.letschat.utils.FCM_SERVER_KEY
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface FcmInterface {

    @Headers("Authorization: key=$FCM_SERVER_KEY", "Content-type:$FCM_CONTENT_TYPE")
    @POST("fcm/send")
    suspend fun sendNotification(
        @Body notification: NotificationBody
    ): Response<ResponseBody>
}