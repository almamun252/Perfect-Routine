package com.almamun252.perfectroutine.view

import androidx.activity.compose.BackHandler
import com.almamun252.perfectroutine.utils.TabNavigation
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
        if (selectedTabId !in listOf(TabNavigation.HOME, TabNavigation.PROFILE) && selectedTab == null) selectedTabId = "__home__"
    }
    if (showSettings) {
        TabSettingsScreen(tabs, allTasks, taskViewModel, onBack = { showSettings = false })
        return
    }
    val navItems = TabNavigation.slots(tabs).map { id ->
        when (id) {
            TabNavigation.HOME -> NavigationItem("Home", Icons.Rounded.Home, id, Color(0xFF4668D9))
            TabNavigation.PROFILE -> NavigationItem("Profile", Icons.Rounded.Person, id, Color(0xFF4668D9))
            TabNavigation.ALL_TABS -> NavigationItem(if (visibleTabs.size > 3) "আরও" else "ট্যাব", Icons.Rounded.GridView, id, Color(0xFF4668D9))
            else -> tabs.first { it.id == id }.let { tab -> NavigationItem(tab.name, tabIcon(tab.iconKey), tab.id, Color(tab.colorArgb)) }
        }
    }
    val selectedItemIndex = navItems.indexOfFirst { it.tabName == selectedTabId }
        .let { if (it >= 0) it else navItems.indexOfFirst { it.tabName == TabNavigation.ALL_TABS }.coerceAtLeast(0) }
    BackHandler(enabled = selectedTabId != TabNavigation.HOME && !showAllTabs) { selectedTabId = TabNavigation.HOME }

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
                    val titleText = if (selectedTabId == TabNavigation.PROFILE) "Profile" else selectedTab?.name ?: "ড্যাশবোর্ড"

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
            CenteredBottomBar(navItems, selectedTabId) { id ->
                if (id == TabNavigation.ALL_TABS) showAllTabs = true else selectedTabId = id
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
                    id == TabNavigation.PROFILE -> ProfileScreen(tabs, allTasks) { showSettings = true }
                    id == "__home__" || tab == null -> {
                        Column {
                            if (tabs.isEmpty()) {
                                TextButton(onClick = { selectedTabId = TabNavigation.PROFILE; showSettings = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("নিজের ট্যাব তৈরি করুন · Profile")
                                }
                            }
                            HomeScreen(allTasks, onTaskStatusChange, onDeleteTask, tabs = tabs) {
                                navController.navigate("task_detail/${it.id}")
                            }
                        }
                    }
                    tab.isQuickList -> key(tab.id) {
                        TempScreen(taskViewModel, tab.id, tab.name, allTasks) { navController.navigate("task_detail/${it.id}") }
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
                            Icon(tabIcon(tab.iconKey), null, tint = Color(tab.colorArgb))
                            Spacer(Modifier.width(12.dp)); Text(tab.name, Modifier.weight(1f))
                        }
                    }
                    item {
                        TextButton(onClick = { showAllTabs = false; selectedTabId = TabNavigation.PROFILE; showSettings = true }) { Text("Profile · ট্যাব পরিচালনা") }
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

@Composable
private fun CenteredBottomBar(items: List<NavigationItem>, selectedId: String, onSelect: (String) -> Unit) {
    Surface(color = Color.White, shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(88.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            items.forEach { item ->
                val selected = selectedId == item.tabName ||
                    (item.tabName == TabNavigation.ALL_TABS && items.none { it.tabName == selectedId })
                Column(Modifier.weight(1f).fillMaxHeight().clickable { onSelect(item.tabName) }.padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    if (item.tabName == TabNavigation.HOME) {
                        Surface(shape = CircleShape, color = Color(0xFF4668D9), shadowElevation = if (selected) 8.dp else 3.dp,
                            modifier = Modifier.size(52.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(item.icon, item.title, tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                    } else {
                        Box(Modifier.size(width = 48.dp, height = 36.dp)
                            .background(if (selected) item.themeColor.copy(alpha = .12f) else Color.Transparent, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center) {
                            Icon(item.icon, item.title, tint = if (selected) item.themeColor else Color(0xFF8A96AA), modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(item.title, color = if (selected) item.themeColor else Color(0xFF7B879C),
                        fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
