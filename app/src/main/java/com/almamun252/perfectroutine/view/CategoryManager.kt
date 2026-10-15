package com.almamun252.perfectroutine.view

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.almamun252.perfectroutine.model.Category
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun CategoryManager(categories: List<Category>, onDelete: suspend (Category) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<Category?>(null) }
    var deleting by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Category") },
        text = {
            if (categories.isEmpty()) Text("কোনো category নেই")
            else LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                items(categories.distinctBy { it.name.lowercase(java.util.Locale.ROOT) }, key = { it.id }) { category ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(category.name, Modifier.weight(1f))
                        IconButton(onClick = { selected = category }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "${category.name} মুছুন",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("বন্ধ করুন") } }
    )
    selected?.let { category ->
        AlertDialog(
            onDismissRequest = { if (!deleting) selected = null },
            title = { Text("${category.name} মুছবেন?") },
            text = { Text("Category তালিকা থেকে সরবে। আগের কাজ ও নোট থাকবে।") },
            confirmButton = {
                TextButton(enabled = !deleting, onClick = {
                    deleting = true
                    scope.launch {
                        try {
                            onDelete(category)
                            selected = null
                        } catch (error: CancellationException) { throw error }
                        catch (_: Exception) {
                            Toast.makeText(context, "Category মুছতে পারেনি। আবার চেষ্টা করুন।", Toast.LENGTH_SHORT).show()
                        } finally { deleting = false }
                    }
                }) { Text("মুছুন", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(enabled = !deleting, onClick = { selected = null }) { Text("বাতিল") } }
        )
    }
}
