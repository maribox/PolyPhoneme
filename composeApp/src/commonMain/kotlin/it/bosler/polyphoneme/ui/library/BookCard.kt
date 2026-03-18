package it.bosler.polyphoneme.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.bosler.polyphoneme.model.BookMeta

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCard(
    book: BookMeta,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    coverImage: @Composable ((Modifier) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        // Cover
        Surface(
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.65f),
        ) {
            if (coverImage != null) {
                coverImage(Modifier.fillMaxSize())
            } else {
                // Generated cover with title text
                GeneratedCover(book)
            }

            // Language badge overlay
            book.language?.let { lang ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.75f),
                    ) {
                        Text(
                            text = lang.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                        )
                    }
                }
            }

            // Progress bar at bottom of cover
            if (book.chapterCount > 1) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    LinearProgressIndicator(
                        progress = { (book.lastReadChapter + 1).toFloat() / book.chapterCount },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Black.copy(alpha = 0.3f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = book.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
        )

        // Author
        Text(
            text = book.author,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun GeneratedCover(book: BookMeta) {
    val hue = (book.title.hashCode() and 0x7FFFFFFF) % 360
    val baseColor = Color.hsl(hue.toFloat(), 0.35f, 0.35f)
    val lightColor = Color.hsl(hue.toFloat(), 0.25f, 0.5f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(lightColor, baseColor),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.sp,
            )
        }
    }
}
