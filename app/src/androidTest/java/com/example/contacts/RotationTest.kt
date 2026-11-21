package com.example.contacts

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.SystemClock
import android.view.View
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Rotation paysage/portrait : liste, saisie en cours, fiche existante et paramètres. */
@RunWith(AndroidJUnit4::class)
class RotationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun resetPreferences() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    @After
    fun resetOrientation() {
        try {
            activityRule.scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        } catch (_: Throwable) {
            // L’activité peut déjà être détruite en fin de test.
        }
    }

    @Test
    fun list_rotation_keepsContactsAndFilter() {
        onView(withText("Ben Salah Ahmed")).check(matches(isDisplayed()))
        onView(withId(R.id.et_filter)).perform(replaceText("sonia"))
        onView(withText("Trabelsi Sonia")).check(matches(isDisplayed()))
        waitUntilGone(withText("Ben Salah Ahmed"))

        rotateToLandscape()
        onView(withText("Trabelsi Sonia")).check(matches(isDisplayed()))
        onView(withId(R.id.et_filter)).check(matches(withText("sonia")))
        waitUntilGone(withText("Ben Salah Ahmed"))
        assertEquals(0, backStackCount())

        rotateToPortrait()
        onView(withText("Trabelsi Sonia")).check(matches(isDisplayed()))
        onView(withId(R.id.et_filter)).check(matches(withText("sonia")))
        assertEquals(0, backStackCount())
    }

    @Test
    fun detail_rotation_keepsInProgressInput_andDoesNotDuplicateFragment() {
        val nom = "Rotation"
        val prenom = "Paysage"
        val telephone = "55 000 111"

        onView(withId(R.id.fab)).perform(click())
        fillField(R.id.et_nom, nom)
        fillField(R.id.et_prenom, prenom)
        fillField(R.id.et_telephone, telephone)

        rotateToLandscape()
        onView(withId(R.id.et_nom)).check(matches(withText(nom)))
        onView(withId(R.id.et_prenom)).check(matches(withText(prenom)))
        onView(withId(R.id.et_telephone)).check(matches(withText(telephone)))
        assertEquals(1, backStackCount())

        rotateToPortrait()
        onView(withId(R.id.et_nom)).check(matches(withText(nom)))
        onView(withId(R.id.et_prenom)).check(matches(withText(prenom)))
        onView(withId(R.id.et_telephone)).check(matches(withText(telephone)))
        assertEquals(1, backStackCount())

        // Annulation : rien ne doit être écrit en base.
        onView(withId(R.id.btn_cancel)).perform(scrollTo(), click())
        onView(withText("Ben Salah Ahmed")).check(matches(isDisplayed()))
        assertEquals(0, backStackCount())
    }

    @Test
    fun existingContact_rotation_keepsLoadedValues() {
        onView(withText("Gharbi Karim")).perform(click())
        onView(withId(R.id.et_nom)).check(matches(withText("Gharbi")))
        onView(withId(R.id.et_prenom)).check(matches(withText("Karim")))

        rotateToLandscape()
        onView(withId(R.id.et_nom)).check(matches(withText("Gharbi")))
        onView(withId(R.id.et_prenom)).check(matches(withText("Karim")))
        onView(withId(R.id.et_telephone)).check(matches(withText("98 765 432")))
        assertEquals(1, backStackCount())

        rotateToPortrait()
        onView(withId(R.id.et_nom)).check(matches(withText("Gharbi")))
        onView(withId(R.id.et_telephone)).check(matches(withText("98 765 432")))

        pressBack()
        onView(withText("Ben Salah Ahmed")).check(matches(isDisplayed()))
        assertEquals(0, backStackCount())
    }

    @Test
    fun settings_rotation_keepsTheScreen_andBackStack() {
        onView(withId(R.id.action_settings)).perform(click())
        onView(withText(R.string.pref_taille_titre)).check(matches(isDisplayed()))
        assertEquals(1, backStackCount())

        rotateToLandscape()
        onView(withText(R.string.pref_taille_titre)).check(matches(isDisplayed()))
        assertEquals(1, backStackCount())

        rotateToPortrait()
        onView(withText(R.string.pref_taille_titre)).check(matches(isDisplayed()))
        assertEquals(1, backStackCount())

        pressBack()
        onView(withText("Ben Salah Ahmed")).check(matches(isDisplayed()))
        assertEquals(0, backStackCount())
    }

    private fun fillField(id: Int, value: String) {
        onView(withId(id)).perform(replaceText(value))
    }

    private fun backStackCount(): Int {
        var count = -1
        activityRule.scenario.onActivity { count = it.supportFragmentManager.backStackEntryCount }
        return count
    }

    private fun rotateToLandscape() = rotateTo(
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        Configuration.ORIENTATION_LANDSCAPE
    )

    private fun rotateToPortrait() = rotateTo(
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        Configuration.ORIENTATION_PORTRAIT
    )

    private fun rotateTo(requested: Int, expected: Int) {
        activityRule.scenario.onActivity { it.requestedOrientation = requested }
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (SystemClock.uptimeMillis() < deadline) {
            try {
                var current = Configuration.ORIENTATION_UNDEFINED
                activityRule.scenario.onActivity {
                    current = it.resources.configuration.orientation
                }
                if (current == expected) {
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                    return
                }
            } catch (_: Throwable) {
                // Recréation en cours : on réessaie.
            }
            SystemClock.sleep(100)
        }
        throw AssertionError("L’orientation n’a pas changé vers $expected")
    }

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
}
