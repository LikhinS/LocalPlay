package com.localplay.app.ui.components
import androidx.compose.animation.AnimatedContent; import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn; import androidx.compose.animation.fadeOut; import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border; import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*; import androidx.compose.runtime.*
import androidx.compose.ui.Modifier; import androidx.compose.ui.draw.alpha; import androidx.compose.ui.unit.dp
@Composable fun FormatBadgeRow(isLossless:Boolean, isHiRes:Boolean, isAtmos:Boolean, isMixing:Boolean, modifier:Modifier=Modifier) {
    val label:String? = when { isMixing->"Mixing"; isAtmos->"Dolby Atmos"; isHiRes->"Hi-Res Lossless"; isLossless->"Lossless"; else->null }
    AnimatedContent(targetState=label, transitionSpec={fadeIn(tween(300)) togetherWith fadeOut(tween(300))}, modifier=modifier, label="badge") { tl ->
        if(tl!=null) { if(tl=="Mixing") MixingBadgePill() else BadgePill(tl) }
    }
}
@Composable fun BadgePill(label:String, modifier:Modifier=Modifier) {
    Text(text=label, style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.primary,
        modifier=modifier.border(1.dp,MaterialTheme.colorScheme.primary,RoundedCornerShape(4.dp)).padding(horizontal=5.dp,vertical=2.dp))
}
@Composable private fun MixingBadgePill() {
    val tr = rememberInfiniteTransition(label="mix")
    val a by tr.animateFloat(0.55f,1.0f,infiniteRepeatable(tween(900,easing=LinearEasing),RepeatMode.Reverse),label="ma")
    Text("Mixing", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.primary,
        modifier=Modifier.alpha(a).border(1.dp,MaterialTheme.colorScheme.primary.copy(alpha=a),RoundedCornerShape(4.dp)).padding(horizontal=5.dp,vertical=2.dp))
}
