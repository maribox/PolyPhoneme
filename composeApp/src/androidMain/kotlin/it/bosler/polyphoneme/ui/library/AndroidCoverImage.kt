package it.bosler.polyphoneme.ui.library

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File

@Composable
actual fun CoverImage(path: String, modifier: Modifier) {
    val bitmap = remember(path) {
        try {
            val file = File(path)
            if (file.exists()) {
                BitmapFactory.decodeFile(path)?.asImageBitmap()
            } else null
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Book cover",
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}
