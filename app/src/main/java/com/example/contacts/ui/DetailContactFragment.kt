package com.example.contacts.ui

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.contacts.R
import com.example.contacts.databinding.FragmentDetailContactBinding
import com.example.contacts.util.ContactField
import com.example.contacts.util.ContactValidator
import com.example.contacts.util.IntentUtils
import com.example.contacts.util.PhotoUtils
import com.example.contacts.util.PreferencesManager
import com.example.contacts.util.ValidationError
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Création (id = 0), consultation, modification et suppression d’un contact. */
class DetailContactFragment : Fragment() {

    private var _binding: FragmentDetailContactBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ContactViewModel by activityViewModels()

    private var contactId = 0L
    private var photoPath: String? = null       // photo actuellement affichée (copie interne)
    private var originalPhoto: String? = null   // photo déjà enregistrée en base
    private var cameraFilePath: String? = null  // fichier temporaire de la caméra
    private var loaded = false                  // champs déjà remplis depuis la base ?
    private var confirmDialog: AlertDialog? = null

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) importPhoto(uri, null)      // annulation : rien à faire
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val file = cameraFilePath?.let { File(it) }
            cameraFilePath = null
            if (success && file != null && file.exists()) importPhoto(Uri.fromFile(file), file)
            else file?.delete()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactId = requireArguments().getLong(ARG_ID, 0L)
        savedInstanceState?.let {
            photoPath = it.getString(S_PHOTO)
            originalPhoto = it.getString(S_ORIGINAL)
            cameraFilePath = it.getString(S_CAMERA)
            loaded = it.getBoolean(S_LOADED)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(S_PHOTO, photoPath)
        outState.putString(S_ORIGINAL, originalPhoto)
        outState.putString(S_CAMERA, cameraFilePath)
        outState.putBoolean(S_LOADED, loaded)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentDetailContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val isNew = contactId == 0L
        binding.btnDelete.isVisible = !isNew
        binding.btnMap.isVisible = !isNew

        applyPreferences()
        binding.btnGallery.setOnClickListener { launchGallery() }
        binding.btnCamera.setOnClickListener { launchCamera() }
        binding.btnCall.setOnClickListener {
            IntentUtils.dial(requireContext(), binding.etTelephone.text?.toString())
        }
        binding.btnEmail.setOnClickListener {
            IntentUtils.email(requireContext(), binding.etEmail.text?.toString())
        }
        binding.btnMap.setOnClickListener { openMap() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnCancel.setOnClickListener {
            discardPendingPhoto()
            parentFragmentManager.popBackStack()
        }
        binding.btnDelete.setOnClickListener { confirmDelete() }

        // On ne remplit les champs que la 1re fois : la saisie en cours survit à la rotation.
        if (!loaded) {
            if (isNew) loaded = true else loadContact()
        }
        showPhoto()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().setTitle(if (contactId == 0L) R.string.title_new else R.string.title_detail)
    }

    // ---------- préférences ----------
    private fun applyPreferences() {
        val prefs = PreferencesManager(requireContext()).load()
        listOf(binding.etNom, binding.etPrenom, binding.etTelephone).forEach {
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, prefs.textSizeSp)
        }
        binding.etNom.setTextColor(prefs.textColor)
        binding.etPrenom.setTextColor(prefs.textColor)
    }

    // ---------- chargement ----------
    private fun loadContact() {
        viewLifecycleOwner.lifecycleScope.launch {
            val c = viewModel.loadContact(contactId)
            if (c == null) {
                toast(R.string.err_introuvable)
                parentFragmentManager.popBackStack()
                return@launch
            }
            binding.etNom.setText(c.nom)
            binding.etPrenom.setText(c.prenom)
            binding.etTelephone.setText(c.telephone)
            binding.etEmail.setText(c.email.orEmpty())
            binding.etLatitude.setText(c.latitude?.toString().orEmpty())
            binding.etLongitude.setText(c.longitude?.toString().orEmpty())
            photoPath = c.photoUri
            originalPhoto = c.photoUri
            loaded = true
            showPhoto()
        }
    }

    private fun showPhoto() = PhotoUtils.display(binding.ivPhoto, photoPath, 28)

    // ---------- photo ----------
    private fun launchGallery() {
        try {
            pickImage.launch("image/*")
        } catch (e: ActivityNotFoundException) {
            toast(R.string.err_no_gallery)
        }
    }

    private fun launchCamera() {
        try {
            val ctx = requireContext()
            val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
            val file = File.createTempFile("capture_", ".jpg", dir)
            cameraFilePath = file.absolutePath
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
            takePicture.launch(uri)
        } catch (e: ActivityNotFoundException) {
            toast(R.string.err_no_camera)
        } catch (e: IOException) {
            toast(R.string.err_photo)
        }
    }

    private fun importPhoto(uri: Uri, temp: File?) {
        val app = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val path = withContext(Dispatchers.IO) {
                PhotoUtils.importImage(app, uri).also { temp?.delete() }
            }
            if (path == null) {
                toast(R.string.err_photo)
                return@launch
            }
            discardPendingPhoto()
            photoPath = path
            showPhoto()
        }
    }

    /** Supprime une photo choisie mais pas encore enregistrée (la photo de la base n’est jamais touchée ici). */
    private fun discardPendingPhoto() {
        val p = photoPath
        if (p != null && p != originalPhoto) PhotoUtils.deletePhoto(p)
    }

    // ---------- validation + enregistrement ----------
    private fun collectAndValidate() = binding.run {
        val layouts = mapOf(
            ContactField.NOM to tilNom,
            ContactField.PRENOM to tilPrenom,
            ContactField.TELEPHONE to tilTelephone,
            ContactField.EMAIL to tilEmail,
            ContactField.LATITUDE to tilLatitude,
            ContactField.LONGITUDE to tilLongitude
        )
        layouts.values.forEach { it.error = null }

        val result = ContactValidator.validate(
            id = contactId,
            nom = etNom.text?.toString(),
            prenom = etPrenom.text?.toString(),
            telephone = etTelephone.text?.toString(),
            email = etEmail.text?.toString(),
            latitude = etLatitude.text?.toString(),
            longitude = etLongitude.text?.toString(),
            photoUri = photoPath
        )
        if (!result.isValid) {
            result.errors.forEach { (field, error) ->
                layouts[field]?.error = errorMessage(field, error)
            }
            toast(R.string.err_champs)
            return null
        }
        result.contact
    }

    private fun errorMessage(field: ContactField, error: ValidationError): String = when (error) {
        ValidationError.REQUIRED -> when (field) {
            ContactField.NOM -> getString(R.string.err_nom_requis)
            ContactField.PRENOM -> getString(R.string.err_prenom_requis)
            ContactField.TELEPHONE -> getString(R.string.err_tel_requis)
            else -> getString(R.string.err_champs)
        }
        ValidationError.EMAIL_INVALID -> getString(R.string.err_email_invalide)
        ValidationError.COORD_REQUIRED -> getString(R.string.err_coord_ensemble)
        ValidationError.COORD_NOT_NUMERIC -> getString(R.string.err_coord_non_numerique)
        ValidationError.COORD_OUT_OF_RANGE -> getString(
            R.string.err_coord_hors_limites,
            if (field == ContactField.LATITUDE) 90 else 180
        )
    }

    private fun save() {
        val contact = collectAndValidate() ?: return      // rien n’est écrit en base si invalide
        setBusy(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val result = viewModel.save(contact).await()
            setBusy(false)
            when (result) {
                OpResult.OK -> {
                    toast(R.string.msg_saved)
                    parentFragmentManager.popBackStack()
                }
                OpResult.NOT_FOUND -> toast(R.string.err_introuvable)
                OpResult.FAILED -> toast(R.string.err_enregistrement)
            }
        }
    }

    private fun setBusy(busy: Boolean) {
        binding.btnSave.isEnabled = !busy
        binding.btnDelete.isEnabled = !busy
    }

    // ---------- suppression ----------
    private fun confirmDelete() {
        val name = "${binding.etPrenom.text} ${binding.etNom.text}".trim()
        confirmDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_title)
            .setMessage(getString(R.string.delete_message, name))
            .setPositiveButton(R.string.btn_delete) { _, _ -> delete() }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun delete() {
        setBusy(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val result = viewModel.delete(contactId).await()
            setBusy(false)
            when (result) {
                OpResult.OK -> {
                    toast(R.string.msg_deleted)
                    parentFragmentManager.popBackStack()
                }
                OpResult.NOT_FOUND -> toast(R.string.err_introuvable)
                OpResult.FAILED -> toast(R.string.err_suppression)
            }
        }
    }

    private fun openMap() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, CarteContactFragment.newInstance(contactId))
            .addToBackStack(null)
            .commit()
    }

    private fun toast(@StringRes res: Int) =
        Toast.makeText(requireContext(), res, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        confirmDialog?.dismiss()
        confirmDialog = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ID = "contact_id"
        private const val S_PHOTO = "state_photo"
        private const val S_ORIGINAL = "state_original_photo"
        private const val S_CAMERA = "state_camera_file"
        private const val S_LOADED = "state_loaded"

        fun newInstance(id: Long) = DetailContactFragment().apply { arguments = bundleOf(ARG_ID to id) }
    }
}
