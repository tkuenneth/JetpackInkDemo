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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

enum class DrawingMode { Free, Circle }

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val colors = mapOf(
                Color.Green to stringResource(R.string.green),
                Color.Red to stringResource(R.string.red),
                Color.Yellow to stringResource(R.string.yellow)
            )
            MaterialTheme(
                colorScheme = defaultColorScheme()
            ) {
                MainScreen(colors = colors)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(colors: Map<Color, String>) {
    val finishedStrokes = remember { mutableStateMapOf<InProgressStrokeId, Stroke>() }
    var showMenu by remember { mutableStateOf(false) }
    var currentColor by remember { mutableStateOf(colors.keys.first()) }
    var drawingMode by remember { mutableStateOf(DrawingMode.Free) }
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
                    IconButton(
                        enabled = finishedStrokes.isNotEmpty(),
                        onClick = { finishedStrokes.clear() }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear)
                        )
                    }
                    IconButton(
                        onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more_options)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        colors.forEach { (color, name) ->
                            ColorSelector(
                                color = color,
                                name = name,
                                selected = color == currentColor,
                                onColorSelected = {
                                    currentColor = it
                                    showMenu = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.free)) },
                            onClick = { drawingMode = DrawingMode.Free },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.circle)) },
                            onClick = { drawingMode = DrawingMode.Circle },
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
                drawingMode = drawingMode,
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
    drawingMode: DrawingMode,
    modifier: Modifier = Modifier,
    addStrokes: (Map<InProgressStrokeId, Stroke>) -> Unit
) {
    val canvasStrokeRenderer = remember { CanvasStrokeRenderer.create() }
    val latestBrush by rememberUpdatedState(brush)
    val latestDrawingMode by rememberUpdatedState(drawingMode)
    var currentPointerId by remember { mutableStateOf<Int?>(null) }
    var currentStrokeId by remember { mutableStateOf<InProgressStrokeId?>(null) }
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            InProgressStrokesView(context).apply {
                addFinishedStrokesListener(object : InProgressStrokesFinishedListener {
                    override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
                        if (latestDrawingMode == DrawingMode.Circle) {
                            addStrokes(strokes)
                        } else {
                            addStrokes(strokes)
                        }
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
    name: String,
    selected: Boolean,
    onColorSelected: (Color) -> Unit
) {
    DropdownMenuItem(
        text = { Text(name) },
        onClick = { onColorSelected(color) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color, CircleShape)
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
    )
}
