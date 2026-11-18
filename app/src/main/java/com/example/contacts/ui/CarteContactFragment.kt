package com.example.contacts.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.contacts.R
import com.example.contacts.data.Contact
import com.example.contacts.databinding.FragmentCarteContactBinding
import com.example.contacts.util.ContactValidator
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class CarteContactFragment : Fragment() {

    private var _binding: FragmentCarteContactBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ContactViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentCarteContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val id = requireArguments().getLong(ARG_ID, 0L)
        viewLifecycleOwner.lifecycleScope.launch { show(viewModel.loadContact(id)) }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.title_map)
    }

    private fun show(contact: Contact?) {
        val lat = contact?.latitude
        val lon = contact?.longitude
        when {
            contact == null -> showMessage(R.string.err_introuvable)
            lat == null || lon == null -> showMessage(R.string.map_no_position)
            !ContactValidator.isValidPosition(lat, lon) ->
                showMessage(R.string.map_invalid_position)
            else -> showMap(contact, lat, lon)
        }
    }

    private fun showMessage(res: Int) {
        binding.map.isVisible = false
        binding.tvMessage.setText(res)
        binding.tvMessage.isVisible = true
    }

    private fun showMap(contact: Contact, lat: Double, lon: Double) {
        binding.tvMessage.isVisible = false
        binding.map.isVisible = true
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment ?: return
        mapFragment.getMapAsync { googleMap ->
            val position = LatLng(lat, lon)
            googleMap.addMarker(MarkerOptions().position(position).title("${contact.prenom} ${contact.nom}"))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 14f))
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ID = "contact_id"
        fun newInstance(id: Long) = CarteContactFragment().apply { arguments = bundleOf(ARG_ID to id) }
    }
}
