package com.almamun252.perfectroutine.view.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almamun252.perfectroutine.model.Task
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    task: Task,
    totalTimeSpent: Long, // <--- ডাটাবেস থেকে পাওয়া মোট সময় রিসিভ করা হচ্ছে
    onNavigateBack: () -> Unit,
    onStartTimerClick: (Task) -> Unit, // ফোকাস টাইমার শুরু করতে
    onEditClick: (Task) -> Unit,       // এডিট করতে
    onCompleteClick: (Task) -> Unit,   // ডান (Done) করতে
    onDeleteClick: (Task) -> Unit      // ডিলিট করতে
) {
    // লাইভ টাইমারের জন্য স্টেট
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L) // প্রতি ১ সেকেন্ড পর পর আপডেট হবে
            currentTime = System.currentTimeMillis()
        }
    }

    // ট্যাবের থিম কালার
    val themeColor = when (task.tabType) {
        "Academic", "Learning" -> Color(0xFF3B82F6)
        "Research" -> Color(0xFF8B5CF6)
        "Work" -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    // ডিলিট কনফার্মেশন ডায়ালগ স্টেট
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = { Text("টাস্ক ডিটেইলস", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1E293B),
                    navigationIconContentColor = Color(0xFF1E293B)
                ),
                modifier = Modifier.shadow(elevation = 2.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // --- ১. লাইভ কাউন্টডাউন এবং ভার্টিক্যাল বার সেকশন ---
            CountdownSection(task = task, currentTime = currentTime, themeColor = themeColor)

            Spacer(modifier = Modifier.height(24.dp))

            // --- ২. মডার্ন অ্যাকশন বাটনস (ফোকাস, এডিট, সম্পন্ন, ডিলিট) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ফোকাস বাটন (কাজ সম্পন্ন হলে এটি গ্রে কালার হয়ে যাবে)
                ModernActionButton(
                    icon = Icons.Rounded.PlayCircle,
                    label = "ফোকাস",
                    color = if (task.isCompleted) Color(0xFF94A3B8) else themeColor,
                    modifier = Modifier.weight(1f),
                    onClick = { if (!task.isCompleted) onStartTimerClick(task) }
                )

                // এডিট বাটন
                ModernActionButton(
                    icon = Icons.Rounded.Edit,
                    label = "এডিট",
                    color = Color(0xFF64748B), // স্লেট গ্রে
                    modifier = Modifier.weight(1f),
                    onClick = { onEditClick(task) }
                )

                // সম্পন্ন বাটন
                ModernActionButton(
                    icon = if (task.isCompleted) Icons.Rounded.Replay else Icons.Rounded.CheckCircle,
                    label = if (task.isCompleted) "আনডু" else "সম্পন্ন",
                    color = if (task.isCompleted) Color(0xFFF59E0B) else Color(0xFF10B981), // আনডু হলে কমলা, সম্পন্ন হলে সবুজ
                    modifier = Modifier.weight(1f),
                    onClick = { onCompleteClick(task) }
                )

                // ডিলিট বাটন
                ModernActionButton(
                    icon = Icons.Rounded.Delete,
                    label = "ডিলিট",
                    color = Color(0xFFF43F5E), // লাল
                    modifier = Modifier.weight(1f),
                    onClick = { showDeleteDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- ৩. টাস্ক ইনফরমেশন কার্ড (এখানে মোট সময় পাঠানো হচ্ছে) ---
            TaskInfoCard(task = task, themeColor = themeColor, totalTimeSpent = totalTimeSpent)

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Delete Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = Color.White,
                title = { Text("মুছে ফেলবেন?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 18.sp) },
                text = { Text("আপনি কি নিশ্চিত যে এই কাজটি মুছে ফেলতে চান? এটি আর ফিরে পাওয়া যাবে না।", fontSize = 14.sp) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            onDeleteClick(task)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("হ্যাঁ, ডিলিট করুন") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("বাতিল", color = Color.Gray) }
                }
            )
        }
    }
}

// --- প্রিমিয়াম অ্যাকশন বাটন কম্পোনেন্ট (Glowlight Shadow সহ) ---
@Composable
fun ModernActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 12.dp, // গ্লো এর গভীরতা
                shape = RoundedCornerShape(16.dp),
                spotColor = color.copy(alpha = 0.6f), // বাটন অনুযায়ী রঙের গ্লো
                ambientColor = color.copy(alpha = 0.2f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .background(color.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
fun CountdownSection(task: Task, currentTime: Long, themeColor: Color) {
    val deadline = task.deadline

    // ডেডলাইন না থাকলে ডিফল্ট ভিউ
    if (deadline == null) {
        Card(
            modifier = Modifier.fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = themeColor.copy(alpha = 0.3f),
                    ambientColor = themeColor.copy(alpha = 0.1f)
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(themeColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AllInclusive, contentDescription = null, tint = themeColor, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("কোনো ডেডলাইন নেই", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text("আপনার সুবিধামতো কাজটি শেষ করুন।", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
        return
    }

    // টাইম ক্যালকুলেশন
    val taskCreationDate = task.taskDate
    val totalDuration = deadline - taskCreationDate
    val timeRemaining = deadline - currentTime

    // প্রগ্রেস (0.0 থেকে 1.0)
    val progress = if (timeRemaining <= 0) 0f else (timeRemaining.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

    // ডাইনামিক কালার (সময় বেশি থাকলে সবুজ, অর্ধেক হলে কমলা, কম থাকলে লাল)
    val barColor = when {
        task.isCompleted -> Color(0xFF10B981) // কাজ শেষ হলে সবুজ
        timeRemaining <= 0 -> Color(0xFFF43F5E) // সময় শেষ হলে লাল
        progress > 0.5f -> Color(0xFF10B981) // ৫০% এর বেশি সময় থাকলে সবুজ
        progress > 0.2f -> Color(0xFFF59E0B) // ২০% এর বেশি থাকলে কমলা
        else -> Color(0xFFF43F5E) // ২০% এর কম থাকলে লাল
    }

    // পালস অ্যানিমেশন (সময় কম থাকলে লাল রঙটি দপদপ করবে)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (progress < 0.2f && !task.isCompleted && timeRemaining > 0) 0.5f else 1f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    // সময় টেক্সট ফরম্যাটিং
    val days = TimeUnit.MILLISECONDS.toDays(timeRemaining)
    val hours = TimeUnit.MILLISECONDS.toHours(timeRemaining) % 24
    val mins = TimeUnit.MILLISECONDS.toMinutes(timeRemaining) % 60
    val secs = TimeUnit.MILLISECONDS.toSeconds(timeRemaining) % 60

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = barColor.copy(alpha = 0.5f), // কার্ডের শ্যাডোটিও সময়ের সাথে সাথে রঙ বদলাবে!
                ambientColor = barColor.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(140.dp).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- ভার্টিক্যাল টাইম বার ---
            Box(
                modifier = Modifier
                    .width(24.dp) // বার কিছুটা স্লিম করা হলো
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFF1F5F9)) // ট্র্যাক কালার
            ) {
                // ফিল কালার (নিচ থেকে উপরে উঠবে)
                val animatedProgress by animateFloatAsState(targetValue = progress, tween(1000), label = "bar_anim")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(if (task.isCompleted) 1f else animatedProgress)
                        .clip(RoundedCornerShape(50))
                        .background(barColor.copy(alpha = pulseAlpha))
                        .align(Alignment.BottomCenter)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // --- লাইভ কাউন্টডাউন টেক্সট ---
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = if (task.isCompleted) "কাজ সম্পন্ন হয়েছে!" else if (timeRemaining <= 0) "সময় শেষ!" else "সময় বাকি আছে:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (task.isCompleted) Color(0xFF10B981) else if (timeRemaining <= 0) Color.Red else Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (!task.isCompleted && timeRemaining > 0) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = String.format("%02d", days), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = barColor)
                        Text(text = " দিন ", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
                        Text(text = String.format("%02d", hours), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = barColor)
                        Text(text = " ঘণ্টা", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = String.format("%02d", mins), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Text(text = " মি. ", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                        Text(text = String.format("%02d", secs), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Text(text = " সে.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                    }
                } else if (task.isCompleted) {
                    Text(text = "দারুণ! আপনি নির্দিষ্ট সময়ের আগেই কাজটি শেষ করেছেন।", fontSize = 14.sp, color = Color(0xFF1E293B), lineHeight = 20.sp)
                } else {
                    Text(text = "এই কাজের ডেডলাইন পার হয়ে গেছে। দ্রুত কাজটি শেষ করার চেষ্টা করুন!", fontSize = 14.sp, color = Color(0xFF1E293B), lineHeight = 20.sp)
                }
            }
        }
    }
}

@Composable
fun TaskInfoCard(task: Task, themeColor: Color, totalTimeSpent: Long) { // <--- totalTimeSpent প্যারামিটার যোগ করা হয়েছে
    val sdf = SimpleDateFormat("dd MMM, yyyy  •  hh:mm a", Locale("bn", "BD"))
    val createdStr = sdf.format(Date(task.taskDate))
    val deadlineStr = task.deadline?.let { sdf.format(Date(it)) } ?: "সেট করা নেই"

    // মোট কাজ করা সময় ফরম্যাট করা
    val spentHours = TimeUnit.MILLISECONDS.toHours(totalTimeSpent)
    val spentMins = TimeUnit.MILLISECONDS.toMinutes(totalTimeSpent) % 60
    val spentSecs = TimeUnit.MILLISECONDS.toSeconds(totalTimeSpent) % 60
    val spentTimeStr = if (totalTimeSpent > 0) "${spentHours} ঘণ্টা, ${spentMins} মিনিট, ${spentSecs} সেকেন্ড" else "এখনো শুরু হয়নি"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = themeColor.copy(alpha = 0.3f), // থিম কালারের গ্লো
                ambientColor = themeColor.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Category, Priority & Status Badges
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(color = themeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                    Text(text = task.subjectName, color = themeColor, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val priorityColor = when(task.priority) {
                        "High" -> Color(0xFFF43F5E)
                        "Medium" -> Color(0xFFF59E0B)
                        else -> Color(0xFF10B981)
                    }
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(priorityColor))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${task.priority} Priority", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = task.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (task.isCompleted) Color.Gray else Color(0xFF1E293B),
                lineHeight = 28.sp,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(16.dp))

            // --- মোট সময়ের হিসাব (নতুন যোগ হলো) ---
            InfoRow(icon = Icons.Rounded.Timer, label = "মোট সময় দেওয়া হয়েছে", value = spentTimeStr)
            Spacer(modifier = Modifier.height(12.dp))

            // Details Row
            InfoRow(icon = Icons.Rounded.EventNote, label = "তৈরি করা হয়েছে", value = createdStr)
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(icon = Icons.Rounded.EventAvailable, label = "ডেডলাইন", value = deadlineStr)

            if (!task.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow(icon = Icons.Rounded.Notes, label = "বিস্তারিত নোট", value = task.description)
            }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 14.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)
        }
    }
}