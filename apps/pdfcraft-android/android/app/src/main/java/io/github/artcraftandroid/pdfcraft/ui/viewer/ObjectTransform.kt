package io.github.artcraftandroid.pdfcraft.ui.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.artcraftandroid.pdfcraft.jText
import io.github.artcraftandroid.pdfcraft.ui.LucideIcons
import io.github.artcraftandroid.pdfcraft.ui.OpenDocument
import kotlin.math.roundToInt

/** Page-space operations: independent of zoom, density, scrolling and interface direction. */
internal fun movedObject(r: Rect, delta: Offset, page: Rect): Rect {
    val x = (r.left + delta.x).coerceIn(page.left, maxOf(page.left, page.right - r.width))
    val y = (r.top + delta.y).coerceIn(page.top, maxOf(page.top, page.bottom - r.height))
    return Rect(x, y, x + r.width, y + r.height)
}

internal fun scaledObject(r: Rect, delta: Offset, page: Rect, minFactor: Float, maxFactor: Float = Float.MAX_VALUE): Rect {
    if (r.width <= 0f || r.height <= 0f) return r
    // Project onto the diagonal: either horizontal or vertical finger movement scales naturally.
    val wanted = 1f + (delta.x * r.width + delta.y * r.height) / (r.width * r.width + r.height * r.height)
    val limit = minOf(maxFactor, (page.right - r.left) / r.width, (page.bottom - r.top) / r.height).coerceAtLeast(0.01f)
    val factor = wanted.coerceIn(minOf(minFactor, limit), limit)
    return Rect(r.left, r.top, r.left + r.width * factor, r.top + r.height * factor)
}

/** Prefer a direct hit, then the closest nearby object, with the topmost winning ties. */
internal fun nearestObject(boxes: List<Rect>, point: Offset, tolerance: Float): Int? =
    boxes.indices.reversed().filter { boxes[it].width > 0f && boxes[it].height > 0f }.minByOrNull { i ->
        val r = boxes[i]
        val dx = maxOf(r.left - point.x, 0f, point.x - r.right)
        val dy = maxOf(r.top - point.y, 0f, point.y - r.bottom)
        dx * dx + dy * dy
    }?.takeIf { i ->
        val r = boxes[i]
        val dx = maxOf(r.left - point.x, 0f, point.x - r.right)
        val dy = maxOf(r.top - point.y, 0f, point.y - r.bottom)
        dx * dx + dy * dy <= tolerance * tolerance
    }

