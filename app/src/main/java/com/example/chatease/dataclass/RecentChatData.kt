package com.example.chatease.dataclass

// Data class representing recent chat information
data class RecentChatData(
    val id: String, // Unique identifier for the user
    var displayName: String, // Display name of the user
    var avatar: String, // Avatar URL for the user's profile picture
    val lastMessage: String, // The last message exchanged in the chat
    val lastMessageSender: String, // The sender of the last message
    val lastMessageTimeStamp: String, // Timestamp of the last message formatted for display
    val timestamp: String, // Original server timestamp for sorting
    val isLastMessageReadByMe: Boolean // To check if the last message from the other used is read by me
)
