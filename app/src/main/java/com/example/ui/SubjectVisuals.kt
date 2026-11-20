package com.example.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.SubjectColorTag
import com.example.data.SubjectIconName
import com.example.ui.theme.HighlightBerryBg
import com.example.ui.theme.HighlightBerryInk
import com.example.ui.theme.HighlightBlushBg
import com.example.ui.theme.HighlightBlushInk
import com.example.ui.theme.HighlightCoralBg
import com.example.ui.theme.HighlightCoralInk
import com.example.ui.theme.HighlightMagentaBg
import com.example.ui.theme.HighlightMagentaInk
import com.example.ui.theme.HighlightPlumBg
import com.example.ui.theme.HighlightPlumInk
import com.example.ui.theme.HighlightRoseBg
import com.example.ui.theme.HighlightRoseInk

data class PinkAccentStyle(
    val container: Color,
    val ink: Color
)

fun SubjectColorTag.toPinkStyle(): PinkAccentStyle = when (this) {
    SubjectColorTag.ROSE -> PinkAccentStyle(HighlightRoseBg, HighlightRoseInk)
    SubjectColorTag.BERRY -> PinkAccentStyle(HighlightBerryBg, HighlightBerryInk)
    SubjectColorTag.BLUSH -> PinkAccentStyle(HighlightBlushBg, HighlightBlushInk)
    SubjectColorTag.CORAL -> PinkAccentStyle(HighlightCoralBg, HighlightCoralInk)
    SubjectColorTag.PLUM -> PinkAccentStyle(HighlightPlumBg, HighlightPlumInk)
    SubjectColorTag.MAGENTA -> PinkAccentStyle(HighlightMagentaBg, HighlightMagentaInk)
}

fun SubjectIconName.toVector(): ImageVector = when (this) {
    SubjectIconName.BOOK -> Icons.Rounded.AutoStories
    SubjectIconName.SCIENCE -> Icons.Rounded.Science
    SubjectIconName.HISTORY -> Icons.Rounded.HistoryEdu
    SubjectIconName.MATH -> Icons.Rounded.Calculate
    SubjectIconName.LANGUAGE -> Icons.Rounded.Translate
    SubjectIconName.ART -> Icons.Rounded.Palette
}

@Composable
fun rememberDecodedThumbnail(base64: String): ImageBitmap? {
    return remember(base64) {
        if (base64.isBlank()) {
            null
        } else {
            try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }
}

fun splitTextIntoHighlightableSegments(text: String): List<String> {
    if (text.isBlank()) return emptyList()
    return text
        .split(Regex("(?<=[.!?])\\s+|\\n+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}
