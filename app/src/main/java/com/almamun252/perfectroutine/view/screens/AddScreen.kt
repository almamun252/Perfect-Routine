package com.almamun252.perfectroutine.view.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.almamun252.perfectroutine.model.Category
import com.almamun252.perfectroutine.model.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    navController: NavController,
    tabType: String, // কোন ট্যাব থেকে এসেছে (Academic, Research ইত্যাদি)
    existingTask: Task? = null, // যদি এডিট করতে আসে
    categories: List<Category>, // ড্রপডাউনে দেখানোর জন্য আগের সাবজেক্টগুলো
    onSaveTask: (Task) -> Unit // সেভ বাটনে ক্লিক করলে ডেটাবেসে পাঠানোর জন্য
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    // --- Form States ---
    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var subjectName by remember { mutableStateOf(existingTask?.subjectName ?: "") }
    var description by remember { mutableStateOf(existingTask?.description ?: "") }
    var priority by remember { mutableStateOf(existingTask?.priority ?: "Medium") }

    // --- Date & Time States ---
    var deadlineMillis by remember { mutableStateOf<Long?>(existingTask?.deadline) }
    var tempDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }

    // থিম কালার (ট্যাব অনুযায়ী পরিবর্তন হবে)
    val themeColor = when (tabType) {
        "Academic" -> Color(0xFF6366F1)       // Indigo (নিলচে)
        "Research" -> Color(0xFF8B5CF6)       // Violet (বেগুনি)
        "Extra Learning" -> Color(0xFF10B981) // Emerald (সবুজ)
        "Work" -> Color(0xFFF59E0B)           // Amber (কমলা)
        else -> Color(0xFF6366F1)             // ডিফল্ট
    }

    val screenTitle = if (existingTask != null) "কাজ এডিট করুন" else "নতুন কাজ যোগ করুন"

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 150 },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // --- Top Bar ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", tint = Color(0xFF334155))
                    }
                    Text(text = screenTitle, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.size(44.dp)) // ব্যালেন্স করার জন্য
                }

                // --- Form Fields ---
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // 1. কাজের নাম (Title)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("কাজের নাম (যেমন: Assignment 1)") },
                        leadingIcon = { Icon(Icons.Rounded.TaskAlt, contentDescription = null, tint = themeColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColor, unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedLabelColor = themeColor, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    // 2. সাবজেক্ট / ক্যাটাগরি (Dropdown + Typed Input)
                    ExposedDropdownMenuBox(
                        expanded = subjectExpanded,
                        onExpandedChange = { subjectExpanded = !subjectExpanded }
                    ) {
                        OutlinedTextField(
                            value = subjectName,
                            onValueChange = {
                                subjectName = it
                                subjectExpanded = true // টাইপ করার সময় ড্রপডাউন ওপেন থাকবে
                            },
                            label = { Text("কোন সাবজেক্ট? (লিখুন বা সিলেক্ট করুন)") },
                            leadingIcon = { Icon(Icons.Rounded.School, contentDescription = null, tint = themeColor) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor, unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedLabelColor = themeColor, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                            ),
                            singleLine = true
                        )

                        // শুধুমাত্র যদি ক্যাটাগরি থাকে তবেই ড্রপডাউন দেখাবে
                        val filteredCategories = categories.filter { it.name.contains(subjectName, ignoreCase = true) }
                        if (filteredCategories.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = subjectExpanded,
                                onDismissRequest = { subjectExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                filteredCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.name, color = Color(0xFF1E293B)) },
                                        onClick = {
                                            subjectName = category.name
                                            subjectExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 3. ডেডলাইন (Date & Time Picker)
                    val sdf = SimpleDateFormat("dd MMM, yyyy  •  hh:mm a", Locale("bn", "BD"))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = deadlineMillis?.let { sdf.format(Date(it)) } ?: "ডেডলাইন সেট করুন",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("কবে শেষ করতে হবে?") },
                            leadingIcon = { Icon(Icons.Rounded.EventAvailable, contentDescription = null, tint = if (deadlineMillis != null) themeColor else Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor, unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedLabelColor = themeColor, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        // ইনপুটের ওপর ক্লিক করলে DatePicker ওপেন হবে
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    showDatePicker = true
                                }
                        )
                        // ডেডলাইন ক্লিয়ার করার বাটন
                        if (deadlineMillis != null) {
                            IconButton(
                                onClick = { deadlineMillis = null },
                                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    }

                    // 4. প্রায়োরিটি (High, Medium, Low)
                    Text("কাজের গুরুত্ব (Priority)", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.padding(top = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("High" to Color(0xFFF43F5E), "Medium" to Color(0xFFF59E0B), "Low" to Color(0xFF10B981)).forEach { (prio, color) ->
                            val isSelected = priority == prio
                            Surface(
                                onClick = { priority = prio },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) color else Color.White,
                                border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE2E8F0)) else null,
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = prio,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    // 5. বিস্তারিত নোট
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("বিস্তারিত নোট (ঐচ্ছিক)") },
                        leadingIcon = { Icon(Icons.Rounded.Subject, contentDescription = null, tint = themeColor) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColor, unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedLabelColor = themeColor, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Save Button ---
                Button(
                    onClick = {
                        val finalTitle = title.trim()
                        val finalSubject = subjectName.trim()

                        if (finalTitle.isEmpty()) {
                            Toast.makeText(context, "কাজের নাম দেওয়া আবশ্যক!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (finalSubject.isEmpty()) {
                            Toast.makeText(context, "সাবজেক্টের নাম দেওয়া আবশ্যক!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // নতুন টাস্ক অবজেক্ট তৈরি
                        val task = Task(
                            id = existingTask?.id ?: 0,
                            title = finalTitle,
                            description = description.trim().ifEmpty { null },
                            subjectName = finalSubject,
                            tabType = tabType,
                            priority = priority,
                            isCompleted = existingTask?.isCompleted ?: false,
                            taskDate = existingTask?.taskDate ?: System.currentTimeMillis(),
                            deadline = deadlineMillis
                        )

                        onSaveTask(task)
                        Toast.makeText(context, "সফলভাবে সেভ হয়েছে!", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                ) {
                    Text(
                        text = if (existingTask != null) "আপডেট করুন" else "সেভ করুন",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // --- Date Picker Dialog ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = deadlineMillis ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { tempDateMillis = it }
                    showDatePicker = false
                    showTimePicker = true // তারিখ সিলেক্ট হলে সময় সিলেক্ট করার পপআপ আসবে
                }) { Text("পরবর্তী (সময়)", fontWeight = FontWeight.Bold, color = themeColor) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("বাতিল", color = Color.Gray) } }
        ) {
            DatePicker(state = datePickerState, title = { Text(" ডেডলাইন নির্বাচন করুন", modifier = Modifier.padding(16.dp)) })
        }
    }

    // --- Time Picker Dialog ---
    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = deadlineMillis ?: System.currentTimeMillis() }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE)
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = tempDateMillis // DatePicker থেকে পাওয়া তারিখ
                    calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    calendar.set(Calendar.MINUTE, timePickerState.minute)
                    deadlineMillis = calendar.timeInMillis
                    showTimePicker = false
                }) {
                    Text("নিশ্চিত করুন", fontWeight = FontWeight.Bold, color = themeColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("বাতিল", color = Color.Gray) }
            },
            title = { Text("সময় নির্বাচন করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            containerColor = Color.White
        )
    }
}