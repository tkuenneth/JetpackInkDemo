package dev.tkuenneth.jetpackinkdemo

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.os.Bundle
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import androidx.input.motionprediction.MotionEventPredictor

class MainActivity : ComponentActivity(), InProgressStrokesFinishedListener {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = defaultColorScheme()
            ) {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize(), topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) })
        }) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            DrawingSurface()
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun DrawingSurface() {
    val context = LocalContext.current
    val finishedStrokes = remember { mutableStateMapOf<InProgressStrokeId, Stroke>() }
    val canvasStrokeRenderer = remember { CanvasStrokeRenderer.create() }
    var currentPointerId by remember { mutableStateOf<Int?>(null) }
    var currentStrokeId by remember { mutableStateOf<InProgressStrokeId?>(null) }
    val onSurface = MaterialTheme.colorScheme.onSurface
    val brush = remember(onSurface) {
        Brush.createWithColorIntArgb(
            family = StockBrushes.pressurePenLatest,
            colorIntArgb = onSurface.toArgb(),
            size = 5F,
            epsilon = 0.1F
        )
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            InProgressStrokesView(context).apply {
                addFinishedStrokesListener(object : InProgressStrokesFinishedListener {
                    override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
                        finishedStrokes.putAll(strokes)
                        removeFinishedStrokes(strokes.keys)
                    }
                })
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
                MotionEventPredictor.newInstance(this@apply).apply {
                    setOnTouchListener { view, event ->
                        record(event)
                        predict().let { predictedEvent ->
                            try {
                                when (event.actionMasked) {
                                    MotionEvent.ACTION_DOWN -> {
                                        view.requestUnbufferedDispatch(event)
                                        val pointerIndex = event.actionIndex
                                        val pointerId = event.getPointerId(pointerIndex)
                                        currentPointerId = pointerId
                                        currentStrokeId = startStroke(
                                            event = event, pointerId = pointerId, brush = brush
                                        )
                                        true
                                    }

                                    MotionEvent.ACTION_MOVE -> {
                                        val pointerId = checkNotNull(currentPointerId)
                                        val strokeId = checkNotNull(currentStrokeId)
                                        for (pointerIndex in 0 until event.pointerCount) {
                                            if (event.getPointerId(pointerIndex) != pointerId) continue
                                            addToStroke(
                                                event, pointerId, strokeId, predictedEvent
                                            )
                                        }
                                        true
                                    }

                                    MotionEvent.ACTION_UP -> {
                                        val pointerIndex = event.actionIndex
                                        val pointerId = event.getPointerId(pointerIndex)
                                        check(pointerId == currentPointerId)
                                        val currentStrokeId = checkNotNull(currentStrokeId)
                                        finishStroke(
                                            event, pointerId, currentStrokeId
                                        )
                                        view.performClick()
                                        true
                                    }

                                    MotionEvent.ACTION_CANCEL -> {
                                        val pointerId = event.getPointerId(event.actionIndex)
                                        check(pointerId == currentPointerId)
                                        val currentStrokeId = checkNotNull(currentStrokeId)
                                        cancelStroke(currentStrokeId, event)
                                        true
                                    }

                                    else -> false
                                }
                            } finally {
                                predictedEvent?.recycle()
                            }
                        }
                    }
                    eagerInit()
                }
            }
        },
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasTransform = Matrix()
        drawContext.canvas.nativeCanvas.concat(canvasTransform)
        val canvas = drawContext.canvas.nativeCanvas
        finishedStrokes.values.forEach { stroke ->
            canvasStrokeRenderer.draw(
                stroke = stroke, canvas = canvas, strokeToScreenTransform = canvasTransform
            )
        }
    }
}