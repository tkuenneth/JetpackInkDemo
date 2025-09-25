package dev.tkuenneth.jetpackinkdemo

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.ink.authoring.compose.InProgressStrokes
import androidx.ink.brush.Brush
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke

@Composable
fun DrawingSurface(
    finishedStrokes: Collection<Stroke>,
    brush: Brush,
    modifier: Modifier = Modifier,
    addStrokes: (Collection<Stroke>) -> Unit
) {
    val canvasStrokeRenderer = remember { CanvasStrokeRenderer.create() }
    val latestBrush by rememberUpdatedState(brush)
    Box(modifier = modifier.clipToBounds()) {
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
