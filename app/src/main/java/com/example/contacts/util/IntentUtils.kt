package com.example.contacts.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import com.example.contacts.R

object IntentUtils {

    /** ACTION_DIAL : ouvre le composeur, aucune permission requise. */
    fun dial(context: Context, phone: String?) {
        val number = phone?.trim().orEmpty()
        if (number.isEmpty()) return toast(context, R.string.err_numero_vide)
        start(context, Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)), R.string.err_no_phone)
    }

    fun email(context: Context, address: String?) {
        val mail = address?.trim().orEmpty()
        if (mail.isEmpty()) return toast(context, R.string.err_email_vide)
        start(context, Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", mail, null)), R.string.err_no_mail)
    }

    private fun start(context: Context, intent: Intent, @StringRes errorRes: Int) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(context, errorRes)
        }
    }

    fun toast(context: Context, @StringRes res: Int) =
        Toast.makeText(context, res, Toast.LENGTH_LONG).show()
}
