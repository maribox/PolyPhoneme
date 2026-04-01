package it.bosler.polyphoneme.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import it.bosler.polyphoneme.model.IpaPosition
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.bosler.polyphoneme.model.AppSettings
import it.bosler.polyphoneme.model.Chapter
import it.bosler.polyphoneme.model.DarkModePreference
import it.bosler.polyphoneme.model.ReaderBackground
import it.bosler.polyphoneme.model.ReaderFont
import it.bosler.polyphoneme.model.ReadingMode
import it.bosler.polyphoneme.model.Token
import it.bosler.polyphoneme.ui.theme.LocalReaderStyle
import it.bosler.polyphoneme.ui.theme.ReaderAmoledBg
import it.bosler.polyphoneme.ui.theme.ReaderDarkBg
import it.bosler.polyphoneme.ui.theme.ReaderSepiaBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    initialChapterIndex: Int,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel(),
) {
    val chapter by viewModel.chapter.collectAsState()
    val chapterIndex by viewModel.chapterIndex.collectAsState()
    val chapterCount by viewModel.chapterCount.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val toc by viewModel.toc.collectAsState()
    val firstContentChapterIndex by viewModel.firstContentChapterIndex.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val bookLanguage by viewModel.bookLanguage.collectAsState()
    val bookTranslationFrequency by viewModel.bookTranslationFrequency.collectAsState()
    val selectedWord by viewModel.selectedWord.collectAsState()
    val selectedPhrase by viewModel.selectedPhrase.collectAsState()
    val phraseTranslation by viewModel.phraseTranslation.collectAsState()
    var showToc by remember { mutableStateOf(false) }
    var showFontControls by remember { mutableStateOf(false) }
    var currentPage by remember(chapterIndex) { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(1) }
    // Land on last page when navigating to previous chapter
    var startFromLastPage by remember { mutableStateOf(false) }
    LaunchedEffect(totalPages) {
        if (startFromLastPage && totalPages > 1) {
            currentPage = totalPages - 1
            startFromLastPage = false
        }
    }
    var scrollProgress by remember { mutableFloatStateOf(0f) }
    val isPageMode = settings.readingMode == ReadingMode.PAGE
    val effectiveTranslationFreq = settings.translationFrequency
    val chapterProgress = if (isPageMode) {
        if (totalPages <= 1) 0f else currentPage.toFloat() / (totalPages - 1)
    } else {
        scrollProgress
    }

    BackHandler {
        when {
            showFontControls -> showFontControls = false
            showToc -> showToc = false
            else -> onBack()
        }
    }

    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId, initialChapterIndex)
    }

    val readerStyle = LocalReaderStyle.current
    val readerBg = readerStyle.background ?: MaterialTheme.colorScheme.surface

    Scaffold(
        bottomBar = {
            Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 2.dp,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    ) {
                        // Chapter progress bar
                        LinearProgressIndicator(
                            progress = { chapterProgress },
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Page arrow: prev (page mode only)
                            if (isPageMode) {
                                RepeatingIconButton(
                                    onClick = {
                                        if (currentPage > 0) currentPage--
                                        else if (chapterIndex > 0) {
                                            startFromLastPage = true
                                            viewModel.prevChapter()
                                        }
                                    },
                                    enabled = currentPage > 0 || chapterIndex > 0,
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous page")
                                }
                            }

                            // Center: chapter title + meta, clickable → TOC
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null,
                                    ) { showToc = true }
                                    .padding(horizontal = 8.dp),
                            ) {
                                val tocTitle = toc.firstOrNull { it.index == chapterIndex }?.title
                                Text(
                                    text = tocTitle ?: "Chapter ${chapterIndex + 1}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isLoading) MaterialTheme.colorScheme.outline
                                            else MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            IconButton(onClick = { showFontControls = !showFontControls }) {
                                Icon(Icons.Default.FormatSize, contentDescription = "Display settings")
                            }

                            // Page arrow: next (page mode only)
                            if (isPageMode) {
                                RepeatingIconButton(
                                    onClick = {
                                        if (currentPage < totalPages - 1) currentPage++
                                        else if (chapterIndex < chapterCount - 1) viewModel.nextChapter()
                                    },
                                    enabled = currentPage < totalPages - 1 || chapterIndex < chapterCount - 1,
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next page")
                                }
                            }
                        }
                    }
                }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(readerBg).padding(padding)) {
            val currentChapter = chapter
            if (currentChapter != null && !isLoading) {
                when (settings.readingMode) {
                    ReadingMode.SCROLL -> ScrollModeContent(
                        chapter = currentChapter,
                        settings = settings,
                        chapterIndex = chapterIndex,
                        chapterCount = chapterCount,
                        translationFrequency = effectiveTranslationFreq,
                        selectedPhrase = selectedPhrase,
                        onWordTapIndexed = { token, pIdx, tIdx -> viewModel.selectWordInParagraph(token, pIdx, tIdx) },
                        onNextChapter = { viewModel.nextChapter() },
                        onProgressChange = { scrollProgress = it },
                    )
                    ReadingMode.PAGE -> PageModeContent(
                        chapter = currentChapter,
                        settings = settings,
                        chapterIndex = chapterIndex,
                        chapterCount = chapterCount,
                        currentPage = currentPage,
                        translationFrequency = effectiveTranslationFreq,
                        selectedPhrase = selectedPhrase,
                        onCurrentPageChange = { currentPage = it },
                        onTotalPagesChange = { totalPages = it },
                        onWordTapIndexed = { token, pIdx, tIdx -> viewModel.selectWordInParagraph(token, pIdx, tIdx) },
                        onNextChapter = { viewModel.nextChapter() },
                        onPrevChapter = { viewModel.prevChapter() },
                    )
                }
            }

            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                CircularProgressIndicator()
            }

            // Scrim — closes settings panel when tapping outside
            if (showFontControls) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                        ) { showFontControls = false },
                )
            }

            // Settings panel overlay — inside padded content, sits above bottom bar
            AnimatedVisibility(
                visible = showFontControls,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                ReaderSettingsPanel(
                    settings = settings,
                    onFontSizeDown = { viewModel.updateFontSize(-1) },
                    onFontSizeUp = { viewModel.updateFontSize(1) },
                    onTranslationFrequency = { viewModel.updateTranslationFrequency(it) },
                    onIpaEnabled = { viewModel.updateIpaEnabled(it) },
                    onIpaPosition = { viewModel.updateIpaPosition(it) },
                    onReaderBackground = { viewModel.updateReaderBackground(it) },
                    onReaderFont = { viewModel.updateReaderFont(it) },
                    onLineSpacing = { viewModel.updateLineSpacing(it) },
                    onLetterSpacing = { viewModel.updateLetterSpacing(it) },
                    onWordSpacing = { viewModel.updateWordSpacing(it) },
                )
            }
        }
    }

    val word = selectedWord
    val phrase = selectedPhrase
    if (word != null) {
        WordDetailSheet(
            token = word,
            bookLanguage = bookLanguage,
            nativeLanguage = settings.nativeLanguage,
            onDismiss = { viewModel.clearSelection() },
            onSpeak = { text, lang -> viewModel.speak(text, lang) },
        )
    } else if (phrase != null) {
        val currentChapter = chapter
        if (currentChapter != null && phrase.first < currentChapter.paragraphs.size) {
            val para = currentChapter.paragraphs[phrase.first]
            val phraseTokens = para.tokens.subList(phrase.second, phrase.third.coerceAtMost(para.tokens.size))
            PhraseDetailSheet(
                tokens = phraseTokens,
                phraseTranslation = phraseTranslation,
                bookLanguage = bookLanguage,
                onDismiss = { viewModel.clearSelection() },
                onSpeak = { text, lang -> viewModel.speak(text, lang) },
            )
        }
    }

    // TOC overlay — full-screen side panel, slides in from right
    AnimatedVisibility(
        visible = showToc,
        enter = fadeIn() + slideInVertically { it / 4 },
        exit = fadeOut() + slideOutVertically { it / 4 },
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                ) { showToc = false },
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.statusBarsPadding()) {
                val tocListState = rememberLazyListState(
                    initialFirstVisibleItemIndex = (toc.indexOfFirst { it.index == chapterIndex }).coerceAtLeast(0),
                )
                LazyColumn(
                    state = tocListState,
                    contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                        ) { /* consume to prevent accidental close */ },
                ) {
                    items(toc.size) { i ->
                        val entry = toc[i]
                        if (entry.index == firstContentChapterIndex && i > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                                Text(
                                    text = "book starts",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                        val isCurrent = entry.index == chapterIndex
                        val bgColor = if (isCurrent)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.surface
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgColor)
                                .clickable {
                                    viewModel.goToChapter(entry.index)
                                    showToc = false
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                text = "${entry.index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.width(28.dp),
                            )
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${entry.index * 100 / chapterCount.coerceAtLeast(1)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(36.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Scroll Mode ─────────────────────────────────────────────────────────────

@Composable
private fun ScrollModeContent(
    chapter: Chapter,
    settings: AppSettings,
    chapterIndex: Int,
    chapterCount: Int,
    translationFrequency: Float,
    selectedPhrase: Triple<Int, Int, Int>?,
    onWordTapIndexed: (Token, Int, Int) -> Unit,
    onNextChapter: () -> Unit,
    onProgressChange: (Float) -> Unit,
) {
    val listState = rememberLazyListState()
    var overscrollAmount by remember { mutableFloatStateOf(0f) }
    val hasNext = chapterIndex < chapterCount - 1

    val isAtEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()
            lastVisible != null && lastVisible.index >= info.totalItemsCount - 1
        }
    }

    // Threshold = half the screen height
    val screenHeightPx = listState.layoutInfo.viewportSize.height.toFloat().let {
        if (it > 0f) it else 1000f
    }
    val overscrollThreshold = screenHeightPx * 0.5f

    val nestedScrollConnection = remember(chapterIndex, hasNext) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Only respond to direct touch drag, not fling inertia
                if (source != NestedScrollSource.UserInput) return Offset.Zero

                if (isAtEnd && available.y < 0 && hasNext) {
                    overscrollAmount = (overscrollAmount + (-available.y))
                        .coerceAtMost(overscrollThreshold * 1.3f)
                    return available
                }
                if (overscrollAmount > 0 && available.y > 0) {
                    val used = available.y.coerceAtMost(overscrollAmount)
                    overscrollAmount -= used
                    return Offset(0f, used)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // Called when user lifts finger — decide whether to advance
                if (overscrollAmount >= overscrollThreshold && hasNext) {
                    overscrollAmount = 0f
                    onNextChapter()
                    return available // consume the fling
                }
                // Not past threshold — reset
                overscrollAmount = 0f
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(chapterIndex) {
        overscrollAmount = 0f
        listState.scrollToItem(0)
    }

    val scrollProgress by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            if (total == 0) 0f
            else {
                val first = listState.firstVisibleItemIndex.toFloat()
                val offset = listState.firstVisibleItemScrollOffset.toFloat()
                val itemHeight = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 1f
                ((first + offset / itemHeight) / total).coerceIn(0f, 1f)
            }
        }
    }

    LaunchedEffect(scrollProgress) {
        onProgressChange(scrollProgress)
    }

    Box(modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        ) {
            items(
                count = chapter.paragraphs.size,
                key = { it },
            ) { index ->
                val phraseRange = if (selectedPhrase != null && selectedPhrase.first == index)
                    selectedPhrase.second until selectedPhrase.third else null
                ParagraphRow(
                    paragraph = chapter.paragraphs[index],
                    ipaPosition = settings.ipaPosition,
                    ipaEnabled = settings.ipaEnabled,
                    fontSize = settings.fontSize,
                    lineSpacing = settings.lineSpacing,
                    letterSpacing = settings.letterSpacing,
                    wordSpacing = settings.wordSpacing,
                    translationFrequency = translationFrequency,
                    paragraphIndex = index,
                    selectedPhraseRange = phraseRange,
                    onWordTapIndexed = onWordTapIndexed,
                )
            }
            item { Spacer(modifier = Modifier.height(48.dp)) }
        }

        // Overscroll next-chapter indicator
        if (overscrollAmount > 0 && hasNext) {
            val progress = (overscrollAmount / overscrollThreshold).coerceIn(0f, 1f)
            val pastThreshold = progress >= 1f

            Box(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(40.dp),
                        color = if (pastThreshold) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        strokeWidth = 3.dp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (pastThreshold) "Release for next chapter" else "Keep scrolling...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── Page Mode ───────────────────────────────────────────────────────────────

@Composable
private fun PageModeContent(
    chapter: Chapter,
    settings: AppSettings,
    chapterIndex: Int,
    chapterCount: Int,
    currentPage: Int,
    translationFrequency: Float,
    selectedPhrase: Triple<Int, Int, Int>?,
    onCurrentPageChange: (Int) -> Unit,
    onTotalPagesChange: (Int) -> Unit,
    onWordTapIndexed: (Token, Int, Int) -> Unit,
    onNextChapter: () -> Unit,
    onPrevChapter: () -> Unit,
) {
    var pageBreaks by remember { mutableStateOf<List<IntRange>>(emptyList()) }
    val totalPages = pageBreaks.size.coerceAtLeast(1)

    LaunchedEffect(totalPages) {
        onTotalPagesChange(totalPages)
    }

    var accumulatedDrag by remember { mutableStateOf(0f) }

    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(currentPage, totalPages, chapterIndex, chapterCount) {
            val minSwipeDistance = size.width * 0.12f
            detectHorizontalDragGestures(
                onDragStart = { accumulatedDrag = 0f },
                onDragEnd = {
                    when {
                        accumulatedDrag < -minSwipeDistance -> {
                            // Swipe left → next page
                            if (currentPage < totalPages - 1) onCurrentPageChange(currentPage + 1)
                            else if (chapterIndex < chapterCount - 1) onNextChapter()
                        }
                        accumulatedDrag > minSwipeDistance -> {
                            // Swipe right → previous page
                            if (currentPage > 0) onCurrentPageChange(currentPage - 1)
                            else if (chapterIndex > 0) onPrevChapter()
                        }
                    }
                    accumulatedDrag = 0f
                },
                onDragCancel = { accumulatedDrag = 0f },
                onHorizontalDrag = { _, dragAmount -> accumulatedDrag += dragAmount },
            )
        },
    ) {
        PaginatedContent(
            paragraphs = chapter.paragraphs,
            settings = settings,
            currentPage = currentPage,
            translationFrequency = translationFrequency,
            selectedPhrase = selectedPhrase,
            onWordTapIndexed = onWordTapIndexed,
            onPageBreaksComputed = { pageBreaks = it },
        )
    }
}

@Composable
private fun PaginatedContent(
    paragraphs: List<it.bosler.polyphoneme.model.Paragraph>,
    settings: AppSettings,
    currentPage: Int,
    translationFrequency: Float,
    selectedPhrase: Triple<Int, Int, Int>?,
    onWordTapIndexed: (Token, Int, Int) -> Unit,
    onPageBreaksComputed: (List<IntRange>) -> Unit,
) {
    val horizontalPaddingPx = with(LocalDensity.current) { 24.dp.roundToPx() }
    val verticalPaddingPx = with(LocalDensity.current) { 20.dp.roundToPx() }

    SubcomposeLayout { constraints ->
        val contentConstraints = androidx.compose.ui.unit.Constraints(
            maxWidth = constraints.maxWidth - horizontalPaddingPx * 2,
        )
        val contentHeight = constraints.maxHeight - verticalPaddingPx * 2

        // Measure each paragraph's actual height
        val heights = paragraphs.mapIndexed { index, paragraph ->
            subcompose("measure_$index") {
                ParagraphRow(
                    paragraph = paragraph,
                    ipaPosition = settings.ipaPosition,
                    ipaEnabled = settings.ipaEnabled,
                    fontSize = settings.fontSize,
                    lineSpacing = settings.lineSpacing,
                    letterSpacing = settings.letterSpacing,
                    wordSpacing = settings.wordSpacing,
                    translationFrequency = translationFrequency,
                    onWordTap = {},
                )
            }.first().measure(contentConstraints).height
        }

        // Compute page breaks from real heights
        val pages = mutableListOf<IntRange>()
        var pageStart = 0
        var usedHeight = 0
        for (i in heights.indices) {
            val h = heights[i]
            if (usedHeight + h > contentHeight && i > pageStart) {
                pages.add(pageStart until i)
                pageStart = i
                usedHeight = h
            } else {
                usedHeight += h
            }
        }
        if (pageStart < paragraphs.size) {
            pages.add(pageStart until paragraphs.size)
        }
        if (pages.isEmpty()) {
            pages.add(0 until 0)
        }

        onPageBreaksComputed(pages)

        // Render only the current page
        val safePageIndex = currentPage.coerceIn(0, pages.size - 1)
        val pageRange = pages[safePageIndex]

        val pageContent = subcompose("page_content") {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                for (i in pageRange) {
                    val phraseRange = if (selectedPhrase != null && selectedPhrase.first == i)
                        selectedPhrase.second until selectedPhrase.third else null
                    ParagraphRow(
                        paragraph = paragraphs[i],
                        ipaPosition = settings.ipaPosition,
                        ipaEnabled = settings.ipaEnabled,
                        fontSize = settings.fontSize,
                        lineSpacing = settings.lineSpacing,
                        letterSpacing = settings.letterSpacing,
                        wordSpacing = settings.wordSpacing,
                        translationFrequency = translationFrequency,
                        paragraphIndex = i,
                        selectedPhraseRange = phraseRange,
                        onWordTapIndexed = onWordTapIndexed,
                    )
                }
            }
        }

        val placeable = pageContent.first().measure(constraints)
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(0, 0)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReaderSettingsPanel(
    settings: AppSettings,
    onFontSizeDown: () -> Unit,
    onFontSizeUp: () -> Unit,
    onTranslationFrequency: (Float) -> Unit,
    onIpaEnabled: (Boolean) -> Unit,
    onIpaPosition: (IpaPosition) -> Unit,
    onReaderBackground: (ReaderBackground) -> Unit,
    onReaderFont: (ReaderFont) -> Unit,
    onLineSpacing: (Float) -> Unit,
    onLetterSpacing: (Float) -> Unit,
    onWordSpacing: (Float) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Font size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onFontSizeDown) { Text("A", fontSize = 14.sp) }
                Text(
                    text = "${settings.fontSize}sp",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                TextButton(onClick = onFontSizeUp) { Text("A", fontSize = 22.sp) }
            }

            // Translation frequency (global)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Transl.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(52.dp),
                )
                Slider(
                    value = settings.translationFrequency,
                    onValueChange = onTranslationFrequency,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = when {
                        settings.translationFrequency <= 0.01f -> "Off"
                        settings.translationFrequency >= 0.99f -> "All"
                        else -> "${(settings.translationFrequency * 100).toInt()}%"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

            // IPA toggle + position
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = { onIpaEnabled(!settings.ipaEnabled) },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = if (settings.ipaEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.width(52.dp),
                ) {
                    Text(
                        text = "IPA",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (settings.ipaEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.then(if (!settings.ipaEnabled) Modifier.alpha(0.35f) else Modifier),
                ) {
                    listOf(
                        IpaPosition.ABOVE, IpaPosition.BELOW,
                        IpaPosition.BEFORE, IpaPosition.BEHIND,
                        IpaPosition.REPLACE,
                    ).forEach { pos ->
                        val isSel = settings.ipaPosition == pos
                        val dotColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        val barColor = if (isSel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
                        Surface(
                            onClick = { onIpaPosition(pos) },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(42.dp, 34.dp),
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) {
                                when (pos) {
                                    IpaPosition.ABOVE -> Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            repeat(3) { Box(Modifier.size(3.dp).background(dotColor, androidx.compose.foundation.shape.CircleShape)) }
                                        }
                                        Box(Modifier.size(22.dp, 3.dp).background(barColor, androidx.compose.foundation.shape.RoundedCornerShape(2.dp)))
                                    }
                                    IpaPosition.BELOW -> Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Box(Modifier.size(22.dp, 3.dp).background(barColor, androidx.compose.foundation.shape.RoundedCornerShape(2.dp)))
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            repeat(3) { Box(Modifier.size(3.dp).background(dotColor, androidx.compose.foundation.shape.CircleShape)) }
                                        }
                                    }
                                    IpaPosition.BEFORE -> Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            repeat(2) { Box(Modifier.size(3.dp).background(dotColor, androidx.compose.foundation.shape.CircleShape)) }
                                        }
                                        Box(Modifier.size(14.dp, 3.dp).background(barColor, androidx.compose.foundation.shape.RoundedCornerShape(2.dp)))
                                    }
                                    IpaPosition.BEHIND -> Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Box(Modifier.size(14.dp, 3.dp).background(barColor, androidx.compose.foundation.shape.RoundedCornerShape(2.dp)))
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            repeat(2) { Box(Modifier.size(3.dp).background(dotColor, androidx.compose.foundation.shape.CircleShape)) }
                                        }
                                    }
                                    IpaPosition.REPLACE -> Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        repeat(3) { Box(Modifier.size(3.dp).background(dotColor, androidx.compose.foundation.shape.CircleShape)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Reader background
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Background",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(76.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        ReaderBackground.DEFAULT to ("Default" to MaterialTheme.colorScheme.surface),
                        ReaderBackground.SEPIA to ("Sepia" to ReaderSepiaBg),
                        ReaderBackground.DARK to ("Dark" to ReaderDarkBg),
                        ReaderBackground.AMOLED to ("AMOLED" to ReaderAmoledBg),
                    ).forEach { (bg, labelAndColor) ->
                        val (label, color) = labelAndColor
                        val isSel = settings.readerBackground == bg
                        Surface(
                            onClick = { onReaderBackground(bg) },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            color = color,
                            border = androidx.compose.foundation.BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (bg == ReaderBackground.DEFAULT) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color(0xFF3D2B1A),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
            }

            // Reader font
            val loraFont = it.bosler.polyphoneme.ui.theme.rememberReaderFontFamily(ReaderFont.LORA)
            val merriFont = it.bosler.polyphoneme.ui.theme.rememberReaderFontFamily(ReaderFont.MERRIWEATHER)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Font",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(52.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    listOf(
                        ReaderFont.DEFAULT      to ("Sans"    to androidx.compose.ui.text.font.FontFamily.Default),
                        ReaderFont.LORA         to ("Lora"    to loraFont),
                        ReaderFont.MERRIWEATHER to ("Merri"   to merriFont),
                        ReaderFont.SERIF        to ("Serif"   to androidx.compose.ui.text.font.FontFamily.Serif),
                        ReaderFont.MONO         to ("Mono"    to androidx.compose.ui.text.font.FontFamily.Monospace),
                        ReaderFont.CURSIVE      to ("Cursive" to androidx.compose.ui.text.font.FontFamily.Cursive),
                    ).forEach { (font, labelAndFont) ->
                        val (label, fontFamily) = labelAndFont
                        val isSel = settings.readerFont == font
                        Surface(
                            onClick = { onReaderFont(font) },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = fontFamily),
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

            // Spacing — local state so dragging is instant; persist only on release
            var lineVal   by remember(settings.lineSpacing)   { mutableStateOf(settings.lineSpacing) }
            var letterVal by remember(settings.letterSpacing) { mutableStateOf(settings.letterSpacing) }
            var wordVal   by remember(settings.wordSpacing)   { mutableStateOf(settings.wordSpacing) }

            // Spacing presets
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            ) {
                data class SpacingPreset(val label: String, val line: Float, val letter: Float, val word: Float)
                val presets = listOf(
                    SpacingPreset("Compact", 0.9f, 0f, 2f),
                    SpacingPreset("Normal", 1.2f, 0f, 4f),
                    SpacingPreset("Comfy", 1.6f, 0.5f, 6f),
                    SpacingPreset("Spacious", 2.2f, 1f, 10f),
                )
                presets.forEach { preset ->
                    val isSel = kotlin.math.abs(lineVal - preset.line) < 0.05f &&
                        kotlin.math.abs(letterVal - preset.letter) < 0.05f &&
                        kotlin.math.abs(wordVal - preset.word) < 0.5f
                    Surface(
                        onClick = {
                            lineVal = preset.line; letterVal = preset.letter; wordVal = preset.word
                            onLineSpacing(preset.line); onLetterSpacing(preset.letter); onWordSpacing(preset.word)
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            listOf(
                Triple("Line",   lineVal,   0.8f..3.0f),
                Triple("Letter", letterVal, 0f..4f),
                Triple("Word",   wordVal,   0f..20f),
            ).forEachIndexed { i, (label, value, range) ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(52.dp),
                    )
                    Slider(
                        value = value,
                        onValueChange = { v ->
                            when (i) { 0 -> lineVal = v; 1 -> letterVal = v; 2 -> wordVal = v }
                        },
                        onValueChangeFinished = {
                            when (i) { 0 -> onLineSpacing(lineVal); 1 -> onLetterSpacing(letterVal); 2 -> onWordSpacing(wordVal) }
                        },
                        valueRange = range,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "%.1f".format(value),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun RepeatingIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var repeatJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.pointerInput(enabled) {
            awaitPointerEventScope {
                while (true) {
                    // wait for press
                    var event = awaitPointerEvent()
                    if (event.changes.none { it.pressed } || !enabled) continue
                    event.changes.forEach { it.consume() }
                    repeatJob = scope.launch {
                        delay(400)
                        while (true) {
                            onClick()
                            delay(150)
                        }
                    }
                    // wait for release
                    while (true) {
                        event = awaitPointerEvent()
                        if (event.changes.all { !it.pressed }) break
                    }
                    repeatJob?.cancel()
                    repeatJob = null
                }
            }
        },
    ) {
        content()
    }
}

private fun readerLanguageDisplayName(code: String): String {
    return when (code.lowercase().split("-", "_").first()) {
        "en" -> "English"
        "de" -> "German"
        "fr" -> "French"
        "es" -> "Spanish"
        "it" -> "Italian"
        "pt" -> "Portuguese"
        "nl" -> "Dutch"
        "ru" -> "Russian"
        "ja" -> "Japanese"
        "zh" -> "Chinese"
        "ko" -> "Korean"
        "ar" -> "Arabic"
        "pl" -> "Polish"
        "sv" -> "Swedish"
        "da" -> "Danish"
        "no" -> "Norwegian"
        "fi" -> "Finnish"
        "cs" -> "Czech"
        "tr" -> "Turkish"
        "el" -> "Greek"
        "hu" -> "Hungarian"
        "ro" -> "Romanian"
        "uk" -> "Ukrainian"
        "hi" -> "Hindi"
        else -> code.uppercase()
    }
}
