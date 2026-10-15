package com.almamun252.perfectroutine.view

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.almamun252.perfectroutine.worker.NotificationHelper
import com.almamun252.perfectroutine.worker.TaskReminderScheduler

// Ask once after installation/update; later changes are initiated from the bell panel.
@Composable
fun NotificationPermissionPrompt() {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("NotificationUi", Context.MODE_PRIVATE) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) TaskReminderScheduler.requestReconcile(context.applicationContext)
    }
    LaunchedEffect(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !prefs.getBoolean("permission_asked", false)) {
            prefs.edit().putBoolean("permission_asked", true).apply()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun NotificationSettingsPanel() {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("NotificationUi", Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(NotificationHelper.taskNotificationsEnabled(context)) }
    var exact by remember { mutableStateOf(TaskReminderScheduler.canUseExactAlarms(context)) }
    val lifecycle = (context as? LifecycleOwner)?.lifecycle
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        enabled = NotificationHelper.taskNotificationsEnabled(context)
        if (granted) TaskReminderScheduler.requestReconcile(context.applicationContext)
    }

    DisposableEffect(context, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = NotificationHelper.taskNotificationsEnabled(context)
                exact = TaskReminderScheduler.canUseExactAlarms(context)
                TaskReminderScheduler.requestReconcile(context.applicationContext)
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }

    // Keep only the next permission action visible; no debug controls or long explanations.
    val needsExactAccess = enabled && !exact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val ready = enabled && !needsExactAccess
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when {
                        !enabled -> Icons.Rounded.NotificationsOff
                        needsExactAccess -> Icons.Rounded.Alarm
                        else -> Icons.Rounded.CheckCircle
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (ready) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when {
                        !enabled -> "Notification বন্ধ"
                        needsExactAccess -> "সময়মতো reminder"
                        else -> "Reminder চালু"
                    },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!ready) {
                    TextButton(
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        onClick = {
                            if (!enabled) {
                                val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                val activity = context as? Activity
                                if (needsPermission && (!prefs.getBoolean("permission_asked", false) ||
                                    activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true)) {
                                    prefs.edit().putBoolean("permission_asked", true).apply()
                                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    openNotificationSettings(context)
                                }
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:${context.packageName}"))
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(intent)
                                }
                            }
                        }
                    ) { Text("চালু করুন") }
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

private fun openNotificationSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    })
}
