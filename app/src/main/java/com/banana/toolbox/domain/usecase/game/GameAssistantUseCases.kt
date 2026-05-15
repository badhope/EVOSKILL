package com.banana.toolbox.domain.usecase.game

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 游戏助手用例
 * 提供悬浮窗、宏录制和回放功能
 */
@Singleton
class GameAssistantUseCases @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val macroPreferences = context.getSharedPreferences("game_macro_prefs", Context.MODE_PRIVATE)
    
    private var floatingWindowView: View? = null
    private var isFloatingWindowVisible = false
    private var isRecordingMacro = false
    private var currentMacroRecording: MutableList<MacroAction>? = null
    private var macroStartTime: Long = 0

    private val _macroRecordingState = MutableStateFlow<MacroRecording?>(null)
    val macroRecordingState: StateFlow<MacroRecording?> = _macroRecordingState.asStateFlow()

    /**
     * 显示悬浮窗
     * 在游戏中显示快捷操作悬浮窗
     */
    fun showFloatingWindow(): Result<Unit> {
        return try {
            if (isFloatingWindowVisible) {
                return Result.success(Unit)
            }

            // 创建悬浮窗视图
            // 注意：这里使用一个占位视图，实际应该使用自定义布局
            val view = View(context).apply {
                setBackgroundColor(0x66000000) // 半透明黑色背景
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 100
            }

            // 添加触摸监听实现拖动
            view.setOnTouchListener(FloatingWindowTouchListener(params))

            windowManager.addView(view, params)
            floatingWindowView = view
            isFloatingWindowVisible = true

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 隐藏悬浮窗
     */
    fun hideFloatingWindow(): Result<Unit> {
        return try {
            floatingWindowView?.let {
                windowManager.removeView(it)
                floatingWindowView = null
                isFloatingWindowVisible = false
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 开始录制宏
     * @param name 宏名称
     * @return 宏录制对象
     */
    fun recordMacro(name: String): Result<MacroRecording> {
        return try {
            if (isRecordingMacro) {
                return Result.failure(Exception("宏录制已在进行中"))
            }

            isRecordingMacro = true
            currentMacroRecording = mutableListOf()
            macroStartTime = SystemClock.elapsedRealtime()

            val recording = MacroRecording(
                id = UUID.randomUUID().toString(),
                name = name,
                startTime = System.currentTimeMillis(),
                actions = mutableListOf()
            )

            _macroRecordingState.value = recording

            Result.success(recording)
        } catch (e: Exception) {
            isRecordingMacro = false
            Result.failure(e)
        }
    }

    /**
     * 停止录制宏
     */
    fun stopRecordingMacro(): Result<MacroInfo> {
        return try {
            if (!isRecordingMacro) {
                return Result.failure(Exception("没有正在进行的宏录制"))
            }

            val actions = currentMacroRecording ?: emptyList()
            val duration = SystemClock.elapsedRealtime() - macroStartTime

            val macroInfo = MacroInfo(
                id = UUID.randomUUID().toString(),
                name = _macroRecordingState.value?.name ?: "未命名宏",
                actions = actions,
                createdAt = System.currentTimeMillis(),
                duration = duration
            )

            // 保存宏
            saveMacro(macroInfo)

            // 重置状态
            isRecordingMacro = false
            currentMacroRecording = null
            _macroRecordingState.value = null

            Result.success(macroInfo)
        } catch (e: Exception) {
            isRecordingMacro = false
            Result.failure(e)
        }
    }

    /**
     * 播放宏
     * @param macroId 宏ID
     */
    suspend fun playMacro(macroId: String): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                val macro = loadMacro(macroId)
                    ?: return@withContext Result.failure(Exception("宏不存在"))

                // 按顺序执行宏动作
                macro.actions.forEach { action ->
                    delay(action.delay)
                    executeMacroAction(action)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 删除宏
     * @param macroId 宏ID
     */
    suspend fun deleteMacro(macroId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val macros = loadAllMacros().toMutableList()
                macros.removeAll { it.id == macroId }
                saveAllMacros(macros)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 获取所有宏列表
     * @return 宏信息列表
     */
    suspend fun getMacros(): Result<List<MacroInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val macros = loadAllMacros()
                Result.success(macros)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 检查悬浮窗是否显示
     */
    fun isFloatingWindowVisible(): Boolean = isFloatingWindowVisible

    /**
     * 检查是否正在录制宏
     */
    fun isRecording(): Boolean = isRecordingMacro

    /**
     * 记录触摸事件到当前宏
     * 在游戏界面触摸事件回调中调用
     */
    fun recordTouchEvent(event: MotionEvent) {
        if (!isRecordingMacro || currentMacroRecording == null) return

        val action = when (event.action) {
            MotionEvent.ACTION_DOWN -> MacroActionType.TOUCH_DOWN
            MotionEvent.ACTION_MOVE -> MacroActionType.TOUCH_MOVE
            MotionEvent.ACTION_UP -> MacroActionType.TOUCH_UP
            else -> return
        }

        val delay = SystemClock.elapsedRealtime() - macroStartTime
        val macroAction = MacroAction(
            type = action,
            x = event.rawX,
            y = event.rawY,
            delay = delay
        )

        currentMacroRecording?.add(macroAction)
    }

    // 私有辅助方法

    private fun saveMacro(macro: MacroInfo) {
        val macros = loadAllMacros().toMutableList()
        macros.add(macro)
        saveAllMacros(macros)
    }

    private fun loadMacro(macroId: String): MacroInfo? {
        return loadAllMacros().find { it.id == macroId }
    }

    private fun loadAllMacros(): List<MacroInfo> {
        val jsonString = macroPreferences.getString("macros", "[]") ?: "[]"
        return parseMacrosFromJson(jsonString)
    }

    private fun saveAllMacros(macros: List<MacroInfo>) {
        val jsonString = macrosToJson(macros)
        macroPreferences.edit().putString("macros", jsonString).apply()
    }

    private fun parseMacrosFromJson(jsonString: String): List<MacroInfo> {
        val macros = mutableListOf<MacroInfo>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val actionsArray = jsonObject.getJSONArray("actions")
                val actions = mutableListOf<MacroAction>()
                
                for (j in 0 until actionsArray.length()) {
                    val actionObj = actionsArray.getJSONObject(j)
                    actions.add(
                        MacroAction(
                            type = MacroActionType.valueOf(actionObj.getString("type")),
                            x = actionObj.getDouble("x").toFloat(),
                            y = actionObj.getDouble("y").toFloat(),
                            delay = actionObj.getLong("delay")
                        )
                    )
                }

                macros.add(
                    MacroInfo(
                        id = jsonObject.getString("id"),
                        name = jsonObject.getString("name"),
                        actions = actions,
                        createdAt = jsonObject.getLong("createdAt"),
                        duration = jsonObject.getLong("duration")
                    )
                )
            }
        } catch (e: Exception) {
            // 解析失败返回空列表
        }
        return macros
    }

    private fun macrosToJson(macros: List<MacroInfo>): String {
        val jsonArray = JSONArray()
        macros.forEach { macro ->
            val actionsArray = JSONArray()
            macro.actions.forEach { action ->
                val actionObj = JSONObject().apply {
                    put("type", action.type.name)
                    put("x", action.x)
                    put("y", action.y)
                    put("delay", action.delay)
                }
                actionsArray.put(actionObj)
            }

            val jsonObject = JSONObject().apply {
                put("id", macro.id)
                put("name", macro.name)
                put("actions", actionsArray)
                put("createdAt", macro.createdAt)
                put("duration", macro.duration)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    private fun executeMacroAction(action: MacroAction) {
        // 实际执行宏动作
        // 这里需要注入触摸事件到系统
        // 需要Root权限或系统权限才能实现
    }

    /**
     * 悬浮窗触摸监听器
     * 实现悬浮窗拖动功能
     */
    private inner class FloatingWindowTouchListener(
        private val params: WindowManager.LayoutParams
    ) : View.OnTouchListener {

        private var initialX = 0
        private var initialY = 0
        private var touchX = 0f
        private var touchY = 0f

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    return true
                }
            }
            return false
        }
    }
}

/**
 * 宏动作类型
 */
enum class MacroActionType {
    TOUCH_DOWN,  // 触摸按下
    TOUCH_MOVE,  // 触摸移动
    TOUCH_UP,    // 触摸抬起
    KEY_PRESS,   // 按键按下
    KEY_RELEASE, // 按键释放
    DELAY        // 延迟等待
}

/**
 * 宏动作数据类
 * @property type 动作类型
 * @property x 触摸X坐标
 * @property y 触摸Y坐标
 * @property delay 延迟时间（毫秒）
 */
data class MacroAction(
    val type: MacroActionType,
    val x: Float = 0f,
    val y: Float = 0f,
    val delay: Long = 0
)

/**
 * 宏录制状态数据类
 * @property id 录制ID
 * @property name 宏名称
 * @property startTime 开始时间
 * @property actions 已录制的动作列表
 */
data class MacroRecording(
    val id: String,
    val name: String,
    val startTime: Long,
    val actions: MutableList<MacroAction>
)

/**
 * 宏信息数据类
 * @property id 宏ID
 * @property name 宏名称
 * @property actions 动作列表
 * @property createdAt 创建时间戳
 * @property duration 录制时长（毫秒）
 */
data class MacroInfo(
    val id: String,
    val name: String,
    val actions: List<MacroAction>,
    val createdAt: Long,
    val duration: Long
) {
    /**
     * 获取格式化的创建时间
     */
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(createdAt))
    }

    /**
     * 获取格式化的时长
     */
    fun getFormattedDuration(): String {
        val seconds = duration / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
    }

    /**
     * 获取动作数量
     */
    fun getActionCount(): Int = actions.size
}
