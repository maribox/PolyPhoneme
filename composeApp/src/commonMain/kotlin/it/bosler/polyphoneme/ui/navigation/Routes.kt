package it.bosler.polyphoneme.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object LibraryRoute

@Serializable
data class ReaderRoute(val bookId: String, val chapterIndex: Int = 0)

@Serializable
object SettingsRoute

@Serializable
object AboutRoute
