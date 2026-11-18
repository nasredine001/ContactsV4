package com.example.contacts.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactValidatorTest {

    @Test
    fun validContact_isAccepted_andValuesAreTrimmed() {
        val result = ContactValidator.validate(
            id = 0,
            nom = "  O’Neil  ",
            prenom = " Sarah ",
            telephone = " +216 55 111 222 ",
            email = "  sarah@example.com "
        )

        assertTrue(result.isValid)
        val contact = requireNotNull(result.contact)
        assertEquals("O’Neil", contact.nom)
        assertEquals("Sarah", contact.prenom)
        assertEquals("+216 55 111 222", contact.telephone)
        assertEquals("sarah@example.com", contact.email)
        assertNull(contact.latitude)
        assertNull(contact.longitude)
    }

    @Test
    fun missingRequiredFields_areRejected_nothingCanBeSaved() {
        val result = ContactValidator.validate(0, "  ", "", "", null, null, null)

        assertFalse(result.isValid)
        assertNull(result.contact)
        assertEquals(ValidationError.REQUIRED, result.errors[ContactField.NOM])
        assertEquals(ValidationError.REQUIRED, result.errors[ContactField.PRENOM])
        assertEquals(ValidationError.REQUIRED, result.errors[ContactField.TELEPHONE])
    }

    @Test
    fun invalidEmail_isRejected_butEmptyEmailIsOptional() {
        val invalid = ContactValidator.validate(1, "A", "B", "123", "not-an-email", null, null)
        assertEquals(ValidationError.EMAIL_INVALID, invalid.errors[ContactField.EMAIL])
        assertNull(invalid.contact)

        val empty = ContactValidator.validate(1, "A", "B", "123", "   ", null, null)
        assertTrue(empty.isValid)
        assertNull(requireNotNull(empty.contact).email)
    }

    @Test
    fun apostropheInNameAndFilter_isAccepted() {
        val contact = ContactValidator.validate(
            0, "L'Ariana", "Yassine", "71 000 111", null, null, null
        ).contact

        assertNotNull(contact)
        assertEquals("L'Ariana", contact?.nom)
    }

    @Test
    fun coordinates_acceptCommaDecimal_andBoundaryValues() {
        val result = ContactValidator.validate(0, "A", "B", "123", null, " 34,7406 ", " 180 ")

        assertTrue(result.isValid)
        assertEquals(34.7406, requireNotNull(result.contact).latitude!!, 0.000001)
        assertEquals(180.0, requireNotNull(result.contact).longitude!!, 0.000001)
    }

    @Test
    fun coordinates_missingNonNumericOrOutOfRange_areRejected() {
        val onlyLatitude = ContactValidator.validate(0, "A", "B", "123", null, "10", "")
        assertEquals(ValidationError.COORD_REQUIRED, onlyLatitude.errors[ContactField.LONGITUDE])

        val notNumeric = ContactValidator.validate(0, "A", "B", "123", null, "abc", "10")
        assertEquals(ValidationError.COORD_NOT_NUMERIC, notNumeric.errors[ContactField.LATITUDE])

        val outOfRange = ContactValidator.validate(0, "A", "B", "123", null, "91", "10")
        assertEquals(ValidationError.COORD_OUT_OF_RANGE, outOfRange.errors[ContactField.LATITUDE])

        val nan = ContactValidator.validate(0, "A", "B", "123", null, "NaN", "10")
        assertEquals(ValidationError.COORD_NOT_NUMERIC, nan.errors[ContactField.LATITUDE])
    }

    @Test
    fun isValidPosition_checksNullsFiniteAndBounds() {
        assertTrue(ContactValidator.isValidPosition(34.7, 10.7))
        assertTrue(ContactValidator.isValidPosition(90.0, -180.0))
        assertFalse(ContactValidator.isValidPosition(null, 10.0))
        assertFalse(ContactValidator.isValidPosition(10.0, null))
        assertFalse(ContactValidator.isValidPosition(Double.NaN, 10.0))
        assertFalse(ContactValidator.isValidPosition(Double.POSITIVE_INFINITY, 10.0))
        assertFalse(ContactValidator.isValidPosition(90.1, 10.0))
        assertFalse(ContactValidator.isValidPosition(10.0, -180.1))
    }
}
