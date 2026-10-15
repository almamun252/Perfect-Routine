package com.almamun252.perfectroutine.data

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class NoteImageStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun savedCopySurvivesGalleryRemovalAndKeepsOriginalBytes() = runBlocking {
        val original = File(context.cacheDir, "gallery-${UUID.randomUUID()}.png")
        val bitmap = Bitmap.createBitmap(32, 48, Bitmap.Config.ARGB_8888)
        original.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val bytes = original.readBytes()
        var draft: String? = null
        var prepared: NoteImageStore.Prepared? = null
        try {
            val imported = NoteImageStore.importImage(context, Uri.fromFile(original))
            draft = imported
            val saved = NoteImageStore.prepare(context, listOf(imported))
            prepared = saved
            NoteImageStore.delete(context, saved.drafts)
            original.delete()
            assertArrayEquals(bytes, NoteImageStore.file(context, saved.paths.single()).readBytes())
            val decoded = NoteImageStore.decode(context, saved.paths.single(), 512)!!
            assertEquals(32, decoded.width)
            assertEquals(48, decoded.height)
            decoded.recycle()
        } finally {
            original.delete()
            draft?.let { NoteImageStore.delete(context, listOf(it)) }
            prepared?.let { NoteImageStore.delete(context, it.created) }
        }
    }

    @Test fun failedPreparationDoesNotDeletePreviouslySavedImages() = runBlocking {
        val original = File(context.cacheDir, "gallery-${UUID.randomUUID()}.png")
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        original.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val draft = NoteImageStore.importImage(context, Uri.fromFile(original))
        val saved = NoteImageStore.prepare(context, listOf(draft))
        try {
            try {
                NoteImageStore.prepare(context, listOf(saved.paths.single(), "note_images/deadbeef.img"))
                fail("Missing image must fail before writing task metadata")
            } catch (_: java.io.IOException) { }
            assertTrue(NoteImageStore.file(context, saved.paths.single()).exists())
        } finally {
            original.delete()
            NoteImageStore.delete(context, saved.created + saved.drafts)
        }
    }
}
