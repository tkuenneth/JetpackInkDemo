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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    val finishedStrokes = remember { mutableStateMapOf<InProgressStrokeId, Stroke>() }
    val colors = listOf(Color.Green, Color.Red, Color.Yellow)
    var currentColor by remember { mutableStateOf(colors[0]) }
    val brush = remember(currentColor) {
        Brush.createWithColorIntArgb(
            family = StockBrushes.pressurePenLatest,
            colorIntArgb = currentColor.toArgb(),
            size = 5F,
            epsilon = 0.1F
        )
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    colors.forEach { color ->
                        ColorSelector(
                            color = color,
                            selected = color == currentColor,
                        ) { currentColor = color }
                    }
                    IconButton(
                        enabled = finishedStrokes.isNotEmpty(),
                        onClick = { finishedStrokes.clear() }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear)
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            DrawingSurface(
                finishedStrokes = finishedStrokes.values,
                brush = brush,
            ) { strokes ->
                finishedStrokes.putAll(strokes)
            }
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun DrawingSurface(
    finishedStrokes: Collection<Stroke>,
    brush: Brush,
    modifier: Modifier = Modifier,
    addStrokes: (Map<InProgressStrokeId, Stroke>) -> Unit
) {
    val context = LocalContext.current
    val canvasStrokeRenderer = remember { CanvasStrokeRenderer.create() }
    val latestBrush by rememberUpdatedState(brush)
    var currentPointerId by remember { mutableStateOf<Int?>(null) }
    var currentStrokeId by remember { mutableStateOf<InProgressStrokeId?>(null) }
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = {
            InProgressStrokesView(context).apply {
                addFinishedStrokesListener(object : InProgressStrokesFinishedListener {
                    override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
                        addStrokes(strokes)
                        removeFinishedStrokes(strokes.keys)
                    }
                })
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
                val predictor = MotionEventPredictor.newInstance(this)
                setOnTouchListener { view, event ->
                    predictor.record(event)
                    predictor.predict().let { predictedEvent ->
                        try {
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN -> {
                                    view.requestUnbufferedDispatch(event)
                                    event.getPointerId(event.actionIndex).let { pointerId ->
                                        currentPointerId = pointerId
                                        currentStrokeId = startStroke(
                                            event = event,
                                            pointerId = pointerId,
                                            brush = latestBrush
                                        )
                                    }
                                    true
                                }

                                MotionEvent.ACTION_MOVE -> {
                                    currentPointerId?.let { pointerId ->
                                        currentStrokeId?.let { strokeId ->
                                            for (pointerIndex in 0 until event.pointerCount) {
                                                if (event.getPointerId(pointerIndex) != pointerId) continue
                                                addToStroke(
                                                    event, pointerId, strokeId, predictedEvent
                                                )
                                            }
                                        }
                                    }
                                    true
                                }

                                MotionEvent.ACTION_UP -> {
                                    val pointerId = event.getPointerId(event.actionIndex)
                                    currentStrokeId?.let { currentStrokeId ->
                                        if (pointerId == currentPointerId) {
                                            finishStroke(
                                                event, pointerId, currentStrokeId
                                            )
                                        }
                                    }
                                    view.performClick()
                                    true
                                }

                                MotionEvent.ACTION_CANCEL -> {
                                    val pointerId = event.getPointerId(event.actionIndex)
                                    currentStrokeId?.let { currentStrokeId ->
                                        if (pointerId == currentPointerId) {
                                            cancelStroke(currentStrokeId, event)
                                        }
                                    }
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
        },
    )
    FinishedStrokes(
        finishedStrokes = finishedStrokes,
        canvasStrokeRenderer = canvasStrokeRenderer
    )
}

@Composable
fun FinishedStrokes(
    finishedStrokes: Collection<Stroke>,
    canvasStrokeRenderer: CanvasStrokeRenderer
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasTransform = Matrix()
        drawContext.canvas.nativeCanvas.concat(canvasTransform)
        finishedStrokes.forEach { stroke ->
            canvasStrokeRenderer.draw(
                stroke = stroke,
                canvas = drawContext.canvas.nativeCanvas,
                strokeToScreenTransform = canvasTransform
            )
        }
    }
}

@Composable
fun ColorSelector(
    color: Color,
    selected: Boolean,
    onColorSelected: (Color) -> Unit
) {
    IconButton(onClick = { onColorSelected(color) }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = color,
                    shape = CircleShape,
                )
                .then(
                    if (selected) Modifier
                        .border(
                            width = 2.dp,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    else Modifier
                )
        )
    }
}
