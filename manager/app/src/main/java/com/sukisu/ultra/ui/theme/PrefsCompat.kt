package com.sukisu.ultra.ui.theme

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit

/**
 * Type-tolerant reads for the appearance preferences.
 *
 * [SharedPreferences] stores a value together with its type and throws [ClassCastException] when
 * the value is read back as a different one. These keys are written by theme import, which
 * round-trips through JSON - and JSON has a single number type, so Android's org.json serialises
 * a whole-valued `Double` as a bare integer (`1.0` becomes `1`) and it parses back as an
 * `Integer`. Storing that with `putInt` made every later `getFloat` throw.
 *
 * Because these loads run from `Application.onCreate`, that became an app that could not start at
 * all: the crash happened before any UI existed, so there was no way to clear the bad value from
 * inside the app. Each helper below therefore coerces whatever is actually stored, repairs the
 * entry so the next launch is clean, and falls back to `default` when the value cannot be
 * salvaged.
 */
private const val TAG = "PrefsCompat"

/** Reads [key] as a Float, converting whatever type it was actually stored with. */
internal fun SharedPreferences.floatPref(key: String, default: Float): Float = try {
    getFloat(key, default)
} catch (e: ClassCastException) {
    val stored = all[key]
    val coerced: Float? = (stored as? Number)?.toFloat() ?: (stored as? String)?.toFloatOrNull()
    if (coerced == null) {
        Log.w(TAG, "dropped unreadable $key (${stored?.javaClass?.simpleName})")
        edit { remove(key) }
        default
    } else {
        Log.w(TAG, "repaired $key (${stored?.javaClass?.simpleName} -> Float)")
        edit { putFloat(key, coerced) }
        coerced
    }
}

/** Reads [key] as an Int, converting whatever type it was actually stored with. */
internal fun SharedPreferences.intPref(key: String, default: Int): Int = try {
    getInt(key, default)
} catch (e: ClassCastException) {
    val stored = all[key]
    val coerced: Int? = when (stored) {
        is Number -> stored.toInt()
        is String -> stored.toIntOrNull()
        is Boolean -> if (stored) 1 else 0
        else -> null
    }
    if (coerced == null) {
        Log.w(TAG, "dropped unreadable $key (${stored?.javaClass?.simpleName})")
        edit { remove(key) }
        default
    } else {
        Log.w(TAG, "repaired $key (${stored?.javaClass?.simpleName} -> Int)")
        edit { putInt(key, coerced) }
        coerced
    }
}

/** Reads [key] as a Long, converting whatever type it was actually stored with. */
internal fun SharedPreferences.longPref(key: String, default: Long): Long = try {
    getLong(key, default)
} catch (e: ClassCastException) {
    val stored = all[key]
    val coerced: Long? = when (stored) {
        is Number -> stored.toLong()
        is String -> stored.toLongOrNull()
        else -> null
    }
    if (coerced == null) {
        Log.w(TAG, "dropped unreadable $key (${stored?.javaClass?.simpleName})")
        edit { remove(key) }
        default
    } else {
        Log.w(TAG, "repaired $key (${stored?.javaClass?.simpleName} -> Long)")
        edit { putLong(key, coerced) }
        coerced
    }
}

/** Reads [key] as a Boolean, converting whatever type it was actually stored with. */
internal fun SharedPreferences.booleanPref(key: String, default: Boolean): Boolean = try {
    getBoolean(key, default)
} catch (e: ClassCastException) {
    val stored = all[key]
    val coerced: Boolean? = when (stored) {
        is Boolean -> stored
        is Number -> stored.toInt() != 0
        is String -> stored.toBooleanStrictOrNull()
        else -> null
    }
    if (coerced == null) {
        Log.w(TAG, "dropped unreadable $key (${stored?.javaClass?.simpleName})")
        edit { remove(key) }
        default
    } else {
        Log.w(TAG, "repaired $key (${stored?.javaClass?.simpleName} -> Boolean)")
        edit { putBoolean(key, coerced) }
        coerced
    }
}

/** Reads [key] as a String, converting whatever type it was actually stored with. */
internal fun SharedPreferences.stringPref(key: String, default: String?): String? = try {
    getString(key, default)
} catch (e: ClassCastException) {
    val stored = all[key]
    if (stored == null) {
        default
    } else {
        val coerced = stored.toString()
        Log.w(TAG, "repaired $key (${stored.javaClass.simpleName} -> String)")
        edit { putString(key, coerced) }
        coerced
    }
}
