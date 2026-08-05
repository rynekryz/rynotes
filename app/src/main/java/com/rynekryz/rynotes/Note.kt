package com.rynekryz.rynotes

import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val pinned: Boolean = false,
    val favorite: Boolean = false,
    val colorTag: String = NoteColor.DEFAULT.name,
    val folderId: String? = null,
    val tags: List<String> = emptyList(),
    val trashedAt: Long? = null,
    val archivedAt: Long? = null,
    val locked: Boolean = false
)

enum class NoteColor {
    DEFAULT, AMBER, SAGE, SKY, ROSE, LAVENDER
}

data class NoteFolder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorTag: String = NoteColor.DEFAULT.name,
    val createdAt: Long = System.currentTimeMillis()
)
