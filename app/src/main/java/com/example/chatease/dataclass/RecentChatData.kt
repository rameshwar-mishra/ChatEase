package com.example.chatease.dataclass

data class RecentChatData(
    val displayName : String,
    val avatar : String,
    val lastMessage : String,
    val lastMessageTimeStamp : String
)