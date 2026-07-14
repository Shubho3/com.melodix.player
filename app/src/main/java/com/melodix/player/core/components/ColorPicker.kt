package com.melodix.player.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A simple RGB color picker: a preview swatch with hex readout above three channel sliders.
 * [color] and the [onColorChange] result are opaque ARGB ints.
 */
@Composable
fun ColorPicker(
    label: String,
    color: Int,
    onColorChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "#%06X".format(color and 0xFFFFFF),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(color))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(8.dp),
                    ),
            )
        }
        ChannelSlider("R", r, Color(0xFFE53935)) { onColorChange(argb(it, g, b)) }
        ChannelSlider("G", g, Color(0xFF43A047)) { onColorChange(argb(r, it, b)) }
        ChannelSlider("B", b, Color(0xFF1E88E5)) { onColorChange(argb(r, g, it)) }
    }
}

@Composable
private fun ChannelSlider(
    name: String,
    value: Int,
    tint: Color,
    onValue: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(14.dp),
        )
        Slider(
            value = value / 255f,
            onValueChange = { onValue((it * 255).roundToInt().coerceIn(0, 255)) },
            modifier = Modifier
                .weight(1f)
                .height(28.dp),
            colors = SliderDefaults.colors(
                thumbColor = tint,
                activeTrackColor = tint,
            ),
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
        )
    }
}

private fun argb(r: Int, g: Int, b: Int): Int =
    (0xFF shl 24) or (r shl 16) or (g shl 8) or b
