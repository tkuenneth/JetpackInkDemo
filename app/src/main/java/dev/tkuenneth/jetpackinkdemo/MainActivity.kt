package dev.tkuenneth.jetpackinkdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.Stroke

enum class BrushFamily { Pen, Highlighter }

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val colors = mapOf(
                Color.Blue to stringResource(R.string.blue),
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
    val finishedStrokes = rememberSaveable(
        saver = with(SerializationHelper()) {
            Saver(
                save = { strokes ->
                    ArrayList(serializeStrokes(strokes))
                },
                restore = { strokes ->
                    mutableStateListOf<Stroke>().apply {
                        addAll(deserializeStrokes(strokes))
                    }
                }
            )
        }
    ) { mutableStateListOf<Stroke>() }
    var currentColor by remember { mutableStateOf(colors.keys.first()) }
    var brushFamily by remember { mutableStateOf(BrushFamily.Pen) }
    val brush = remember(currentColor, brushFamily) {
        Brush.createWithColorIntArgb(
            family = when (brushFamily) {
                BrushFamily.Pen -> StockBrushes.pressurePenLatest
                BrushFamily.Highlighter -> StockBrushes.highlighterLatest
            },
            colorIntArgb = when (brushFamily) {
                BrushFamily.Pen -> currentColor.toArgb()
                BrushFamily.Highlighter -> currentColor.copy(alpha = 0.4F).toArgb()
            },
            size = when (brushFamily) {
                BrushFamily.Pen -> 5F
                BrushFamily.Highlighter -> 55F
            },
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
                },
            )
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            ) {
                Colors(
                    colors = colors,
                    currentColor = currentColor
                ) {
                    currentColor = it
                }
                Tools(brushFamily = brushFamily) { brushFamily = it }
                DrawingSurface(
                    finishedStrokes = finishedStrokes,
                    brush = brush,
                ) { strokes ->
                    finishedStrokes.addAll(strokes)
                }
            }
        }
    }
}
