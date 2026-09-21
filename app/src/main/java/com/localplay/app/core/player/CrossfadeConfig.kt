package com.localplay.app.core.player

/**
 * Crossfade settings, stored in memory and exposed via PlayerRepository.
 *
 * crossfadeDurationMs: how long the volume overlap lasts (0 = off).
 * mixingLabelLeadMs:   how many ms before the fade starts to show "Mixing".
 *                      Apple Music shows it roughly 1-2 seconds before the
 *                      crossfade begins, so we default to 1500 ms.
 */
data class CrossfadeConfig(
    val crossfadeDurationMs: Long = 0L,     // 0 = crossfade disabled
    val mixingLabelLeadMs: Long   = 1_500L  // show "Mixing" 1.5s before fade
) {
    val isEnabled: Boolean get() = crossfadeDurationMs > 0L
}
