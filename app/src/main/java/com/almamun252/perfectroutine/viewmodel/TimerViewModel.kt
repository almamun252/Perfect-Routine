package com.almamun252.perfectroutine.viewmodel

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almamun252.perfectroutine.data.RoutineRepository
import com.almamun252.perfectroutine.model.TimeSession
import com.almamun252.perfectroutine.utils.TimeFormatter
import com.almamun252.perfectroutine.service.FocusTimerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TimerMode { POMODORO, STOPWATCH }

class TimerViewModel(private val repository: RoutineRepository) : ViewModel() {

    private var timerJob: Job? = null
    private val saveMutex = Mutex()

    private val _timeMillis = MutableStateFlow(0L)
    val timeMillis: StateFlow<Long> = _timeMillis.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentMode = MutableStateFlow(TimerMode.POMODORO)
    val currentMode: StateFlow<TimerMode> = _currentMode.asStateFlow()

    // কতক্ষণ কাজ করা হয়েছে তার লাইভ স্টেট
    private val _sessionDuration = MutableStateFlow(0L)
    val sessionDuration: StateFlow<Long> = _sessionDuration.asStateFlow()

    private var currentTaskId: Int? = null
    private var sessionStartTime: Long = 0L
    private var lastTick: Long = 0L
    private var timerContext: Context? = null

    // ⚠️ ডিফল্ট টাইম 0 করে দেওয়া হলো
    private var customPomodoroTime = 0L

    init {
        resetTimer()
    }

    // কাস্টম টাইম সেট করার ফাংশন
    fun setCustomTime(minutes: Int) {
        customPomodoroTime = minutes.coerceIn(1, 1440).toLong() * 60_000L
        if (_currentMode.value == TimerMode.POMODORO && !_isRunning.value) {
            _timeMillis.value = customPomodoroTime
            _progress.value = 1f
        }
    }

    fun setTimerMode(mode: TimerMode) {
        if (_isRunning.value) return
        val accrued = _sessionDuration.value
        _currentMode.value = mode
        resetTimer()
        _sessionDuration.value = accrued
    }

    // Finish the previous task's session before showing another task's timer.
    suspend fun prepareTask(context: Context, taskId: Int) {
        if (currentTaskId == taskId) return
        if (currentTaskId != null) stopAndSaveTimer(context, currentTaskId!!)
        currentTaskId = taskId
        resetTimer()
    }

    fun toggleTimer(context: Context, taskId: Int, taskTitle: String) {
        if (_isRunning.value) {
            pauseTimer(context)
        } else {
            startTimer(context, taskId, taskTitle)
        }
    }

    private fun startTimer(context: Context, taskId: Int, taskTitle: String) {
        if (currentTaskId != taskId) {
            currentTaskId = taskId
            sessionStartTime = System.currentTimeMillis()
            _sessionDuration.value = 0L
            resetTimer()
        }

        if (_currentMode.value == TimerMode.POMODORO && _timeMillis.value <= 0L) {
            Toast.makeText(context, "আগে ফোকাসের সময় সেট করুন।", Toast.LENGTH_SHORT).show()
            return
        }
        if (_sessionDuration.value == 0L) sessionStartTime = System.currentTimeMillis()
        _isRunning.value = true
        lastTick = SystemClock.elapsedRealtime()

        val startIntent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_START
            putExtra(FocusTimerService.EXTRA_TASK_NAME, taskTitle)
        }
        try {
            ContextCompat.startForegroundService(context, startIntent)
            timerContext = context.applicationContext
        } catch (_: Exception) {
            _isRunning.value = false
            Toast.makeText(context, "টাইমার চালু হয়নি। App সামনে রেখে আবার চেষ্টা করুন।", Toast.LENGTH_LONG).show()
            return
        }

        timerJob = viewModelScope.launch {
            while (_isRunning.value) {
                delay(1000)
                advanceClock()
                if (_currentMode.value == TimerMode.POMODORO && _timeMillis.value == 0L) {
                    pauseTimer(context)
                    return@launch
                }

                val updateIntent = Intent(context, FocusTimerService::class.java).apply {
                    action = FocusTimerService.ACTION_UPDATE
                    putExtra(FocusTimerService.EXTRA_TASK_NAME, taskTitle)
                    putExtra(FocusTimerService.EXTRA_TIME_STRING, TimeFormatter.formatTimer(_timeMillis.value))
                }
                try { context.startService(updateIntent) }
                catch (_: Exception) {
                    pauseTimer(context)
                    Toast.makeText(context, "টাইমার থেমেছে। সেশন সেভ করে আবার চালু করুন।", Toast.LENGTH_LONG).show()
                    return@launch
                }
            }
        }
    }

    private fun advanceClock() {
        if (!_isRunning.value) return
        val now = SystemClock.elapsedRealtime()
        val elapsed = (now - lastTick).coerceAtLeast(0L)
        lastTick = now
        if (_currentMode.value == TimerMode.POMODORO) {
            val counted = minOf(elapsed, _timeMillis.value)
            _sessionDuration.value += counted
            _timeMillis.value -= counted
            _progress.value = if (customPomodoroTime > 0) _timeMillis.value.toFloat() / customPomodoroTime else 0f
        } else {
            _sessionDuration.value += elapsed
            _timeMillis.value += elapsed
            _progress.value = (_timeMillis.value % 60000).toFloat() / 60000f
        }
    }

    fun pauseTimer(context: Context) {
        advanceClock()
        _isRunning.value = false
        timerJob?.cancel()
        context.stopService(Intent(context, FocusTimerService::class.java))
    }

    suspend fun stopAndSaveTimer(context: Context, taskId: Int) = saveMutex.withLock {
        pauseTimer(context)
        // Complete the write and state reset together, even if this screen is rotated.
        withContext(NonCancellable) {
            if (_sessionDuration.value > 0) {
                repository.insertTimeSession(TimeSession(
                    taskId = currentTaskId ?: taskId,
                    startTime = sessionStartTime,
                    endTime = System.currentTimeMillis(),
                    duration = _sessionDuration.value
                ))
            }
            resetTimer()
            currentTaskId = null
        }
    }

    private fun resetTimer() {
        if (_currentMode.value == TimerMode.POMODORO) {
            _timeMillis.value = customPomodoroTime
            _progress.value = if (customPomodoroTime > 0) 1f else 0f
        } else {
            _timeMillis.value = 0L
            _progress.value = 0f
        }
        _sessionDuration.value = 0L
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        timerContext?.let { it.stopService(Intent(it, FocusTimerService::class.java)) }
    }
}