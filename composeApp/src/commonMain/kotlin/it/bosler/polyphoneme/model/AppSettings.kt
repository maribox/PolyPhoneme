package it.bosler.polyphoneme.model

enum class IpaPosition { ABOVE, BELOW, BEFORE, BEHIND, REPLACE }

enum class ReadingMode { SCROLL, PAGE }

data class AppSettings(
    val nativeLanguage: String = "en",
    val ipaPosition: IpaPosition = IpaPosition.BELOW,
    val translationFrequency: Float = 0.5f,
    val fontSize: Int = 16,
    val lineSpacing: Float = 1.5f,
    val readingMode: ReadingMode = ReadingMode.PAGE,
    val hasSeenPageModeTutorial: Boolean = false,
)
