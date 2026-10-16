package com.almamun252.perfectroutine

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.almamun252.perfectroutine.data.AppDatabase
import com.almamun252.perfectroutine.data.RoutineRepository
import com.almamun252.perfectroutine.ui.theme.PerfectRoutineTheme
import com.almamun252.perfectroutine.view.MainScreen
import com.almamun252.perfectroutine.view.DatabaseStartupGate
import com.almamun252.perfectroutine.view.NotificationPermissionPrompt
import com.almamun252.perfectroutine.view.screens.AddScreen
import com.almamun252.perfectroutine.view.screens.TaskDetailScreen
import com.almamun252.perfectroutine.view.screens.TimerScreen
import com.almamun252.perfectroutine.viewmodel.TaskViewModel
import com.almamun252.perfectroutine.viewmodel.TimerViewModel
import com.almamun252.perfectroutine.worker.NotificationHelper
import com.almamun252.perfectroutine.worker.TaskReminderScheduler

/**
 * MainActivity হলো অ্যাপের এন্ট্রি পয়েন্ট।
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ১. নোটিফিকেশন চ্যানেল তৈরি করা (রিমাইন্ডারের জন্য)
        NotificationHelper.createNotificationChannel(this)

        // ২. Room Database এবং Repository সেটআপ
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = RoutineRepository(
            context = applicationContext,
            taskDao = database.taskDao(),
            categoryDao = database.categoryDao(),
            timeSessionDao = database.timeSessionDao()
        )

        // ৩. ViewModel Factory তৈরি করা (TaskViewModel এবং TimerViewModel উভয়ের জন্য)
        val viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                    return TaskViewModel(repository) as T
                }
                if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
                    return TimerViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        enableEdgeToEdge()
        setContent {
            PerfectRoutineTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DatabaseStartupGate(database) {
                        NotificationPermissionPrompt()
                        LaunchedEffect(database) {
                            TaskReminderScheduler.requestReconcile(applicationContext)
                        }
                        // ViewModels ইনিশিয়ালাইজ করা
                        val taskViewModel: TaskViewModel = viewModel(factory = viewModelFactory)
                        val timerViewModel: TimerViewModel = viewModel(factory = viewModelFactory)

                        // ডেটাবেস থেকে স্টেটগুলো কালেক্ট করা
                        val allTasks by taskViewModel.allTasks.collectAsState()
                        val tabs by taskViewModel.tabs.collectAsState()
                        val allCategories by taskViewModel.allCategories.collectAsState()

                        // ন্যাভিগেশন সেটআপ
                        val navController = rememberNavController()

                        NavHost(
                            navController = navController,
                            startDestination = "main_screen"
                        ) {
                            // প্রধান স্ক্রিন (Tabs: Home, Learning, Research, Work, Temp)
                            composable(
                                "main_screen",
                                deepLinks = listOf(navDeepLink { uriPattern = "perfectroutine://home" })
                            ) {
                                MainScreen(
                                    navController = navController,
                                    taskViewModel = taskViewModel, // ⚠️ এই লাইনটি মিসিং ছিল, এখন যোগ করা হয়েছে
                                    allTasks = allTasks,
                                    tabs = tabs,
                                    onTaskStatusChange = { task, isCompleted ->
                                        taskViewModel.updateTaskStatus(task, isCompleted)
                                    },
                                    onDeleteTask = { task ->
                                        taskViewModel.deleteTask(task)
                                    }
                                )
                            }

                            // টাস্ক ডিটেইল স্ক্রিন
                            composable(
                                route = "task_detail/{taskId}",
                                deepLinks = listOf(navDeepLink { uriPattern = "perfectroutine://task/{taskId}" }),
                                arguments = listOf(navArgument("taskId") { type = NavType.IntType })
                            ) { backStackEntry ->
                                val taskId = backStackEntry.arguments?.getInt("taskId") ?: return@composable
                                val task = allTasks.find { it.id == taskId }

                                if (task != null) {
                                    // --- নতুন: ডাটাবেস থেকে মোট কাজের সময় কালেক্ট করা হচ্ছে ---
                                    val totalTimeSpent by taskViewModel.getTotalTimeSpentFlow(task.id).collectAsState(initial = 0L)

                                    TaskDetailScreen(
                                        task = task,
                                        tabColor = Color(tabs.firstOrNull { it.id == task.tabType }?.colorArgb ?: 0xFF3B82F6L),
                                        totalTimeSpent = totalTimeSpent ?: 0L, // প্যারামিটারটি পাস করা হলো
                                        onNavigateBack = { navController.popBackStack() },
                                        onStartTimerClick = { t ->
                                            navController.navigate("timer_screen/${t.id}")
                                        },
                                        onEditClick = { t ->
                                            navController.navigate("add_screen/${android.net.Uri.encode(t.tabType)}?taskId=${t.id}")
                                        },
                                        onCompleteClick = { t ->
                                            taskViewModel.updateTaskStatus(t, !t.isCompleted)
                                        },
                                        onDeleteClick = { t ->
                                            taskViewModel.deleteTask(t)
                                            navController.popBackStack()
                                        }
                                    )
                                }
                            }

                            // টাইমার স্ক্রিন
                            composable(
                                route = "timer_screen/{taskId}",
                                arguments = listOf(navArgument("taskId") { type = NavType.IntType })
                            ) { backStackEntry ->
                                val taskId = backStackEntry.arguments?.getInt("taskId") ?: return@composable
                                val task = allTasks.find { it.id == taskId }

                                if (task != null) {
                                    val timeMillis by timerViewModel.timeMillis.collectAsState()
                                    val sessionDuration by timerViewModel.sessionDuration.collectAsState()
                                    val isRunning by timerViewModel.isRunning.collectAsState()
                                    val progress by timerViewModel.progress.collectAsState()
                                    val currentMode by timerViewModel.currentMode.collectAsState()
                                    val context = LocalContext.current

                                    val themeColor = Color(tabs.firstOrNull { it.id == task.tabType }?.colorArgb ?: 0xFF3B82F6L)

                                    TimerScreen(
                                        taskTitle = task.title,
                                        timeMillis = timeMillis,
                                        sessionDuration = sessionDuration,
                                        isRunning = isRunning,
                                        progress = progress,
                                        currentMode = currentMode,
                                        themeColor = themeColor,
                                        onModeChange = { mode -> timerViewModel.setTimerMode(mode) },
                                        onSetCustomTime = { mins -> timerViewModel.setCustomTime(mins) },
                                        onPlayPauseClick = { timerViewModel.toggleTimer(context, task.id, task.title) },
                                        onPauseTimer = { timerViewModel.pauseTimer(context) },
                                        onSaveSession = { isCompleted ->
                                            // --- আসল ফিক্স: এখানে কমপ্লিট লজিক কাজ করবে ---
                                            if (isCompleted) {
                                                taskViewModel.updateTaskStatus(task, true) // কাজ সম্পন্ন করা হলো
                                            }
                                            timerViewModel.stopAndSaveTimer(context, task.id) // টাইম সেভ করা হলো
                                            Toast.makeText(context, "সেশন সেভ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        },
                                        onNavigateBack = {
                                            navController.popBackStack()
                                        }
                                    )
                                }
                            }

                            // কুইক অ্যাড এবং এডিট স্ক্রিন
                            composable(
                                route = "add_screen/{tabType}?taskId={taskId}",
                                deepLinks = listOf(navDeepLink { uriPattern = "perfectroutine://add/{tabType}" }),
                                arguments = listOf(
                                    navArgument("tabType") { type = NavType.StringType },
                                    navArgument("taskId") {
                                        type = NavType.IntType
                                        defaultValue = -1
                                    }
                                )
                            ) { backStackEntry ->
                                val tabType = backStackEntry.arguments?.getString("tabType") ?: "Learning"
                                val taskId = backStackEntry.arguments?.getInt("taskId") ?: -1

                                val existingTask = if (taskId != -1) {
                                    allTasks.find { it.id == taskId }
                                } else null

                                val resolvedTab = tabs.firstOrNull { it.id == (existingTask?.tabType ?: tabType) }
                                if (resolvedTab == null) {
                                    androidx.compose.foundation.layout.Column {
                                        androidx.compose.material3.Text("ট্যাবটি পাওয়া যায়নি। Settings থেকে একটি ট্যাব তৈরি বা নির্বাচন করুন।")
                                        androidx.compose.material3.TextButton(onClick = { navController.popBackStack() }) {
                                            androidx.compose.material3.Text("ফিরে যান")
                                        }
                                    }
                                    return@composable
                                }
                                androidx.compose.runtime.key(resolvedTab.id, taskId) {
                                AddScreen(
                                    navController = navController,
                                    tabType = resolvedTab.id,
                                    tabColor = Color(resolvedTab.colorArgb),
                                    existingTask = existingTask,
                                    categories = allCategories.filter { it.tabType == resolvedTab.id },
                                    onSaveTask = { task, key -> taskViewModel.saveTask(task, key) },
                                    onDeleteCategory = { category -> taskViewModel.deleteCategory(category) }
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
