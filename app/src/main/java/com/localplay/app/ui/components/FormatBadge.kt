package com.localplay.app.ui.components
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable fun FormatBadgeRow(isLossless:Boolean, isHiRes:Boolean, isAtmos:Boolean, isMixing:Boolean, modifier:Modifier=Modifier) {
    if(!isLossless && !isAtmos && !isMixing) return
    Row(modifier=modifier, verticalAlignment=Alignment.CenterVertically) {
        if(isLossless) BadgePill(if(isHiRes) "Hi-Res Lossless" else "Lossless")
        if(isAtmos) { if(isLossless) Spacer(Modifier.width(6.dp)); BadgePill("Dolby Atmos") }
        if(isMixing) { if(isLossless||isAtmos) Spacer(Modifier.width(6.dp)); BadgePill("Mixing") }
    }
}
@Composable fun BadgePill(label:String, modifier:Modifier=Modifier) {
    Text(text=label, style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.primary,
        modifier=modifier.border(1.dp,MaterialTheme.colorScheme.primary,RoundedCornerShape(4.dp)).padding(horizontal=5.dp,vertical=2.dp))
}
