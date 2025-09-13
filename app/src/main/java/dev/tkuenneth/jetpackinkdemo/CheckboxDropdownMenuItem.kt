package dev.tkuenneth.jetpackinkdemo

import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
fun CheckboxDropdownMenuItem(
    text: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    DropdownMenuItem(text = { Text(text) }, modifier = modifier, enabled = enabled, leadingIcon = {
        Checkbox(
            checked = checked, onCheckedChange = null, enabled = enabled
        )
    }, onClick = {
        onCheckedChange(!checked)
    })
}
