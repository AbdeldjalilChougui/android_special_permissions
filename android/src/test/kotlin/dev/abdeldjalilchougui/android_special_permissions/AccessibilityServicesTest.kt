package dev.abdeldjalilchougui.android_special_permissions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class AccessibilityServicesTest {
    private val pkg = "com.example.app"

    @Test
    fun normalize_expandsLeadingDot() {
        assertEquals("com.example.app.MyService", AccessibilityServices.normalizeClassName(pkg, ".MyService"))
    }

    @Test
    fun normalize_expandsBareSimpleName() {
        assertEquals("com.example.app.MyService", AccessibilityServices.normalizeClassName(pkg, "MyService"))
    }

    @Test
    fun normalize_keepsFullyQualifiedName() {
        assertEquals("org.other.Svc", AccessibilityServices.normalizeClassName(pkg, "org.other.Svc"))
    }

    @Test
    fun flatten_buildsLongComponentName() {
        assertEquals("com.example.app/com.example.app.sub.Svc", AccessibilityServices.flatten(pkg, ".sub.Svc"))
    }

    @Test
    fun nullOrBlankSetting_isNotEnabled() {
        assertFalse(AccessibilityServices.isEnabled(null, pkg, "com.example.app.MyService"))
        assertFalse(AccessibilityServices.isEnabled("", pkg, "com.example.app.MyService"))
        assertFalse(AccessibilityServices.isEnabled("   ", pkg, "com.example.app.MyService"))
    }

    @Test
    fun longForm_matches() {
        assertTrue(AccessibilityServices.isEnabled("com.example.app/com.example.app.MyService", pkg, "com.example.app.MyService"))
    }

    @Test
    fun shortForm_matchesFullyQualifiedInput() {
        assertTrue(AccessibilityServices.isEnabled("com.example.app/.MyService", pkg, "com.example.app.MyService"))
    }

    @Test
    fun longForm_matchesRelativeInput() {
        assertTrue(AccessibilityServices.isEnabled("com.example.app/com.example.app.MyService", pkg, ".MyService"))
    }

    @Test
    fun mixedShortAndLongEntries_areAllParsed() {
        val setting = "com.google.android.marvin.talkback/.TalkBackService:" +
            "org.other/org.other.a11y.Reader:" +
            "com.example.app/.sub.MyService"
        assertTrue(AccessibilityServices.isEnabled(setting, pkg, "com.example.app.sub.MyService"))
        assertFalse(AccessibilityServices.isEnabled(setting, pkg, "com.example.app.MyService"))
    }

    @Test
    fun sameClassInAnotherPackage_doesNotMatch() {
        assertFalse(AccessibilityServices.isEnabled("org.other/com.example.app.MyService", pkg, "com.example.app.MyService"))
    }

    @Test
    fun longerClassWithSamePrefix_doesNotMatch() {
        assertFalse(AccessibilityServices.isEnabled("com.example.app/.MyServiceExtra", pkg, ".MyService"))
    }

    @Test
    fun whitespaceEmptyEntriesAndCase_areTolerated() {
        val setting = " :  COM.EXAMPLE.APP/.MYSERVICE  ::"
        assertTrue(AccessibilityServices.isEnabled(setting, pkg, "com.example.app.MyService"))
    }

    @Test
    fun nestedClassNames_match() {
        assertTrue(AccessibilityServices.isEnabled("com.example.app/.Outer\$Inner", pkg, "com.example.app.Outer\$Inner"))
    }

    @Test
    fun entryWithoutSlash_isIgnored() {
        assertFalse(AccessibilityServices.isEnabled("com.example.app.MyService", pkg, "com.example.app.MyService"))
    }
}
