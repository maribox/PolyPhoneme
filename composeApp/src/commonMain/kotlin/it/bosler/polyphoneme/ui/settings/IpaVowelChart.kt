package it.bosler.polyphoneme.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.bosler.polyphoneme.ui.theme.rememberIpaFontFamily

/**
 * IPA Vowel Trapezoid Chart.
 *
 * All positions are computed mathematically from the trapezoid geometry:
 * - The trapezoid has 4 corners defined in normalized [0,1] space.
 * - Each row is a horizontal slice of the trapezoid at a given y.
 * - The left/right x of each row is interpolated from the trapezoid edges.
 * - "Front" vowels sit at the left edge, "Back" at the right, "Central" at midpoint.
 * - Unrounded/rounded pairs are offset slightly left/right of their column.
 */

// --- Trapezoid geometry ---
// Normalized coordinates [0,1]. Y: 0=top(close), 1=bottom(open).
// Left edge (front): slants inward from top to bottom.
// Right edge (back): nearly vertical.
// Inset from edges so pair offsets don't push vowels outside the canvas
private const val H_MARGIN = 0.06f
private const val FRONT_X_TOP = H_MARGIN          // front-close
private const val FRONT_X_BOTTOM = 0.33f           // front-open: indented right
private const val BACK_X_TOP = 1.0f - H_MARGIN    // back-close
private const val BACK_X_BOTTOM = 0.97f - H_MARGIN // back-open

/** Interpolate the left (front) edge x at a given normalized row y [0..1]. */
private fun frontEdgeX(yNorm: Float) = FRONT_X_TOP + (FRONT_X_BOTTOM - FRONT_X_TOP) * yNorm

/** Interpolate the right (back) edge x at a given normalized row y [0..1]. */
private fun backEdgeX(yNorm: Float) = BACK_X_TOP + (BACK_X_BOTTOM - BACK_X_TOP) * yNorm

// --- Row definitions ---
// 7 rows evenly spaced: y goes from 0/6 to 6/6
private enum class Row(val index: Int, val label: String, val bold: Boolean, val dashed: Boolean) {
    CLOSE(0, "Close", true, false),
    NEAR_CLOSE(1, "Near-close", false, true),
    CLOSE_MID(2, "Close-mid", true, false),
    MID(3, "Mid", false, true),
    OPEN_MID(4, "Open-mid", true, false),
    NEAR_OPEN(5, "Near-open", false, true),
    OPEN(6, "Open", true, false);

    val yNorm: Float get() = index / 6f
}

// --- Column definitions ---
// Front = left edge, Central = midpoint, Back = right edge
// pairOffset: how far left/right the unrounded/rounded pair members sit from the column center
private const val PAIR_OFFSET = 0.045f

private enum class Col {
    FRONT, CENTRAL, BACK;

    /** Get the x position of this column at a given row, in [0,1] space. */
    fun xAt(yNorm: Float): Float {
        val left = frontEdgeX(yNorm)
        val right = backEdgeX(yNorm)
        return when (this) {
            FRONT -> left
            CENTRAL -> (left + right) / 2f
            BACK -> right
        }
    }
}

// --- Vowel data ---
private data class VowelPoint(
    val symbol: String,
    val row: Row,
    val col: Col,
    val rounded: Boolean,
    val description: String,
)

