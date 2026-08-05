package com.rynekryz.rynotes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

@Composable
fun PinDots(length: Int, filled: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(length) { index ->
            val isFilled = index < filled
            val scaleAnim by animateFloatAsState(
                targetValue = if (isFilled) 1f else 0.7f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "pinDot"
            )
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .scale(scaleAnim)
                    .clip(CircleShape)
                    .background(
                        if (isFilled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

@Composable
fun PinPad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "back")
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(Modifier.size(64.dp))
                        "back" -> IconButton(onClick = onBackspace, modifier = Modifier.size(64.dp)) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace")
                        }
                        else -> ExpressivePadKey(key = key) { onDigit(key) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressivePadKey(key: String, onClick: () -> Unit) {
    val haptics = rememberHaptics(LocalHapticsEnabled.current)
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scaleAnim by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = if (pressed) {
            tween(durationMillis = 40)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        },
        label = "keyScale"
    )
    val shapeCorner by animateFloatAsState(
        targetValue = if (pressed) 28f else 32f,
        animationSpec = if (pressed) {
            tween(durationMillis = 40)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        },
        label = "keyShape"
    )
    Surface(
        onClick = { haptics.click(); onClick() },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(shapeCorner.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.size(64.dp).scale(scaleAnim)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                key,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String = "Password") {
    var visible by remember { mutableStateOf(value = false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = "Toggle visibility")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun PatternPad(onPatternComplete: (String) -> Unit) {
    val dotCount = 9
    var selected by remember { mutableStateOf(listOf<Int>()) }
    var dragPoint by remember { mutableStateOf<Offset?>(null) }
    var positions by remember { mutableStateOf(listOf<Offset>()) }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outline

    val dotScales = remember { (0 until dotCount).map { Animatable(1f) } }
    val haptics = rememberHaptics(LocalHapticsEnabled.current)

    LaunchedEffect(selected.lastOrNull()) {
        val last = selected.lastOrNull() ?: return@LaunchedEffect
        haptics.tick()
        dotScales[last].snapTo(0.7f)
        dotScales[last].animateTo(
            1.28f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
    }

    fun nearestDot(offset: Offset): Int? {
        if (positions.isEmpty()) return null
        val threshold = canvasSize.minDimension / 7.5f
        return positions.asSequence().withIndex().minByOrNull { (_, pos) ->
            val dx = pos.x - offset.x
            val dy = pos.y - offset.y
            sqrt((dx * dx) + (dy * dy))
        }?.let { (index, pos) ->
            val dx = pos.x - offset.x
            val dy = pos.y - offset.y
            if (sqrt((dx * dx) + (dy * dy)) < threshold) index else null
        }
    }

    Surface(
        shape = RoundedCornerShape(36.dp),
        color = surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.size(280.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            selected = emptyList()
                            dragPoint = offset
                            nearestDot(offset)?.let { selected = listOf(it) }
                        },
                        onDrag = { change, _ ->
                            dragPoint = change.position
                            nearestDot(change.position)?.let { dot ->
                                if (dot !in selected) selected += dot
                            }
                        },
                        onDragEnd = {
                            if (selected.isNotEmpty()) onPatternComplete(selected.joinToString(""))
                            selected = emptyList()
                            dragPoint = null
                        }
                    )
                }
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                canvasSize = size
                val cols = 3
                val cellW = size.width / cols
                val cellH = size.height / cols
                val pts = (0 until dotCount).map { i ->
                    val row = i / cols
                    val col = i % cols
                    Offset(cellW * col + cellW / 2, cellH * row + cellH / 2)
                }
                positions = pts

                if (selected.size > 1) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(pts[selected[0]].x, pts[selected[0]].y)
                        for (i in 1 until selected.size) {
                            lineTo(pts[selected[i]].x, pts[selected[i]].y)
                        }
                        dragPoint?.let { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path = path,
                        color = primaryColor.copy(alpha = 0.9f),
                        style = Stroke(width = 14f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                    )
                } else if (selected.size == 1 && dragPoint != null) {
                    drawLine(
                        color = primaryColor.copy(alpha = 0.5f),
                        start = pts[selected[0]],
                        end = dragPoint!!,
                        strokeWidth = 10f,
                        cap = StrokeCap.Round
                    )
                }

                pts.forEachIndexed { index, pt ->
                    val isSelected = index in selected
                    val nodeScale = dotScales.getOrNull(index)?.value ?: 1f

                    drawCircle(
                        color = if (isSelected) containerColor else surfaceVariant.copy(alpha = 0.6f),
                        radius = 26f * nodeScale,
                        center = pt
                    )
                    drawCircle(
                        color = if (isSelected) primaryColor else outline.copy(alpha = 0.35f),
                        radius = 9f * nodeScale,
                        center = pt
                    )
                    if (isSelected) {
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.18f),
                            radius = 34f * nodeScale,
                            center = pt
                        )
                    }
                }
            }
        }
    }
}
