package com.example.chatease.dataclass

data class MessageUserData(
    val id : String,
    val sender : String,
    val content : String,
    val timestamp: String,
    val hasRead : Boolean
)