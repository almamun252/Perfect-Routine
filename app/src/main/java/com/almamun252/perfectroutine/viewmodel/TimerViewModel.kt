package com.almamun252.perfectroutine.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almamun252.perfectroutine.data.RoutineRepository
import com.almamun252.perfectroutine.model.TimeSession
import com.almamun252.perfectroutine.utils.TimeFormatter
import com.almamun252.perfectroutine.service.FocusTimerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TimerMode { POMODORO, STOPWATCH }

class TimerViewModel(private val repository: RoutineRepository) : ViewModel() {

    private var timerJob: Job? = null

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

    // ⚠️ ডিফল্ট টাইম 0 করে দেওয়া হলো
    private var customPomodoroTime = 0L

    init {
        resetTimer()
    }

    // কাস্টম টাইম সেট করার ফাংশন
    fun setCustomTime(minutes: Int) {
        customPomodoroTime = minutes * 60 * 1000L
        if (_currentMode.value == TimerMode.POMODORO && !_isRunning.value) {
            _timeMillis.value = customPomodoroTime
            _progress.value = 1f
        }
    }

    fun setTimerMode(mode: TimerMode) {
        if (_isRunning.value) return
        _currentMode.value = mode
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

        _isRunning.value = true

        val startIntent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_START
            putExtra(FocusTimerService.EXTRA_TASK_NAME, taskTitle)
        }
        ContextCompat.startForegroundService(context, startIntent)

        timerJob = viewModelScope.launch {
            while (_isRunning.value) {
                delay(1000)
                _sessionDuration.value += 1000 // কাজের সময় যোগ হচ্ছে

                if (_currentMode.value == TimerMode.POMODORO) {
                    if (_timeMillis.value > 0) {
                        _timeMillis.value -= 1000
                        // 0 দিয়ে ভাগ হওয়া এড়াতে চেক
                        _progress.value = if (customPomodoroTime > 0) _timeMillis.value.toFloat() / customPomodoroTime.toFloat() else 0f
                    } else {
                        pauseTimer(context) // সময় শেষ হলে শুধু পজ হবে
                    }
                } else {
                    _timeMillis.value += 1000
                    _progress.value = (_timeMillis.value % 60000).toFloat() / 60000f
                }

                val updateIntent = Intent(context, FocusTimerService::class.java).apply {
                    action = FocusTimerService.ACTION_UPDATE
                    putExtra(FocusTimerService.EXTRA_TASK_NAME, taskTitle)
                    putExtra(FocusTimerService.EXTRA_TIME_STRING, TimeFormatter.formatTimer(_timeMillis.value))
                }
                context.startService(updateIntent)
            }
        }
    }

    fun pauseTimer(context: Context) {
        _isRunning.value = false
        timerJob?.cancel()
        val stopIntent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_STOP
        }
        context.startService(stopIntent)
    }

    fun stopAndSaveTimer(context: Context, taskId: Int) {
        pauseTimer(context)

        // যদি অন্তত ১ সেকেন্ডও কাজ করে থাকে, তবে সেটি সেভ হবে
        if (_sessionDuration.value > 0) {
            val timeSession = TimeSession(
                taskId = taskId,
                startTime = sessionStartTime,
                endTime = System.currentTimeMillis(),
                duration = _sessionDuration.value
            )
            viewModelScope.launch {
                repository.insertTimeSession(timeSession)
            }
        }
        resetTimer()
        currentTaskId = null
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
    }
}