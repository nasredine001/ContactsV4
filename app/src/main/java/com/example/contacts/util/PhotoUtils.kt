package com.example.contacts.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.ImageView
import androidx.exifinterface.media.ExifInterface
import com.example.contacts.R
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/** Photos : copie réduite dans le stockage interne (filesDir/photos), jamais d’URI de galerie temporaire. */
object PhotoUtils {
    private const val MAX_SIDE = 512

    /** À appeler hors du thread principal. @return le chemin absolu de la copie, ou null en cas d’échec. */
    fun importImage(context: Context, uri: Uri): String? {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                ?: return null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return null

            val orientation = resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }
            val scale = MAX_SIDE.toFloat() / max(decoded.width, decoded.height)
            if (scale < 1f) matrix.postScale(scale, scale)
            val finalBitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

            val dir = File(context.filesDir, "photos").apply { mkdirs() }
            val out = File(dir, "contact_${System.currentTimeMillis()}.jpg")
            FileOutputStream(out).use { finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            out.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun deletePhoto(path: String?) {
        if (path.isNullOrEmpty()) return
        try { File(path).delete() } catch (e: SecurityException) { }
    }

    /** Affiche la photo, ou une icône par défaut si absente/illisible. */
    fun display(view: ImageView, path: String?, placeholderPaddingDp: Int) {
        val bitmap = path?.let { if (File(it).exists()) BitmapFactory.decodeFile(it) else null }
        if (bitmap != null) {
            view.setPadding(0, 0, 0, 0)
            view.scaleType = ImageView.ScaleType.CENTER_CROP
            view.setImageBitmap(bitmap)
        } else {
            val p = (placeholderPaddingDp * view.resources.displayMetrics.density).toInt()
            view.setPadding(p, p, p, p)
            view.scaleType = ImageView.ScaleType.FIT_CENTER
            view.setImageResource(R.drawable.ic_person)
        }
    }
}
