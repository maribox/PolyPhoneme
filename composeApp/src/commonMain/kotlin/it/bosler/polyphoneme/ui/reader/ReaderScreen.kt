package it.bosler.polyphoneme.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import it.bosler.polyphoneme.model.ReadingMode
import it.bosler.polyphoneme.model.Token

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
    val isLoading by viewModel.isLoading.collectAsState()
    val bookLanguage by viewModel.bookLanguage.collectAsState()
    val selectedWord by viewModel.selectedWord.collectAsState()
    var showToc by remember { mutableStateOf(false) }
    var showFontControls by remember { mutableStateOf(false) }

    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId, initialChapterIndex)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = chapter?.title ?: "Loading...",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${chapterIndex + 1} of $chapterCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "  \u00b7  ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                                Text(
                                    text = bookLanguage.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showFontControls = !showFontControls }) {
                            Icon(Icons.Default.FormatSize, contentDescription = "Font size")
                        }
                        IconButton(onClick = { showToc = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Table of Contents")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
                AnimatedVisibility(visible = showFontControls) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 1.dp,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = { viewModel.updateFontSize(-1) }) {
                                Text("A", fontSize = 14.sp)
                            }
                            Text(
                                text = "${settings.fontSize}",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            TextButton(onClick = { viewModel.updateFontSize(1) }) {
                                Text("A", fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val currentChapter = chapter
            if (currentChapter != null && !isLoading) {
                when (settings.readingMode) {
                    ReadingMode.SCROLL -> ScrollModeContent(
                        chapter = currentChapter,
                        settings = settings,
                        chapterIndex = chapterIndex,
                        chapterCount = chapterCount,
                        onWordTap = { viewModel.selectWord(it) },
                        onNextChapter = { viewModel.nextChapter() },
                        onPrevChapter = { viewModel.prevChapter() },
                    )
                    ReadingMode.PAGE -> PageModeContent(
                        chapter = currentChapter,
                        settings = settings,
                        chapterIndex = chapterIndex,
                        chapterCount = chapterCount,
                        hasSeenTutorial = settings.hasSeenPageModeTutorial,
                        onWordTap = { viewModel.selectWord(it) },
                        onNextChapter = { viewModel.nextChapter() },
                        onPrevChapter = { viewModel.prevChapter() },
                        onDismissTutorial = { viewModel.dismissPageModeTutorial() },
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
        }
    }

    val word = selectedWord
    if (word != null) {
        WordDetailSheet(
            token = word,
            bookLanguage = bookLanguage,
            nativeLanguage = settings.nativeLanguage,
            onDismiss = { viewModel.selectWord(null) },
            onSpeak = { text, lang -> viewModel.speak(text, lang) },
        )
    }

    if (showToc) {
        ModalBottomSheet(
            onDismissRequest = { showToc = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = "Table of Contents",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
                HorizontalDivider()
                LazyColumn {
                    items(toc.size) { i ->
                        val entry = toc[i]
                        val isCurrent = entry.index == chapterIndex
                        TextButton(
                            onClick = {
                                viewModel.goToChapter(entry.index)
                                showToc = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "${entry.index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(32.dp),
                                )
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            }
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
    onWordTap: (Token) -> Unit,
    onNextChapter: () -> Unit,
    onPrevChapter: () -> Unit,
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

    Column(modifier = Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            progress = { scrollProgress },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        Box(modifier = Modifier.weight(1f).nestedScroll(nestedScrollConnection)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            ) {
                items(
                    count = chapter.paragraphs.size,
                    key = { it },
                ) { index ->
                    ParagraphRow(
                        paragraph = chapter.paragraphs[index],
                        ipaPosition = settings.ipaPosition,
                        fontSize = settings.fontSize,
                        lineSpacing = settings.lineSpacing,
                        onWordTap = onWordTap,
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

        // Bottom bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevChapter, enabled = chapterIndex > 0) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                }
                Text(
                    text = "${(scrollProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = onNextChapter, enabled = hasNext) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
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
    hasSeenTutorial: Boolean,
    onWordTap: (Token) -> Unit,
    onNextChapter: () -> Unit,
    onPrevChapter: () -> Unit,
    onDismissTutorial: () -> Unit,
) {
    var currentPage by remember(chapter) { mutableIntStateOf(0) }
    var showTutorial by remember { mutableStateOf(!hasSeenTutorial) }
    // Stores computed page breaks: list of (startIdx, endIdx) for paragraphs
    var pageBreaks by remember { mutableStateOf<List<IntRange>>(emptyList()) }

    LaunchedEffect(hasSeenTutorial) {
        if (hasSeenTutorial) showTutorial = false
    }

    val totalPages = pageBreaks.size.coerceAtLeast(1)
    val paragraphs = chapter.paragraphs

    Column(modifier = Modifier.fillMaxSize()) {
        // Progress
        LinearProgressIndicator(
            progress = {
                if (totalPages <= 1) 0f
                else currentPage.toFloat() / (totalPages - 1).coerceAtLeast(1)
            },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        // Content area — uses SubcomposeLayout to measure paragraphs and paginate
        Box(modifier = Modifier.weight(1f)) {
            PaginatedContent(
                paragraphs = paragraphs,
                settings = settings,
                currentPage = currentPage,
                onWordTap = onWordTap,
                onPageBreaksComputed = { pageBreaks = it },
            )
        }

        // Bottom bar with page navigation
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        if (currentPage > 0) currentPage--
                        else if (chapterIndex > 0) onPrevChapter()
                    },
                    enabled = currentPage > 0 || chapterIndex > 0,
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous page")
                }
                Text(
                    text = "${currentPage + 1} / $totalPages",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(
                    onClick = {
                        if (currentPage < totalPages - 1) currentPage++
                        else if (chapterIndex < chapterCount - 1) onNextChapter()
                    },
                    enabled = currentPage < totalPages - 1 || chapterIndex < chapterCount - 1,
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next page")
                }
            }
        }
    }

    // Tutorial overlay
    if (showTutorial) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .pointerInput(Unit) {
                    detectTapGestures {
                        showTutorial = false
                        onDismissTutorial()
                    }
                },
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = "Tap to\ngo back",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = "Tap to\ngo forward",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                text = "Tap anywhere to dismiss",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
            )
        }
    }
}

@Composable
private fun PaginatedContent(
    paragraphs: List<it.bosler.polyphoneme.model.Paragraph>,
    settings: AppSettings,
    currentPage: Int,
    onWordTap: (Token) -> Unit,
    onPageBreaksComputed: (List<IntRange>) -> Unit,
) {
    val horizontalPaddingPx = with(LocalDensity.current) { 20.dp.roundToPx() }
    val verticalPaddingPx = with(LocalDensity.current) { 16.dp.roundToPx() }

    SubcomposeLayout { constraints ->
        val contentWidth = constraints.maxWidth - horizontalPaddingPx * 2
        val contentHeight = constraints.maxHeight - verticalPaddingPx * 2

        // Estimate paragraph heights without composing them (avoids OOM on large chapters)
        val fontSizePx = (settings.fontSize * constraints.maxWidth / 360f).toInt().coerceAtLeast(1)
        val isStacked = settings.ipaPosition == IpaPosition.ABOVE || settings.ipaPosition == IpaPosition.BELOW
        val lineHeightPx = if (isStacked) {
            (fontSizePx * 2.2f * settings.lineSpacing).toInt()  // word + IPA stacked
        } else {
            (fontSizePx * 1.6f * settings.lineSpacing).toInt()  // inline IPA
        }
        val avgCharWidth = fontSizePx * 0.52f  // approximate average character width
        val verticalPadding = (8 * constraints.maxWidth / 360f).toInt()  // ~8dp

        val heights = paragraphs.map { paragraph ->
            val totalChars = paragraph.tokens.sumOf { token ->
                val wordLen = token.leadingPunctuation.length + token.word.length + token.trailingPunctuation.length
                if (isStacked) wordLen + 2  // spacing between word blocks
                else {
                    val ipaLen = token.ipa?.length ?: 0
                    wordLen + ipaLen + 2
                }
            }
            val estimatedLines = ((totalChars * avgCharWidth) / contentWidth).toInt().coerceAtLeast(1)
            estimatedLines * lineHeightPx + verticalPadding
        }

        // Compute page breaks
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

        val pageContent = subcompose("page_$safePageIndex") {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                for (i in pageRange) {
                    ParagraphRow(
                        paragraph = paragraphs[i],
                        ipaPosition = settings.ipaPosition,
                        fontSize = settings.fontSize,
                        lineSpacing = settings.lineSpacing,
                        onWordTap = onWordTap,
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
