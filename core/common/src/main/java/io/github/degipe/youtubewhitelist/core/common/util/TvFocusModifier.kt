package io.github.degipe.youtubewhitelist.core.common.util

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adds a bright visible border when this composable gains D-pad / TV focus.
 * Use on any focusable element (Button, Card, IconButton, etc.) to make
 * TV navigation clearly visible.
 */
fun Modifier.tvFocusBorder(
    width: Dp = 3.dp,
    shape: Shape = RoundedCornerShape(8.dp)
): Modifier = composed {
    val primaryColor = MaterialTheme.colorScheme.primary
    var isFocused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { isFocused = it.isFocused }
        .focusable()
        .then(
            if (isFocused) Modifier.border(width, primaryColor, shape)
            else Modifier
        )
}
