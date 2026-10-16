package com.almamun252.perfectroutine.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almamun252.perfectroutine.model.UserTab
import com.almamun252.perfectroutine.model.Task
import com.almamun252.perfectroutine.viewmodel.TaskViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val TabColors = listOf(0xFF4668D9L, 0xFF8B5CF6L, 0xFF0D9488L, 0xFFF59E0BL, 0xFFEC4899L, 0xFF64748BL)
private val TabInk = Color(0xFF172D52)
private val TabMuted = Color(0xFF7B879C)
private val TabBlue = Color(0xFF4668D9)

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
    BackHandler { if (!busy && edit == null && deleting == null) onBack() }
    Scaffold(containerColor = Color(0xFFF6F8FC), topBar = {
        TopAppBar(title = { Text("আমার ট্যাব", fontWeight = FontWeight.Bold, color = TabInk) }, navigationIcon = {
            IconButton(onClick = onBack, enabled = !busy) { Icon(Icons.Rounded.ChevronLeft, "Profile-এ ফিরুন", tint = TabInk) }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF6F8FC)))
    }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("একেক কাজে একেক জায়গা", color = TabInk, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("নাম, আইকন ও রং বেছে সাজান", color = TabMuted, fontSize = 13.sp)
                    }
                    Surface(shape = CircleShape, color = Color(0xFFE8EDFC)) {
                        Text("${tabs.size}", Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = TabBlue, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Button(onClick = { error = null; edit = UserTab("", "", tabs.size, colorArgb = 0xFF4668D9L) }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = TabBlue)) {
                    Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("নতুন ট্যাব", fontWeight = FontWeight.Bold)
                }
                if (error != null && edit == null && deleting == null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }
            itemsIndexed(tabs, key = { _, tab -> tab.id }) { index, tab ->
                val accent = Color(tab.colorArgb)
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).background(accent.copy(alpha = .12f), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                                Icon(tabIcon(tab.iconKey), null, tint = accent)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tab.name, color = TabInk, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${tasks.count { it.tabType == tab.id }} কাজ · ${if (tab.isVisible) "দেখানো হচ্ছে" else "লুকানো"}", color = TabMuted, fontSize = 12.sp)
                            }
                            Switch(checked = tab.isVisible, enabled = !busy,
                                modifier = Modifier.semantics { contentDescription = "${tab.name} দেখান বা লুকান" },
                                colors = SwitchDefaults.colors(checkedTrackColor = accent),
                                onCheckedChange = { visible -> perform { viewModel.saveTab(tab.copy(isVisible = visible)) } })
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFF0F3F8))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { perform { viewModel.moveTab(tab.id, -1) } }, enabled = !busy && index > 0) {
                                Icon(Icons.Rounded.KeyboardArrowUp, "উপরে সরান")
                            }
                            IconButton(onClick = { perform { viewModel.moveTab(tab.id, 1) } }, enabled = !busy && index < tabs.lastIndex) {
                                Icon(Icons.Rounded.KeyboardArrowDown, "নিচে সরান")
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { error = null; edit = tab }, enabled = !busy) { Text("সম্পাদনা", color = TabBlue) }
                            IconButton(onClick = { error = null; deleting = tab }, enabled = !busy) { Icon(Icons.Rounded.DeleteOutline, "ট্যাব সরান", tint = Color(0xFFB96972)) }
                        }
                    }
                }
            }
            item {
                Text(if (tabs.isEmpty()) "আপনার প্রথম ট্যাব তৈরি করুন। Home ও Profile সবসময় থাকবে।"
                    else "ট্যাব লুকালেও তার কাজ Home-এ থাকবে এবং reminder চালু থাকবে।", color = TabMuted, fontSize = 12.sp, modifier = Modifier.padding(4.dp))
            }
        }
    }
    edit?.let { tab ->
        key(tab.id) {
            TabEditorSheet(tab = tab, busy = busy, error = error, onDismiss = { if (!busy) edit = null }, onSave = { changed ->
                perform { viewModel.saveTab(changed); edit = null }
            })
        }
    }
    deleting?.let { tab ->
        var target by remember(tab.id) { mutableStateOf<String?>(null) }
        val count = tasks.count { it.tabType == tab.id }
        AlertDialog(onDismissRequest = { if (!busy) deleting = null }, title = { Text("${tab.name} সরাবেন?") },
            text = { Column {
                Text(if (count > 0) "${count}টি কাজ মুছবে না। কাজগুলো রাখার ট্যাব বেছে নিন।" else "এই ট্যাবের category suggestion-ও সরবে।")
                if (count > 0 && tabs.size == 1) Text("আগে আরেকটি ট্যাব তৈরি করুন, অথবা এই ট্যাবটি লুকিয়ে রাখুন।")
                if (count > 0) Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                    tabs.filter { it.id != tab.id }.forEach { other ->
                        Row(Modifier.fillMaxWidth().clickable(enabled = !busy) { target = other.id }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = target == other.id, onClick = { target = other.id }, enabled = !busy)
                            Icon(tabIcon(other.iconKey), null, tint = Color(other.colorArgb), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp)); Text(other.name)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabEditorSheet(tab: UserTab, busy: Boolean, error: String?, onDismiss: () -> Unit, onSave: (UserTab) -> Unit) {
    var name by remember { mutableStateOf(tab.name) }
    var color by remember { mutableStateOf(tab.colorArgb) }
    var iconKey by remember { mutableStateOf(tab.iconKey) }
    var quick by remember { mutableStateOf(tab.isQuickList) }
    val accent = Color(color)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
            .navigationBarsPadding().padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).background(accent.copy(alpha = .12f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    Icon(tabIcon(iconKey), null, tint = accent, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (tab.id.isEmpty()) "নতুন ট্যাব" else "ট্যাব সাজান", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TabInk)
                    Text(name.ifBlank { "আপনার কাজের নতুন জায়গা" }, color = TabMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            OutlinedTextField(value = name, onValueChange = { if (it.length <= 30) name = it }, label = { Text("ট্যাবের নাম") },
                placeholder = { Text("যেমন: University, ML, Personal") }, singleLine = true, enabled = !busy,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
            Text("আইকন বেছে নিন", color = TabInk, fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TabIconOptions.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { option ->
                            val chosen = iconKey == option.key
                            Surface(onClick = { iconKey = option.key }, enabled = !busy,
                                modifier = Modifier.weight(1f).semantics { contentDescription = option.label },
                                shape = RoundedCornerShape(14.dp), color = if (chosen) accent.copy(alpha = .1f) else Color(0xFFF6F8FC),
                                border = BorderStroke(if (chosen) 1.5.dp else 0.dp, if (chosen) accent else Color.Transparent)) {
                                Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(option.image, null, tint = if (chosen) accent else TabMuted, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text(option.label, color = if (chosen) accent else TabMuted, fontSize = 10.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
            Text("রং", color = TabInk, fontWeight = FontWeight.SemiBold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (TabColors + color).distinct().forEach { value ->
                    Surface(onClick = { color = value }, enabled = !busy, shape = CircleShape, color = Color(value), modifier = Modifier.size(42.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            if (value == color) Icon(Icons.Rounded.Check, "নির্বাচিত রং", tint = Color.White)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(selected = !quick, onClick = { quick = false }, enabled = !busy, label = { Text("Task list") }, leadingIcon = { Icon(Icons.Rounded.Checklist, null, Modifier.size(18.dp)) })
                FilterChip(selected = quick, onClick = { quick = true }, enabled = !busy, label = { Text("Quick list") }, leadingIcon = { Icon(Icons.Rounded.FlashOn, null, Modifier.size(18.dp)) })
            }
            Text(if (quick) "দ্রুত কাজ লিখুন ও চেপে ধরে ক্রম বদলান।" else "নোট, ছবি, deadline ও focus timer সহ কাজ রাখুন।", fontSize = 12.sp, color = TabMuted)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { onSave(tab.copy(name = name, colorArgb = color, iconKey = iconKey, isQuickList = quick)) },
                enabled = !busy && name.isNotBlank(), modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                Text(if (busy) "সেভ হচ্ছে…" else "সেভ করুন", fontWeight = FontWeight.Bold)
            }
        }
    }
}
