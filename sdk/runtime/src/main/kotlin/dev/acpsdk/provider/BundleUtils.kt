package dev.acpsdk.provider

import android.os.Bundle

/**
 * Converts a Bundle to a Map<String, Any?>.
 */
fun Bundle.toAcpMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    for (key in keySet()) {
        @Suppress("DEPRECATION")
        map[key] = get(key)
    }
    return map
}

/**
 * Converts a Map<String, Any?> to a Bundle.
 */
fun Map<String, Any?>.toBundle(): Bundle {
    val bundle = Bundle()
    for ((key, value) in this) {
        when (value) {
            is String -> bundle.putString(key, value)
            is Int -> bundle.putInt(key, value)
            is Long -> bundle.putLong(key, value)
            is Double -> bundle.putDouble(key, value)
            is Float -> bundle.putFloat(key, value)
            is Boolean -> bundle.putBoolean(key, value)
            is ArrayList<*> -> {
                @Suppress("UNCHECKED_CAST")
                bundle.putStringArrayList(key, value as ArrayList<String>)
            }
            null -> bundle.putString(key, null)
            else -> bundle.putString(key, value.toString())
        }
    }
    return bundle
}
