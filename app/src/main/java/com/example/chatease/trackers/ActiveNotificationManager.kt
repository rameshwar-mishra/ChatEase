package com.example.chatease.trackers

import java.util.concurrent.atomic.AtomicBoolean

object ActiveNotificationManager {
    var activeNotification = mutableSetOf<String>()
    var hasMessageArrived = AtomicBoolean(false)
}