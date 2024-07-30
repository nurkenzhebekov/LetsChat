package com.example.letschat.utils

import android.annotation.SuppressLint
import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtil {

    @SuppressLint("SimpleDateFormat")
    fun getFormattedTime(timeInMillis: Long): String {
        val date = Date(timeInMillis)
        val fullFormattedTime = SimpleDateFormat("d/M, h:mm a")
        val onlyTime = SimpleDateFormat("h:mm a")

        return when {
            isToday(date) -> onlyTime.format(date)
            else -> fullFormattedTime.format(date)
        }
    }

    private fun isToday(d: Date): Boolean {
        return DateUtils.isToday(d.time)
    }

    @SuppressLint("SimpleDateFormat")
    fun createNotifyId(timeInMillis: Long): Int {
        return SimpleDateFormat("ddHHmmss").format(timeInMillis).toInt()
    }

}