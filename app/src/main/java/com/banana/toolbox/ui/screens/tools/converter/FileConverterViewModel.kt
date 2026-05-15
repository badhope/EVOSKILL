package com.banana.toolbox.ui.screens.tools.converter

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banana.toolbox.domain.usecase.tools.FileConverterUseCases
import com.banana.toolbox.domain.usecase.tools.ImageFormat
import com.banana.toolbox.domain.usecase.tools.TextFormat
import com.banana.toolbox.domain.usecase.tools.WatermarkPosition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * 文件转换 ViewModel
 *
 * 封装 [FileConverterUseCases]，管理图片转换、文本转换、文档转换的 UI 状态。
 */
@HiltViewModel
class FileConverterViewModel @Inject constructor(
    private val fileConverterUseCases: FileConverterUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileConverterUiState())
    val uiState: StateFlow<FileConverterUiState> = _uiState.asStateFlow()

    // ==================== Tab 切换 ====================

    fun setActiveTab(tab: ConverterTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    // ==================== 图片转换 ====================

    /**
     * 选择图片文件
     */
    fun selectFile(uri: Uri, filePath: String) {
        val extension = filePath.substringAfterLast('.', "").lowercase()
        val sourceFormat = when (extension) {
            "jpg", "jpeg" -> ImageFormat.JPEG
            "png" -> ImageFormat.PNG
            "webp" -> ImageFormat.WEBP
            else -> ImageFormat.JPEG
        }
        _uiState.update {
            it.copy(
                selectedFile = filePath,
                selectedFileUri = uri,
                sourceFormat = sourceFormat,
                result = null,
                error = null
            )
        }
    }

    /**
     * 设置目标图片格式
     */
    fun setTargetFormat(format: ImageFormat) {
        _uiState.update { it.copy(targetFormat = format) }
    }

    /**
     * 设置压缩质量 (1-100)
     */
    fun setQuality(quality: Int) {
        _uiState.update { it.copy(quality = quality.coerceIn(1, 100)) }
    }

    /**
     * 执行图片格式转换
     */
    fun convertImage() {
        val state = _uiState.value
        val inputPath = state.selectedFile ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isConverting = true, error = null, result = null) }

            val baseName = inputPath.substringBeforeLast(".")
            val outputPath = "$baseName_converted.${state.targetFormat.extension}"

            fileConverterUseCases.convertImage(
                inputPath = inputPath,
                outputPath = outputPath,
                targetFormat = state.targetFormat,
                quality = state.quality
            ).onSuccess { resultPath ->
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        result = resultPath,
                        error = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        error = throwable.message ?: "图片转换失败"
                    )
                }
            }
        }
    }

    /**
     * 压缩图片
     */
    fun compressImage() {
        val state = _uiState.value
        val inputPath = state.selectedFile ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isConverting = true, error = null, result = null) }

            val baseName = inputPath.substringBeforeLast(".")
            val outputPath = "$baseName_compressed.jpg"

            fileConverterUseCases.compressImage(
                inputPath = inputPath,
                outputPath = outputPath,
                quality = state.quality
            ).onSuccess { compressResult ->
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        result = compressResult.outputPath,
                        compressResult = compressResult,
                        error = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        error = throwable.message ?: "图片压缩失败"
                    )
                }
            }
        }
    }

    /**
     * 添加水印
     */
    fun addWatermark(text: String, position: WatermarkPosition) {
        val state = _uiState.value
        val inputPath = state.selectedFile ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConverting = true,
                    error = null,
                    result = null,
                    watermarkText = text,
                    watermarkPosition = position
                )
            }

            val baseName = inputPath.substringBeforeLast(".")
            val outputPath = "$baseName_watermark.jpg"

            fileConverterUseCases.addWatermark(
                inputPath = inputPath,
                outputPath = outputPath,
                watermarkText = text,
                position = position
            ).onSuccess { resultPath ->
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        result = resultPath,
                        error = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        error = throwable.message ?: "添加水印失败"
                    )
                }
            }
        }
    }

    // ==================== 文本转换 ====================

    /**
     * 设置输入文本
     */
    fun setInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /**
     * 设置源文本格式
     */
    fun setSourceTextFormat(format: TextFormat) {
        _uiState.update { it.copy(sourceTextFormat = format) }
    }

    /**
     * 设置目标文本格式
     */
    fun setTargetTextFormat(format: TextFormat) {
        _uiState.update { it.copy(targetTextFormat = format) }
    }

    /**
     * 执行文本格式转换
     * 将输入文本写入临时文件，调用 UseCase 转换后读取结果
     */
    fun convertText() {
        val state = _uiState.value
        val inputText = state.inputText
        if (inputText.isBlank()) {
            _uiState.update { it.copy(error = "请输入需要转换的文本") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isConverting = true, error = null, outputText = "") }

            try {
                // 将输入文本写入临时文件
                val inputFile = File.createTempFile("input", ".${state.sourceTextFormat.extension}")
                inputFile.writeText(inputText)
                val outputFile = File.createTempFile("output", ".${state.targetTextFormat.extension}")

                fileConverterUseCases.convertTextFormat(
                    inputPath = inputFile.absolutePath,
                    outputPath = outputFile.absolutePath,
                    targetFormat = state.targetTextFormat
                ).onSuccess { resultPath ->
                    val convertedText = File(resultPath).readText()
                    _uiState.update {
                        it.copy(
                            isConverting = false,
                            outputText = convertedText,
                            error = null
                        )
                    }
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isConverting = false,
                            error = throwable.message ?: "文本转换失败"
                        )
                    }
                }

                // 清理临时文件
                inputFile.delete()
                outputFile.delete()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        error = e.message ?: "文本转换失败"
                    )
                }
            }
        }
    }

    // ==================== 通用 ====================

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 重置当前 Tab 的状态
     */
    fun resetCurrentTab() {
        _uiState.update {
            when (it.activeTab) {
                ConverterTab.IMAGE -> it.copy(
                    selectedFile = null,
                    selectedFileUri = null,
                    result = null,
                    error = null,
                    compressResult = null,
                    watermarkText = "",
                    watermarkPosition = WatermarkPosition.BOTTOM_RIGHT
                )
                ConverterTab.TEXT -> it.copy(
                    inputText = "",
                    outputText = "",
                    error = null
                )
                ConverterTab.DOCUMENT -> it.copy(error = null)
            }
        }
    }
}

// ==================== UI 状态 ====================

/**
 * 文件转换页面 UI 状态
 */
data class FileConverterUiState(
    // 通用
    val activeTab: ConverterTab = ConverterTab.IMAGE,
    val isConverting: Boolean = false,
    val error: String? = null,

    // 图片转换
    val selectedFile: String? = null,
    val selectedFileUri: Uri? = null,
    val sourceFormat: ImageFormat = ImageFormat.JPEG,
    val targetFormat: ImageFormat = ImageFormat.PNG,
    val quality: Int = 85,
    val result: String? = null,
    val compressResult: com.banana.toolbox.domain.usecase.tools.ImageCompressResult? = null,
    val watermarkText: String = "",
    val watermarkPosition: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,

    // 文本转换
    val inputText: String = "",
    val sourceTextFormat: TextFormat = TextFormat.CSV,
    val targetTextFormat: TextFormat = TextFormat.JSON,
    val outputText: String = ""
)

/**
 * 转换器 Tab 枚举
 */
enum class ConverterTab(val label: String) {
    IMAGE("图片转换"),
    TEXT("文本转换"),
    DOCUMENT("文档转换")
}
