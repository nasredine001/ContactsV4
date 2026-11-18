package com.example.contacts.util

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.view.ContextThemeWrapper
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.contacts.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesManagerTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private lateinit var context: Context
    private lateinit var prefs: android.content.SharedPreferences

    /** On force le thème clair pour que la couleur testée ne soit pas éclaircie par le mode nuit. */
    @Before
    fun setUp() {
        val base = ContextThemeWrapper(appContext, R.style.Theme_ContactsV4)
        val config = Configuration(base.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
        context = base.createConfigurationContext(config)
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().clear().commit()
    }

    @After
    fun tearDown() {
        prefs.edit().clear().commit()
    }

    @Test
    fun defaults_areApplied_whenNothingIsStored() {
        val values = PreferencesManager(context).load()

        assertEquals(16f, values.textSizeSp)
        assertFalse(values.callEnabled)
        assertEquals(Color.parseColor("#000000"), values.textColor)
    }

    @Test
    fun savedValues_areReadConverted_andSurviveANewManager() {
        prefs.edit()
            .putString(PreferencesManager.KEY_SIZE, "20")
            .putString(PreferencesManager.KEY_COLOR, "#1565C0")
            .putBoolean(PreferencesManager.KEY_CALL, true)
            .commit()

        val values = PreferencesManager(context).load()
        assertEquals(20f, values.textSizeSp)
        assertEquals(Color.parseColor("#1565C0"), values.textColor)
        assertTrue(values.callEnabled)

        // Une nouvelle lecture simule le retour depuis l’écran Paramètres / le relance de l’app.
        val reread = PreferencesManager(context).load()
        assertEquals(values, reread)
    }

    @Test
    fun invalidStoredValues_fallBackToDefaults() {
        prefs.edit()
            .putString(PreferencesManager.KEY_SIZE, "pas-un-nombre")
            .putString(PreferencesManager.KEY_COLOR, "#ZZZZZZ")
            .commit()

        val values = PreferencesManager(context).load()
        assertEquals(16f, values.textSizeSp)
        assertEquals(Color.parseColor("#000000"), values.textColor)
    }
}
