package com.cussou.parallax.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cussou.parallax.R

/**
 * Reminder that the model is simplified and that the head-movement test at the range
 * remains the reference (NF-11). Shown in the About section and in the help.
 */
@Composable
fun Disclaimer(modifier: Modifier = Modifier, textTag: String? = null) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // The icon carries no extra meaning for a screen reader: the text says it all.
        Icon(Icons.Outlined.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            stringResource(R.string.disclaimer),
            modifier = if (textTag != null) Modifier.testTag(textTag) else Modifier,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