private val VOWELS = listOf(
    // Close
    VowelPoint("i", Row.CLOSE, Col.FRONT, false, "close front unrounded"),
    VowelPoint("y", Row.CLOSE, Col.FRONT, true, "close front rounded"),
    VowelPoint("ɨ", Row.CLOSE, Col.CENTRAL, false, "close central unrounded"),
    VowelPoint("ʉ", Row.CLOSE, Col.CENTRAL, true, "close central rounded"),
    VowelPoint("ɯ", Row.CLOSE, Col.BACK, false, "close back unrounded"),
    VowelPoint("u", Row.CLOSE, Col.BACK, true, "close back rounded"),
    // Near-close
    VowelPoint("ɪ", Row.NEAR_CLOSE, Col.FRONT, false, "near-close front unrounded"),
    VowelPoint("ʏ", Row.NEAR_CLOSE, Col.FRONT, true, "near-close front rounded"),
    VowelPoint("ʊ", Row.NEAR_CLOSE, Col.BACK, true, "near-close back rounded"),
    // Close-mid
    VowelPoint("e", Row.CLOSE_MID, Col.FRONT, false, "close-mid front unrounded"),
    VowelPoint("ø", Row.CLOSE_MID, Col.FRONT, true, "close-mid front rounded"),
    VowelPoint("ɘ", Row.CLOSE_MID, Col.CENTRAL, false, "close-mid central unrounded"),
    VowelPoint("ɵ", Row.CLOSE_MID, Col.CENTRAL, true, "close-mid central rounded"),
    VowelPoint("ɤ", Row.CLOSE_MID, Col.BACK, false, "close-mid back unrounded"),
    VowelPoint("o", Row.CLOSE_MID, Col.BACK, true, "close-mid back rounded"),
    // Mid
    VowelPoint("e̞", Row.MID, Col.FRONT, false, "mid front unrounded"),
    VowelPoint("ø̞", Row.MID, Col.FRONT, true, "mid front rounded"),
    VowelPoint("ə", Row.MID, Col.CENTRAL, false, "mid central (schwa)"),
    VowelPoint("ɤ̞", Row.MID, Col.BACK, false, "mid back unrounded"),
    VowelPoint("o̞", Row.MID, Col.BACK, true, "mid back rounded"),
    // Open-mid
    VowelPoint("ɛ", Row.OPEN_MID, Col.FRONT, false, "open-mid front unrounded"),
    VowelPoint("œ", Row.OPEN_MID, Col.FRONT, true, "open-mid front rounded"),
    VowelPoint("ɜ", Row.OPEN_MID, Col.CENTRAL, false, "open-mid central unrounded"),
    VowelPoint("ɞ", Row.OPEN_MID, Col.CENTRAL, true, "open-mid central rounded"),
    VowelPoint("ʌ", Row.OPEN_MID, Col.BACK, false, "open-mid back unrounded"),
    VowelPoint("ɔ", Row.OPEN_MID, Col.BACK, true, "open-mid back rounded"),
    // Near-open
    VowelPoint("æ", Row.NEAR_OPEN, Col.FRONT, false, "near-open front unrounded"),
    VowelPoint("ɐ", Row.NEAR_OPEN, Col.CENTRAL, false, "near-open central"),
    // Open
    VowelPoint("a", Row.OPEN, Col.FRONT, false, "open front unrounded"),
    VowelPoint("ɶ", Row.OPEN, Col.FRONT, true, "open front rounded"),
    VowelPoint("ä", Row.OPEN, Col.CENTRAL, false, "open central unrounded"),
    VowelPoint("ɑ", Row.OPEN, Col.BACK, false, "open back unrounded"),
    VowelPoint("ɒ", Row.OPEN, Col.BACK, true, "open back rounded"),
)

/** Compute the normalized x position of a vowel, accounting for pair offset. */
private fun VowelPoint.xNorm(): Float {
    val base = col.xAt(row.yNorm)
    // Check if this vowel has a pair partner (another vowel at same row+col)
    val hasPair = VOWELS.any { it !== this && it.row == row && it.col == col }
    if (!hasPair) return base
    return if (rounded) base + PAIR_OFFSET else base - PAIR_OFFSET
}

/** Strip combining diacritics to get the base vowel symbol. */
private fun baseVowel(symbol: String): String =
    symbol.filter { it.category != CharCategory.NON_SPACING_MARK }

