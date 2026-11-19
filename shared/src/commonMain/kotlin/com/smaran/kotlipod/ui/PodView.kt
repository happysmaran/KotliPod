package com.smaran.kotlipod.ui

// the number of imports needed is insane
// flutter final boss
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import com.smaran.kotlipod.core.Input
import com.smaran.kotlipod.core.ListScreen
import com.smaran.kotlipod.core.WheelGeometry
import com.smaran.kotlipod.core.WheelGesture

@Composable
fun PodView(
    screen: ListScreen,
    onInput: (Input) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val gesture = remember { WheelGesture() }
    val currentOnInput by rememberUpdatedState(onInput)
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Backdrop)
            .onKeyEvent { event ->
                val input = keyToInput(event)
                if (input != null) currentOnInput(input)
                input != null
            }
            .focusRequester(focusRequester)
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        val pxPerUnit = minOf(
            constraints.maxWidth / PodLayout.BODY_WIDTH,
            constraints.maxHeight / PodLayout.BODY_HEIGHT,
        )
        val width = with(density) { (PodLayout.BODY_WIDTH * pxPerUnit).toDp() }
        val height = with(density) { (PodLayout.BODY_HEIGHT * pxPerUnit).toDp() }

        Canvas(
            modifier = Modifier
                .size(width, height)
                .pointerInput(pxPerUnit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val (downX, downY) = PodLayout.toWheel(
                            down.position.x / pxPerUnit,
                            down.position.y / pxPerUnit,
                        )
                        gesture.down(downX, downY, down.uptimeMillis)
                        do {
                            val change = awaitPointerEvent().changes.first()
                            if (change.pressed) {
                                val (x, y) = PodLayout.toWheel(
                                    change.position.x / pxPerUnit,
                                    change.position.y / pxPerUnit,
                                )
                                val detents = gesture.move(x, y, change.uptimeMillis)
                                if (detents != 0) currentOnInput(Input.Scroll(detents))
                            }
                            change.consume()
                        } while (change.pressed)
                        gesture.up()?.let { currentOnInput(it) }
                    }
                },
        ) {
            scale(pxPerUnit, pivot = Offset.Zero) {
                drawPod(screen, textMeasurer)
            }
        }
    }
}

// Keyboard shortcuts let the interface be exercised without a mouse while tuning the wheel.
private fun keyToInput(event: KeyEvent): Input? {
    if (event.type != KeyEventType.KeyDown) return null
    return when (event.key) {
        Key.DirectionDown -> Input.Scroll(1)
        Key.DirectionUp -> Input.Scroll(-1)
        Key.Enter -> Input.Select
        Key.Escape, Key.Backspace -> Input.Back
        Key.Spacebar -> Input.PlayPause
        Key.DirectionRight -> Input.Next
        Key.DirectionLeft -> Input.Previous
        else -> null
    }
}

private fun DrawScope.drawPod(screen: ListScreen, text: TextMeasurer) {
    drawRoundRect(
        color = Palette.Body,
        size = Size(PodLayout.BODY_WIDTH, PodLayout.BODY_HEIGHT),
        cornerRadius = CornerRadius(PodLayout.BODY_CORNER),
    )
    drawRoundRect(
        color = Palette.BodyEdge,
        size = Size(PodLayout.BODY_WIDTH, PodLayout.BODY_HEIGHT),
        cornerRadius = CornerRadius(PodLayout.BODY_CORNER),
        style = Stroke(width = 1.5f),
    )
    drawRoundRect(
        color = Palette.Bezel,
        topLeft = Offset(PodLayout.SCREEN_LEFT - 5f, PodLayout.SCREEN_TOP - 5f),
        size = Size(PodLayout.SCREEN_WIDTH + 10f, PodLayout.SCREEN_HEIGHT + 10f),
        cornerRadius = CornerRadius(5f),
    )
    drawScreen(screen, text)
    drawWheel(text)
}

private fun DrawScope.drawScreen(screen: ListScreen, text: TextMeasurer) {
    val left = PodLayout.SCREEN_LEFT
    val top = PodLayout.SCREEN_TOP
    val width = PodLayout.SCREEN_WIDTH

    clipRect(left, top, left + width, top + PodLayout.SCREEN_HEIGHT) {
        drawRect(
            color = Palette.ScreenBackground,
            topLeft = Offset(left, top),
            size = Size(width, PodLayout.SCREEN_HEIGHT),
        )

        val firstRow = screen.firstVisibleIndex
        val endRow = minOf(screen.rows.size, firstRow + screen.visibleRowCount)
        for (index in firstRow until endRow) {
            val rowTop = top + PodLayout.TITLE_BAR_HEIGHT + (index - firstRow) * PodLayout.ROW_HEIGHT
            val rowCenter = rowTop + PodLayout.ROW_HEIGHT / 2f
            val selected = index == screen.selectedIndex
            val ink = if (selected) Color.White else Color.Black
            if (selected) {
                drawRect(
                    color = Palette.Selection,
                    topLeft = Offset(left, rowTop),
                    size = Size(width, PodLayout.ROW_HEIGHT),
                )
            }
            val row = screen.rows[index]
            drawLabel(
                measurer = text,
                label = row.text,
                x = left + 8f,
                centerY = rowCenter,
                maxWidth = width - 28f,
                style = TextStyle(fontSize = 12f.toSp(), color = ink),
            )
            if (row.hasChildren) drawChevron(left + width - 11f, rowCenter, ink)
        }

        drawTitleBar(screen.title, text)
    }
}

