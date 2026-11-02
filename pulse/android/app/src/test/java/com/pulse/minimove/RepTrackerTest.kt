package com.pulse.minimove

import org.junit.Assert.*
import org.junit.Test

class RepTrackerTest {
    @Test fun oneControlledSquat() {
        val tracker = RepTracker(108f, 158f)
        var time = 1000L
        fun send(a: Float?): Boolean { time+=130; return tracker.observe(a, time) }
        repeat(3){assertFalse(send(172f))}
        repeat(3){assertFalse(send(94f))}
        assertFalse(send(166f))
        assertTrue(send(166f))
        assertEquals(1,tracker.count)
        repeat(10){send(170f)}
        assertEquals(1,tracker.count)
    }
    @Test fun noCountFromNoiseOrIncompleteMotion() {
        val tracker=RepTracker(108f,158f)
        var time=1000L
        fun send(a:Float?) {time+=130;tracker.observe(a,time)}
        listOf(164f,165f,164f,130f,122f,135f,163f,164f,164f).forEach(::send)
        assertEquals(0,tracker.count)
        listOf(98f,98f,165f,164f).forEach(::send)
        assertEquals(1,tracker.count)
    }
    @Test fun resetAndMissingPoseRequireRecalibration() {
        val tracker=RepTracker(85f,145f)
        var time=1000L
        fun send(a:Float?) {time+=130;tracker.observe(a,time)}
        repeat(3){send(166f)}
        repeat(2){send(65f)}
        time+=2500;tracker.observe(null,time)
        repeat(2){send(165f)}
        assertEquals(0,tracker.count)
        repeat(3){send(165f)}
        repeat(2){send(60f)}
        repeat(2){send(165f)}
        assertEquals(1,tracker.count)
        tracker.reset(4)
        assertEquals(4,tracker.count)
        assertEquals(RepTracker.Phase.SEARCHING,tracker.phase)
    }
}
