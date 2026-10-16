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
import androidx.compose.runtime.saveable.rememberSaveable
import com.almamun252.perfectroutine.model.UserTab
import com.almamun252.perfectroutine.view.tabs.TaskListScreen
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
    tabs: List<UserTab>,
    onTaskStatusChange: (Task, Boolean) -> Unit,
    onDeleteTask: (Task) -> Unit
) {
    var selectedTabId by rememberSaveable { mutableStateOf("__home__") }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showAllTabs by remember { mutableStateOf(false) }
    val visibleTabs = tabs.filter { it.isVisible }
    val selectedTab = tabs.firstOrNull { it.id == selectedTabId && it.isVisible }
    LaunchedEffect(tabs, selectedTabId) {
        if (selectedTabId != "__home__" && selectedTab == null) selectedTabId = "__home__"
    }
    if (showSettings) {
        TabSettingsScreen(tabs, allTasks, taskViewModel, onBack = { showSettings = false })
        return
    }
    val shownTabs = if (visibleTabs.size > 4) visibleTabs.take(3) else visibleTabs
    val navItems = buildList {
        add(NavigationItem("Home", Icons.Rounded.Home, "__home__", Color(0xFF0EA5E9)))
        shownTabs.forEach { tab ->
            add(NavigationItem(tab.name, if (tab.isQuickList) Icons.Rounded.FlashOn else Icons.Rounded.Folder,
                tab.id, Color(tab.colorArgb)))
        }
        if (visibleTabs.size > 4) add(NavigationItem("আরও", Icons.Rounded.MoreHoriz, "__more__", Color(0xFF64748B)))
    }
    val selectedItemIndex = navItems.indexOfFirst { it.tabName == selectedTabId }
        .let { if (it >= 0) it else navItems.lastIndex }

    // --- টপ বার ও নোটিফিকেশনের স্টেট ---
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
                    val titleText = selectedTab?.name ?: "ড্যাশবোর্ড"

                    Text(
                        text = titleText,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    // ডান পাশের থিম টগল এবং নোটিফিকেশন
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings ও ট্যাব পরিচালনা",
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
                                    onClick = { if (item.tabName == "__more__") showAllTabs = true else selectedTabId = item.tabName }
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
                                ) { if (currentItem.tabName == "__more__") showAllTabs = true },
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
                targetState = selectedTabId,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "tab_transition"
            ) { id ->
                val tab = tabs.firstOrNull { it.id == id }
                when {
                    id == "__home__" || tab == null -> {
                        Column {
                            if (tabs.isEmpty()) {
                                TextButton(onClick = { showSettings = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("আপনার প্রথম ট্যাব তৈরি করুন · Settings")
                                }
                            }
                            HomeScreen(allTasks, onTaskStatusChange, onDeleteTask, tabs = tabs) {
                                navController.navigate("task_detail/${it.id}")
                            }
                        }
                    }
                    tab.isQuickList -> key(tab.id) {
                        TempScreen(taskViewModel, tab.id, tab.name, allTasks) { }
                    }
                    else -> key(tab.id) {
                        TaskListScreen(navController, allTasks.filter { it.tabType == tab.id },
                            onTaskStatusChange, onDeleteTask,
                            { navController.navigate("task_detail/${it.id}") }, tab.id, Color(tab.colorArgb))
                    }
                }
            }
        }
        if (showAllTabs) {
            ModalBottomSheet(onDismissRequest = { showAllTabs = false }) {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    item { Text("আমার ট্যাব", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                    items(visibleTabs, key = { it.id }) { tab ->
                        TextButton(onClick = { selectedTabId = tab.id; showAllTabs = false }, modifier = Modifier.fillMaxWidth()) {
                            Icon(if (tab.isQuickList) Icons.Rounded.FlashOn else Icons.Rounded.Folder, null, tint = Color(tab.colorArgb))
                            Spacer(Modifier.width(12.dp)); Text(tab.name, Modifier.weight(1f))
                        }
                    }
                    item {
                        TextButton(onClick = { showAllTabs = false; showSettings = true }) { Text("Settings · ট্যাব পরিচালনা") }
                    }
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
                    Spacer(modifier = Modifier.height(12.dp))
                    NotificationSettingsPanel()

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
