package com.example.contacts.ui

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.contacts.data.Contact
import com.example.contacts.databinding.ItemContactBinding
import com.example.contacts.util.DisplayPrefs
import com.example.contacts.util.PhotoUtils

/** L’adapter reçoit des préférences déjà converties (sp, couleur Int, booléen). Contact identifié par son _id. */
class ContactAdapter(
    private var prefs: DisplayPrefs,
    private val onContactClick: (Long) -> Unit,
    private val onCallClick: (String) -> Unit
) : ListAdapter<Contact, ContactAdapter.VH>(Diff) {

    fun updatePrefs(newPrefs: DisplayPrefs) {
        if (newPrefs == prefs) return
        prefs = newPrefs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemContactBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(c: Contact) {
            b.tvNom.text = "${c.nom} ${c.prenom}"
            b.tvTelephone.text = c.telephone
            b.tvNom.setTextSize(TypedValue.COMPLEX_UNIT_SP, prefs.textSizeSp)
            b.tvTelephone.setTextSize(TypedValue.COMPLEX_UNIT_SP, prefs.textSizeSp)
            b.tvNom.setTextColor(prefs.textColor)
            PhotoUtils.display(b.ivPhoto, c.photoUri, 12)

            b.root.setOnClickListener { onContactClick(c.id) }
            b.btnCall.isVisible = prefs.callEnabled          // GONE si désactivé
            // Le bouton consomme son propre clic : la fiche détaillée ne s’ouvre pas.
            b.btnCall.setOnClickListener { onCallClick(c.telephone) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(a: Contact, b: Contact) = a.id == b.id
        override fun areContentsTheSame(a: Contact, b: Contact) = a == b
    }
}
