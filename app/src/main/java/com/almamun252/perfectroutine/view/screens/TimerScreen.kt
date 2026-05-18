package com.almamun252.perfectroutine.view.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almamun252.perfectroutine.utils.TimeFormatter
import com.almamun252.perfectroutine.viewmodel.TimerMode
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    taskTitle: String,
    timeMillis: Long,
    sessionDuration: Long, // কতক্ষণ কাজ হলো
    isRunning: Boolean,
    progress: Float,
    currentMode: TimerMode,
    themeColor: Color = Color(0xFF10B981),
    onModeChange: (TimerMode) -> Unit,
    onSetCustomTime: (Int) -> Unit, // কাস্টম টাইম সেট করার জন্য
    onPlayPauseClick: () -> Unit,
    onPauseTimer: () -> Unit, // পপ-আপ দেখালে টাইমার পজ করতে
    onSaveSession: (Boolean) -> Unit, // টাস্ক সম্পন্ন হয়েছে কি না সেই প্যারামিটার সহ
    onNavigateBack: () -> Unit
) {
    val bgColor = Color(0xFF0F172A)
    val surfaceColor = Color(0xFF1E293B)
    val textColor = Color(0xFFF8FAFC)

    var showTimePicker by remember { mutableStateOf(false) }
    var timeInput by remember { mutableStateOf("") } // ⚠️ ডিফল্ট ইনপুট ফাঁকা করে দেওয়া হলো

    var showCompletionDialog by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "progress_anim"
    )

    Scaffold(
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = { Text("ফোকাস মোড", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = "Back", tint = textColor, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = taskTitle, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                    color = textColor, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.clip(RoundedCornerShape(50)).background(surfaceColor).padding(4.dp)) {
                    ModeToggleOption("পোমোডোরো", currentMode == TimerMode.POMODORO, themeColor, textColor) { onModeChange(TimerMode.POMODORO) }
                    ModeToggleOption("স্টপওয়াচ", currentMode == TimerMode.STOPWATCH, themeColor, textColor) { onModeChange(TimerMode.STOPWATCH) }
                }
            }

            // বিশাল টাইমার ও এডিট বাটন
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
                CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = surfaceColor, strokeWidth = 16.dp, strokeCap = StrokeCap.Round)
                CircularProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxSize(), color = themeColor, strokeWidth = 16.dp, strokeCap = StrokeCap.Round)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = TimeFormatter.formatTimer(timeMillis), fontSize = 64.sp, fontWeight = FontWeight.Bold, color = textColor)

                    // পোমোডোরো মোডে টাইমার বন্ধ থাকলে 'এডিট' আইকন দেখাবে
                    if (currentMode == TimerMode.POMODORO && !isRunning) {
                        IconButton(onClick = { showTimePicker = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit Time", tint = textColor.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            // কন্ট্রোল বাটনস
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                FloatingActionButton(
                    onClick = {
                        // ⚠️ যদি পোমোডোরো মোড হয় এবং সময় 0 হয়, তবে প্লে করার বদলে সরাসরি সময় সেট করার পপ-আপ আসবে!
                        if (currentMode == TimerMode.POMODORO && timeMillis <= 0L) {
                            showTimePicker = true
                        } else {
                            onPlayPauseClick()
                        }
                    },
                    containerColor = themeColor, contentColor = Color.White, shape = CircleShape,
                    modifier = Modifier.size(80.dp).shadow(16.dp, CircleShape, spotColor = themeColor.copy(alpha = 0.6f))
                ) {
                    Icon(if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(40.dp))
                }

                Spacer(modifier = Modifier.width(32.dp))

                Surface(
                    onClick = {
                        onPauseTimer() // প্রথমে টাইমার পজ করা হবে
                        showCompletionDialog = true // তারপর পপ-আপ দেখানো হবে
                    },
                    shape = CircleShape, color = surfaceColor, modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Stop, contentDescription = "Stop", tint = Color(0xFFF43F5E), modifier = Modifier.size(32.dp))
                    }
                }
            }
        }

        // --- টাইম সেট করার পপ-আপ (Time Picker Dialog) ---
        if (showTimePicker) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                containerColor = Color.White,
                title = { Text("কতক্ষণ কাজ করবেন?", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = timeInput,
                        onValueChange = { if (it.all { char -> char.isDigit() }) timeInput = it },
                        label = { Text("মিনিট (যেমন: 30)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val mins = timeInput.toIntOrNull() ?: 0
                        if (mins > 0) {
                            onSetCustomTime(mins)
                            showTimePicker = false
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) {
                        Text("সেট করুন", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("বাতিল", color = Color.Gray) }
                }
            )
        }

        // --- কাজ শেষের মোটিভেশনাল পপ-আপ (Completion Dialog) ---
        if (showCompletionDialog) {
            val workedMinutes = TimeUnit.MILLISECONDS.toMinutes(sessionDuration)
            AlertDialog(
                onDismissRequest = { /* বাইরে ক্লিক করে কাটা যাবে না */ },
                containerColor = Color.White,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("দারুণ কাজ!", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                    }
                },
                text = {
                    Text(
                        "আপনি একটানা ${if(workedMinutes > 0) workedMinutes else "কিছু"} মিনিট ফোকাস করেছেন।\nকাজটি কি পুরোপুরি সম্পন্ন হয়েছে?",
                        fontSize = 15.sp, color = Color(0xFF475569)
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        showCompletionDialog = false
                        onSaveSession(true) // 'হ্যাঁ' দিলে টাস্ক সম্পন্ন হবে
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))) {
                        Text("হ্যাঁ, সম্পন্ন", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        showCompletionDialog = false
                        onSaveSession(false) // 'না' দিলে পেন্ডিং লিস্টে থাকবে
                    }, border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
                        Text("না, বাকি আছে", color = Color(0xFF64748B))
                    }
                }
            )
        }
    }
}

@Composable
fun ModeToggleOption(text: String, isSelected: Boolean, selectedColor: Color, textColor: Color, onClick: () -> Unit) {
    val bgColor by animateColorAsState(if (isSelected) selectedColor else Color.Transparent, label = "bg_color")
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bgColor).clickable { onClick() }.padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = if (isSelected) Color.White else textColor.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}