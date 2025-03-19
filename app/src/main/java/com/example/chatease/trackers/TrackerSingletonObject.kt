package com.example.chatease.trackers

import java.util.concurrent.atomic.AtomicBoolean

object TrackerSingletonObject {
    var activeNotification = mutableSetOf<String>()
    var hasMessageArrived = AtomicBoolean(false)
    var isChatActivityOpenedViaNotification = AtomicBoolean(false)
    var isTyping = AtomicBoolean(false)
    var isAppForeground = AtomicBoolean(false)
    var chatPartnerUserID : String? = null
    var groupChatID : String? = null
}