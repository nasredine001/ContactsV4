package com.example.contacts.ui

import android.app.Application
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.contacts.data.Contact
import com.example.contacts.data.ContactDao
import com.example.contacts.data.ContactDbHelper
import com.example.contacts.util.PhotoUtils
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class OpResult { OK, NOT_FOUND, FAILED }

/** ViewModel partagé : tous les accès SQLite passent par Dispatchers.IO. */
class ContactViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = ContactDao(ContactDbHelper.getInstance(app))

    private var all: List<Contact> = emptyList()
    private var query: String = ""

    private val _contacts = MutableLiveData<List<Contact>>(emptyList())
    /** Liste déjà filtrée, triée par nom puis prénom (tri fait par SQLite). */
    val contacts: LiveData<List<Contact>> = _contacts

    init { refresh() }

    /** Recharge la liste depuis la base. */
    fun refresh() {
        viewModelScope.launch { reload() }
    }

    private suspend fun reload() {
        try {
            all = withContext(Dispatchers.IO) { dao.getAll() }
            publish()
        } catch (e: SQLiteException) {
            Log.e(TAG, "Lecture de la liste impossible", e)
        }
    }

    /**
     * Filtrage en MÉMOIRE (choix justifié) : la liste est chargée de toute façon, le LIKE de SQLite
     * ne gère la casse que pour l’ASCII (« é » ≠ « É »), alors que lowercase() est correct en Unicode,
     * et il n’y a aucune requête supplémentaire à chaque frappe.
     */
    fun setQuery(text: String) {
        if (text == query) return
        query = text
        publish()
    }

    private fun publish() {
        val q = query.trim().lowercase()
        _contacts.value = if (q.isEmpty()) all else all.filter {
            it.nom.lowercase().contains(q) ||
                it.prenom.lowercase().contains(q) ||
                it.telephone.lowercase().contains(q)
        }
    }

    suspend fun loadContact(id: Long): Contact? = withContext(Dispatchers.IO) {
        try { dao.getById(id) } catch (e: SQLiteException) { null }
    }

    /** async + viewModelScope : l’écriture se termine même si le fragment est détruit (rotation). */
    fun save(contact: Contact): Deferred<OpResult> = viewModelScope.async {
        val result = withContext(Dispatchers.IO) { doSave(contact) }
        if (result == OpResult.OK) reload()
        result
    }

    fun delete(id: Long): Deferred<OpResult> = viewModelScope.async {
        val result = withContext(Dispatchers.IO) {
            try {
                val previous = dao.getById(id)
                if (dao.delete(id) > 0) {
                    PhotoUtils.deletePhoto(previous?.photoUri)
                    OpResult.OK
                } else OpResult.NOT_FOUND
            } catch (e: SQLiteException) {
                OpResult.FAILED
            }
        }
        if (result == OpResult.OK) reload()
        result
    }

    private fun doSave(contact: Contact): OpResult {
        return try {
            if (contact.id == 0L) {
                if (dao.insert(contact) > 0) OpResult.OK else OpResult.FAILED
            } else {
                val previous = dao.getById(contact.id) ?: return OpResult.NOT_FOUND
                if (dao.update(contact) > 0) {
                    if (previous.photoUri != null && previous.photoUri != contact.photoUri) {
                        PhotoUtils.deletePhoto(previous.photoUri)
                    }
                    OpResult.OK
                } else OpResult.FAILED
            }
        } catch (e: SQLiteException) {
            OpResult.FAILED
        }
    }

    private companion object { const val TAG = "ContactViewModel" }
}
