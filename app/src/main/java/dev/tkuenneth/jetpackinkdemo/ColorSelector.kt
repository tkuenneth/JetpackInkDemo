package dev.tkuenneth.jetpackinkdemo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp


@Composable
fun ColorSelector(
    color: Color,
    name: String,
    selected: Boolean,
    onColorSelected: (Color) -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(color, CircleShape)
            .clickable {
                onColorSelected(color)
            }
            .semantics { contentDescription = name }
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

@Composable
fun Colors(colors: Map<Color, String>, currentColor: Color, onColorSelected: (Color) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { (color, name) ->
            ColorSelector(
                color = color,
                name = name,
                selected = color == currentColor,
                onColorSelected = onColorSelected
            )
        }
    }
}
