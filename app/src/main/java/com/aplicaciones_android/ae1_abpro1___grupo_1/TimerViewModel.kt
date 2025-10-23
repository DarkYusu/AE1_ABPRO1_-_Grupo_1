package com.aplicaciones_android.ae1_abpro1___grupo_1

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aplicaciones_android.ae1_abpro1___grupo_1.SingleLiveEvent

class TimerViewModel : ViewModel() {
    private val _timeLeft = MutableLiveData<Long>()
    val timeLeft: LiveData<Long> = _timeLeft

    private val _isRunning = MutableLiveData<Boolean>(false)
    val isRunning: LiveData<Boolean> = _isRunning

    private var timer: CountDownTimer? = null
    private var millisLeft: Long = 60000 // 1 minuto por defecto

    // --- Temporizador de fondo ---
    private val _bgTimeLeft = MutableLiveData<Long>()
    val bgTimeLeft: LiveData<Long> = _bgTimeLeft
    private val _bgIsRunning = MutableLiveData<Boolean>(false)
    val bgIsRunning: LiveData<Boolean> = _bgIsRunning
    private var bgDuration: Long = 60000 // 1 minuto por defecto
    private var bgEndTime: Long = 0L
    private var bgTicker: CountDownTimer? = null

    // Eventos para notificaciones
    val timerFinishedEvent = SingleLiveEvent<String>()
    val bgTimerFinishedEvent = SingleLiveEvent<String>()

    fun startTimer() {
        if (_isRunning.value == true) return
        _isRunning.value = true
        timer = object : CountDownTimer(millisLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                millisLeft = millisUntilFinished
                _timeLeft.value = millisLeft / 1000
            }
            override fun onFinish() {
                _isRunning.value = false
                _timeLeft.value = 0
                timerFinishedEvent.value = "Temporizador principal finalizado"
            }
        }.start()
    }

    fun stopTimer() {
        timer?.cancel()
        _isRunning.value = false
    }

    fun pauseTimer() {
        timer?.cancel()
        _isRunning.value = false
    }

    fun resumeTimer() {
        if (millisLeft > 0 && _isRunning.value == false) {
            startTimer()
        }
    }

    fun resetTimer() {
        stopTimer()
        millisLeft = 60000
        _timeLeft.value = millisLeft / 1000
    }

    // --- Temporizador de fondo ---
    fun startBgTimer() {
        if (_bgIsRunning.value == true) return
        bgEndTime = System.currentTimeMillis() + bgDuration
        _bgIsRunning.value = true
        startBgTicker()
    }

    fun stopBgTimer() {
        _bgIsRunning.value = false
        bgTicker?.cancel()
        _bgTimeLeft.value = bgDuration / 1000
    }

    private fun startBgTicker() {
        bgTicker?.cancel()
        bgTicker = object : CountDownTimer(bgDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val timeLeft = bgEndTime - System.currentTimeMillis()
                if (timeLeft > 0) {
                    _bgTimeLeft.value = timeLeft / 1000
                } else {
                    _bgTimeLeft.value = 0
                    _bgIsRunning.value = false
                    cancel()
                }
            }
            override fun onFinish() {
                _bgTimeLeft.value = 0
                _bgIsRunning.value = false
                bgTimerFinishedEvent.value = "Temporizador de fondo finalizado"
            }
        }.start()
    }

    fun updateBgTimerOnResume() {
        if (_bgIsRunning.value == true) {
            val timeLeft = bgEndTime - System.currentTimeMillis()
            if (timeLeft > 0) {
                bgDuration = timeLeft
                startBgTicker()
            } else {
                _bgTimeLeft.value = 0
                _bgIsRunning.value = false
            }
        }
    }

    fun setTimerDuration(seconds: Long) {
        millisLeft = seconds * 1000
        _timeLeft.value = millisLeft / 1000
    }

    fun setBgTimerDuration(seconds: Long) {
        bgDuration = seconds * 1000
        _bgTimeLeft.value = bgDuration / 1000
    }

    init {
        _timeLeft.value = millisLeft / 1000
        _bgTimeLeft.value = bgDuration / 1000
    }
}
