package com.example.chatease.dataclass

data class GroupMessageData(
    val id: String,
    val senderName: String,
    val senderID: String,
    val content: String,
    val formattedTimestamp: String,
    val timestamp: Long,
    val hasRead: Boolean = false,
    var everyoneRead: Boolean = false
)