package it.bosler.polyphoneme.model

import kotlinx.serialization.Serializable

@Serializable
data class BookMeta(
    val id: String,
    val title: String,
    val author: String,
    val language: String? = null,
    val filePath: String,
    val coverPath: String? = null,
    val chapterCount: Int,
    val lastReadChapter: Int = 0,
    val importedAt: Long,
)
