package com.example.letschat.fcmapi

import com.example.letschat.utils.FCM_BASE_URL
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(FCM_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: FcmInterface by lazy {
        retrofit.create(FcmInterface::class.java)
    }

}