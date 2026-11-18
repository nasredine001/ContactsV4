package com.example.contacts

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.example.contacts.databinding.ActivityMainBinding
import com.example.contacts.ui.ListeContactsFragment
import com.example.contacts.ui.ParametresFragment

/** Activité unique : héberge les fragments et la barre d’application (accès aux Paramètres). */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Écrit les valeurs par défaut de preferences.xml (sans écraser les choix existants).
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        // Pas de fragment ajouté deux fois après rotation : le FragmentManager restaure l’existant.
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ListeContactsFragment())
                .commit()
        }
        supportFragmentManager.addOnBackStackChangedListener { updateUpButton() }
        updateUpButton()
    }

    private fun updateUpButton() {
        supportActionBar?.setDisplayHomeAsUpEnabled(supportFragmentManager.backStackEntryCount > 0)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_settings -> { openSettings(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun openSettings() {
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) is ParametresFragment) return
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ParametresFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
