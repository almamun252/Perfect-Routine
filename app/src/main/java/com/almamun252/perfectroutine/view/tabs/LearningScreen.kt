package com.almamun252.perfectroutine.view.tabs

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.almamun252.perfectroutine.model.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LearningScreen(
    navController: NavController,
    tasks: List<Task>,
    onTaskStatusChange: (Task, Boolean) -> Unit,
    onDeleteTask: (Task) -> Unit, // যদিও ডিলিট এখন ডিটেইল স্ক্রিনে হবে, মেইন স্ক্রিনের সাথে সিঙ্ক রাখতে এটি রেখে দেওয়া হলো
    onTaskClick: (Task) -> Unit   // সরাসরি ডিটেইল স্ক্রিনে যাওয়ার জন্য
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    // Tab State: 0 = Pending, 1 = Completed
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    // Theme Colors
    val themeColor = Color(0xFF3B82F6) // Blue
    val lightThemeColor = Color(0xFFDBEAFE)
    val bgColor = Color(0xFFF8FAFC)

    // Filtering Logic
    val filteredTasks = tasks.filter { tx ->
        val searchMatch = searchQuery.isEmpty() ||
                tx.title.contains(searchQuery, ignoreCase = true) ||
                tx.subjectName.contains(searchQuery, ignoreCase = true)

        val tabMatch = if (selectedTabIndex == 0) !tx.isCompleted else tx.isCompleted
        searchMatch && tabMatch
    }.sortedBy { it.deadline ?: Long.MAX_VALUE }

    val pendingCount = tasks.count { !it.isCompleted }
    val completedCount = tasks.count { it.isCompleted }

    Scaffold(
        containerColor = bgColor
        // Floating Action Button রিমুভ করা হয়েছে
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Header & Tabs ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                CustomTaskTab(
                    selectedIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    pendingCount = pendingCount,
                    completedCount = completedCount,
                    themeColor = themeColor
                )
            }

            // --- Filters & List Section ---
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600)),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Modern Search Bar & Add Button (পাশাপাশি)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("খুঁজুন...", color = Color.Gray, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = themeColor, modifier = Modifier.size(20.dp)) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp), // স্লিম হাইট
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.4f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Add Button with Glow Light Shadow
                        Button(
                            onClick = { navController.navigate("add_screen/Learning") },
                            modifier = Modifier
                                .height(50.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    spotColor = themeColor, // গ্লো ইফেক্ট
                                    ambientColor = themeColor
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("নতুন লার্নিং", fontWeight = FontWeight.Bold, fontSize = 12.sp) // ছোট টেক্সট সাইজ
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // List View
                    AnimatedContent(
                        targetState = filteredTasks.isEmpty(),
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                        label = "list_anim"
                    ) { isEmpty ->
                        if (isEmpty) {
                            Box(modifier = Modifier.fillMaxSize().padding(bottom = 60.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier.size(80.dp).clip(CircleShape).background(lightThemeColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (selectedTabIndex == 0) Icons.Rounded.Assignment else Icons.Rounded.TaskAlt,
                                            contentDescription = null,
                                            tint = themeColor.copy(alpha = 0.6f),
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (selectedTabIndex == 0) "কোনো পেন্ডিং কাজ নেই!" else "কোনো কাজ সম্পন্ন হয়নি!",
                                        fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredTasks, key = { it.id }) { task ->
                                    ModernTaskCard(
                                        task = task,
                                        themeColor = themeColor,
                                        modifier = Modifier.animateItem(),
                                        onStatusChange = { onTaskStatusChange(task, it) },
                                        onClick = { onTaskClick(task) } // <--- সরাসরি ডিটেইল স্ক্রিনে যাবে
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Minimalist Tab Switcher ---
@Composable
fun CustomTaskTab(selectedIndex: Int, onTabSelected: (Int) -> Unit, pendingCount: Int, completedCount: Int, themeColor: Color) {
    val tabTitles = listOf("পেন্ডিং ($pendingCount)", "সম্পন্ন ($completedCount)")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FAFC))
            .border(BorderStroke(1.dp, Color(0xFFF1F5F9)), RoundedCornerShape(14.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabTitles.forEachIndexed { index, title ->
            val isSelected = selectedIndex == index

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(if (isSelected) Modifier.shadow(1.dp, RoundedCornerShape(10.dp), spotColor = Color.LightGray) else Modifier)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp, // টেক্সট সাইজ ক্লাসিক করা হলো
                    color = if (isSelected) themeColor else Color(0xFF94A3B8)
                )
            }
        }
    }
}

// --- Modern Task Card with Glow Shadow ---
@Composable
fun ModernTaskCard(
    task: Task,
    themeColor: Color,
    modifier: Modifier = Modifier,
    onStatusChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale("bn", "BD"))
    val deadlineStr = task.deadline?.let { sdf.format(Date(it)) } ?: "ডেডলাইন নেই"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp, // গ্লো লাইট শ্যাডো
                shape = RoundedCornerShape(14.dp),
                spotColor = themeColor.copy(alpha = 0.25f), // রঙিন শ্যাডো
                ambientColor = themeColor.copy(alpha = 0.1f)
            )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(0.5.dp, themeColor.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onStatusChange(!task.isCompleted) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = "Complete Task",
                        tint = if (task.isCompleted) themeColor else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 14.sp, // ক্লাসিক এবং প্রিমিয়াম ফন্ট সাইজ
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) Color.Gray else Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${task.subjectName} • $deadlineStr",
                        fontSize = 11.sp, // সাবটাইটেল সাইজ
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }

                val priorityColor = when(task.priority) {
                    "High" -> Color(0xFFF43F5E)
                    "Medium" -> Color(0xFFF59E0B)
                    else -> Color(0xFF10B981)
                }
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(priorityColor))
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}