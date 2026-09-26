package dev.abdeldjalilchougui.android_special_permissions

/** Parsing of `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`. */
object AccessibilityServices {
    /**
     * Expands a class name the way the manifest does: `.Svc` and `Svc`
     * are relative to [packageName]; anything with a dot inside is kept.
     */
    fun normalizeClassName(packageName: String, className: String): String = when {
        className.startsWith(".") -> packageName + className
        !className.contains('.') -> "$packageName.$className"
        else -> className
    }

    /** Long form `package/fully.qualified.Class`. */
    fun flatten(packageName: String, className: String): String =
        "$packageName/${normalizeClassName(packageName, className)}"

    /**
     * Whether the colon-separated [setting] lists the service
     * [className] of [packageName]. Entries may use the short
     * (`pkg/.Svc`) or long (`pkg/pkg.Svc`) component form.
     */
    fun isEnabled(setting: String?, packageName: String, className: String): Boolean {
        if (setting.isNullOrBlank()) return false
        val expected = flatten(packageName, className)
        return setting.split(':').asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { unflatten(it) }
            .any { it.equals(expected, ignoreCase = true) }
    }

    /** Mirrors `ComponentName.unflattenFromString`: only a leading dot is expanded. */
    private fun unflatten(entry: String): String? {
        val slash = entry.indexOf('/')
        if (slash <= 0 || slash == entry.length - 1) return null
        val pkg = entry.substring(0, slash)
        val cls = entry.substring(slash + 1)
        return if (cls.startsWith(".")) "$pkg/$pkg$cls" else "$pkg/$cls"
    }
}
