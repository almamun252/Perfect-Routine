package com.almamun252.perfectroutine.view.screens

import android.widget.Toast
import com.almamun252.perfectroutine.view.TaskScheduleFields
import com.almamun252.perfectroutine.utils.TaskSchedule
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import com.almamun252.perfectroutine.data.NoteImageStore
import com.almamun252.perfectroutine.view.NoteImages
import com.almamun252.perfectroutine.view.CategoryManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.almamun252.perfectroutine.model.Category
import com.almamun252.perfectroutine.model.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    navController: NavController,
    tabColor: Color = Color(0xFF3B82F6),
    tabType: String, // কোন ট্যাব থেকে এসেছে (Academic, Research ইত্যাদি)
    existingTask: Task? = null, // যদি এডিট করতে আসে
    categories: List<Category>, // ড্রপডাউনে দেখানোর জন্য আগের সাবজেক্টগুলো
    onSaveTask: suspend (Task, String) -> Unit,
    onDeleteCategory: suspend (Category) -> Unit
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    // --- Form States ---
    var title by rememberSaveable { mutableStateOf(existingTask?.title ?: "") }
    var subjectName by rememberSaveable { mutableStateOf(existingTask?.subjectName ?: "") }
    var description by rememberSaveable { mutableStateOf(existingTask?.description ?: "") }
    var priority by rememberSaveable { mutableStateOf(existingTask?.priority ?: "Medium") }

    val scope = rememberCoroutineScope()
    val saveRequestKey = rememberSaveable { java.util.UUID.randomUUID().toString() }
    var imagePaths by rememberSaveable { mutableStateOf(ArrayList(existingTask?.imagePaths.orEmpty())) }
    var importing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }
    val busy = importing || saving
    BackHandler(enabled = busy) { }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) {
            importing = true
            scope.launch {
                val added = mutableListOf<String>()
                try {
                    withContext(Dispatchers.IO) { NoteImageStore.pruneOldDrafts(context.applicationContext, imagePaths) }
                    for (uri in uris.take(10)) added += NoteImageStore.importImage(context.applicationContext, uri)
                    imagePaths = ArrayList(imagePaths + added)
                    if (uris.size > 10) Toast.makeText(context, "একবারে ১০টি ছবি যোগ করা যায়", Toast.LENGTH_SHORT).show()
                } catch (error: CancellationException) {
                    withContext(Dispatchers.IO + NonCancellable) { NoteImageStore.delete(context, added) }
                    throw error
                } catch (_: Exception) {
                    withContext(Dispatchers.IO) { NoteImageStore.delete(context, added) }
                    Toast.makeText(context, "ছবি যোগ হয়নি। অন্য ছবি দিয়ে চেষ্টা করুন।", Toast.LENGTH_LONG).show()
                } finally { importing = false }
            }
        }
    }

    // --- Date & Time States ---
    var deadlineMillis by rememberSaveable { mutableStateOf<Long?>(existingTask?.deadline) }
    var startMillis by rememberSaveable { mutableStateOf(existingTask?.let { it.startDate ?: it.taskDate }) }

    var subjectExpanded by remember { mutableStateOf(false) }

    // থিম কালার (ট্যাব অনুযায়ী পরিবর্তন হবে)
    val themeColor = tabColor

    val screenTitle = if (existingTask != null) "কাজ এডিট করুন" else "নতুন কাজ যোগ করুন"

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 150 },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding().statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // --- Top Bar ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .clickable(enabled = !busy) { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", tint = Color(0xFF334155))
                    }
                    Text(text = screenTitle, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.size(44.dp)) // ব্যালেন্স করার জন্য
                }

                // --- Form Fields ---
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // 1. কাজের নাম (Title)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("কাজের নাম (ছবি দিলে ঐচ্ছিক)") },
                        leadingIcon = { Icon(Icons.Rounded.TaskAlt, contentDescription = null, tint = themeColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColor, unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedLabelColor = themeColor, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    // 2. সাবজেক্ট / ক্যাটাগরি (Dropdown + Typed Input)
                    ExposedDropdownMenuBox(
                        expanded = subjectExpanded,
                        onExpandedChange = { subjectExpanded = !subjectExpanded }
                    ) {
                        OutlinedTextField(
                            value = subjectName,
                            onValueChange = {
                                subjectName = it
                                subjectExpanded = true // টাইপ করার সময় ড্রপডাউন ওপেন থাকবে
                            },
                            label = { Text("কোন সাবজেক্ট? (লিখুন বা সিলেক্ট করুন)") },
                            leadingIcon = { Icon(Icons.Rounded.School, contentDescription = null, tint = themeColor) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor, unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedLabelColor = themeColor, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                            ),
                            singleLine = true
                        )

                        // শুধুমাত্র যদি ক্যাটাগরি থাকে তবেই ড্রপডাউন দেখাবে
                        val filteredCategories = categories.filter { it.name.contains(subjectName, ignoreCase = true) }
                        if (filteredCategories.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = subjectExpanded,
                                onDismissRequest = { subjectExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                filteredCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.name, color = Color(0xFF1E293B)) },
                                        onClick = {
                                            subjectName = category.name
                                            subjectExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (categories.isNotEmpty()) {
                        TextButton(onClick = { showCategories = true }, enabled = !busy,
                            modifier = Modifier.align(Alignment.End)) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Category পরিচালনা")
                        }
                    }

                    TaskScheduleFields(startMillis, deadlineMillis, !busy,
                        onStart = { startMillis = it }, onDeadline = { deadlineMillis = it })

                    // 4. প্রায়োরিটি (High, Medium, Low)
                    Text("কাজের গুরুত্ব (Priority)", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.padding(top = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("High" to Color(0xFFF43F5E), "Medium" to Color(0xFFF59E0B), "Low" to Color(0xFF10B981)).forEach { (prio, color) ->
                            val isSelected = priority == prio
                            Surface(
                                onClick = { priority = prio },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) color else Color.White,
                                border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE2E8F0)) else null,
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = prio,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    // 5. বিস্তারিত নোট
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("বিস্তারিত নোট (ঐচ্ছিক)") },
                        leadingIcon = { Icon(Icons.Rounded.Subject, contentDescription = null, tint = themeColor) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColor, unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedLabelColor = themeColor, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                        )
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("নোটের ছবি", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        TextButton(enabled = !busy, onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ছবি যোগ করুন")
                        }
                    }
                    if (importing) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (imagePaths.isNotEmpty()) {
                        NoteImages(paths = imagePaths, enabled = !busy, onRemove = { path ->
                            imagePaths = ArrayList(imagePaths.filterNot { it == path })
                            scope.launch(Dispatchers.IO) { NoteImageStore.deleteDraft(context.applicationContext, path) }
                        })
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Save Button ---
                Button(
                    onClick = {
                        val finalTitle = title.trim().ifEmpty { if (imagePaths.isNotEmpty()) "ছবির নোট" else "" }
                        val finalSubject = subjectName.trim().ifEmpty { if (imagePaths.isNotEmpty()) "সাধারণ" else "" }

                        if (finalTitle.isEmpty()) {
                            Toast.makeText(context, "কাজের নাম দেওয়া আবশ্যক!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (finalSubject.isEmpty()) {
                            Toast.makeText(context, "সাবজেক্টের নাম দেওয়া আবশ্যক!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val now = System.currentTimeMillis()
                        val resolvedStart = startMillis ?: now
                        val changedSchedule = existingTask == null ||
                            resolvedStart != (existingTask.startDate ?: existingTask.taskDate) || deadlineMillis != existingTask.deadline
                        if (changedSchedule && TaskSchedule.invalid(resolvedStart, deadlineMillis)) {
                            Toast.makeText(context, "শেষের সময় শুরুর সময়ের আগে হতে পারে না।", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        // নতুন টাস্ক অবজেক্ট তৈরি
                        val task = Task(
                            id = existingTask?.id ?: 0,
                            title = finalTitle,
                            description = description.trim().ifEmpty { null },
                            subjectName = finalSubject,
                            tabType = tabType,
                            priority = priority,
                            isCompleted = existingTask?.isCompleted ?: false,
                            taskDate = existingTask?.taskDate ?: now,
                            startDate = resolvedStart,
                            sortOrder = existingTask?.sortOrder,
                            deadline = deadlineMillis,
                            imagePaths = imagePaths.toList()
                        )

                        saving = true
                        scope.launch {
                            try {
                                onSaveTask(task, saveRequestKey)
                                Toast.makeText(context, "সফলভাবে সেভ হয়েছে!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            } catch (error: CancellationException) { throw error }
                            catch (_: Exception) {
                                Toast.makeText(context, "সেভ হয়নি। আবার চেষ্টা করুন।", Toast.LENGTH_LONG).show()
                            } finally { saving = false }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                ) {
                    Text(
                        text = if (saving) "সেভ হচ্ছে…" else if (existingTask != null) "আপডেট করুন" else "সেভ করুন",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    if (showCategories) {
        CategoryManager(categories, onDeleteCategory, onDismiss = { showCategories = false })
    }

}
