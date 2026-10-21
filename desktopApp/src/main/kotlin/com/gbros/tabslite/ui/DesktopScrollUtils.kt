package com.gbros.tabslite.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Universal mouse-wheel horizontal redirection modifier for ScrollState.
 * Intercepts vertical and horizontal mouse-wheel events and applies them to horizontal scrolling,
 * consuming the event so outer vertical scroll containers do not steal it.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.horizontalMouseWheelScroll(
    scrollState: ScrollState,
    scope: CoroutineScope,
    speedMultiplier: Float = 110f
): Modifier = this.onPointerEvent(PointerEventType.Scroll) { event ->
    val change = event.changes.firstOrNull()
    val scrollDelta = change?.scrollDelta
    if (scrollDelta != null) {
        val delta = if (abs(scrollDelta.x) > 0.001f) scrollDelta.x else scrollDelta.y
        if (abs(delta) > 0.001f) {
            event.changes.forEach { it.consume() }
            scope.launch {
                scrollState.scrollBy(delta * speedMultiplier)
            }
        }
    }
}

/**
 * Universal mouse-wheel horizontal redirection modifier for LazyListState.
 * Intercepts vertical and horizontal mouse-wheel events and applies them to horizontal scrolling,
 * consuming the event so outer vertical scroll containers do not steal it.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.horizontalMouseWheelScroll(
    lazyState: LazyListState,
    scope: CoroutineScope,
    speedMultiplier: Float = 110f
): Modifier = this.onPointerEvent(PointerEventType.Scroll) { event ->
    val change = event.changes.firstOrNull()
    val scrollDelta = change?.scrollDelta
    if (scrollDelta != null) {
        val delta = if (abs(scrollDelta.x) > 0.001f) scrollDelta.x else scrollDelta.y
        if (abs(delta) > 0.001f) {
            event.changes.forEach { it.consume() }
            scope.launch {
                lazyState.scrollBy(delta * speedMultiplier)
            }
        }
    }
}

/**
 * Universally reusable horizontal filter bar with mouse-wheel support,
 * persistent navigation chevron buttons, auto-scroll to active item, and badge labels.
 */
@Composable
fun <T> HorizontalScrollableFilterBar(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    badgeProvider: ((T) -> String)? = null,
    modifier: Modifier = Modifier
) {
    val colors = AppColors
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to selected item when it changes
    LaunchedEffect(selectedItem) {
        val index = items.indexOf(selectedItem)
        if (index != -1) {
            val visibleIndices = listState.layoutInfo.visibleItemsInfo.map { it.index }
            if (index !in visibleIndices) {
                listState.animateScrollToItem((index - 1).coerceAtLeast(0))
            }
        }
    }

    val canScrollBackward by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    val canScrollForward by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems <= 0) return@derivedStateOf false
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf true
            lastVisible < totalItems - 1
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Scroll Chevron Button
        Surface(
            onClick = {
                if (canScrollBackward) {
                    scope.launch {
                        listState.animateScrollBy(-280f)
                    }
                }
            },
            shape = CircleShape,
            color = if (canScrollBackward) colors.cardElevated else colors.surface.copy(alpha = 0.35f),
            border = BorderStroke(
                1.dp,
                if (canScrollBackward) colors.borderHover else colors.border.copy(alpha = 0.25f)
            ),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "Scroll Left",
                    tint = if (canScrollBackward) colors.textPrimary else colors.textMuted.copy(alpha = 0.3f),
                    modifier = Modifier.size(11.dp).offset(x = 1.dp)
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        // Scrollable LazyRow of Pills
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .weight(1f)
                .horizontalMouseWheelScroll(listState, scope)
        ) {
            items(items) { item ->
                val isSelected = item == selectedItem
                val label = labelProvider(item)
                val badge = badgeProvider?.invoke(item) ?: ""

                Surface(
                    onClick = { onItemSelected(item) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) colors.cardElevated else colors.card,
                    border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = TextStyle(
                                fontSize = 12.sp,
                                lineHeight = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.accent else colors.textSecondary
                            ),
                            modifier = Modifier.offset(y = (-0.8).dp)
                        )
                        if (badge.isNotBlank()) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = colors.surface,
                                modifier = Modifier.height(18.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                ) {
                                    Text(
                                        text = badge,
                                        style = TextStyle(
                                            fontSize = 9.sp,
                                            lineHeight = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textMuted
                                        ),
                                        modifier = Modifier.offset(y = (-0.6).dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(6.dp))

        // Right Scroll Chevron Button
        Surface(
            onClick = {
                if (canScrollForward) {
                    scope.launch {
                        listState.animateScrollBy(280f)
                    }
                }
            },
            shape = CircleShape,
            color = if (canScrollForward) colors.cardElevated else colors.surface.copy(alpha = 0.35f),
            border = BorderStroke(
                1.dp,
                if (canScrollForward) colors.borderHover else colors.border.copy(alpha = 0.25f)
            ),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Scroll Right",
                    tint = if (canScrollForward) colors.textPrimary else colors.textMuted.copy(alpha = 0.3f),
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}
