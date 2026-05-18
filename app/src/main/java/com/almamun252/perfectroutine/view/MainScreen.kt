package com.almamun252.perfectroutine.view

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.almamun252.perfectroutine.model.Task
import com.almamun252.perfectroutine.view.screens.TempScreen
import com.almamun252.perfectroutine.view.tabs.HomeScreen
import com.almamun252.perfectroutine.view.tabs.LearningScreen
import com.almamun252.perfectroutine.view.tabs.ResearchScreen
import com.almamun252.perfectroutine.view.tabs.WorkScreen
import com.almamun252.perfectroutine.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// বটম নেভিগেশন আইটেমের ডেটা ক্লাস
data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val tabName: String,
    val themeColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    taskViewModel: TaskViewModel,
    allTasks: List<Task>,
    learningTasks: List<Task>,
    researchTasks: List<Task>,
    workTasks: List<Task>,
    onTaskStatusChange: (Task, Boolean) -> Unit,
    onDeleteTask: (Task) -> Unit
) {
    // ⚠️ ডিফল্ট পজিশন ২ করে দেওয়া হলো (মাঝখানে 'Home' থাকবে)
    var selectedItemIndex by remember { mutableIntStateOf(2) }

    // ট্যাবের সিরিয়াল পরিবর্তন করা হয়েছে (Home কে মাঝখানে আনা হয়েছে)
    val navItems = listOf(
        NavigationItem("Learning", Icons.Rounded.AutoStories, "Learning", Color(0xFF3B82F6)), // 0
        NavigationItem("Research", Icons.Rounded.Science, "Research", Color(0xFF8B5CF6)),  // 1
        NavigationItem("Home", Icons.Rounded.Home, "Home", Color(0xFF0EA5E9)),       // 2 (Center Default)
        NavigationItem("Work", Icons.Rounded.Work, "Work", Color(0xFFF59E0B)),        // 3
        NavigationItem("Temp", Icons.Rounded.FlashOn, "Temp", Color(0xFFEF4444))      // 4
    )

    // --- টপ বার ও নোটিফিকেশনের স্টেট ---
    var isDarkMode by remember { mutableStateOf(false) }
    var showNotificationSheet by remember { mutableStateOf(false) }

    val upcomingReminders = remember(allTasks) {
        val currentTime = System.currentTimeMillis()
        allTasks.filter { !it.isCompleted && it.deadline != null }.filter { tx ->
            val diff = tx.deadline!! - currentTime
            diff < 86400000L * 3
        }.sortedBy { it.deadline }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            Surface(
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // বাম পাশের অংশ (ডাইনামিক টাইটেল)
                    val titleText = when (selectedItemIndex) {
                        0 -> "পড়াশোনা ও লার্নিং"
                        1 -> "রিসার্চ ড্যাশবোর্ড"
                        2 -> "ড্যাশবোর্ড" // Home
                        3 -> "ওয়ার্ক ড্যাশবোর্ড"
                        4 -> "কুইক টাস্ক (Temp)"
                        else -> "ড্যাশবোর্ড"
                    }

                    Text(
                        text = titleText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    // ডান পাশের থিম টগল এবং নোটিফিকেশন
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isDarkMode = !isDarkMode },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = "থিম পরিবর্তন",
                                tint = navItems[selectedItemIndex].themeColor
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(navItems[selectedItemIndex].themeColor.copy(alpha = 0.1f))
                                .clickable { showNotificationSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = "নোটিফিকেশন",
                                tint = navItems[selectedItemIndex].themeColor,
                                modifier = Modifier.size(20.dp)
                            )

                            if (upcomingReminders.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // --- ডাইনামিক অ্যানিমেটেড নচ (Moving Cutout) ---
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.BottomCenter
            ) {
                // প্রতিটি আইটেমের প্রস্থ
                val itemWidth = maxWidth / navItems.size

                // যেই আইটেম সিলেক্ট করা হবে, নচটি সেই পজিশনে স্লাইড করে চলে যাবে
                val animatedXOffset by animateDpAsState(
                    targetValue = itemWidth * selectedItemIndex,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                    label = "notch_anim"
                )

                // ১. ব্যাকগ্রাউন্ড সাদা বার
                Surface(
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .shadow(elevation = 16.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {}

                // ২. ন্যাভ আইটেমগুলো (Icons & Text)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems.forEachIndexed { index, item ->
                        val isSelected = selectedItemIndex == index

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { selectedItemIndex = index }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // আইটেম সিলেক্ট হলে ফ্লাট আইকন হাইড হয়ে যাবে এবং কাটআউট বাটন আসবে
                            NavItemContent(item = item, isSelected = isSelected)
                        }
                    }
                }

                // ৩. মুভিং কাটআউট এবং ফ্লোটিং বাটন
                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .height(98.dp) // 70.dp বারের উপর 28.dp ভাসমান থাকবে
                        .offset(x = animatedXOffset)
                        .align(Alignment.BottomStart)
                ) {
                    val currentItem = navItems[selectedItemIndex]
                    // কালার স্মুথ ট্রানজিশনের জন্য
                    val animatedFabColor by animateColorAsState(
                        targetValue = currentItem.themeColor,
                        animationSpec = tween(400),
                        label = "fab_color"
                    )

                    // কাটআউটের রিং (স্ক্যাফোল্ডের ব্যাকগ্রাউন্ড কালার দিয়ে সাদা বার "কাটা" হয়েছে)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF8FAFC)) // Scaffold Color
                            .padding(6.dp), // Cutout এর থিকনেস/গ্যাপ
                        contentAlignment = Alignment.Center
                    ) {
                        // আসল রঙিন ফ্লোটিং বাটন
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(8.dp, CircleShape, spotColor = animatedFabColor)
                                .clip(CircleShape)
                                .background(animatedFabColor)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { /* ক্লিক করাই আছে */ },
                            contentAlignment = Alignment.Center
                        ) {
                            // আইকন পাল্টানোর স্মুথ অ্যানিমেশন
                            AnimatedContent(
                                targetState = currentItem,
                                transitionSpec = {
                                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                                },
                                label = "fab_icon"
                            ) { item ->
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // স্ক্রিন সুইচিং (নতুন ইন্ডেক্স অনুযায়ী ম্যাপ করা হয়েছে)
            AnimatedContent(
                targetState = selectedItemIndex,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "tab_transition"
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> LearningScreen(navController, learningTasks, onTaskStatusChange, onDeleteTask) { navController.navigate("task_detail/${it.id}") }
                    1 -> ResearchScreen(navController, researchTasks, onTaskStatusChange, onDeleteTask) { navController.navigate("task_detail/${it.id}") }
                    2 -> HomeScreen(allTasks, onTaskStatusChange, onDeleteTask) { navController.navigate("task_detail/${it.id}") }
                    3 -> WorkScreen(navController, workTasks, onTaskStatusChange, onDeleteTask) { navController.navigate("task_detail/${it.id}") }
                    4 -> TempScreen(taskViewModel) { /* Timer not needed */ }
                }
            }
        }

        // --- নোটিফিকেশন বটম শিট ---
        if (showNotificationSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showNotificationSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text("আপনার নোটিফিকেশন", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(24.dp))

                    if (upcomingReminders.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.NotificationsNone, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("আপনার পেন্ডিং বা ওভারডিউ কোনো কাজ নেই।", color = Color.Gray, fontSize = 16.sp)
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                            items(upcomingReminders) { tx ->
                                val sdf = SimpleDateFormat("dd MMM, yyyy • hh:mm a", Locale("bn", "BD"))
                                val dateStr = tx.deadline?.let { sdf.format(Date(it)) } ?: ""
                                val isOverdue = tx.deadline!! < System.currentTimeMillis()

                                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (isOverdue) Color.Red else Color(0xFFF59E0B)).padding(top = 4.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isOverdue) "সতর্কতা: '${tx.title}' এর সময় পার হয়ে গেছে!" else "রিমাইন্ডার: '${tx.title}' এর ডেডলাইন কাছাকাছি।",
                                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("ডেডলাইন: $dateStr", fontSize = 13.sp, color = if (isOverdue) Color.Red.copy(alpha = 0.8f) else Color.Gray)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ⚠️ আনসিলেক্ট করা আইটেমগুলোর সাইজ ছোট করা হয়েছে যাতে স্পেসিং বেশি মনে হয়
@Composable
fun NavItemContent(item: NavigationItem, isSelected: Boolean) {
    AnimatedVisibility(
        visible = !isSelected,
        enter = fadeIn(tween(200, delayMillis = 200)) + scaleIn(tween(200, delayMillis = 200)),
        exit = fadeOut(tween(200)) + scaleOut(tween(200))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp) // সাইজ 24.dp থেকে কমিয়ে 20.dp করা হয়েছে
            )
            Spacer(modifier = Modifier.height(2.dp)) // স্পেস 4.dp থেকে কমিয়ে 2.dp করা হয়েছে
            Text(
                text = item.title,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp // ফন্ট সাইজ 11.sp থেকে কমিয়ে 10.sp করা হয়েছে
            )
        }
    }
}