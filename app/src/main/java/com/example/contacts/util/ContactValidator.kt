package com.example.contacts.util

import com.example.contacts.data.Contact
import kotlin.math.abs

/** Champ concerné par une erreur de validation (permet à la vue d’afficher le bon message). */
enum class ContactField { NOM, PRENOM, TELEPHONE, EMAIL, LATITUDE, LONGITUDE }

enum class ValidationError {
    REQUIRED, EMAIL_INVALID, COORD_REQUIRED, COORD_NOT_NUMERIC, COORD_OUT_OF_RANGE
}

data class ContactValidation(
    val contact: Contact?,
    val errors: Map<ContactField, ValidationError> = emptyMap()
) {
    val isValid: Boolean get() = contact != null
}

/**
 * Validation pure (aucune dépendance Android) : testable en JVM et partagée par la fiche détail
 * et la carte. Les chaînes sont normalisées ici, la vue ne fait que mapper les erreurs en textes.
 */
object ContactValidator {

    fun validate(
        id: Long,
        nom: String?,
        prenom: String?,
        telephone: String?,
        email: String?,
        latitude: String? = null,
        longitude: String? = null,
        photoUri: String? = null
    ): ContactValidation {
        val errors = LinkedHashMap<ContactField, ValidationError>()

        val cleanNom = nom.orEmpty().trim()
        val cleanPrenom = prenom.orEmpty().trim()
        val cleanTel = telephone.orEmpty().trim()
        val cleanEmail = email.orEmpty().trim().ifEmpty { null }

        if (cleanNom.isEmpty()) errors[ContactField.NOM] = ValidationError.REQUIRED
        if (cleanPrenom.isEmpty()) errors[ContactField.PRENOM] = ValidationError.REQUIRED
        if (cleanTel.isEmpty()) errors[ContactField.TELEPHONE] = ValidationError.REQUIRED
        if (cleanEmail != null && !EMAIL_REGEX.matches(cleanEmail)) {
            errors[ContactField.EMAIL] = ValidationError.EMAIL_INVALID
        }

        val latText = latitude.orEmpty().trim()
        val lonText = longitude.orEmpty().trim()
        val hasCoordinates = latText.isNotEmpty() || lonText.isNotEmpty()
        val lat = if (hasCoordinates) parseCoordinate(latText, LAT_LIMIT, ContactField.LATITUDE, errors) else null
        val lon = if (hasCoordinates) parseCoordinate(lonText, LON_LIMIT, ContactField.LONGITUDE, errors) else null

        if (errors.isNotEmpty()) return ContactValidation(null, errors)
        return ContactValidation(
            Contact(id, cleanNom, cleanPrenom, cleanTel, cleanEmail, photoUri, lat, lon)
        )
    }

    /** Coordonnées complètes, numériques et dans les limites (-90..90 / -180..180). */
    fun isValidPosition(latitude: Double?, longitude: Double?): Boolean =
        latitude != null && longitude != null &&
            latitude.isFinite() && longitude.isFinite() &&
            abs(latitude) <= LAT_LIMIT && abs(longitude) <= LON_LIMIT

    private fun parseCoordinate(
        text: String,
        limit: Double,
        field: ContactField,
        errors: MutableMap<ContactField, ValidationError>
    ): Double? {
        if (text.isEmpty()) {
            errors[field] = ValidationError.COORD_REQUIRED
            return null
        }
        val value = text.replace(',', '.').toDoubleOrNull()
        if (value == null || !value.isFinite()) {
            errors[field] = ValidationError.COORD_NOT_NUMERIC
            return null
        }
        if (abs(value) > limit) {
            errors[field] = ValidationError.COORD_OUT_OF_RANGE
            return null
        }
        return value
    }

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private const val LAT_LIMIT = 90.0
    private const val LON_LIMIT = 180.0
}
