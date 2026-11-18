package com.example.contacts.data

/** Modèle d’un contact. id = 0 tant que le contact n’est pas enregistré en base. */
data class Contact(
    val id: Long = 0,
    val nom: String,
    val prenom: String,
    val telephone: String,
    val email: String? = null,
    val photoUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
