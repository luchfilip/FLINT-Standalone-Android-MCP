package dev.acpsdk.hub.acp

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility for verifying ContentProvider communication security.
 *
 * In production, validates that an ACP provider belongs to the expected package
 * by resolving the provider authority and comparing the package name.
 *
 * In development mode ([devModeEnabled] = true), all providers are trusted.
 */
@Singleton
class AcpSecurity @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** When true, [isProviderTrusted] always returns true (development convenience). */
    var devModeEnabled: Boolean = true

    /**
     * Check whether the ContentProvider identified by [authority] belongs to [expectedPackage].
     *
     * @param authority The content provider authority to verify (e.g. "com.example.app.acp").
     * @param expectedPackage The package name the provider is expected to belong to.
     * @return true if the provider is trusted, false otherwise.
     */
    fun isProviderTrusted(authority: String, expectedPackage: String): Boolean {
        if (devModeEnabled) return true

        return try {
            val providerInfo = context.packageManager.resolveContentProvider(authority, 0)
                ?: return false
            providerInfo.packageName == expectedPackage
        } catch (e: Exception) {
            false
        }
    }
}
