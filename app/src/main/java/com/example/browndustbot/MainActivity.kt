package com.example.browndustbot

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var btnRequestOverlay: Button
    private lateinit var btnOpenAccessibility: Button
    private lateinit var btnRequestCapture: Button
    private lateinit var btnLoadConfig: Button
    private lateinit var btnStartTask: Button
    private lateinit var btnStopTask: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView

    private lateinit var screenCaptureManager: ScreenCaptureManager
    private lateinit var imageMatcher: ImageMatcher
    private lateinit var textRecognizer: GameTextRecognizer
    private var taskEngine: TaskEngine? = null
    private var currentTaskConfig: TaskConfig? = null
    private val logBuilder = StringBuilder()

    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
        private const val REQUEST_NOTIFICATION_PERMISSION = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        initComponents()
        setupButtons()
        requestNotificationPermission()
    }

    private fun bindViews() {
        btnRequestOverlay = findViewById(R.id.btnRequestOverlay)
        btnOpenAccessibility = findViewById(R.id.btnOpenAccessibility)
        btnRequestCapture = findViewById(R.id.btnRequestCapture)
        btnLoadConfig = findViewById(R.id.btnLoadConfig)
        btnStartTask = findViewById(R.id.btnStartTask)
        btnStopTask = findViewById(R.id.btnStopTask)
        tvStatus = findViewById(R.id.tvStatus)
        tvLog = findViewById(R.id.tvLog)
    }

    private fun initComponents() {
        screenCaptureManager = ScreenCaptureManager(this)
        imageMatcher = ImageMatcher()
        textRecognizer = GameTextRecognizer()
    }

    private fun setupButtons() {
        btnRequestOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                @Suppress("DEPRECATION")
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
            } else {
                toast("已有悬浮窗权限")
            }
        }

        btnOpenAccessibility.setOnClickListener {
            if (AutoClickService.instance == null) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            } else {
                toast("无障碍服务已开启")
            }
        }

        btnRequestCapture.setOnClickListener {
            val captureServiceIntent = Intent(this, ScreenCaptureService::class.java)
            startForegroundService(captureServiceIntent)
            screenCaptureManager.requestPermission(this)
        }

        btnLoadConfig.setOnClickListener {
            loadTaskConfig()
        }

        btnStartTask.setOnClickListener {
            startTask()
        }

        btnStopTask.setOnClickListener {
            taskEngine?.stopTask()
            updateStatus("已停止")
        }
    }

    private fun loadTaskConfig() {
        val configFile = File(getExternalFilesDir(null), "task_config.json")
        if (!configFile.exists()) {
            val sampleConfig = TaskConfig(
                name = "示例任务",
                targetPackage = "com.example.game",
                steps = listOf(
                    TaskStep(
                        name = "等待3秒",
                        action = ActionType.WAIT,
                        waitTimeoutMs = 3000
                    )
                ),
                loopCount = 1,
                loopDelayMs = 1000
            )
            configFile.writeText(Gson().toJson(sampleConfig))
            appendLog("已创建示例配置: ${configFile.absolutePath}")
        }

        try {
            val json = configFile.readText()
            currentTaskConfig = Gson().fromJson(json, TaskConfig::class.java)
            appendLog("已加载配置: ${currentTaskConfig?.name}")
            toast("配置加载成功")
        } catch (e: Exception) {
            appendLog("加载配置失败: ${e.message}")
            toast("加载配置失败")
        }
    }

    private fun startTask() {
        val config = currentTaskConfig
        if (config == null) {
            toast("请先加载任务配置")
            return
        }

        val clickService = AutoClickService.instance
        if (clickService == null) {
            toast("请先开启无障碍服务")
            return
        }

        if (!screenCaptureManager.isInitialized()) {
            toast("请先授权截屏权限")
            return
        }

        if (taskEngine == null) {
            taskEngine = TaskEngine(screenCaptureManager, imageMatcher, textRecognizer, clickService)
            taskEngine?.onStatusChanged = { status ->
                runOnUiThread { updateStatus(status) }
            }
            taskEngine?.onLogMessage = { message ->
                runOnUiThread { appendLog(message) }
            }
        }

        taskEngine?.startTask(config, lifecycleScope)
        updateStatus("运行中: ${config.name}")
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ScreenCaptureManager.REQUEST_CODE_SCREEN_CAPTURE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val metrics = DisplayMetrics()
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay.getMetrics(metrics)
                    screenCaptureManager.init(resultCode, data, metrics)
                    appendLog("截屏权限已授权")
                    toast("截屏权限已授权")
                } else {
                    toast("截屏权限被拒绝")
                }
            }
            REQUEST_OVERLAY_PERMISSION -> {
                if (Settings.canDrawOverlays(this)) {
                    toast("悬浮窗权限已授权")
                    startFloatingWindowService()
                } else {
                    toast("悬浮窗权限被拒绝")
                }
            }
        }
    }

    private fun startFloatingWindowService() {
        val intent = Intent(this, FloatingWindowService::class.java)
        startForegroundService(intent)
    }

    private fun updateStatus(status: String) {
        tvStatus.text = "状态：$status"
    }

    private fun appendLog(message: String) {
        logBuilder.appendLine(message)
        tvLog.text = logBuilder.toString()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        taskEngine?.stopTask()
        textRecognizer.release()
        screenCaptureManager.release()
    }
}
