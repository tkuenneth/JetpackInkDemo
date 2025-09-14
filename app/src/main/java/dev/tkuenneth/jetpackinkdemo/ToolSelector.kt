package dev.tkuenneth.jetpackinkdemo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun ToolSelector(
    text: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable {
            onCheckedChange(!checked)
        }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled
        )
        Text(text)
    }
}

@Composable
fun Tools(brushFamily: BrushFamily, updateBrushFamily: (BrushFamily) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ToolSelector(
            text = stringResource(R.string.pen),
            checked = brushFamily == BrushFamily.Pen,
        ) {
            updateBrushFamily(BrushFamily.Pen)
        }
        ToolSelector(
            text = stringResource(R.string.highlighter),
            checked = brushFamily == BrushFamily.Highlighter,
        ) {
            updateBrushFamily(BrushFamily.Highlighter)
        }
    }
}
