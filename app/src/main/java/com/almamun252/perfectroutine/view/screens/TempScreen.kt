package com.almamun252.perfectroutine.view.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalDensity
import com.almamun252.perfectroutine.view.TaskScheduleFields
import com.almamun252.perfectroutine.utils.TaskSchedule
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almamun252.perfectroutine.model.Task
import com.almamun252.perfectroutine.viewmodel.TaskViewModel

@Composable
fun TempScreen(viewModel: TaskViewModel, tabId: String, tabName: String, tasks: List<Task>, onOpenTask: (Task) -> Unit) {
    val tempTasks = tasks.filter { it.tabType == tabId }.sortedByDescending { it.sortOrder ?: it.taskDate }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var quickSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var taskToConfirmComplete by remember { mutableStateOf<Task?>(null) }

    // ম্যানুয়াল সর্টিং (Drag and Drop) এর জন্য লোকাল স্টেট
    var localTasks by remember(tempTasks) { mutableStateOf(tempTasks.filter { !it.isCompleted }) }

    val latestTasks by rememberUpdatedState(localTasks)
    val dragThreshold = with(LocalDensity.current) { 48.dp.toPx() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFF59E0B), // অ্যাম্বার/অরেঞ্জ কালার
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Temp Task")
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = tabName,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "যে কাজগুলো সাময়িক, সেগুলোর লিস্ট। চেপে ধরে উপরে-নিচে সরাতে পারবেন।",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (localTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("কোনো কুইক টাস্ক নেই।", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(localTasks, key = { _, task -> task.id }) { index, task ->
                        // ড্র্যাগ করার সময় কার্ড কিছুটা ভেসে উঠবে
                        var isDragging by remember { mutableStateOf(false) }
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 2.dp, label = "drag_elev")

                        TempTaskCard(
                            task = task,
                            elevation = elevation,
                            onCompleteClick = { taskToConfirmComplete = task },
                            onOpen = { onOpenTask(task) },
                            modifier = Modifier.pointerInput(task.id) {
                                var distance = 0f
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { isDragging = true; distance = 0f },
                                    onDragEnd = { isDragging = false; viewModel.updateTasksOrder(latestTasks) },
                                    onDragCancel = { isDragging = false; viewModel.updateTasksOrder(latestTasks) },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        distance += amount.y
                                        val from = latestTasks.indexOfFirst { it.id == task.id }
                                        val direction = if (distance > dragThreshold) 1 else if (distance < -dragThreshold) -1 else 0
                                        val to = from + direction
                                        if (direction != 0 && from >= 0 && to in latestTasks.indices) {
                                            localTasks = latestTasks.toMutableList().apply { add(to, removeAt(from)) }
                                            distance = 0f
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // ১. কুইক অ্যাড টাস্ক ডায়ালগ
    if (showAddDialog) {
        val requestKey = rememberSaveable { java.util.UUID.randomUUID().toString() }
        QuickAddTempDialog(
            onDismiss = { if (!quickSaving) showAddDialog = false },
            saving = quickSaving,
            onSave = { title, note, priority, start, deadline ->
                val newTask = Task(
                    title = title,
                    description = note,
                    subjectName = "Quick", // ডিফল্ট সাবজেক্ট
                    priority = priority,
                    tabType = tabId, // এটি হোম স্ক্রিন থেকে আলাদা রাখবে
                    taskDate = System.currentTimeMillis(),
                    startDate = start,
                    deadline = deadline
                )
                if (!quickSaving) {
                    quickSaving = true
                    scope.launch {
                        try {
                            viewModel.saveTask(newTask, requestKey)
                            showAddDialog = false
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            Toast.makeText(context, "কাজ সেভ হয়নি। আবার চেষ্টা করুন।", Toast.LENGTH_LONG).show()
                        } finally {
                            quickSaving = false
                        }
                    }
                }
            }
        )
    }

    // ২. কাজ শেষের কনফার্মেশন ডায়ালগ
    taskToConfirmComplete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToConfirmComplete = null },
            containerColor = Color.White,
            title = { Text("কাজ সম্পন্ন?", fontWeight = FontWeight.Bold) },
            text = { Text("আপনি কি এই টেম্পোরারি কাজটি পুরোপুরি শেষ করেছেন? শেষ হয়ে থাকলে এটি সম্পন্ন হিসেবে রাখা হবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTaskStatus(task, true) // Keep completed tasks in history.
                        taskToConfirmComplete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("হ্যাঁ, সম্পন্ন")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToConfirmComplete = null }) {
                    Text("না, বাকি আছে", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun TempTaskCard(
    task: Task,
    elevation: androidx.compose.ui.unit.Dp,
    onCompleteClick: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityColor = when (task.priority) {
        "High" -> Color(0xFFF43F5E)
        "Medium" -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // গোল চেকবক্স
            Icon(
                imageVector = Icons.Rounded.CheckCircleOutline,
                contentDescription = "Complete Task",
                tint = Color(0xFF94A3B8),
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onCompleteClick() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            // কাজের নাম ও নোট
            Column(modifier = Modifier.weight(1f).clickable(onClick = onOpen)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(priorityColor))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
                if (!task.description.isNullOrBlank()) {
                    Text(
                        text = task.description,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp, start = 14.dp)
                    )
                }
            }

            // ড্র্যাগ হ্যান্ডেল আইকন
            Icon(
                imageVector = Icons.Rounded.DragIndicator,
                contentDescription = "Drag to reorder",
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun QuickAddTempDialog(onDismiss: () -> Unit, saving: Boolean = false, onSave: (String, String, String, Long, Long?) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf("Medium") }

    var start by rememberSaveable { mutableStateOf<Long?>(null) }
    var deadline by rememberSaveable { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("নতুন কুইক টাস্ক", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("কী কাজ?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("নোট (অপশনাল)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                TaskScheduleFields(start, deadline, !saving, { start = it; error = null }, { deadline = it; error = null })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(16.dp))
                Text("প্রায়োরিটি:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PriorityChip("High", priority, Color(0xFFF43F5E)) { priority = "High" }
                    PriorityChip("Medium", priority, Color(0xFFF59E0B)) { priority = "Medium" }
                    PriorityChip("Low", priority, Color(0xFF10B981)) { priority = "Low" }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val resolved = start ?: System.currentTimeMillis()
                    if (TaskSchedule.invalid(resolved, deadline)) error = "শেষের সময় শুরুর সময়ের আগে হতে পারে না।"
                    else if (title.isNotBlank()) onSave(title.trim(), note.trim(), priority, resolved, deadline)
                },
                enabled = !saving && title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
            ) { Text("সেভ করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text("বাতিল", color = Color.Gray) }
        }
    )
}

@Composable
fun PriorityChip(label: String, selected: String, color: Color, onClick: () -> Unit) {
    val isSelected = selected == label
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) color else Color(0xFFF1F5F9))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color.Gray
        )
    }
}