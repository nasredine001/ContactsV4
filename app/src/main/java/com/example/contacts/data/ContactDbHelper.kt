package com.example.contacts.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.contacts.data.ContactContract as C

/**
 * Helper SQLite (singleton, construit avec applicationContext pour ne pas retenir une Activity).
 * onCreate n’est exécuté que si le fichier contacts.db n’existe pas encore :
 * les contacts de démonstration ne sont donc insérés qu’une seule fois.
 */
class ContactDbHelper internal constructor(context: Context, databaseName: String = DATABASE_NAME) :
    SQLiteOpenHelper(context.applicationContext, databaseName, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(C.CREATE_TABLE)
        // onCreate s’exécute déjà dans une transaction : insertion directe.
        demoContacts().forEach { db.insert(C.TABLE, null, it.toContentValues()) }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 : stratégie simple (recréation). À remplacer par des ALTER TABLE
        // dès qu’il faut conserver les données (ex. bonus : colonne favori en v2).
        db.execSQL(C.DROP_TABLE)
        onCreate(db)
    }

    private fun demoContacts() = listOf(
        Contact(nom = "Ben Salah", prenom = "Ahmed", telephone = "+216 20 123 456",
            email = "ahmed.bensalah@example.com", latitude = 34.7406, longitude = 10.7603),
        Contact(nom = "Trabelsi", prenom = "Sonia", telephone = "+216 22 456 789",
            email = "sonia.trabelsi@example.com", latitude = 36.8065, longitude = 10.1815),
        Contact(nom = "Gharbi", prenom = "Karim", telephone = "98 765 432",
            email = "karim.gharbi@example.com", latitude = 35.8256, longitude = 10.6084),
        // Sans coordonnées GPS (et avec une apostrophe : test d’injection SQL)
        Contact(nom = "O’Neil", prenom = "Sarah", telephone = "+216 55 111 222",
            email = "sarah.oneil@example.com"),
        Contact(nom = "L'Ariana", prenom = "Yassine", telephone = "71 000 111")
    )

    companion object {
        const val DATABASE_NAME = "contacts.db"
        const val DATABASE_VERSION = 1

        @Volatile
        private var instance: ContactDbHelper? = null

        fun getInstance(context: Context): ContactDbHelper =
            instance ?: synchronized(this) {
                instance ?: ContactDbHelper(context.applicationContext).also { instance = it }
            }
    }
}
