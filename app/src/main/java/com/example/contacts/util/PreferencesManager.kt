package com.example.contacts.util

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.preference.PreferenceManager
import com.google.android.material.color.MaterialColors

/** Valeurs déjà converties, prêtes à être appliquées aux vues. */
data class DisplayPrefs(
    val textSizeSp: Float,
    val textColor: Int,
    val callEnabled: Boolean
)

/** Lecture centralisée des SharedPreferences. À instancier avec un Context « thémé » (fragment/activité). */
class PreferencesManager(private val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun load(): DisplayPrefs = DisplayPrefs(
        textSizeSp = prefs.getString(KEY_SIZE, DEFAULT_SIZE)?.toFloatOrNull() ?: DEFAULT_SIZE.toFloat(),
        textColor = readableColor(parseColor(prefs.getString(KEY_COLOR, DEFAULT_COLOR))),
        callEnabled = prefs.getBoolean(KEY_CALL, false)
    )

    private fun parseColor(hex: String?): Int =
        try { Color.parseColor(hex ?: DEFAULT_COLOR) } catch (e: IllegalArgumentException) { Color.BLACK }

    /** Garantit la lisibilité : « Noir » suit la couleur de texte du thème ; les autres sont éclaircies en thème sombre. */
    private fun readableColor(raw: Int): Int {
        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        if (raw == Color.BLACK) {
            return MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
        }
        return if (night) ColorUtils.blendARGB(raw, Color.WHITE, 0.55f) else raw
    }

    companion object {
        const val KEY_SIZE = "taille_police"
        const val KEY_COLOR = "couleur_preferee"
        const val KEY_CALL = "appel_depuis_liste"
        private const val DEFAULT_SIZE = "16"
        private const val DEFAULT_COLOR = "#000000"
    }
}
