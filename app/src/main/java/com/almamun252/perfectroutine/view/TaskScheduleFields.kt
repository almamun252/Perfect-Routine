package com.almamun252.perfectroutine.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.almamun252.perfectroutine.utils.TaskSchedule
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScheduleFields(start: Long?, deadline: Long?, enabled: Boolean = true,
                       onStart: (Long?) -> Unit, onDeadline: (Long?) -> Unit) {
    var target by rememberSaveable { mutableStateOf<String?>(null) }
    var choosingTime by rememberSaveable { mutableStateOf(false) }
    var date by rememberSaveable { mutableLongStateOf(0L) }
    val format = SimpleDateFormat("dd MMM yyyy · hh:mm a", Locale.getDefault())
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScheduleField("শুরুর সময়", start?.let { format.format(Date(it)) } ?: "সেভ করার সময় থেকে", start != null, enabled,
            onClick = { target = "start"; choosingTime = false }, onClear = { onStart(null) })
        ScheduleField("শেষের সময়", deadline?.let { format.format(Date(it)) } ?: "ডেডলাইন নেই", deadline != null, enabled,
            onClick = { target = "deadline"; choosingTime = false }, onClear = { onDeadline(null) })
    }
    if (target != null) {
        val initial = (if (target == "start") start else deadline) ?: start ?: System.currentTimeMillis()
        if (!choosingTime) {
            val state = rememberDatePickerState(initialSelectedDateMillis = TaskSchedule.pickerDate(initial))
            DatePickerDialog(onDismissRequest = { target = null }, confirmButton = {
                TextButton(enabled = state.selectedDateMillis != null, onClick = {
                    date = state.selectedDateMillis!!
                    choosingTime = true
                }) { Text("সময় বাছুন") }
            }, dismissButton = { TextButton(onClick = { target = null }) { Text("বাতিল") } }) {
                DatePicker(state = state, title = { Text(if (target == "start") "শুরুর তারিখ" else "শেষের তারিখ", Modifier.padding(16.dp)) })
            }
        } else {
            val cal = Calendar.getInstance().apply { timeInMillis = initial }
            val state = rememberTimePickerState(initialHour = cal.get(Calendar.HOUR_OF_DAY), initialMinute = cal.get(Calendar.MINUTE))
            AlertDialog(onDismissRequest = { target = null }, title = { Text("সময় নির্বাচন করুন") },
                text = { Column(Modifier.verticalScroll(rememberScrollState())) { TimeInput(state = state) } },
                confirmButton = { TextButton(onClick = {
                    val value = TaskSchedule.localTime(date, state.hour, state.minute)
                    if (target == "start") onStart(value) else onDeadline(value)
                    target = null
                }) { Text("নিশ্চিত করুন") } },
                dismissButton = { TextButton(onClick = { choosingTime = false }) { Text("তারিখে ফিরুন") } })
        }
    }
}

@Composable
private fun ScheduleField(label: String, value: String, clearable: Boolean, enabled: Boolean,
                          onClick: () -> Unit, onClear: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(16.dp), color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Schedule, null, tint = Color(0xFF4668D9))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            if (clearable) IconButton(onClick = onClear, enabled = enabled) {
                Icon(Icons.Rounded.Close, "$label মুছুন")
            } else Spacer(Modifier.width(12.dp))
        }
    }
}
