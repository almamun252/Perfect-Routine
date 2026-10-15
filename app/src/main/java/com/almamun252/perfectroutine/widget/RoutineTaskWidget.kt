package com.almamun252.perfectroutine.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.Button
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.almamun252.perfectroutine.MainActivity
import com.almamun252.perfectroutine.R
import com.almamun252.perfectroutine.data.AppDatabase
import com.almamun252.perfectroutine.model.Task
import com.almamun252.perfectroutine.worker.TaskReminderScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TaskIdKey = ActionParameters.Key<Int>("routine_widget_task_id")
private val RefreshKey = longPreferencesKey("routine_widget_refresh")
private val Ink = Color(0xFF1E293B)
private val Blue = Color(0xFF2563EB)
private val Muted = Color(0xFF64748B)

// Read the same Room data as the app; each widget has independent refresh state.
class RoutineTaskWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tasksFlow = AppDatabase.getDatabase(context).taskDao().getAllTasks()
        val initialTasks = try {
            tasksFlow.first()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("RoutineTaskWidget", "Database unavailable; no reset attempted", error)
            provideContent {
                Column(modifier = GlanceModifier.fillMaxSize().background(Color.White).padding(16.dp)) {
                    Text("Perfect Routine", style = TextStyle(color = ColorProvider(Blue), fontSize = 17.sp))
                    Text("তথ্য খোলা যাচ্ছে না। App খুলে আবার চেষ্টা করুন।",
                        modifier = GlanceModifier.padding(vertical = 12.dp),
                        style = TextStyle(color = ColorProvider(Ink), fontSize = 14.sp))
                    Button("App খুলুন", actionStartActivity(screenIntent(context, "home")))
                    Button("Refresh", actionRunCallback<RefreshWidgetAction>())
                }
            }
        }
        provideContent {
            val tasks by tasksFlow.collectAsState(initialTasks)
            val tick = currentState<Preferences>()[RefreshKey] ?: 0L
            val now = maxOf(tick, System.currentTimeMillis())
            WidgetContent(context, widgetTasks(tasks, now), now)
        }
    }

    companion object {
        // A widget refresh failure must not make a successfully saved task look failed.
        suspend fun refreshAll(context: Context) {
            try {
                val appContext = context.applicationContext
                val widget = RoutineTaskWidget()
                GlanceAppWidgetManager(appContext).getGlanceIds(RoutineTaskWidget::class.java)
                    .forEach { id ->
                        updateAppWidgetState(appContext, id) {
                            it[RefreshKey] = System.currentTimeMillis()
                        }
                        widget.update(appContext, id)
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("RoutineTaskWidget", "Unable to refresh widget", error)
            }
        }
    }
}

class RoutineTaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RoutineTaskWidget()
}

// Direct activity actions work on Android 12+, without a broadcast trampoline.
private fun screenIntent(context: Context, path: String): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse("perfectroutine://$path")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

