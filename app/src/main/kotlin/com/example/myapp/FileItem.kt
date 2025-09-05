package com.example.myapp

import android.net.Uri

data class FileItem(
    val name: String,
    val type: String,
    val size: String,
    val date: String,
    var isFavorite: Boolean,
    val uri: Uri
)
