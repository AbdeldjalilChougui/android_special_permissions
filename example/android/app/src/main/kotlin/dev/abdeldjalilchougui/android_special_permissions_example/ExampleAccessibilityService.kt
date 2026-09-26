package dev.abdeldjalilchougui.android_special_permissions_example

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/** Does nothing; it only exists so the example can show the enable flow. */
class ExampleAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
