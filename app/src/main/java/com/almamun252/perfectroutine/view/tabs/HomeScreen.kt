package com.almamun252.perfectroutine.view.tabs

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almamun252.perfectroutine.model.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    allTasks: List<Task>,
    onTaskStatusChange: (Task, Boolean) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onTaskClick: (Task) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    // --- Date Filter States ---
    var selectedDateFilter by remember { mutableStateOf("আগামী ৩ দিনের কাজ") }
    var showCustomDateDialog by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    // --- New States for UI Flow ---
    var showCompletedTasksSheet by remember { mutableStateOf(false) }
    var taskToConfirm by remember { mutableStateOf<Task?>(null) } // ভুল ক্লিক রোধ করার জন্য

    // --- Filtering Logic ---
    val filteredAllTasksByDate = allTasks.filter { tx ->
        val currentCal = Calendar.getInstance()
        val txDeadline = tx.deadline

        when (selectedDateFilter) {
            "সব কাজ" -> true
            "আজকের কাজ" -> {
                if (txDeadline == null) false else {
                    val txCal = Calendar.getInstance().apply { timeInMillis = txDeadline }
                    txCal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) &&
                            txCal.get(Calendar.DAY_OF_YEAR) == currentCal.get(Calendar.DAY_OF_YEAR)
                }
            }
            "আগামীকালের কাজ" -> {
                if (txDeadline == null) false else {
                    val txCal = Calendar.getInstance().apply { timeInMillis = txDeadline }
                    val tmrCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                    txCal.get(Calendar.YEAR) == tmrCal.get(Calendar.YEAR) &&
                            txCal.get(Calendar.DAY_OF_YEAR) == tmrCal.get(Calendar.DAY_OF_YEAR)
                }
            }
            "আগামী ৩ দিনের কাজ" -> {
                if (txDeadline == null) false else {
                    val endOf3Days = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 3) }.timeInMillis
                    txDeadline in (currentCal.timeInMillis - 86400000L)..endOf3Days
                }
            }
            "এই সপ্তাহের" -> {
                if (txDeadline == null) false else {
                    val endOfWeek = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis
                    txDeadline in (currentCal.timeInMillis - 86400000L)..endOfWeek
                }
            }
            "কাস্টম রেঞ্জ" -> {
                if (txDeadline == null) false else {
                    val start = customStartDate ?: 0L
                    val end = customEndDate?.let { it + 86400000L - 1L } ?: Long.MAX_VALUE
                    txDeadline in start..end
                }
            }
            else -> true
        }
    }

    val filteredPendingTasks = filteredAllTasksByDate.filter { !it.isCompleted }.sortedWith(
        compareBy(
            { if (it.priority == "High") 0 else if (it.priority == "Medium") 1 else 2 },
            { it.deadline ?: Long.MAX_VALUE }
        )
    )

    // সম্পন্ন হওয়া কাজগুলো আলাদা করে রাখা হলো
    val filteredCompletedTasks = filteredAllTasksByDate.filter { it.isCompleted }.sortedWith(
        compareBy(
            { it.deadline ?: Long.MAX_VALUE }
        )
    )

    val bgColor = Color(0xFFF8FAFC)
    val themeColor = Color(0xFF0EA5E9)

    Scaffold(
        containerColor = bgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Header & Summary ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 12.dp, bottom = 12.dp, start = 20.dp, end = 20.dp)
            ) {
                TaskDateRangeFilter(
                    selectedOption = selectedDateFilter,
                    onOptionSelected = { option ->
                        if (option == "কাস্টম রেঞ্জ") {
                            showCustomDateDialog = true
                        } else {
                            selectedDateFilter = option
                        }
                    },
                    themeColor = themeColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Summary Cards
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(
                        title = "এই লিস্টে বাকি",
                        count = filteredPendingTasks.size.toString(),
                        icon = Icons.Rounded.PendingActions,
                        color = Color(0xFFF43F5E),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "সম্পন্ন হয়েছে",
                        count = filteredCompletedTasks.size.toString(),
                        icon = Icons.Rounded.TaskAlt,
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                        onClick = { showCompletedTasksSheet = true } // সম্পন্ন হওয়া কাজ দেখার অপশন
                    )
                }
            }

            // --- Custom Date Range Dialogs ---
            if (showCustomDateDialog) {
                val sdf = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
                val startStr = customStartDate?.let { sdf.format(Date(it)) } ?: "শুরুর তারিখ নির্বাচন করুন"
                val endStr = customEndDate?.let { sdf.format(Date(it)) } ?: "শেষের তারিখ নির্বাচন করুন"

                AlertDialog(
                    onDismissRequest = { showCustomDateDialog = false },
                    containerColor = Color.White,
                    title = { Text("ডেডলাইন রেঞ্জ নির্বাচন করুন", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(
                                onClick = { showStartDatePicker = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp), tint = themeColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(startStr, fontSize = 16.sp, color = Color(0xFF1E293B))
                            }
                            OutlinedButton(
                                onClick = { showEndDatePicker = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp), tint = themeColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(endStr, fontSize = 16.sp, color = Color(0xFF1E293B))
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showCustomDateDialog = false; selectedDateFilter = "কাস্টম রেঞ্জ" },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                        ) {
                            Text("নিশ্চিত করুন", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomDateDialog = false; customStartDate = null; customEndDate = null }) {
                            Text("বাতিল", color = Color.Gray)
                        }
                    }
                )
            }

            if (showStartDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = customStartDate ?: System.currentTimeMillis())
                DatePickerDialog(
                    onDismissRequest = { showStartDatePicker = false },
                    confirmButton = { TextButton(onClick = { customStartDate = datePickerState.selectedDateMillis; showStartDatePicker = false }) { Text("ঠিক আছে", color = themeColor) } },
                    dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("বাতিল", color = Color.Gray) } }
                ) { DatePicker(state = datePickerState) }
            }

            if (showEndDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = customEndDate ?: System.currentTimeMillis())
                DatePickerDialog(
                    onDismissRequest = { showEndDatePicker = false },
                    confirmButton = { TextButton(onClick = { customEndDate = datePickerState.selectedDateMillis; showEndDatePicker = false }) { Text("ঠিক আছে", color = themeColor) } },
                    dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("বাতিল", color = Color.Gray) } }
                ) { DatePicker(state = datePickerState) }
            }

            // --- Task List Section ---
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600)),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "কাজের তালিকা ($selectedDateFilter)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AnimatedContent(
                        targetState = filteredPendingTasks.isEmpty(),
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                        label = "home_list_anim"
                    ) { isEmpty ->
                        if (isEmpty) {
                            Box(modifier = Modifier.fillMaxSize().padding(bottom = 60.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFFE0F2FE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Celebration,
                                            contentDescription = null,
                                            tint = themeColor,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "এই সময়ে আপনার কোনো কাজ বাকি নেই।",
                                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 100.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredPendingTasks, key = { it.id }) { task ->
                                    HomeTaskCard(
                                        task = task,
                                        modifier = Modifier.animateItem(),
                                        onStatusChange = { isCompleting ->
                                            if (isCompleting) {
                                                taskToConfirm = task // কাজ সম্পন্ন করতে চাইলে পপআপ আসবে
                                            } else {
                                                onTaskStatusChange(task, false) // আনচেক করলে সরাসরি পেন্ডিং হয়ে যাবে
                                            }
                                        },
                                        onClick = { onTaskClick(task) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Task Completion Confirmation Dialog ---
        taskToConfirm?.let { task ->
            AlertDialog(
                onDismissRequest = { taskToConfirm = null },
                containerColor = Color.White,
                title = { Text("কাজটি সম্পন্ন হয়েছে?", fontWeight = FontWeight.Bold) },
                text = { Text("আপনি কি নিশ্চিত যে '${task.title}' কাজটি পুরোপুরি সম্পন্ন হয়েছে?") },
                confirmButton = {
                    Button(
                        onClick = {
                            onTaskStatusChange(task, true)
                            taskToConfirm = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("হ্যাঁ, সম্পন্ন", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { taskToConfirm = null }) {
                        Text("না, বাকি আছে", color = Color.Gray)
                    }
                }
            )
        }

        // --- Completed Tasks Bottom Sheet ---
        if (showCompletedTasksSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showCompletedTasksSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "সম্পন্ন হওয়া কাজসমূহ",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (filteredCompletedTasks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("এই রেঞ্জে কোনো কাজ সম্পন্ন হয়নি।", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(filteredCompletedTasks, key = { it.id }) { task ->
                                HomeTaskCard(
                                    task = task,
                                    onStatusChange = { isCompleting ->
                                        if (!isCompleting) {
                                            onTaskStatusChange(task, false)
                                        }
                                    },
                                    onClick = {
                                        showCompletedTasksSheet = false
                                        onTaskClick(task)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Date Filter Dropdown Component ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDateRangeFilter(selectedOption: String, onOptionSelected: (String) -> Unit, themeColor: Color) {
    val options = listOf("সব কাজ", "আজকের কাজ", "আগামীকালের কাজ", "আগামী ৩ দিনের কাজ", "এই সপ্তাহের", "কাস্টম রেঞ্জ")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedOption,
            onValueChange = { },
            leadingIcon = { Icon(Icons.Rounded.FilterAlt, contentDescription = null, tint = themeColor) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColor,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF8FAFC)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = selectionOption,
                            fontWeight = if (selectedOption == selectionOption) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedOption == selectionOption) themeColor else Color(0xFF475569)
                        )
                    },
                    onClick = {
                        onOptionSelected(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

// --- SummaryCard ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryCard(title: String, count: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = count, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun HomeTaskCard(
    task: Task,
    modifier: Modifier = Modifier,
    onStatusChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale("bn", "BD"))
    val deadlineStr = task.deadline?.let { sdf.format(Date(it)) } ?: "ডেডলাইন নেই"

    val (tabIcon, tabColor) = when(task.tabType) {
        "Academic", "Learning" -> Icons.Rounded.AutoStories to Color(0xFF3B82F6)
        "Research" -> Icons.Rounded.Science to Color(0xFF8B5CF6)
        "Work" -> Icons.Rounded.Work to Color(0xFFF59E0B)
        else -> Icons.Rounded.Star to Color(0xFF10B981)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = tabColor.copy(alpha = 0.15f),
                ambientColor = tabColor.copy(alpha = 0.05f)
            )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(0.5.dp, tabColor.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onStatusChange(!task.isCompleted) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = "Complete",
                        tint = if (task.isCompleted) tabColor else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(tabIcon, contentDescription = null, tint = tabColor, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${task.subjectName} • $deadlineStr",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                val priorityColor = when(task.priority) {
                    "High" -> Color(0xFFF43F5E)
                    "Medium" -> Color(0xFFF59E0B)
                    else -> Color(0xFF10B981)
                }
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(priorityColor))
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}