package com.example.contacts.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Tests instrumentés : vraie base SQLite Android, une base neuve par test. */
@RunWith(AndroidJUnit4::class)
class ContactDaoTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var helper: ContactDbHelper? = null
    private val dbName: String = "test_contacts_${System.nanoTime()}.db"

    private fun newHelper(): ContactDbHelper {
        context.deleteDatabase(dbName)
        return ContactDbHelper(context, dbName).also { helper = it }
    }

    @After
    fun tearDown() {
        helper?.close()
        helper = null
        context.deleteDatabase(dbName)
    }

    @Test
    fun onCreate_insertsDemoContactsOnlyOnce() {
        val first = newHelper()
        val dao = ContactDao(first)
        val all = dao.getAll()

        assertEquals(5, all.size)
        assertEquals(2, all.count { it.latitude == null && it.longitude == null })
        assertEquals(
            listOf("Ben Salah", "Gharbi", "L'Ariana", "O’Neil", "Trabelsi"),
            all.map { it.nom }
        )

        // Une réouverture ne doit pas réinsérer les contacts de démonstration.
        first.close()
        val reopened = ContactDbHelper(context, dbName)
        helper = reopened
        assertEquals(5, ContactDao(reopened).getAll().size)
    }

    @Test
    fun insertUpdateDelete_returnExpectedValues_andPersist() {
        val dao = ContactDao(newHelper())
        val id = dao.insert(Contact(nom="Exemple", prenom="Test", telephone="+216 00 000 000"))
        assertTrue(id > 0)

        val saved = dao.getById(id)
        assertEquals("Exemple", saved?.nom)
        assertEquals("+216 00 000 000", saved?.telephone)

        val updated = saved!!.copy(nom = "Modifié", telephone = "11 222 333", latitude = 34.0, longitude = 10.0)
        assertEquals(1, dao.update(updated))
        dao.getById(id)!!.let {
            assertEquals("Modifié", it.nom)
            assertEquals("11 222 333", it.telephone)
            assertEquals(34.0, it.latitude!!, 0.0001)
        }

        assertEquals(1, dao.delete(id))
        assertNull(dao.getById(id))
        assertEquals(0, dao.delete(id))
    }

    @Test
    fun apostrophesAndLikeWildcards_areHandledSafely() {
        val dao = ContactDao(newHelper())
        val apostropheId = dao.insert(Contact(nom = "O'Neil", prenom = "Test", telephone = "111"))
        dao.insert(Contact(nom = "100%", prenom = "Test", telephone = "222"))
        dao.insert(Contact(nom = "A_B", prenom = "Test", telephone = "333"))

        assertTrue(apostropheId > 0)
        assertEquals(1, dao.search("O'Neil").size)
        // Les jokers saisis sont échappés : '%' et '_' ne sont pas des wildcards.
        assertEquals(1, dao.search("%").size)
        assertEquals(1, dao.search("_").size)
    }

    @Test
    fun optionalColumns_areStoredAsNull() {
        val dao = ContactDao(newHelper())
        val id = dao.insert(Contact(nom = "Sans", prenom = "Details", telephone = "123"))

        val contact = dao.getById(id)!!
        assertNull(contact.email)
        assertNull(contact.photoUri)
        assertNull(contact.latitude)
        assertNull(contact.longitude)
    }

    @Test
    fun missingId_updateDeleteAndGetById_areSafe() {
        val dao = ContactDao(newHelper())
        val unknownId = 999_999L

        assertNull(dao.getById(unknownId))
        assertEquals(0, dao.delete(unknownId))
        assertEquals(0, dao.update(Contact(unknownId, "A", "B", "123")))
    }

    @Test
    fun getAll_isSortedByNameThenPrenom_caseInsensitive() {
        val helper = newHelper()
        helper.writableDatabase.delete(ContactContract.TABLE, null, null)
        val dao = ContactDao(helper)

        dao.insert(Contact(nom = "zulia", prenom = "B", telephone = "1"))
        dao.insert(Contact(nom = "alpha", prenom = "Z", telephone = "2"))
        dao.insert(Contact(nom = "Alpha", prenom = "A", telephone = "3"))
        dao.insert(Contact(nom = "beta", prenom = "A", telephone = "4"))

        assertEquals(
            listOf("Alpha", "alpha", "beta", "zulia"),
            dao.getAll().map { it.nom }
        )
    }
}