/** Finger-sized controls after placement; one completed gesture is one undoable engine edit. */
@Composable
fun BoxScope.ObjectTransformOverlay(
    doc: OpenDocument,
    identity: Any,
    page: Int,
    rect: Rect,
    minFactor: Float = 0.1f,
    maxFactor: Float = Float.MAX_VALUE,
    onMove: (Float, Float, () -> Unit) -> Unit,
    onScale: (Float, () -> Unit) -> Unit,
    onActive: (Boolean) -> Unit = {},
) {
    val density = LocalDensity.current
    val pageSize = doc.viewer.pages.getOrNull(page) ?: return
    val bounds = Rect(0f, 0f, pageSize.width, pageSize.height)
    val ppp = doc.viewer.pixelsPerPoint(page).coerceAtLeast(0.01f)
    var draft by remember(identity, rect) { mutableStateOf(rect) }
    var active by remember(identity, rect) { mutableStateOf(false) }
    var pending by remember(identity, rect) { mutableStateOf(false) }
    val a = doc.viewer.pageToScreen(page, draft.left, draft.top)
    val b = doc.viewer.pageToScreen(page, draft.right, draft.bottom)
    val frame = Rect(a, b)
    val viewport = Rect(0f, doc.viewer.padTop, doc.viewer.viewW, doc.viewer.viewH - doc.viewer.padBottom)
    if (!frame.overlaps(viewport)) return
    val accent = Color(0xFF448AFF)
    Canvas(Modifier.fillMaxSize()) {
        drawRect(accent.copy(alpha = if (active) 0.12f else 0.04f), frame.topLeft, frame.size)
        drawRect(accent, frame.topLeft, frame.size, style = Stroke(2.dp.toPx()))
    }
    fun finish(scale: Boolean) {
        active = false
        onActive(false)
        if (draft == rect) return
        pending = true
        val finished = { pending = false; draft = rect }
        if (scale) onScale(draft.width / rect.width, finished) else onMove(draft.left - rect.left, draft.top - rect.top, finished)
    }
    fun cancel() { draft = rect; active = false; onActive(false) }
    // The document remains scrollable outside this touch target. A tap does not open the keyboard.
    val touch = with(density) { 48.dp.toPx() }
    val body = Rect(frame.center.x - maxOf(frame.width, touch) / 2, frame.center.y - maxOf(frame.height, touch) / 2,
        frame.center.x + maxOf(frame.width, touch) / 2, frame.center.y + maxOf(frame.height, touch) / 2).intersect(viewport)
    Box(Modifier.align(AbsoluteAlignment.TopLeft).zIndex(1f).absoluteOffset { IntOffset(body.left.roundToInt(), body.top.roundToInt()) }
        .size(with(density) { body.width.toDp() }, with(density) { body.height.toDp() })
        .semantics { contentDescription = jText("Drag selected item", "اسحب العنصر المحدد") }
        .pointerInput(identity, rect, ppp, pending) {
            var total = Offset.Zero
            detectDragGestures(onDragStart = { total = Offset.Zero; if (!pending) { active = true; onActive(true) } },
                onDragEnd = { if (!pending) finish(false) }, onDragCancel = { if (!pending) cancel() }) { change, delta ->
                change.consume()
                if (!pending) { total += delta / ppp; draft = movedObject(rect, total, bounds) }
            }
        })
    // Controls sit outside small text boxes so their touch targets do not cover the whole text.
    for (resize in listOf(false, true)) {
        val desired = if (resize) frame.bottomRight + Offset(touch / 2, touch / 2) else Offset(frame.left - touch / 2, frame.center.y)
        val center = Offset(desired.x.coerceIn(touch * 0.75f, maxOf(touch * 0.75f, doc.viewer.viewW - touch * 0.75f)),
            desired.y.coerceIn(doc.viewer.padTop + touch / 2, maxOf(doc.viewer.padTop + touch / 2, doc.viewer.viewH - doc.viewer.padBottom - touch / 2)))
        Canvas(Modifier.fillMaxSize()) { drawLine(accent, if (resize) frame.bottomRight else frame.centerLeft, center, 1.dp.toPx()) }
        Box(Modifier.align(AbsoluteAlignment.TopLeft).zIndex(2f).absoluteOffset { IntOffset((center.x - touch / 2).roundToInt(), (center.y - touch / 2).roundToInt()) }
            .size(48.dp)
            .semantics { contentDescription = if (resize) jText("Resize selected item", "تغيير حجم العنصر المحدد") else jText("Move selected item", "تحريك العنصر المحدد") }
            .pointerInput(identity, rect, ppp, pending) {
                var total = Offset.Zero
                detectDragGestures(onDragStart = { total = Offset.Zero; if (!pending) { active = true; onActive(true) } },
                    onDragEnd = { if (!pending) finish(resize) }, onDragCancel = { if (!pending) cancel() }) { change, delta ->
                    change.consume()
                    if (!pending) {
                        total += delta / ppp
                        draft = if (resize) scaledObject(rect, total, bounds, minFactor, maxFactor) else movedObject(rect, total, bounds)
                    }
                }
            }, contentAlignment = Alignment.Center) {
            Box(Modifier.size(30.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                Icon(if (resize) LucideIcons.Maximize2 else LucideIcons.Move, null, tint = accent, modifier = Modifier.size(22.dp))
            }
        }
    }
}
