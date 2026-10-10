package com.gbros.tabslite.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.ui.text.style.TextOverflow
import com.gbros.tabslite.utilities.AudioTuner
import com.gbros.tabslite.utilities.TuningPreset
import com.gbros.tabslite.utilities.TuningString
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TunerScreen(modifier: Modifier = Modifier) {
    val colors = AppColors
    val settings by com.gbros.tabslite.data.DesktopDatabase.instance.settingsFlow.collectAsState()
    val tunerState by AudioTuner.tunerState.collectAsState()
    val initialCat = if (settings.defaultInstrument.contains("Ukulele", ignoreCase = true)) {
        AudioTuner.INSTRUMENTS.find { it.name.contains("Ukulele", ignoreCase = true) } ?: AudioTuner.INSTRUMENTS.first()
    } else {
        AudioTuner.INSTRUMENTS.first()
    }
    var selectedCategory by remember(settings.defaultInstrument) { mutableStateOf(initialCat) }
    val categoryPresets = remember(selectedCategory) {
        AudioTuner.PRESETS.filter { it.categoryId == selectedCategory.id }
    }
    val initialPreset = if (settings.defaultInstrument.contains("Drop D", ignoreCase = true)) {
        categoryPresets.find { it.name.contains("Drop D", ignoreCase = true) } ?: categoryPresets.first()
    } else {
        categoryPresets.first()
    }
    var selectedPreset by remember(selectedCategory, settings.defaultInstrument) { mutableStateOf(initialPreset) }
    var selectedString by remember { mutableStateOf<TuningString?>(null) }

    // Update selectedPreset when category changes
    LaunchedEffect(selectedCategory) {
        val matchingPresets = AudioTuner.PRESETS.filter { it.categoryId == selectedCategory.id }
        if (matchingPresets.isNotEmpty()) {
            selectedPreset = matchingPresets.first()
            selectedString = null
        }
    }

    // Start listening on mount, stop on unmount
    DisposableEffect(Unit) {
        AudioTuner.startListening()
        onDispose {
            AudioTuner.stopListening()
        }
    }

    LaunchedEffect(selectedPreset) {
        AudioTuner.setPreset(selectedPreset)
        selectedString = null
    }

    LaunchedEffect(selectedString) {
        AudioTuner.setLockedString(selectedString)
    }

    val animatedCents by animateFloatAsState(
        targetValue = tunerState.centsOffset.toFloat(),
        animationSpec = tween(durationMillis = 120)
    )

    val meterColor by animateColorAsState(
        targetValue = when {
            tunerState.isInTune -> Color(0xFF10B981) // Neon Emerald
            abs(tunerState.centsOffset) < 15 -> Color(0xFFF59E0B) // Amber close
            else -> Color(0xFFEF4444) // Red
        },
        animationSpec = tween(durationMillis = 150)
    )

    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = "Acoustic & Electric Tuner",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Microphone pitch detector with Karplus-Strong physical modeling string synthesis",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted
                    )
                }

                // Live Mic Toggle
            Surface(
                onClick = {
                    if (tunerState.isListening) AudioTuner.stopListening()
                    else AudioTuner.startListening()
                },
                shape = RoundedCornerShape(10.dp),
                color = if (tunerState.isListening) Color(0xFF16382C) else Color(0xFF261D24),
                border = BorderStroke(1.dp, if (tunerState.isListening) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (tunerState.isListening) Color(0xFF10B981) else Color(0xFFEF4444))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (tunerState.isListening) "Microphone Active" else "Microphone Paused",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (tunerState.isListening) Color(0xFFA7F3D0) else Color(0xFFFCA5A5)
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // 1. Instrument Category Selector Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Instrument:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
                modifier = Modifier.padding(end = 10.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AudioTuner.INSTRUMENTS) { instrument ->
                    val isSelected = selectedCategory == instrument
                    val instrumentIcon = when (instrument.iconKey) {
                        "ukulele", "banjo" -> Icons.Default.MusicNote
                        "bass" -> Icons.Default.GraphicEq
                        "violin" -> Icons.Default.Tune
                        else -> Icons.Default.MusicNote
                    }
                    Surface(
                        onClick = { selectedCategory = instrument },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) colors.cardElevated else colors.card,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) colors.accent else colors.border
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = instrumentIcon,
                                contentDescription = null,
                                tint = if (isSelected) colors.accent else colors.textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = instrument.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.accent else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 2. Preset Tunings Chips Bar for Selected Instrument
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Tuning:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                modifier = Modifier.padding(end = 10.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categoryPresets) { preset ->
                    val isSelected = selectedPreset == preset
                    Surface(
                        onClick = { selectedPreset = preset },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) colors.accent else colors.card,
                        border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border)
                    ) {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Main Tuner Dial & Gauge Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.width(620.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Detected Note & Target
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TARGET NOTE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted
                        )
                        Text(
                            text = tunerState.targetString?.noteName ?: "--",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                    }

                    // Main Big Center Note Display
                    Surface(
                        color = colors.cardElevated,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, meterColor.copy(alpha = 0.8f)),
                        modifier = Modifier.size(96.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (tunerState.detectedFrequency > 35.0) tunerState.closestNoteName else "--",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                fontSize = 38.sp,
                                color = if (tunerState.isInTune) Color(0xFF10B981) else Color.White
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "FREQUENCY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted
                        )
                        Text(
                            text = if (tunerState.detectedFrequency > 35.0) "%.1f Hz".format(tunerState.detectedFrequency) else "-- Hz",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // High-Precision Cents Deviation Meter
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(colors.bg, RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Meter Ticks Background (-50 to +50)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(-50, -40, -30, -20, -10, 0, 10, 20, 30, 40, 50).forEach { tick ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .width(if (tick == 0) 2.5.dp else 1.dp)
                                        .height(if (tick == 0) 20.dp else if (abs(tick) % 20 == 0) 14.dp else 8.dp)
                                        .background(if (tick == 0) Color(0xFF10B981) else colors.borderHover)
                                )
                                Spacer(Modifier.height(2.dp))
                                if (abs(tick) % 20 == 0 || tick == 0) {
                                    Text(
                                        text = if (tick == 0) "0" else (if (tick > 0) "+$tick" else "$tick"),
                                        fontSize = 9.sp,
                                        fontWeight = if (tick == 0) FontWeight.Bold else FontWeight.Normal,
                                        color = if (tick == 0) Color(0xFF10B981) else colors.textMuted
                                    )
                                }
                            }
                        }
                    }

                    // Needle Indicator
                    if (tunerState.detectedFrequency > 35.0) {
                        val fraction = ((animatedCents + 50.0) / 100.0).coerceIn(0.0, 1.0)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction.toFloat())
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.CenterEnd)
                                        .offset(x = 8.dp)
                                        .clip(CircleShape)
                                        .background(meterColor)
                                        .border(BorderStroke(2.dp, Color.White), CircleShape)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Status Banner Text
                Surface(
                    color = meterColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, meterColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = tunerState.statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = meterColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Interactive Strings Headstock & Pluck Section
        Text(
            text = "Click any string peg to focus or play reference tone:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            selectedPreset.strings.forEachIndexed { index, stringItem ->
                val isTarget = tunerState.targetString == stringItem
                val isLocked = selectedString == stringItem

                Card(
                    onClick = {
                        if (isLocked) {
                            selectedString = null
                        } else {
                            selectedString = stringItem
                        }
                        AudioTuner.playTone(stringItem.frequency)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTarget && tunerState.isInTune) Color(0xFF122E22)
                        else if (isLocked) colors.cardElevated
                        else colors.card
                    ),
                    border = BorderStroke(
                        width = if (isTarget || isLocked) 1.5.dp else 1.dp,
                        color = if (isTarget && tunerState.isInTune) Color(0xFF10B981)
                        else if (isLocked) colors.accent
                        else colors.border
                    ),
                    modifier = Modifier
                        .width(92.dp)
                        .height(115.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp).fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // String Number
                        Text(
                            text = "String ${selectedPreset.strings.size - index}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Note Name
                        Text(
                            text = stringItem.noteName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isTarget && tunerState.isInTune) Color(0xFF10B981) else colors.textPrimary,
                            maxLines = 1
                        )

                        // Frequency & Play Icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play tone", modifier = Modifier.size(13.dp), tint = colors.accent)
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "%.0fHz".format(stringItem.frequency),
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 4.dp, horizontal = 2.dp),
            style = defaultScrollbarStyle().copy(
                unhoverColor = Color.White.copy(alpha = 0.18f),
                hoverColor = colors.accent,
                thickness = 8.dp,
                shape = RoundedCornerShape(4.dp)
            )
        )
    }
}
