package com.aplicaciones_android.ae1_abpro1___grupo_1

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import android.text.Editable
import android.text.TextWatcher
import android.os.PowerManager
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val timerViewModel: TimerViewModel by viewModels()
    private lateinit var timerTextView: TextView
    private lateinit var startStopButton: Button
    private lateinit var bgTimerTextView: TextView
    private lateinit var bgStartStopButton: Button
    private lateinit var timerInput: EditText
    private lateinit var bgTimerInput: EditText
    private lateinit var timerInputStartButton: Button
    private lateinit var bgTimerInputStartButton: Button
    private lateinit var startBothButton: Button

    private val CHANNEL_ID = "timer_notifications"
    private val NOTIF_ID_MAIN = 1
    private val NOTIF_ID_BG = 2

    private fun acquireWakeLock() {
        val powerManager = getSystemService(PowerManager::class.java)
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AE1_ABPRO1::TimerWakeLock"
        )
        wakeLock.acquire(10 * 60 * 1000L /* 10 minutos */)
    }

    private fun releaseWakeLock() {
        // Eliminado el parámetro wakeLock porque siempre es null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        Log.d("CICLO_VIDA", "onCreate ejecutado")
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        timerTextView = findViewById(R.id.timerTextView)
        startStopButton = findViewById(R.id.startStopButton)
        bgTimerTextView = findViewById(R.id.bgTimerTextView)
        bgStartStopButton = findViewById(R.id.bgStartStopButton)
        timerInput = findViewById(R.id.timerInput)
        bgTimerInput = findViewById(R.id.bgTimerInput)
        timerInputStartButton = findViewById(R.id.timerInputStartButton)
        bgTimerInputStartButton = findViewById(R.id.bgTimerInputStartButton)
        startBothButton = findViewById(R.id.startBothButton)

        createNotificationChannel()
        // Solicitar permiso de notificaciones si es Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        // Observar eventos de finalización de temporizadores
        timerViewModel.timerFinishedEvent.observe(this, Observer<String> { msg ->
            showNotification(msg, NOTIF_ID_MAIN)
        })
        timerViewModel.bgTimerFinishedEvent.observe(this, Observer<String> { msg ->
            showNotification(msg, NOTIF_ID_BG)
        })

        timerViewModel.timeLeft.observe(this, Observer { seconds ->
            timerTextView.text = formatSecondsToMMSS(seconds)
        })
        timerViewModel.isRunning.observe(this, Observer { running ->
            startStopButton.text = if (running) "Detener" else "Iniciar"
        })
        startStopButton.setOnClickListener {
            val input = parseTimeInput(timerInput.text.toString())
            if (timerViewModel.isRunning.value == true) {
                timerViewModel.stopTimer()
            } else {
                if (input != null && input > 0) {
                    timerViewModel.setTimerDuration(input)
                }
                timerViewModel.startTimer()
            }
        }
        timerViewModel.bgTimeLeft.observe(this, Observer { seconds ->
            bgTimerTextView.text = formatSecondsToMMSS(seconds)
        })
        timerViewModel.bgIsRunning.observe(this, Observer { running ->
            bgStartStopButton.text = if (running) "Detener fondo" else "Iniciar fondo"
        })
        bgStartStopButton.setOnClickListener {
            val input = parseTimeInput(bgTimerInput.text.toString())
            if (timerViewModel.bgIsRunning.value == true) {
                timerViewModel.stopBgTimer()
            } else {
                if (input != null && input > 0) {
                    timerViewModel.setBgTimerDuration(input)
                }
                timerViewModel.startBgTimer()
            }
        }
        // Mostrar/ocultar botón iniciar principal
        timerInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                timerInputStartButton.visibility =
                    if (parseTimeInput(s.toString()) != null && s.toString().isNotEmpty()) View.VISIBLE else View.GONE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        // Mostrar/ocultar botón iniciar fondo
        bgTimerInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                bgTimerInputStartButton.visibility =
                    if (parseTimeInput(s.toString()) != null && s.toString().isNotEmpty()) View.VISIBLE else View.GONE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        timerInputStartButton.setOnClickListener {
            val input = parseTimeInput(timerInput.text.toString())
            if (input != null && input > 0) {
                timerViewModel.stopTimer()
                timerViewModel.setTimerDuration(input)
                timerViewModel.startTimer()
            }
            // Ocultar teclado
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(timerInput.windowToken, 0)
        }
        bgTimerInputStartButton.setOnClickListener {
            val input = parseTimeInput(bgTimerInput.text.toString())
            if (input != null && input > 0) {
                timerViewModel.stopBgTimer()
                timerViewModel.setBgTimerDuration(input)
                timerViewModel.startBgTimer()
            }
            // Ocultar teclado
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(bgTimerInput.windowToken, 0)
        }
        startBothButton.setOnClickListener {
            val input1 = parseTimeInput(timerInput.text.toString())
            val input2 = parseTimeInput(bgTimerInput.text.toString())
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            // Detener ambos timers
            timerViewModel.stopTimer()
            timerViewModel.stopBgTimer()
            // Setear duración si es válida
            if (input1 != null && input1 > 0) {
                timerViewModel.setTimerDuration(input1)
            }
            if (input2 != null && input2 > 0) {
                timerViewModel.setBgTimerDuration(input2)
            }
            // Iniciar ambos timers
            if (input1 != null && input1 > 0) {
                timerViewModel.startTimer()
            }
            if (input2 != null && input2 > 0) {
                timerViewModel.startBgTimer()
            }
            // Ocultar teclado de ambos campos
            imm.hideSoftInputFromWindow(timerInput.windowToken, 0)
            imm.hideSoftInputFromWindow(bgTimerInput.windowToken, 0)
        }
        acquireWakeLock()
    }

    private fun parseTimeInput(input: String): Long? {
        val parts = input.split(":")
        return try {
            when (parts.size) {
                2 -> {
                    val min = parts[0].toLong()
                    val sec = parts[1].toLong()
                    min * 60 + sec
                }
                1 -> parts[0].toLong()
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatSecondsToMMSS(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notificaciones de temporizador"
            val descriptionText = "Avisos cuando un temporizador finaliza"
            val importance = NotificationManager.IMPORTANCE_HIGH // heads-up
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existingChannel != null) {
                if (existingChannel.importance != NotificationManager.IMPORTANCE_HIGH) {
                    Log.d("MainActivity", "El canal existente no tiene la importancia correcta. Se recreará.")
                    notificationManager.deleteNotificationChannel(CHANNEL_ID)
                    notificationManager.createNotificationChannel(channel)
                } else {
                    Log.d("MainActivity", "El canal existente tiene la importancia correcta.")
                }
            } else {
                Log.d("MainActivity", "Creando un nuevo canal de notificaciones.")
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun showNotification(message: String, notifId: Int) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w("MainActivity", "Permiso de notificaciones no otorgado. No se puede mostrar la notificación.")
            return
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Temporizador Finalizado")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX) // Asegurar heads-up
            .setCategory(NotificationCompat.CATEGORY_ALARM) // Categoría de alarma para forzar heads-up
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Asegurar visibilidad pública
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sonido, vibración y luz
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true) // Forzar heads-up notification

        Log.d("MainActivity", "Mostrando notificación con ID: $notifId y mensaje: $message")
        Log.d("MainActivity", "Permisos de notificación otorgados: ${ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED}")
        Log.d("MainActivity", "Configuración del canal: ${NotificationManagerCompat.from(this).areNotificationsEnabled()}")

        try {
            with(NotificationManagerCompat.from(this)) {
                notify(notifId, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e("MainActivity", "Error al mostrar la notificación: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Liberar el WakeLock al destruir la actividad
        releaseWakeLock()
        Log.d("CICLO_VIDA", "onDestroy ejecutado")
    }
    override fun onStart() {
        super.onStart()
        Log.d("CICLO_VIDA", "onStart ejecutado")
    }

    override fun onResume() {
        super.onResume()
        Log.d("CICLO_VIDA", "onResume ejecutado")
        timerViewModel.resumeTimer()
        timerViewModel.updateBgTimerOnResume()

    }

    override fun onPause() {
        super.onPause()
        Log.d("CICLO_VIDA", "onPause ejecutado")
    }

    override fun onStop() {
        super.onStop()
        Log.d("CICLO_VIDA", "onStop ejecutado")
        timerViewModel.pauseTimer()
    }
}