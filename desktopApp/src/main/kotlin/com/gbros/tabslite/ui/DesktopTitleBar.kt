package com.gbros.tabslite.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState

@Composable
fun WindowScope.DesktopTitleBar(
    windowState: WindowState,
    title: String = "Chord Studio",
    subtitle: String = "",
    onCloseRequest: () -> Unit
) {
    val colors = AppColors
    val isMaximized = windowState.placement == WindowPlacement.Maximized

    Surface(
        color = colors.surface,
        modifier = Modifier.fillMaxWidth().height(36.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Draggable Window Area (with double-click to maximize/restore)
            WindowDraggableArea(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(isMaximized) {
                        detectTapGestures(
                            onDoubleTap = {
                                windowState.placement = if (isMaximized) WindowPlacement.Floating else WindowPlacement.Maximized
                            }
                        )
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App Logo Badge
                    Image(
                        painter = painterResource("app_icon_48.png"),
                        contentDescription = "Chord Studio Logo",
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        letterSpacing = 0.3.sp
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = "  /  $subtitle",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMuted
                        )
                    }
                }
            }

            // Native-Style Window Controls (Outside draggable area to prevent click/hover stealing)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxHeight()
            ) {
                // Minimize Button
                WindowsTitleBarButton(
                    contentDescription = "Minimize",
                    hoverColor = colors.cardElevated,
                    iconTint = colors.textSecondary,
                    onClick = { windowState.isMinimized = true }
                ) { tint ->
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(1.dp)
                            .background(tint)
                    )
                }

                // Maximize / Restore Button
                WindowsTitleBarButton(
                    contentDescription = if (isMaximized) "Restore" else "Maximize",
                    hoverColor = colors.cardElevated,
                    iconTint = colors.textSecondary,
                    onClick = {
                        windowState.placement = if (isMaximized) WindowPlacement.Floating else WindowPlacement.Maximized
                    }
                ) { tint ->
                    if (isMaximized) {
                        // Windows 11 Overlapping Restore Rectangles
                        Box(modifier = Modifier.size(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(7.5.dp)
                                    .align(Alignment.TopEnd)
                                    .border(1.dp, tint)
                            )
                            Box(
                                modifier = Modifier
                                    .size(7.5.dp)
                                    .align(Alignment.BottomStart)
                                    .background(colors.surface)
                                    .border(1.dp, tint)
                            )
                        }
                    } else {
                        // Windows 11 Single Square Maximize
                        Box(
                            modifier = Modifier
                                .size(9.5.dp)
                                .border(1.dp, tint)
                        )
                    }
                }

                // Close Button (Windows 11 Red Hover Effect)
                WindowsTitleBarButton(
                    contentDescription = "Close",
                    hoverColor = Color(0xFFC42B1C),
                    hoverIconTint = Color.White,
                    iconTint = colors.textSecondary,
                    onClick = onCloseRequest
                ) { tint ->
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = tint,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WindowsTitleBarButton(
    contentDescription: String,
    hoverColor: Color,
    iconTint: Color,
    hoverIconTint: Color = iconTint,
    onClick: () -> Unit,
    content: @Composable (Color) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .width(46.dp)
            .fillMaxHeight()
            .background(if (isHovered) hoverColor else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content(if (isHovered) hoverIconTint else iconTint)
    }
}
