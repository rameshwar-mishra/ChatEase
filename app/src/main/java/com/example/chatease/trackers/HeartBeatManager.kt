package com.example.chatease.trackers

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore


object HeartBeatManager {
    private const val HEARTBEATINTERVAL = 30 * 1000L // 30 Seconds
    private lateinit var heartBeatHandler: Handler
    private lateinit var heartBeatRunnable: Runnable
    private var isHearBeatRunning = false

    fun startHeartBeat(userID: String) {
        if (!isHearBeatRunning) {
            heartBeatHandler = Handler(Looper.getMainLooper())
            heartBeatRunnable = Runnable {
                Firebase.firestore.collection("users").document(userID)
                    .update("lastHeartBeat", FieldValue.serverTimestamp())
                    .addOnFailureListener { e ->
                        Log.e("HeartBeatUpdationFailed", e.stackTrace.toString())
                    }
                heartBeatHandler.postDelayed(heartBeatRunnable, HEARTBEATINTERVAL)
            }
            heartBeatHandler.post(heartBeatRunnable)
            isHearBeatRunning = true
        }
    }

    fun stopHeartBeat() {
        if (isHearBeatRunning) {
            heartBeatHandler.removeCallbacksAndMessages(heartBeatRunnable)
            isHearBeatRunning = false
        }
    }
}