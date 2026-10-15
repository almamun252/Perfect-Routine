package com.almamun252.perfectroutine.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.almamun252.perfectroutine.data.NoteImageStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NoteImages(paths: List<String>, onRemove: ((String) -> Unit)? = null, enabled: Boolean = true) {
    var opened by remember { mutableStateOf<String?>(null) }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(paths, key = { _, path -> path }) { index, path ->
            Box(Modifier.size(112.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE2E8F0))) {
                NoteImage(path, 512, Modifier.fillMaxSize().clickable { opened = path }, ContentScale.Crop,
                    "নোটের ছবি ${index + 1}")
                if (onRemove != null) {
                    IconButton(onClick = { onRemove(path) }, enabled = enabled,
                        modifier = Modifier.align(Alignment.TopEnd).size(48.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "ছবি সরান",
                            tint = Color.White, modifier = Modifier.background(Color.Black.copy(alpha = 0.6f),
                                RoundedCornerShape(16.dp)).padding(4.dp))
                    }
                }
            }
        }
    }
    opened?.let { path ->
        Dialog(onDismissRequest = { opened = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            var scale by remember(path) { mutableFloatStateOf(1f) }
            var offset by remember(path) { mutableStateOf(Offset.Zero) }
            Box(Modifier.fillMaxSize().background(Color.Black).clip(RoundedCornerShape(0.dp))) {
                Box(Modifier.fillMaxSize().pointerInput(path) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 6f)
                        offset = if (scale <= 1f) Offset.Zero else Offset(
                            (offset.x + pan.x).coerceIn(-size.width * (scale - 1f) / 2f, size.width * (scale - 1f) / 2f),
                            (offset.y + pan.y).coerceIn(-size.height * (scale - 1f) / 2f, size.height * (scale - 1f) / 2f))
                    }
                }) {
                    NoteImage(path, 2560, Modifier.fillMaxSize().graphicsLayer {
                        scaleX = scale; scaleY = scale
                        translationX = offset.x; translationY = offset.y
                    }, ContentScale.Fit, "নোটের ছবি, দুই আঙুল দিয়ে বড় করুন")
                }
                IconButton(onClick = { opened = null },
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "ছবি বন্ধ করুন", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun NoteImage(path: String, maxEdge: Int, modifier: Modifier, scale: ContentScale, description: String) {
    val context = LocalContext.current.applicationContext
    var loading by remember(path, maxEdge) { mutableStateOf(true) }
    val bitmap by produceState<ImageBitmap?>(null, path, maxEdge) {
        value = withContext(Dispatchers.IO) {
            try { NoteImageStore.decode(context, path, maxEdge)?.asImageBitmap() }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { null }
        }
        loading = false
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val image = bitmap
        if (image != null) Image(image, description, Modifier.fillMaxSize(), contentScale = scale)
        else if (loading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        else Icon(Icons.Rounded.BrokenImage, contentDescription = "ছবি পাওয়া যায়নি", tint = Color.Gray)
    }
}
