package com.example.contacts

import android.content.Context
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.AllOf.allOf
import org.hamcrest.core.IsNot.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/** Parcours utilisateur complet : liste, filtre, CRUD et préférences appliquées immédiatement. */
@RunWith(AndroidJUnit4::class)
class ContactFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun resetPreferences() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    @Test
    fun demoContactsAreShown_andFilterHidesOtherContacts() {
        // Le chargement SQLite se fait dans une coroutine ; Espresso attend la vue.
        onView(withText("Ben Salah Ahmed")).check(matches(isDisplayed()))

        onView(withId(R.id.et_filter)).perform(replaceText("sonia"))
        onView(withText("Trabelsi Sonia")).check(matches(isDisplayed()))
        waitUntilGone(withText("Ben Salah Ahmed"))

        onView(withId(R.id.et_filter)).perform(replaceText("'"))
        onView(withText("L'Ariana Yassine")).check(matches(isDisplayed()))
    }

    @Test
    fun createEditDeleteFlow_updatesTheList() {
        val suffix = System.currentTimeMillis().toString().takeLast(6)
        val nom = "UI$suffix"
        val prenom = "O'Neil"
        val fullName = "$nom $prenom"

        onView(withId(R.id.fab)).perform(click())
        fillField(R.id.et_nom, nom)
        fillField(R.id.et_prenom, prenom)
        fillField(R.id.et_telephone, "123 456 789")
        fillField(R.id.et_email, "ui@example.com")
        closeSoftKeyboard()
        onView(withId(R.id.btn_save)).perform(click())

        onView(withText(fullName)).check(matches(isDisplayed()))

        // Modification (on attend le chargement depuis la base avant d’écrire dans les champs)
        onView(withText(fullName)).perform(click())
        onView(withId(R.id.et_nom)).check(matches(withText(nom)))
        fillField(R.id.et_nom, "${nom}Mod")
        closeSoftKeyboard()
        onView(withId(R.id.btn_save)).perform(click())

        val modifiedName = "${nom}Mod $prenom"
        onView(withText(modifiedName)).check(matches(isDisplayed()))

        // Suppression avec confirmation
        onView(withText(modifiedName)).perform(click())
        onView(withId(R.id.et_nom)).check(matches(withText("${nom}Mod")))
        onView(withId(R.id.btn_delete)).perform(scrollTo(), click())
        onView(allOf(withText(R.string.btn_delete), isDisplayed())).perform(click())

        waitUntilGone(withText(modifiedName))
    }

    @Test
    fun preferencesAreAppliedImmediately_andKeptInSharedPreferences() {
        openSettings()

        onView(withText(R.string.pref_taille_titre)).perform(click())
        onView(withText("Grande (20 sp)")).perform(click())
        onView(withText(R.string.pref_appel_titre)).perform(click())
        pressBack()

        onView(withId(R.id.recycler)).check(
            matches(hasDescendant(allOf(withId(R.id.tv_nom), withTextSizeSp(20f))))
        )
        onView(withId(R.id.recycler)).check(
            matches(hasDescendant(allOf(withId(R.id.btn_call), isDisplayed())))
        )

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        assertEqualsString("20", prefs.getString("taille_police", null))
        org.junit.Assert.assertTrue(prefs.getBoolean("appel_depuis_liste", false))

        // Désactivation : le bouton Appeler disparaît sans redémarrage.
        openSettings()
        onView(withText(R.string.pref_appel_titre)).perform(click())
        pressBack()
        onView(withId(R.id.recycler)).check(
            matches(not(hasDescendant(allOf(withId(R.id.btn_call), isDisplayed()))))
        )
    }

    /** Espresso ne retente pas « doesNotExist » : on attend la fin du refresh asynchrone. */
    private fun waitUntilGone(matcher: Matcher<View>, timeoutMs: Long = 8_000) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        var lastError: Throwable? = null
        while (SystemClock.uptimeMillis() < deadline) {
            try {
                onView(matcher).check(doesNotExist())
                return
            } catch (e: Throwable) {
                lastError = e
                SystemClock.sleep(100)
            }
        }
        throw lastError ?: AssertionError("View never disappeared: $matcher")
    }

    private fun fillField(id: Int, value: String) {
        onView(withId(id)).perform(replaceText(value))
    }

    private fun openSettings() {
        onView(withId(R.id.action_settings)).perform(click())
    }

    private fun assertEqualsString(expected: String?, actual: String?) {
        org.junit.Assert.assertEquals(expected, actual)
    }

    @Suppress("DEPRECATION")
    private fun withTextSizeSp(expectedSp: Float): Matcher<View> = object : TypeSafeMatcher<View>() {
        override fun matchesSafely(item: View): Boolean {
            if (item !is TextView) return false
            val actualSp = item.textSize / item.resources.displayMetrics.scaledDensity
            return abs(actualSp - expectedSp) < 0.6f
        }

        override fun describeTo(description: Description) {
            description.appendText("TextView with text size ${expectedSp}sp")
        }
    }
}