@Composable
private fun WidgetContent(context: Context, tasks: List<Task>, now: Long) {
    Column(
        modifier = GlanceModifier.fillMaxSize().background(Color.White)
            .cornerRadius(20.dp).padding(14.dp)
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = GlanceModifier.defaultWeight().clickable(
                actionStartActivity(screenIntent(context, "home"))
            )) {
                Text("Perfect Routine", style = TextStyle(color = ColorProvider(Blue),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                Text("আগামী ৩ দিন · Overdue", modifier = GlanceModifier.padding(top = 2.dp),
                    style = TextStyle(color = ColorProvider(Muted), fontSize = 11.sp), maxLines = 1)
            }
            Box(modifier = GlanceModifier.width(48.dp).height(48.dp).clickable(
                actionRunCallback<RefreshWidgetAction>()
            ), contentAlignment = Alignment.Center) {
                Image(ImageProvider(R.drawable.widget_refresh), contentDescription = "Refresh tasks",
                    modifier = GlanceModifier.size(20.dp))
            }
        }
        val overdue = tasks.count { it.deadline!! < now }
        Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp)) {
            Text("${tasks.size} pending", modifier = GlanceModifier.background(Color(0xFFF1F5F9))
                .cornerRadius(8.dp).padding(horizontal = 8.dp, vertical = 4.dp),
                style = TextStyle(color = ColorProvider(Ink), fontSize = 11.sp))
            if (overdue > 0) {
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text("$overdue overdue", modifier = GlanceModifier.background(Color(0xFFFEF2F2))
                    .cornerRadius(8.dp).padding(horizontal = 8.dp, vertical = 4.dp),
                    style = TextStyle(color = ColorProvider(Color(0xFFDC2626)), fontSize = 11.sp))
            }
        }
        if (tasks.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                contentAlignment = Alignment.Center) {
                Text("আগামী ৩ দিনের বা overdue কোনো pending task নেই।",
                    style = TextStyle(color = ColorProvider(Muted), fontSize = 14.sp))
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                items(tasks, itemId = { it.id.toLong() }) { task ->
                    WidgetTaskRow(context, task, now)
                }
            }
        }
        Row(modifier = GlanceModifier.fillMaxWidth().background(Color(0xFFEFF6FF))
            .cornerRadius(10.dp).clickable(
            actionStartActivity(screenIntent(context, "add/Learning"))
        ).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(R.drawable.widget_add), contentDescription = null,
                modifier = GlanceModifier.size(16.dp))
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text("নতুন কাজ", style = TextStyle(color = ColorProvider(Blue),
                fontSize = 13.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun WidgetTaskRow(context: Context, task: Task, now: Long) {
    val deadline = task.deadline ?: return
    val overdue = deadline < now
    val accent = if (overdue) Color(0xFFDC2626) else when (task.tabType) {
        "Research" -> Color(0xFF8B5CF6)
        "Work" -> Color(0xFFD97706)
        "Temp" -> Color(0xFF059669)
        else -> Blue
    }
    val deadlineText = SimpleDateFormat("dd MMM · hh:mm a", Locale.getDefault())
        .format(Date(deadline))
    Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 7.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth()
            .background(if (overdue) Color(0xFFFEF2F2) else Color(0xFFF8FAFC))
            .cornerRadius(12.dp).padding(start = 10.dp, end = 2.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = GlanceModifier.width(3.dp).height(38.dp).background(accent).cornerRadius(2.dp)) {}
            Spacer(modifier = GlanceModifier.width(8.dp))
            Column(modifier = GlanceModifier.defaultWeight().clickable(
                actionStartActivity(screenIntent(context, "task/${task.id}"))
            )) {
                Text(task.title, maxLines = 2, style = TextStyle(
                    color = ColorProvider(Ink), fontSize = 13.sp, fontWeight = FontWeight.Bold))
                Text("${task.tabType} · ${task.subjectName} · ${task.priority}", maxLines = 1,
                    style = TextStyle(color = ColorProvider(Muted), fontSize = 11.sp))
                Text("${if (overdue) "Overdue · " else ""}$deadlineText", maxLines = 2,
                    style = TextStyle(color = ColorProvider(accent), fontSize = 11.sp))
            }
            Spacer(modifier = GlanceModifier.width(2.dp))
            // Keep a comfortable touch target while drawing a small completion icon.
            Box(modifier = GlanceModifier.width(48.dp).height(48.dp).clickable(
                actionRunCallback<CompleteWidgetTaskAction>(
                actionParametersOf(TaskIdKey to task.id)
            )), contentAlignment = Alignment.Center) {
                Image(ImageProvider(R.drawable.widget_complete),
                    contentDescription = "Complete task: ${task.title}",
                    modifier = GlanceModifier.size(24.dp))
            }
        }
    }
}

// Completing is idempotent: repeated taps cannot toggle a completed task back.
class CompleteWidgetTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TaskIdKey] ?: return
        AppDatabase.getDatabase(context).taskDao().updateTaskStatus(taskId, true)
        TaskReminderScheduler.reconcileTask(context, taskId)
        RoutineTaskWidget.refreshAll(context)
    }
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        RoutineTaskWidget.refreshAll(context)
    }
}
