package com.localplay.app.core.player
data class CrossfadeConfig(
    val crossfadeDurationMs: Long = 0L,
    val mixingLabelLeadMs: Long = 1_500L
) { val isEnabled: Boolean get() = crossfadeDurationMs > 0L }
