package dev.tkuenneth.jetpackinkdemo

import android.annotation.SuppressLint
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.authoring.compose.InProgressStrokes
import androidx.ink.brush.Brush
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke

@SuppressLint("ClickableViewAccessibility")
@Composable
fun DrawingSurface(
    finishedStrokes: Collection<Stroke>,
    brush: Brush,
    modifier: Modifier = Modifier,
    addStrokes: (Collection<Stroke>) -> Unit
) {
    val canvasStrokeRenderer = remember { CanvasStrokeRenderer.create() }
    val latestBrush by rememberUpdatedState(brush)
//    var inkingHandlerInstance by remember { mutableStateOf<InkingHandler?>(null) }
    Box(modifier = modifier.clipToBounds()) {
//        AndroidView(
//            factory = { context ->
//                val view = InProgressStrokesView(context)
//                val handler = InkingHandler(view, addStrokes).also {
//                    inkingHandlerInstance = it
//                }
//                view.addFinishedStrokesListener(handler)
//                view.layoutParams = FrameLayout.LayoutParams(
//                    FrameLayout.LayoutParams.MATCH_PARENT,
//                    FrameLayout.LayoutParams.MATCH_PARENT,
//                )
//                view.setOnTouchListener { _, event ->
//                    handler.handleMotionEvent(event, latestBrush)
//                }
//                view.eagerInit()
//                view
//            },
//            onRelease = { view ->
//                view.setOnTouchListener(null)
//                inkingHandlerInstance?.let { handler ->
//                    view.removeFinishedStrokesListener(handler)
//                }
//                inkingHandlerInstance = null
//            }
//        )
        InProgressStrokes(
            defaultBrush = brush,
            nextBrush = { latestBrush },
            onStrokesFinished = addStrokes
        )
        FinishedStrokes(
            finishedStrokes = finishedStrokes,
            canvasStrokeRenderer = canvasStrokeRenderer
        )
    }
}
