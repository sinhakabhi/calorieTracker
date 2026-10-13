package com.veyra.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun formatPrescription_combinesSetsAndTarget() {
        assertEquals("4 × 8-10 reps", formatPrescription(4, "8-10 reps"))
        assertEquals("5 km easy pace", formatPrescription(null, " 5 km easy pace "))
        assertEquals("3 sets", formatPrescription(3, ""))
        assertEquals("1 set", formatPrescription(1, " "))
        assertEquals("", formatPrescription(null, ""))
    }

    @Test
    fun formatLitres_dropsTrailingZeros() {
        assertEquals("0", formatLitres(0))
        assertEquals("1.25", formatLitres(1250))
        assertEquals("3.5", formatLitres(3500))
    }
}
