package com.example.letschat.utils

import android.content.Context
import android.content.SharedPreferences

const val VERSION = "1.0"

const val PREFS_NAME = "CHAT_APP_$VERSION"

const val USER_EMAIL = "USER_EMAIL_$VERSION"

const val USER_PWD = "USER_PWD_$VERSION"

const val ENDLESS_ALARM = "ENDLESS_ALARM_$VERSION"

private fun getPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

fun putString(context: Context, prefsKey: String, value: String) {
    val sharedPrefs = getPreferences(context)
    sharedPrefs.edit().let {
        it.putString(prefsKey, value)
        it.apply()
    }
}

fun getString(context: Context, prefsKey: String): String? {
    val sharedPrefs = getPreferences(context)
    return sharedPrefs.getString(prefsKey, "")
}

fun putBoolean(context: Context, prefsKey: String, value: Boolean) {
    val sharedPrefs = getPreferences(context)
    sharedPrefs.edit().let {
        it.putBoolean(prefsKey, value)
        it.apply()
    }
}

fun getBoolean(context: Context, prefsKey: String): Boolean {
    val sharedPrefs = getPreferences(context)
    return sharedPrefs.getBoolean(prefsKey, false)
}