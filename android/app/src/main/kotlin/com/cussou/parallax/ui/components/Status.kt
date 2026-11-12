package com.cussou.parallax.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cussou.parallax.R
import com.cussou.parallax.domain.TargetStatus
import com.cussou.parallax.ui.theme.LocalStatusColors

// A status is never a color alone: it always comes with its icon, and a text where there is room
// or for screen readers (F-54).

/** ✓ / ⚠ / ✗ for a status; nothing but its space for NEUTRAL. */
@Composable
fun StatusIcon(status: TargetStatus, contentDescription: String?, modifier: Modifier = Modifier) {
    val icon = when (status) {
        TargetStatus.GREEN -> Icons.Outlined.Check
        TargetStatus.ORANGE -> Icons.Outlined.Warning
        TargetStatus.RED -> Icons.Outlined.Close
        TargetStatus.NEUTRAL -> null
    }
    // 24 dp unless the caller asks for another size, e.g. in match mode.
    Box(Modifier.size(24.dp).then(modifier)) {
        if (icon != null) {
            Icon(icon, contentDescription = contentDescription, tint = statusColor(status), modifier = Modifier.matchParentSize())
        }
    }
}

@Composable
fun statusColor(status: TargetStatus): Color = when (status) {
    TargetStatus.GREEN -> LocalStatusColors.current.green
    TargetStatus.ORANGE -> LocalStatusColors.current.orange
    TargetStatus.RED -> LocalStatusColors.current.red
    TargetStatus.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** "On target", "Marginal" or "Off target"; null for NEUTRAL. */
@Composable
fun statusText(status: TargetStatus): String? = when (status) {
    TargetStatus.GREEN -> stringResource(R.string.status_green)
    TargetStatus.ORANGE -> stringResource(R.string.status_orange)
    TargetStatus.RED -> stringResource(R.string.status_red)
    TargetStatus.NEUTRAL -> null
}
