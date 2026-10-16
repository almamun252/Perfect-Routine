package com.almamun252.perfectroutine.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.almamun252.perfectroutine.model.UserTab
import com.almamun252.perfectroutine.model.Task
import com.almamun252.perfectroutine.viewmodel.TaskViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val TabColors = listOf(0xFF3B82F6L, 0xFF8B5CF6L, 0xFFF59E0BL, 0xFF10B981L, 0xFFEF4444L)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSettingsScreen(tabs: List<UserTab>, tasks: List<Task>, viewModel: TaskViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var edit by remember { mutableStateOf<UserTab?>(null) }
    var deleting by remember { mutableStateOf<UserTab?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun perform(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                action()
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "পরিবর্তন সেভ হয়নি। আবার চেষ্টা করুন।"
            } finally {
                busy = false
            }
        }
    }
    BackHandler { if (!busy) onBack() }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Settings · আমার ট্যাব") }, navigationIcon = {
            IconButton(onClick = onBack, enabled = !busy) { Icon(Icons.Rounded.ArrowBack, "Back") }
        })
    }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("নিজের প্রয়োজন অনুযায়ী ট্যাব তৈরি করুন। Home-এ সব ট্যাবের কাজ একসঙ্গে থাকবে।")
                Text("ট্যাব লুকালেও কাজ, reminder ও Home থেকে কাজ সরবে না।", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { edit = UserTab("", "", tabs.size) }, enabled = !busy) {
                    Icon(Icons.Rounded.Add, null); Text(" নতুন ট্যাব")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (tabs.isEmpty()) Text("এখনো কোনো ট্যাব নেই। যেমন: University, Personal, ML বা Projects।")
            }
            itemsIndexed(tabs, key = { _, tab -> tab.id }) { index, tab ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp).background(Color(tab.colorArgb), CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tab.name, fontWeight = FontWeight.Bold)
                                Text("${tasks.count { it.tabType == tab.id }} কাজ · ${if (tab.isQuickList) "Quick list" else "Task list"}")
                            }
                            Switch(checked = tab.isVisible, enabled = !busy,
                                onCheckedChange = { visible -> perform { viewModel.saveTab(tab.copy(isVisible = visible)) } })
                        }
                        Row {
                            IconButton(onClick = { perform { viewModel.moveTab(tab.id, -1) } }, enabled = !busy && index > 0) {
                                Icon(Icons.Rounded.ArrowUpward, "Move up")
                            }
                            IconButton(onClick = { perform { viewModel.moveTab(tab.id, 1) } }, enabled = !busy && index < tabs.lastIndex) {
                                Icon(Icons.Rounded.ArrowDownward, "Move down")
                            }
                            TextButton(onClick = { edit = tab }, enabled = !busy) { Text("সম্পাদনা") }
                            TextButton(onClick = { deleting = tab }, enabled = !busy) { Text("সরান") }
                        }
                    }
                }
            }
            item { NotificationSettingsPanel() }
        }
    }
    edit?.let { tab ->
        key(tab.id) {
            var name by remember { mutableStateOf(tab.name) }
            var color by remember { mutableStateOf(tab.colorArgb) }
            var quick by remember { mutableStateOf(tab.isQuickList) }
            AlertDialog(onDismissRequest = { if (!busy) edit = null },
                title = { Text(if (tab.id.isEmpty()) "নতুন ট্যাব" else "ট্যাব সম্পাদনা") },
                text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = name, onValueChange = { if (it.length <= 30) name = it },
                        label = { Text("ট্যাবের নাম") }, singleLine = true, enabled = !busy)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TabColors.forEach { value ->
                            FilterChip(selected = value == color, onClick = { color = value },
                                enabled = !busy, label = { Text(if (value == color) "✓" else "●", color = Color(value)) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Quick list", Modifier.weight(1f))
                        Switch(quick, { quick = it }, enabled = !busy)
                    }
                    Text("Quick list-এ দ্রুত কাজ যোগ ও ক্রম বদলানো যায়। Task list-এ search, note, deadline ও timer ব্যবহার করা যায়।",
                        style = MaterialTheme.typography.bodySmall)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                } },
                confirmButton = { TextButton(enabled = !busy && name.isNotBlank(), onClick = {
                    perform { viewModel.saveTab(tab.copy(name = name, colorArgb = color, isQuickList = quick)); edit = null }
                }) { Text("সেভ করুন") } },
                dismissButton = { TextButton(enabled = !busy, onClick = { edit = null }) { Text("বাতিল") } })
        }
    }
    deleting?.let { tab ->
        var target by remember(tab.id) { mutableStateOf<String?>(null) }
        val count = tasks.count { it.tabType == tab.id }
        AlertDialog(onDismissRequest = { if (!busy) deleting = null }, title = { Text("${tab.name} সরাবেন?") },
            text = { Column {
                Text(if (count > 0) "$countটি কাজ মুছবে না। কাজগুলো রাখার ট্যাব বেছে নিন।" else "এই ট্যাবের category suggestion-ও সরবে।")
                if (count > 0 && tabs.size == 1) Text("আগে আরেকটি ট্যাব তৈরি করুন, অথবা এই ট্যাবটি লুকিয়ে রাখুন।")
                if (count > 0) Column(Modifier.heightIn(max = 240.dp).verticalScrollCompat()) {
                    tabs.filter { it.id != tab.id }.forEach { other ->
                        Row(Modifier.fillMaxWidth().clickable(enabled = !busy) { target = other.id }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = target == other.id, onClick = { target = other.id }, enabled = !busy)
                            Text(other.name)
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { TextButton(enabled = !busy && (count == 0 || target != null), onClick = {
                perform { viewModel.deleteTab(tab.id, target); deleting = null }
            }) { Text(if (count > 0) "কাজ সরিয়ে ট্যাব মুছুন" else "ট্যাব মুছুন") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleting = null }) { Text("বাতিল") } })
    }
}

@Composable
private fun Modifier.verticalScrollCompat(): Modifier =
    this.then(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()))
