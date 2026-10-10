package com.gbros.tabslite.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gbros.tabslite.data.StrummingPattern

@Composable
fun StrummingSection(
    strummingPatterns: List<StrummingPattern>,
    modifier: Modifier = Modifier
) {
    if (strummingPatterns.isEmpty()) return

    val colors = AppColors
    var selectedPatternIndex by remember { mutableStateOf(0) }
    val currentPattern = strummingPatterns.getOrNull(selectedPatternIndex) ?: strummingPatterns.first()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: STRUMMING title + Part selector tabs + Part Name / BPM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "STRUMMING",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.width(12.dp))
                    if (strummingPatterns.size > 1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            strummingPatterns.forEachIndexed { idx, p ->
                                val isSelected = idx == selectedPatternIndex
                                Surface(
                                    onClick = { selectedPatternIndex = idx },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) colors.cardElevated else colors.surface,
                                    border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border)
                                ) {
                                    Text(
                                        text = p.part.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) colors.accent else colors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section Part & BPM
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentPattern.part.ifBlank { "WHOLE SONG" }.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${currentPattern.bpm} bpm",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Visual Strumming Diagram with Accents, Arrows, Beat Numbers, and Beam Brackets
            StrummingDiagram(pattern = currentPattern)
        }
    }
}

@Composable
private fun StrummingDiagram(
    pattern: StrummingPattern
) {
    val measures = pattern.measures
    if (measures.isEmpty()) return

    val denuminator = if (pattern.denuminator > 0) pattern.denuminator else 16
    val isTriplet = pattern.isTriplet
    val beatLabels = remember(measures, denuminator, isTriplet) {
        computeBeatLabels(measures, denuminator, isTriplet)
    }

    val horizontalScrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(horizontalScrollState)
            .padding(vertical = 4.dp)
    ) {
        measures.forEachIndexed { index, measureCode ->
            val label = beatLabels.getOrElse(index) { "" }

            val arrowColor = Color(0xFFE2E8F0)
            val accentColor = Color(0xFF94A3B8)

            val isAccent = measureCode == 3 || measureCode == 103
            val isMute = measureCode == 2 || measureCode == 102

            val arrowText = when (measureCode) {
                1, 2, 3 -> "↓"
                101, 102, 103 -> "↑"
                201 -> "P"
                203 -> "||"
                else -> "" // 202 is sustain/pause
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(28.dp)
                    .padding(vertical = 2.dp)
            ) {
                // 1. Accent row (>)
                Box(
                    modifier = Modifier.height(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAccent) {
                        Text(
                            text = ">",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = accentColor
                        )
                    } else if (isMute) {
                        Text(
                            text = "x",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // 2. Arrow row (↓ / ↑)
                Box(
                    modifier = Modifier.height(26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (arrowText.isNotBlank()) {
                        Text(
                            text = arrowText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = arrowColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // 3. Beat label row (1, &, 2, &, etc.)
                Box(
                    modifier = Modifier.height(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (label.isNotBlank()) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (label[0].isDigit()) FontWeight.Bold else FontWeight.Medium,
                            color = if (label[0].isDigit()) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                        )
                    }
                }

                // 4. Bracket / Note Duration Beam Canvas
                RhythmBracket(
                    index = index,
                    denuminator = denuminator,
                    isTriplet = isTriplet,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun RhythmBracket(
    index: Int,
    denuminator: Int,
    isTriplet: Boolean,
    color: Color
) {
    val isDoubleBeam = denuminator >= 16

    Canvas(modifier = Modifier.size(width = 28.dp, height = 16.dp)) {
        val w = size.width
        val h = size.height

        val stemX = w / 2f
        val topY = 2f
        val bottomY = h - 2f
        val beamY1 = bottomY
        val beamY2 = bottomY - 3.5f

        val strokeWidth = 1.8f

        // Draw vertical tick down from center
        drawLine(
            color = color,
            start = Offset(stemX, topY),
            end = Offset(stemX, bottomY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Square
        )

        // Draw horizontal beam connecting beats
        val groupSize = if (isTriplet) 3 else 2
        val posInGroup = index % groupSize

        val startX = if (posInGroup == 0) stemX else 0f
        val endX = if (posInGroup == groupSize - 1) stemX else w

        // Primary beam
        drawLine(
            color = color,
            start = Offset(startX, beamY1),
            end = Offset(endX, beamY1),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Square
        )

        // Secondary double beam for 16th notes
        if (isDoubleBeam) {
            drawLine(
                color = color,
                start = Offset(startX, beamY2),
                end = Offset(endX, beamY2),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Square
            )
        }
    }
}

/**
 * Calculates the exact beat count labels matching Ultimate Guitar's formula:
 * Denominator 8: 1, &, 2, &, 3, &, 4, &
 * Denominator 16: 1, (blank), &, (blank), 2, (blank), &, (blank), 3...
 */
private fun computeBeatLabels(measures: List<Int>, denuminator: Int, isTriplet: Boolean): List<String> {
    return measures.indices.map { i ->
        if (isTriplet) {
            if (i % 3 != 0) ""
            else if (denuminator == 8) "${i / 3 + 1}"
            else if (denuminator == 16) {
                if ((i / 3) % 2 != 0) "&" else "${i / 6 + 1}"
            } else "${2 * i / 3 + 1}"
        } else {
            if (denuminator == 8) {
                if (i % 2 != 0) "&" else "${i / 2 + 1}"
            } else if (denuminator == 16) {
                if (i % 2 != 0) ""
                else if (i % 4 != 0) "&"
                else "${i / 4 + 1}"
            } else {
                "${i + 1}"
            }
        }
    }
}
