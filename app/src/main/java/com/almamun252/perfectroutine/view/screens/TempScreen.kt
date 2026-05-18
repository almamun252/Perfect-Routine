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
fun TempScreen(viewModel: TaskViewModel, onNavigateToTimer: (Task) -> Unit) {
    val tempTasks by viewModel.tempTasks.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToConfirmComplete by remember { mutableStateOf<Task?>(null) }

    // ম্যানুয়াল সর্টিং (Drag and Drop) এর জন্য লোকাল স্টেট
    var localTasks by remember(tempTasks) { mutableStateOf(tempTasks) }

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
                text = "Quick Tasks ⚡",
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
                            modifier = Modifier.pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { isDragging = true },
                                    onDragEnd = {
                                        isDragging = false
                                        // ড্র্যাগ শেষ হলে ডাটাবেসে নতুন সিরিয়াল সেভ করা হবে
                                        viewModel.updateTasksOrder(localTasks)
                                    },
                                    onDragCancel = { isDragging = false },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        // নিচে নামালে (y > 0) পরের আইটেমের সাথে সোয়াপ হবে
                                        if (dragAmount.y > 20f && index < localTasks.size - 1) {
                                            val newList = localTasks.toMutableList()
                                            val temp = newList[index]
                                            newList[index] = newList[index + 1]
                                            newList[index + 1] = temp
                                            localTasks = newList
                                        }
                                        // উপরে উঠালে (y < 0) আগের আইটেমের সাথে সোয়াপ হবে
                                        else if (dragAmount.y < -20f && index > 0) {
                                            val newList = localTasks.toMutableList()
                                            val temp = newList[index]
                                            newList[index] = newList[index - 1]
                                            newList[index - 1] = temp
                                            localTasks = newList
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
        QuickAddTempDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, note, priority ->
                val newTask = Task(
                    title = title,
                    description = note,
                    subjectName = "Quick", // ডিফল্ট সাবজেক্ট
                    priority = priority,
                    tabType = "Temp", // এটি হোম স্ক্রিন থেকে আলাদা রাখবে
                    taskDate = System.currentTimeMillis()
                )
                viewModel.addTask(newTask)
                showAddDialog = false
            }
        )
    }

    // ২. কাজ শেষের কনফার্মেশন ডায়ালগ
    taskToConfirmComplete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToConfirmComplete = null },
            containerColor = Color.White,
            title = { Text("কাজ সম্পন্ন?", fontWeight = FontWeight.Bold) },
            text = { Text("আপনি কি এই টেম্পোরারি কাজটি পুরোপুরি শেষ করেছেন? শেষ হয়ে থাকলে এটি লিস্ট থেকে মুছে ফেলা হবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTask(task) // ডান হলে সরাসরি মুছে যাবে
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
            Column(modifier = Modifier.weight(1f)) {
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
fun QuickAddTempDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("নতুন কুইক টাস্ক", fontWeight = FontWeight.Bold) },
        text = {
            Column {
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
                onClick = { if (title.isNotBlank()) onSave(title, note, priority) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
            ) { Text("সেভ করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল", color = Color.Gray) }
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