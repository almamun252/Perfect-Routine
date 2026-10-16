package com.almamun252.perfectroutine.view

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almamun252.perfectroutine.model.Task
import com.almamun252.perfectroutine.model.UserTab

@Composable
fun ProfileScreen(tabs: List<UserTab>, tasks: List<Task>, onManageTabs: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("local_profile", Context.MODE_PRIVATE) }
    var name by rememberSaveable { mutableStateOf(preferences.getString("name", "").orEmpty()) }
    var editingName by remember { mutableStateOf(false) }
    var showReminders by rememberSaveable { mutableStateOf(false) }
    val completed = tasks.count { it.isCompleted }
    Column(Modifier.fillMaxSize().background(Color(0xFFF6F8FC)).verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Surface(shape = RoundedCornerShape(26.dp), color = Color.Transparent) {
            Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF172D52), Color(0xFF355FC0))))
                .padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(62.dp).background(Color.White.copy(alpha = .16f), CircleShape), contentAlignment = Alignment.Center) {
                        if (name.isBlank()) Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(34.dp))
                        else Text(name.take(1).uppercase(), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name.ifBlank { "আমার প্রোফাইল" }, color = Color.White, fontSize = 21.sp,
                            fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("আপনার সময়, আপনার মতো", color = Color(0xFFCFDEFF), fontSize = 13.sp)
                    }
                    IconButton(onClick = { editingName = true }) { Icon(Icons.Rounded.Edit, "নাম পরিবর্তন", tint = Color.White) }
                }
                HorizontalDivider(color = Color.White.copy(alpha = .18f))
                Row(Modifier.fillMaxWidth()) {
                    ProfileStat("${tasks.size - completed}", "বাকি কাজ", Modifier.weight(1f))
                    ProfileStat("$completed", "সম্পন্ন", Modifier.weight(1f))
                    ProfileStat("${tabs.size}", "আমার ট্যাব", Modifier.weight(1f))
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("নিজের মতো সাজান", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172D52))
            ProfileSettingRow(Icons.Rounded.DashboardCustomize, "আমার ট্যাব", "নাম, আইকন, রং ও ক্রম", Color(0xFF4668D9), onManageTabs)
            ProfileSettingRow(Icons.Rounded.NotificationsActive, "রিমাইন্ডার", "নোটিফিকেশন ও সময়ের অনুমতি", Color(0xFFB7791F)) {
                showReminders = !showReminders
            }
            if (showReminders) Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                Column(Modifier.padding(16.dp)) { NotificationSettingsPanel() }
            }
        }
        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFEAF0FC)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.TipsAndUpdates, null, tint = Color(0xFF4668D9), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text("Work দিয়ে শুরু করুন। পড়াশোনা, project বা ব্যক্তিগত কাজের জন্য নিজের ট্যাব যোগ করতে পারেন।",
                    color = Color(0xFF485976), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    if (editingName) {
        var draft by remember { mutableStateOf(name) }
        AlertDialog(onDismissRequest = { editingName = false }, title = { Text("আপনার নাম") },
            text = { OutlinedTextField(value = draft, onValueChange = { if (it.length <= 40) draft = it }, singleLine = true, label = { Text("নাম") }) },
            confirmButton = { TextButton(onClick = {
                name = draft.trim()
                preferences.edit().putString("name", name).apply()
                editingName = false
            }) { Text("সেভ করুন") } },
            dismissButton = { TextButton(onClick = { editingName = false }) { Text("বাতিল") } })
    }
}

@Composable
private fun ProfileStat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFFCFDEFF), fontSize = 12.sp)
    }
}

@Composable
private fun ProfileSettingRow(icon: ImageVector, title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = Color.White, tonalElevation = 0.dp) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(accent.copy(alpha = .1f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF172D52), fontSize = 16.sp)
                Text(subtitle, color = Color(0xFF7B879C), fontSize = 12.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFF9CA8BC))
        }
    }
}
