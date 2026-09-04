package com.thestudypath.pdfviewer.ui.sign

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream

@Composable
fun SignaturePad(
    strokes: List<SignatureStroke>,
    onStrokesChanged: (List<SignatureStroke>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var currentStroke by remember { mutableStateOf<List<SignaturePoint>>(emptyList()) }

    Box(
        modifier = modifier
            .height(180.dp)
            .background(ComposeColor.White, RoundedCornerShape(12.dp))
            .border(1.dp, ComposeColor(0xFFD0D7DE), RoundedCornerShape(12.dp))
            .onGloballyPositioned { coordinates -> canvasSize = coordinates.size }
            .pointerInput(strokes, canvasSize) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (canvasSize.width == 0 || canvasSize.height == 0) return@detectDragGestures
                        currentStroke = listOf(offset.toSignaturePoint(canvasSize))
                    },
                    onDrag = { change, _ ->
                        if (canvasSize.width == 0 || canvasSize.height == 0) return@detectDragGestures
                        currentStroke = currentStroke + change.position.toSignaturePoint(canvasSize)
                        change.consume()
                    },
                    onDragEnd = {
                        if (currentStroke.isNotEmpty()) {
                            onStrokesChanged(strokes + SignatureStroke(currentStroke))
                        }
                        currentStroke = emptyList()
                    },
                    onDragCancel = {
                        currentStroke = emptyList()
                    },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val allStrokes = if (currentStroke.isEmpty()) strokes else strokes + SignatureStroke(currentStroke)
            allStrokes.forEach { stroke ->
                drawSignatureStroke(stroke, size.width, size.height)
            }
        }
    }
}

data class SignatureStroke(
    val points: List<SignaturePoint>,
)

data class SignaturePoint(
    val xFraction: Float,
    val yFraction: Float,
)

fun renderSignaturePng(
    strokes: List<SignatureStroke>,
    width: Int = 1200,
    height: Int = 400,
    strokeWidthPx: Float = 10f,
): ByteArray {
    if (strokes.isEmpty()) return ByteArray(0)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        this.strokeWidth = strokeWidthPx
    }

    strokes.forEach { stroke ->
        if (stroke.points.isEmpty()) return@forEach
        val path = Path()
        val firstPoint = stroke.points.first()
        path.moveTo(firstPoint.xFraction * width, firstPoint.yFraction * height)
        stroke.points.drop(1).forEach { point ->
            path.lineTo(point.xFraction * width, point.yFraction * height)
        }
        if (stroke.points.size == 1) {
            val point = stroke.points.first()
            canvas.drawPoint(point.xFraction * width, point.yFraction * height, paint)
        } else {
            canvas.drawPath(path, paint)
        }
    }

    return ByteArrayOutputStream().use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        bitmap.recycle()
        output.toByteArray()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSignatureStroke(
    stroke: SignatureStroke,
    canvasWidth: Float,
    canvasHeight: Float,
) {
    if (stroke.points.isEmpty()) return
    if (stroke.points.size == 1) {
        val point = stroke.points.first()
        drawCircle(
            color = ComposeColor.Black,
            radius = 3.5f,
            center = Offset(point.xFraction * canvasWidth, point.yFraction * canvasHeight),
        )
        return
    }

    val path = ComposePath().apply {
        val first = stroke.points.first()
        moveTo(first.xFraction * canvasWidth, first.yFraction * canvasHeight)
        stroke.points.drop(1).forEach { point ->
            lineTo(point.xFraction * canvasWidth, point.yFraction * canvasHeight)
        }
    }
    drawPath(
        path = path,
        color = ComposeColor.Black,
        style = Stroke(width = 4f, cap = StrokeCap.Round),
    )
}

private fun Offset.toSignaturePoint(size: IntSize): SignaturePoint {
    val width = size.width.toFloat().coerceAtLeast(1f)
    val height = size.height.toFloat().coerceAtLeast(1f)
    return SignaturePoint(
        xFraction = (x / width).coerceIn(0f, 1f),
        yFraction = (y / height).coerceIn(0f, 1f),
    )
}

