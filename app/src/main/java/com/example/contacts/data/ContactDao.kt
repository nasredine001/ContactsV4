package com.example.contacts.data

import android.content.ContentValues
import android.database.Cursor
import com.example.contacts.data.ContactContract as C

/** Contact -> ContentValues (NULL explicite pour les champs optionnels absents). */
internal fun Contact.toContentValues(): ContentValues = ContentValues().apply {
    put(C.COL_NOM, nom)
    put(C.COL_PRENOM, prenom)
    put(C.COL_TELEPHONE, telephone)
    if (email != null) put(C.COL_EMAIL, email) else putNull(C.COL_EMAIL)
    if (photoUri != null) put(C.COL_PHOTO_URI, photoUri) else putNull(C.COL_PHOTO_URI)
    if (latitude != null) put(C.COL_LATITUDE, latitude) else putNull(C.COL_LATITUDE)
    if (longitude != null) put(C.COL_LONGITUDE, longitude) else putNull(C.COL_LONGITUDE)
}

/**
 * Seule classe contenant du SQL. Toutes les valeurs variables passent par des arguments « ? ».
 * À appeler depuis un thread secondaire (Dispatchers.IO).
 */
class ContactDao(private val helper: ContactDbHelper) {

    /** @return l’_id généré, ou -1 en cas d’échec (contrainte non respectée...). */
    fun insert(contact: Contact): Long =
        helper.writableDatabase.insert(C.TABLE, null, contact.toContentValues())

    /** @return le nombre de lignes modifiées (0 = contact introuvable). */
    fun update(contact: Contact): Int =
        helper.writableDatabase.update(
            C.TABLE, contact.toContentValues(), "${C.COL_ID} = ?", arrayOf(contact.id.toString())
        )

    /** @return le nombre de lignes supprimées (0 = contact introuvable). */
    fun delete(id: Long): Int =
        helper.writableDatabase.delete(C.TABLE, "${C.COL_ID} = ?", arrayOf(id.toString()))

    fun getAll(): List<Contact> =
        helper.readableDatabase
            .query(C.TABLE, null, null, null, null, null, C.ORDER_BY)
            .use { it.readAll() }

    fun getById(id: Long): Contact? =
        helper.readableDatabase
            .query(C.TABLE, null, "${C.COL_ID} = ?", arrayOf(id.toString()), null, null, null, "1")
            .use { if (it.moveToFirst()) it.toContact() else null }

    /** Recherche SQL (LIKE) sur nom, prénom et téléphone. Les jokers % _ saisis sont échappés. */
    fun search(texte: String): List<Contact> {
        val escaped = texte.trim()
            .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        val like = "%$escaped%"
        val selection = "${C.COL_NOM} LIKE ? ESCAPE '\\' OR " +
            "${C.COL_PRENOM} LIKE ? ESCAPE '\\' OR " +
            "${C.COL_TELEPHONE} LIKE ? ESCAPE '\\'"
        return helper.readableDatabase
            .query(C.TABLE, null, selection, arrayOf(like, like, like), null, null, C.ORDER_BY)
            .use { it.readAll() }
    }

    // ---------- lecture du Cursor ----------
    private fun Cursor.readAll(): List<Contact> {
        val list = ArrayList<Contact>(count)
        while (moveToNext()) list.add(toContact())
        return list
    }

    private fun Cursor.string(col: String): String = getString(getColumnIndexOrThrow(col))

    private fun Cursor.stringOrNull(col: String): String? {
        val i = getColumnIndexOrThrow(col)
        return if (isNull(i)) null else getString(i)
    }

    private fun Cursor.doubleOrNull(col: String): Double? {
        val i = getColumnIndexOrThrow(col)
        return if (isNull(i)) null else getDouble(i)
    }

    private fun Cursor.toContact() = Contact(
        id = getLong(getColumnIndexOrThrow(C.COL_ID)),
        nom = string(C.COL_NOM),
        prenom = string(C.COL_PRENOM),
        telephone = string(C.COL_TELEPHONE),
        email = stringOrNull(C.COL_EMAIL),
        photoUri = stringOrNull(C.COL_PHOTO_URI),
        latitude = doubleOrNull(C.COL_LATITUDE),
        longitude = doubleOrNull(C.COL_LONGITUDE)
    )
}
