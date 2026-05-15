package com.banana.toolbox.ui.screens.tools

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.banana.toolbox.domain.usecase.tools.ToolUseCases
import com.google.zxing.common.BitMatrix
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * 实用工具 ViewModel
 */
@HiltViewModel
class ToolViewModel @Inject constructor(
    private val toolUseCases: ToolUseCases
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ToolUiState())
    val uiState: StateFlow<ToolUiState> = _uiState.asStateFlow()
    
    init {
        loadDeviceInfo()
    }
    
    // ==================== 二维码 ====================
    
    fun generateQRCode(content: String, size: Int = 512) {
        toolUseCases.generateQRCode(content, size)
            .onSuccess { matrix ->
                _uiState.update { it.copy(qrCodeMatrix = matrix) }
            }
            .onFailure { error ->
                _uiState.update { it.copy(error = error.message) }
            }
    }
    
    // ==================== 单位换算 ====================
    
    fun convertLength(value: Double, from: ToolUseCases.LengthUnit, to: ToolUseCases.LengthUnit) {
        val result = ToolUseCases.UnitConverter.convertLength(value, from, to)
        _uiState.update { it.copy(conversionResult = "%.4f".format(result)) }
    }
    
    fun convertWeight(value: Double, from: ToolUseCases.WeightUnit, to: ToolUseCases.WeightUnit) {
        val result = ToolUseCases.UnitConverter.convertWeight(value, from, to)
        _uiState.update { it.copy(conversionResult = "%.4f".format(result)) }
    }
    
    fun convertTemperature(value: Double, from: ToolUseCases.TemperatureUnit, to: ToolUseCases.TemperatureUnit) {
        val result = ToolUseCases.UnitConverter.convertTemperature(value, from, to)
        _uiState.update { it.copy(conversionResult = "%.2f".format(result)) }
    }
    
    fun convertDataSize(value: Double, from: ToolUseCases.DataUnit, to: ToolUseCases.DataUnit) {
        val result = ToolUseCases.UnitConverter.convertDataSize(value, from, to)
        _uiState.update { it.copy(conversionResult = "%.2f".format(result)) }
    }
    
    // ==================== 颜色工具 ====================
    
    fun parseColor(colorString: String) {
        toolUseCases.parseColor(colorString)?.let { colorInfo ->
            _uiState.update { 
                it.copy(
                    parsedColor = colorInfo,
                    colorInput = colorString
                )
            }
        } ?: run {
            _uiState.update { it.copy(error = "无效的颜色格式") }
        }
    }
    
    // ==================== 文本处理 ====================
    
    fun textOperation(operation: TextOperation, input: String) {
        val result = when (operation) {
            TextOperation.BASE64_ENCODE -> ToolUseCases.TextProcessor.base64Encode(input)
            TextOperation.BASE64_DECODE -> ToolUseCases.TextProcessor.base64Decode(input)
            TextOperation.URL_ENCODE -> ToolUseCases.TextProcessor.urlEncode(input)
            TextOperation.URL_DECODE -> ToolUseCases.TextProcessor.urlDecode(input)
            TextOperation.MD5 -> ToolUseCases.TextProcessor.md5(input)
            TextOperation.SHA1 -> ToolUseCases.TextProcessor.sha1(input)
            TextOperation.SHA256 -> ToolUseCases.TextProcessor.sha256(input)
            TextOperation.UPPER_CASE -> ToolUseCases.TextProcessor.toUpperCase(input)
            TextOperation.LOWER_CASE -> ToolUseCases.TextProcessor.toLowerCase(input)
            TextOperation.REVERSE -> ToolUseCases.TextProcessor.reverseText(input)
            TextOperation.WORD_COUNT -> ToolUseCases.TextProcessor.countWords(input).toString()
            TextOperation.CHAR_COUNT -> ToolUseCases.TextProcessor.countChars(input).toString()
            TextOperation.LINE_COUNT -> ToolUseCases.TextProcessor.countLines(input).toString()
        }
        _uiState.update { it.copy(textResult = result) }
    }
    
    // ==================== 设备信息 ====================
    
    fun loadDeviceInfo() {
        val info = toolUseCases.getDeviceInfo()
        _uiState.update { it.copy(deviceInfo = info) }
    }
    
    // ==================== 计算器 ====================
    
    fun calculate(expression: String) {
        ToolUseCases.Calculator.evaluate(expression)?.let { result ->
            _uiState.update { 
                it.copy(
                    calcResult = if (result == result.toLong().toDouble()) {
                        result.toLong().toString()
                    } else {
                        "%.8f".format(result).trimEnd('0').trimEnd('.')
                    }
                )
            }
        } ?: run {
            _uiState.update { it.copy(calcResult = "Error") }
        }
    }
    
    fun appendToExpression(text: String) {
        _uiState.update { it.copy(calcExpression = it.calcExpression + text) }
    }
    
    fun clearCalculator() {
        _uiState.update { it.copy(calcExpression = "", calcResult = "") }
    }
    
    fun deleteLastChar() {
        _uiState.update { 
            it.copy(calcExpression = it.calcExpression.dropLast(1)) 
        }
    }
    
    // ==================== 时间戳 ====================
    
    fun timestampToDate(timestamp: Long) {
        val date = ToolUseCases.TimeUtils.timestampToDate(timestamp)
        _uiState.update { it.copy(timeResult = date) }
    }
    
    fun dateToTimestamp(date: String) {
        ToolUseCases.TimeUtils.dateToTimestamp(date)?.let { timestamp ->
            _uiState.update { it.copy(timeResult = timestamp.toString()) }
        } ?: run {
            _uiState.update { it.copy(error = "日期格式错误") }
        }
    }
    
    // ==================== 通用 ====================
    
    fun setCurrentTool(tool: ToolType) {
        _uiState.update { 
            it.copy(
                currentTool = tool,
                textResult = "",
                conversionResult = "",
                calcExpression = "",
                calcResult = "",
                timeResult = ""
            )
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI 状态
 */
data class ToolUiState(
    val currentTool: ToolType = ToolType.QR_CODE,
    
    // 二维码
    val qrCodeMatrix: BitMatrix? = null,
    
    // 单位换算
    val conversionResult: String = "",
    
    // 颜色
    val parsedColor: ToolUseCases.ColorInfo? = null,
    val colorInput: String = "",
    
    // 文本处理
    val textResult: String = "",
    
    // 设备信息
    val deviceInfo: ToolUseCases.DeviceInfo? = null,
    
    // 计算器
    val calcExpression: String = "",
    val calcResult: String = "",
    
    // 时间戳
    val timeResult: String = "",
    
    val error: String? = null
)

enum class ToolType {
    QR_CODE,
    UNIT_CONVERTER,
    COLOR_PICKER,
    TEXT_PROCESSOR,
    DEVICE_INFO,
    CALCULATOR,
    CLIPBOARD
}

enum class TextOperation {
    BASE64_ENCODE,
    BASE64_DECODE,
    URL_ENCODE,
    URL_DECODE,
    MD5,
    SHA1,
    SHA256,
    UPPER_CASE,
    LOWER_CASE,
    REVERSE,
    WORD_COUNT,
    CHAR_COUNT,
    LINE_COUNT
}
