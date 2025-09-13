package dev.tkuenneth.jetpackinkdemo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


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
