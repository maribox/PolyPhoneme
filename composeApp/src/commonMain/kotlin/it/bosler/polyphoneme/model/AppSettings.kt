package it.bosler.polyphoneme.model

enum class IpaPosition { ABOVE, BELOW, BEFORE, BEHIND, REPLACE }
enum class ReadingMode { SCROLL, PAGE }
enum class AppTheme { INDIGO, SAGE, AMBER, CRIMSON, SLATE }
enum class ReaderBackground { DEFAULT, SEPIA, DARK, AMOLED }
enum class ReaderFont { DEFAULT, SERIF }

data class AppSettings(
    val nativeLanguage: String = "en",
    val ipaPosition: IpaPosition = IpaPosition.BELOW,
    val translationFrequency: Float = 0.5f,
    val fontSize: Int = 16,
    val lineSpacing: Float = 1.5f,
    val readingMode: ReadingMode = ReadingMode.PAGE,
    val hasSeenPageModeTutorial: Boolean = false,
    val languageRegions: Map<String, String> = emptyMap(),
    val appTheme: AppTheme = AppTheme.INDIGO,
    val readerBackground: ReaderBackground = ReaderBackground.DEFAULT,
    val readerFont: ReaderFont = ReaderFont.DEFAULT,
)