/** Set of all IPA vowel symbols on the chart (for detection from IPA strings). */
val IPA_VOWEL_SYMBOLS: Set<String> = VOWELS.map { it.symbol }.toSet() + setOf(
    "ɑ̃", "ɛ̃", "ɔ̃", "œ̃", // nasalized vowels
)

@Composable
fun IpaVowelChart(
    modifier: Modifier = Modifier,
    highlightedVowels: Set<String> = emptySet(),
    onVowelSelected: ((String?) -> Unit)? = null,
) {
    val ipaFont = rememberIpaFontFamily()
    val highlightedBaseVowels = remember(highlightedVowels) {
        highlightedVowels.map { baseVowel(it) }.toSet()
    }
    val initialSelection = remember(highlightedVowels) {
        VOWELS.firstOrNull { it.symbol in highlightedBaseVowels }
    }
    var selectedVowel by remember(highlightedVowels) { mutableStateOf(initialSelection) }

    val chartHeight = 280.dp
    val labelWidth = 72.dp
    val verticalInset = 16.dp

    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        // Column headers aligned to trapezoid columns at Close row (y=0)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(start = labelWidth, end = 8.dp)) {
            val density = LocalDensity.current
            val headerWidthPx = with(density) { maxWidth.toPx() }
            val textMeasurer = rememberTextMeasurer()
            val headerStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)

            for ((label, col) in listOf("Front" to Col.FRONT, "Central" to Col.CENTRAL, "Back" to Col.BACK)) {
                val xNorm = col.xAt(0f) // position at Close row
                val measured = remember(label, headerStyle) { textMeasurer.measure(label, headerStyle) }
                val xPx = (xNorm * headerWidthPx - measured.size.width / 2f).toInt().coerceAtLeast(0)
                Text(
                    text = label,
                    style = headerStyle,
                    color = onSurfaceVariantColor,
                    modifier = Modifier.offset { IntOffset(xPx, 0) },
                )
            }
        }

        // Main chart area
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
            val density = LocalDensity.current
            val totalHeightPx = with(density) { maxHeight.toPx() }
            val insetPx = with(density) { verticalInset.toPx() }
            val drawHeight = totalHeightPx - 2 * insetPx
            val labelHeightDp = 14.dp
            val halfLabelPx = with(density) { (labelHeightDp / 2).toPx() }.toInt()

            // Row labels on the left
            for (row in Row.entries) {
                val yPx = (insetPx + row.yNorm * drawHeight).toInt() - halfLabelPx
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (row.bold) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 10.sp,
                    ),
                    color = onSurfaceVariantColor,
                    modifier = Modifier
                        .offset { IntOffset(0, yPx) }
                        .padding(end = 4.dp),
                )
            }

            // Canvas for lines + vowels
            val textMeasurer = rememberTextMeasurer()
            val hitRadius = with(density) { 14.dp.toPx() }

            val vowelMeasurements = remember(ipaFont) {
                VOWELS.map { vowel ->
                    vowel to textMeasurer.measure(
                        vowel.symbol,
                        TextStyle(
                            fontFamily = ipaFont,
                            fontSize = 14.sp,
                            fontWeight = if (vowel.rounded) FontWeight.Bold else FontWeight.Normal,
                        ),
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .padding(start = labelWidth, end = 8.dp)
                    .pointerInput(selectedVowel) {
                        detectTapGestures { tapOffset ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val vi = verticalInset.toPx()
                            val dh = h - 2 * vi

                            val tapped = VOWELS.minByOrNull { v ->
                                val vx = v.xNorm() * w
                                val vy = vi + v.row.yNorm * dh
                                (tapOffset.x - vx) * (tapOffset.x - vx) + (tapOffset.y - vy) * (tapOffset.y - vy)
                            }
                            if (tapped != null) {
                                val vx = tapped.xNorm() * w
                                val vy = vi + tapped.row.yNorm * dh
                                val dist = kotlin.math.sqrt(
                                    (tapOffset.x - vx) * (tapOffset.x - vx) +
                                    (tapOffset.y - vy) * (tapOffset.y - vy)
                                )
                                if (dist < hitRadius * 1.5f) {
                                    selectedVowel = tapped
                                    onVowelSelected?.invoke(tapped.symbol)
                                }
                            }
                        }
                    },
            ) {
                val w = size.width
                val h = size.height
                val vi = verticalInset.toPx()
                val dh = h - 2 * vi

                fun toPixel(xNorm: Float, yNorm: Float) =
                    Offset(xNorm * w, vi + yNorm * dh)

                // --- Draw trapezoid edges ---
                // Left edge (front diagonal)
                drawLine(lineColor, toPixel(FRONT_X_TOP, 0f), toPixel(FRONT_X_BOTTOM, 1f), 1.5f)
                // Right edge (back, nearly vertical)
                drawLine(lineColor, toPixel(BACK_X_TOP, 0f), toPixel(BACK_X_BOTTOM, 1f), 1.5f)

                // --- Draw horizontal row lines ---
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                for (row in Row.entries) {
                    val y = row.yNorm
                    val leftX = frontEdgeX(y)
                    val rightX = backEdgeX(y)
                    drawLine(
                        color = lineColor,
                        start = toPixel(leftX, y),
                        end = toPixel(rightX, y),
                        strokeWidth = 1.5f,
                        pathEffect = if (row.dashed) dashEffect else null,
                    )
                }

                // --- Draw vowel symbols ---
                for ((vowel, measured) in vowelMeasurements) {
                    val cx = vowel.xNorm() * w
                    val cy = vi + vowel.row.yNorm * dh
                    val tw = measured.size.width.toFloat()
                    val th = measured.size.height.toFloat()

                    val isSelected = selectedVowel == vowel
                    val isHighlighted = vowel.symbol in highlightedBaseVowels

                    val pad = 3.dp.toPx()
                    val bgW = tw + pad * 2
                    val bgH = th + pad * 2

                    if (isSelected && isHighlighted) {
                        // Same vowel: filled background only
                        drawRoundRect(
                            color = primaryContainerColor,
                            topLeft = Offset(cx - bgW / 2, cy - bgH / 2),
                            size = Size(bgW, bgH),
                            cornerRadius = CornerRadius(4.dp.toPx()),
                        )
                    } else if (isSelected) {
                        // Tapped on chart: filled background
                        drawRoundRect(
                            color = primaryContainerColor,
                            topLeft = Offset(cx - bgW / 2, cy - bgH / 2),
                            size = Size(bgW, bgH),
                            cornerRadius = CornerRadius(4.dp.toPx()),
                        )
                    } else if (isHighlighted) {
                        // Current phoneme: outline only
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(cx - bgW / 2, cy - bgH / 2),
                            size = Size(bgW, bgH),
                            cornerRadius = CornerRadius(4.dp.toPx()),
                            style = Stroke(width = 1.5f),
                        )
                    }

                    val textColor = when {
                        isSelected -> primaryColor
                        isHighlighted -> primaryColor
                        vowel.rounded -> primaryColor.copy(alpha = 0.7f)
                        else -> onSurfaceColor
                    }
                    drawText(
                        textLayoutResult = measured,
                        color = textColor,
                        topLeft = Offset(cx - tw / 2f, cy - th / 2f),
                    )
                }
            }
        }

        // Built-in info box (only when no external callback)
        if (onVowelSelected == null && selectedVowel != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(surfaceColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Column {
                    Text(
                        text = selectedVowel!!.symbol,
                        style = TextStyle(
                            fontFamily = ipaFont,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                        ),
                    )
                    Text(
                        text = selectedVowel!!.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariantColor,
                    )
                    Text(
                        text = if (selectedVowel!!.rounded) "rounded" else "unrounded",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariantColor,
                    )
                }
            }
        }
    }
}
