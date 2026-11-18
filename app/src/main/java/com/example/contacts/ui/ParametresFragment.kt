package com.example.contacts.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.contacts.R

class ParametresFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.title_settings)
    }
}
