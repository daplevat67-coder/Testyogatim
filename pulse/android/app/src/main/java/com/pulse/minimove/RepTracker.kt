package com.pulse.minimove

/**
 * Simple finite-state counter: complete extension -> controlled flexion -> complete extension.
 * Ignores untracked frames and requires consecutive observations to reject jitter.
 */
class RepTracker(private val bentBelow: Float, private val straightAbove: Float) {
    enum class Phase { SEARCHING, READY, BENT }
    var phase: Phase = Phase.SEARCHING
        private set
    var count: Int = 0
        private set
    private var stable = 0
    private var lastCountAt = 0L
    private var lastSeenAt = 0L

    fun reset(initial: Int = 0) {
        count = initial.coerceIn(0, 2000)
        phase = Phase.SEARCHING
        stable = 0
        lastCountAt = 0L
        lastSeenAt = 0L
    }

    /** Returns true exactly once for each full, well-observed repetition. */
    fun observe(angle: Float?, now: Long): Boolean {
        if (angle == null || !angle.isFinite() || angle !in 0f..180f) {
            if (lastSeenAt != 0L && now - lastSeenAt > 1300L) {
                phase = Phase.SEARCHING
                stable = 0
            }
            return false
        }
        lastSeenAt = now
        when (phase) {
            Phase.SEARCHING -> {
                stable = if (angle > straightAbove) stable + 1 else 0
                if (stable >= 3) { phase = Phase.READY; stable = 0 }
            }
            Phase.READY -> {
                stable = if (angle < bentBelow) stable + 1 else 0
                if (stable >= 2) { phase = Phase.BENT; stable = 0 }
            }
            Phase.BENT -> {
                stable = if (angle > straightAbove) stable + 1 else 0
                if (stable >= 2 && now - lastCountAt >= 650L) {
                    count++
                    phase = Phase.READY
                    lastCountAt = now
                    stable = 0
                    return true
                }
            }
        }
        return false
    }
}
