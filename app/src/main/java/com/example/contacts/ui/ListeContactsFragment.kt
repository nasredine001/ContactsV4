package com.example.contacts.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contacts.R
import com.example.contacts.databinding.FragmentListeContactsBinding
import com.example.contacts.util.IntentUtils
import com.example.contacts.util.PreferencesManager

class ListeContactsFragment : Fragment() {

    private var _binding: FragmentListeContactsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ContactViewModel by activityViewModels()
    private lateinit var adapter: ContactAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentListeContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ContactAdapter(
            prefs = PreferencesManager(requireContext()).load(),
            onContactClick = { id -> openDetail(id) },
            onCallClick = { phone -> IntentUtils.dial(requireContext(), phone) }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        viewModel.contacts.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmpty.isVisible = list.isEmpty()
        }
        binding.etFilter.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }
        binding.fab.setOnClickListener { openDetail(0L) }   // 0 = mode création
    }

    override fun onStart() {
        super.onStart()
        viewModel.refresh()   // Read : rechargé depuis la base à chaque affichage
    }

    override fun onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.title_list)
        // Application immédiate des préférences au retour de l’écran Paramètres.
        adapter.updatePrefs(PreferencesManager(requireContext()).load())
    }

    private fun openDetail(id: Long) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, DetailContactFragment.newInstance(id))
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        binding.recycler.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
