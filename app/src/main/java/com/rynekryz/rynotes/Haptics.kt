package com.rynekryz.rynotes

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

private var cachedVibrator: Vibrator? = null
private var vibratorFetched = false

private fun Context.vibratorOrNull(): Vibrator? {
    if (vibratorFetched) return cachedVibrator
    vibratorFetched = true
    cachedVibrator = runCatching {
        (applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    }.getOrNull()
    return cachedVibrator
}

private fun Vibrator.oneShot(durationMs: Long, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
    if (!hasVibrator()) return
    runCatching {
        vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
    }
}

private fun Vibrator.pattern(timings: LongArray, amplitudes: IntArray) {
    if (!hasVibrator()) return
    runCatching {
        vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }
}

class Haptics(
    private val vibrator: Vibrator?,
    private val enabled: () -> Boolean,
) {
    fun tick() {
        if (!enabled()) return
        vibrator?.oneShot(20, 90)
    }

    fun click() {
        if (!enabled()) return
        vibrator?.oneShot(25, 150)
    }

    fun confirm() {
        if (!enabled()) return
        vibrator?.pattern(longArrayOf(0, 12, 40, 18), intArrayOf(0, 140, 0, 200))
    }

    fun reject() {
        if (!enabled()) return
        vibrator?.pattern(longArrayOf(0, 20, 60, 20, 60, 20), intArrayOf(0, 180, 0, 180, 0, 180))
    }
}

@Composable
fun rememberHaptics(enabled: Boolean): Haptics {
    val context = LocalContext.current
    val enabledState = rememberUpdatedState(enabled)
    return remember {
        Haptics(context.vibratorOrNull()) { enabledState.value }
    }
}

val LocalHapticsEnabled = compositionLocalOf { true }
