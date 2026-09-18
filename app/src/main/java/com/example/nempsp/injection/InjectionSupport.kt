package com.example.nempsp.injection

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Aide à l'activation du service d'accessibilité.
 *
 * Android n'autorise aucune application à activer un service d'accessibilité elle-même :
 * l'utilisateur doit le faire dans les réglages. On vérifie donc l'état réel et on ouvre la
 * bonne page.
 */
object InjectionSupport {

    /** Le service figure-t-il dans la liste des services d'accessibilité activés ? */
    fun isServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, NemPspInjectionService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    /** L'accessibilité est-elle activée globalement sur l'appareil ? */
    fun isAccessibilityGloballyEnabled(context: Context): Boolean =
        Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1

    /**
     * Ouvre les réglages d'accessibilité, en essayant d'arriver directement sur la fiche du
     * service NEMPSP (raccourci non garanti selon les appareils, d'où le repli).
     */
    fun openAccessibilitySettings(context: Context) {
        val component = ComponentName(context, NemPspInjectionService::class.java).flattenToString()
        val direct = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            putExtra(":settings:fragment_args_key", component)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val generic = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launched = runCatching { context.startActivity(direct) }.isSuccess
        if (!launched) runCatching { context.startActivity(generic) }
    }
}
