package dev.abdeldjalilchougui.android_special_permissions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ResumeWaiterTest {
    @Test
    fun idle_resumeDoesNothing() {
        val w = ResumeWaiter<String>()
        assertFalse(w.isWaiting)
        assertNull(w.onResumed())
    }

    @Test
    fun resumeBeforeLeaving_doesNotComplete() {
        val w = ResumeWaiter<String>()
        assertTrue(w.start("a"))
        assertNull(w.onResumed(), "a resume that was not preceded by a pause must be ignored")
        assertTrue(w.isWaiting)
    }

    @Test
    fun pauseThenResume_completesOnce() {
        val w = ResumeWaiter<String>()
        w.start("a")
        w.onPaused()
        assertEquals("a", w.onResumed())
        assertFalse(w.isWaiting)
        assertNull(w.onResumed())
    }

    @Test
    fun secondStartWhileWaiting_isRejected() {
        val w = ResumeWaiter<String>()
        assertTrue(w.start("a"))
        assertFalse(w.start("b"))
        w.onPaused()
        assertEquals("a", w.onResumed())
        assertTrue(w.start("b"))
    }

    @Test
    fun cancel_returnsPendingAndResets() {
        val w = ResumeWaiter<String>()
        w.start("a")
        w.onPaused()
        assertEquals("a", w.cancel())
        assertFalse(w.isWaiting)
        assertNull(w.cancel())
        assertNull(w.onResumed())
    }

    @Test
    fun pauseFromEarlierRequest_doesNotLeakIntoNext() {
        val w = ResumeWaiter<String>()
        w.onPaused()
        w.start("a")
        assertNull(w.onResumed())
    }
}