private fun DrawScope.drawTitleBar(title: String, text: TextMeasurer) {
    val left = PodLayout.SCREEN_LEFT
    val top = PodLayout.SCREEN_TOP
    val height = PodLayout.TITLE_BAR_HEIGHT
    val width = PodLayout.SCREEN_WIDTH

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Palette.TitleBarTop, Palette.TitleBarBottom),
            startY = top,
            endY = top + height,
        ),
        topLeft = Offset(left, top),
        size = Size(width, height),
    )
    drawLine(
        color = Palette.TitleBarLine,
        start = Offset(left, top + height),
        end = Offset(left + width, top + height),
        strokeWidth = 1f,
    )
    drawLabel(
        measurer = text,
        label = title,
        x = left + width / 2f,
        centerY = top + height / 2f,
        maxWidth = width - 56f,
        style = TextStyle(fontSize = 12f.toSp(), fontWeight = FontWeight.Bold, color = Color.Black),
        centered = true,
    )

    val batteryLeft = left + width - 26f
    val batteryTop = top + 6f
    drawRect(
        color = Color(0xFF444444),
        topLeft = Offset(batteryLeft, batteryTop),
        size = Size(16f, 8f),
        style = Stroke(width = 1f),
    )
    drawRect(Color(0xFF444444), Offset(batteryLeft + 16f, batteryTop + 2.5f), Size(2f, 3f))
    drawRect(Palette.Battery, Offset(batteryLeft + 2f, batteryTop + 2f), Size(12f, 4f))
}

private fun DrawScope.drawWheel(text: TextMeasurer) {
    val center = Offset(PodLayout.WHEEL_CENTER_X, PodLayout.WHEEL_CENTER_Y)
    val radius = PodLayout.WHEEL_RADIUS
    val buttonRadius = radius * WheelGeometry.CENTER_FRACTION.toFloat()

    drawCircle(Palette.WheelRing, radius, center)
    drawCircle(Palette.BodyEdge, radius, center, style = Stroke(width = 1f))
    drawCircle(Palette.WheelButton, buttonRadius, center)
    drawCircle(Palette.BodyEdge, buttonRadius, center, style = Stroke(width = 1f))

    val glyph = Palette.WheelGlyph
    // Legends sit near the wheel's outer edge, leaving a small margin for the glyph's own size.
    val reach = radius - 16f

    drawLabel(
        measurer = text,
        label = "MENU",
        x = center.x,
        centerY = center.y - reach,
        maxWidth = 60f,
        style = TextStyle(fontSize = 10f.toSp(), fontWeight = FontWeight.Bold, color = glyph),
        centered = true,
    )

    val bottom = center.y + reach
    drawTriangle(center.x - 5f, bottom, 6f, glyph, pointingRight = true)
    drawRect(glyph, Offset(center.x + 2f, bottom - 5f), Size(2.5f, 10f))
    drawRect(glyph, Offset(center.x + 6.5f, bottom - 5f), Size(2.5f, 10f))

    val right = center.x + reach
    drawTriangle(right - 6f, center.y, 6f, glyph, pointingRight = true)
    drawTriangle(right + 2f, center.y, 6f, glyph, pointingRight = true)
    drawRect(glyph, Offset(right + 8.5f, center.y - 6f), Size(2.5f, 12f))

    val left = center.x - reach
    drawTriangle(left + 6f, center.y, 6f, glyph, pointingRight = false)
    drawTriangle(left - 2f, center.y, 6f, glyph, pointingRight = false)
    drawRect(glyph, Offset(left - 11f, center.y - 6f), Size(2.5f, 12f))
}

// Drawn as paths because symbol glyphs such as the play triangle are not present in every system font.
// *looks at Windows*
private fun DrawScope.drawTriangle(
    centerX: Float,
    centerY: Float,
    half: Float,
    color: Color,
    pointingRight: Boolean,
) {
    val direction = if (pointingRight) 1f else -1f
    val path = Path().apply {
        moveTo(centerX - half * direction, centerY - half)
        lineTo(centerX + half * direction, centerY)
        lineTo(centerX - half * direction, centerY + half)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawChevron(x: Float, centerY: Float, color: Color) {
    val path = Path().apply {
        moveTo(x - 2f, centerY - 4f)
        lineTo(x + 2f, centerY)
        lineTo(x - 2f, centerY + 4f)
    }
    drawPath(path, color, style = Stroke(width = 1.8f))
}

private fun DrawScope.drawLabel(
    measurer: TextMeasurer,
    label: String,
    x: Float,
    centerY: Float,
    maxWidth: Float,
    style: TextStyle,
    centered: Boolean = false,
) {
    val layout = measurer.measure(
        text = label,
        style = style,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        constraints = Constraints(maxWidth = maxWidth.toInt()),
    )
    val originX = if (centered) x - layout.size.width / 2f else x
    drawText(layout, topLeft = Offset(originX, centerY - layout.size.height / 2f))
}