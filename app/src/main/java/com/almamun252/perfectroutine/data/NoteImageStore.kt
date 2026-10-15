package com.almamun252.perfectroutine.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.roundToInt

// Keep original image bytes privately; the database stores relative paths, not gallery URIs.
object NoteImageStore {
    private const val SAVED = "note_images"
    private const val DRAFTS = "note_image_drafts"
    private const val MAX_BYTES = 50L * 1024L * 1024L

    fun file(context: Context, path: String): File {
        val parts = path.split('/')
        require(parts.size == 2 && parts[0] in listOf(SAVED, DRAFTS) &&
            parts[1].matches(Regex("[a-f0-9-]+\\.img"))) { "Invalid note image path" }
        return File(context.filesDir, path)
    }

    fun pruneOldDrafts(context: Context, keep: List<String>) {
        val threshold = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L
        val keepNames = keep.toSet()
        File(context.filesDir, DRAFTS).listFiles()?.forEach { draft ->
            val path = "$DRAFTS/${draft.name}"
            if (draft.isFile && draft.lastModified() < threshold && path !in keepNames) draft.delete()
        }
    }

    suspend fun importImage(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val path = "$DRAFTS/${UUID.randomUUID()}.img"
        val target = file(context, path)
        target.parentFile?.mkdirs()
        try {
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Image unavailable")
            input.use { source ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var total = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = source.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_BYTES) throw IOException("Image exceeds 50 MB")
                        output.write(buffer, 0, count)
                    }
                }
            }
            val preview = decode(context, path, 128) ?: throw IOException("Unsupported image")
            preview.recycle()
            path
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
    }

    data class Prepared(val paths: List<String>, val created: List<String>, val drafts: List<String>)

    // Copy drafts before committing Room; failed saves roll back only newly copied files.
    fun prepare(context: Context, paths: List<String>): Prepared {
        val created = mutableListOf<String>()
        val drafts = mutableListOf<String>()
        try {
            val saved = paths.distinct().map { path ->
                val source = file(context, path)
                if (!source.isFile) throw IOException("Note image missing")
                if (path.startsWith("$SAVED/")) path else {
                    val destination = "$SAVED/${UUID.randomUUID()}.img"
                    created += destination
                    val target = file(context, destination)
                    target.parentFile?.mkdirs()
                    source.copyTo(target)
                    drafts += path
                    destination
                }
            }
            return Prepared(saved, created, drafts)
        } catch (error: Throwable) {
            delete(context, created)
            throw error
        }
    }

    fun delete(context: Context, paths: List<String>) {
        paths.distinct().forEach { path -> file(context, path).delete() }
    }

    fun deleteDraft(context: Context, path: String) {
        if (path.startsWith("$DRAFTS/")) file(context, path).delete()
    }

    // Decode to the displayed size off the main thread instead of allocating a full camera bitmap.
    fun decode(context: Context, path: String, maxEdge: Int): Bitmap? {
        val source = file(context, path)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(source)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val ratio = minOf(1f, maxEdge.toFloat() / max(info.size.width, info.size.height))
                decoder.setTargetSize(max(1, (info.size.width * ratio).roundToInt()),
                    max(1, (info.size.height * ratio).roundToInt()))
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxEdge) sample *= 2
        val bitmap = BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply {
            inSampleSize = sample
        }) ?: return null
        val orientation = try {
            ExifInterface(source.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (_: IOException) { ExifInterface.ORIENTATION_NORMAL }
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
        if (matrix.isIdentity) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    }
}
