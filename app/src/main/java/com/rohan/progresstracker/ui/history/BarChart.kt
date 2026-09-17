package com.rohan.progresstracker.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

data class BarEntry(val label: String, val value: Int)

/**
 * A simple vertical bar chart drawn directly with Compose (no chart library needed).
 * Shows the maximum value on the Y axis and the bar labels on the X axis.
 */
@Composable
fun BarChart(
    entries: List<BarEntry>,
    formatValue: (Int) -> String,
    /** Show only every n-th X label, so 30 days stay readable. */
    labelEvery: Int,
    description: String,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val measurer = rememberTextMeasurer()
    val maxValue = entries.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .semantics { contentDescription = description },
    ) {
        val maxText = measurer.measure(formatValue(maxValue), labelStyle)
        val zeroText = measurer.measure("0", labelStyle)
        val labelHeight = measurer.measure("M", labelStyle).size.height
        val gap = 6.dp.toPx()

        val left = maxOf(maxText.size.width, zeroText.size.width) + gap
        val top = maxText.size.height / 2f
        val bottom = size.height - labelHeight - gap
        val chartHeight = bottom - top
        val chartWidth = size.width - left

        // Y axis labels and guide lines for the max value and zero.
        drawText(maxText, topLeft = Offset(left - gap - maxText.size.width, 0f))
        drawText(zeroText, topLeft = Offset(left - gap - zeroText.size.width, bottom - zeroText.size.height / 2f))
        drawLine(gridColor, Offset(left, top), Offset(size.width, top), strokeWidth = 1.dp.toPx())
        drawLine(gridColor, Offset(left, bottom), Offset(size.width, bottom), strokeWidth = 1.dp.toPx())

        if (entries.isEmpty()) return@Canvas
        val slot = chartWidth / entries.size
        val barWidth = slot * 0.6f
        val radius = CornerRadius(minOf(barWidth / 2f, 3.dp.toPx()))

        entries.forEachIndexed { index, entry ->
            val centerX = left + slot * index + slot / 2f
            if (entry.value > 0) {
                val barHeight = chartHeight * entry.value / maxValue
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(centerX - barWidth / 2f, bottom - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = radius,
                )
            }
            val isLast = index == entries.lastIndex
            if (index % labelEvery == 0 || (isLast && labelEvery == 1)) {
                val text = measurer.measure(entry.label, labelStyle)
                val x = (centerX - text.size.width / 2f).coerceIn(left, size.width - text.size.width)
                drawText(text, topLeft = Offset(x, bottom + gap / 2f))
            }
        }
    }
}
