package dev.abdeldjalilchougui.android_special_permissions

/**
 * Tracks one pending request that completes when the app comes back to
 * the foreground after having left it for a Settings screen.
 */
class ResumeWaiter<T : Any> {
    private var pending: T? = null
    private var left = false

    val isWaiting: Boolean get() = pending != null

    /** Starts waiting for [request]; false if another request is pending. */
    fun start(request: T): Boolean {
        if (pending != null) return false
        pending = request
        left = false
        return true
    }

    /** The host activity was paused or stopped. */
    fun onPaused() {
        if (pending != null) left = true
    }

    /** The host activity resumed; returns the request to complete, if any. */
    fun onResumed(): T? {
        if (pending == null || !left) return null
        return cancel()
    }

    /** Drops the pending request and returns it. */
    fun cancel(): T? {
        val request = pending
        pending = null
        left = false
        return request
    }
}
