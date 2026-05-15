package com.banana.toolbox.ui.screens.tools.generator

import androidx.lifecycle.ViewModel
import com.banana.toolbox.domain.usecase.tools.GeneratorUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * 生成工具 ViewModel
 *
 * 封装 [GeneratorUseCases]，管理 UUID 生成、条形码、调色板、随机数据、批量生成的 UI 状态。
 */
@HiltViewModel
class GeneratorViewModel @Inject constructor(
    private val generatorUseCases: GeneratorUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    // ==================== Tab 切换 ====================

    fun setActiveTab(tab: GeneratorTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    // ==================== UUID 生成 ====================

    fun setUuidVersion(version: GeneratorUseCases.UuidVersion) {
        _uiState.update { it.copy(uuidVersion = version) }
    }

    fun setUuidCount(count: Int) {
        _uiState.update { it.copy(uuidCount = count.coerceIn(1, 100)) }
    }

    fun generateUuids() {
        val state = _uiState.value
        val results = if (state.uuidCount == 1) {
            listOf(generatorUseCases.generateUUID(state.uuidVersion))
        } else {
            generatorUseCases.generateUUIDs(state.uuidCount, state.uuidVersion)
        }
        _uiState.update { it.copy(uuidResults = results) }
    }

    // ==================== 条形码 ====================

    fun setBarcodeContent(content: String) {
        _uiState.update { it.copy(barcodeContent = content) }
    }

    fun setBarcodeType(type: GeneratorUseCases.BarcodeType) {
        _uiState.update { it.copy(barcodeType = type) }
    }

    fun generateBarcode() {
        val state = _uiState.value
        if (state.barcodeContent.isBlank()) {
            _uiState.update { it.copy(error = "请输入条形码内容") }
            return
        }
        val data = generatorUseCases.generateBarcodeData(state.barcodeContent, state.barcodeType)
        _uiState.update { it.copy(barcodeData = data, error = null) }
    }

    // ==================== 调色板 ====================

    fun setBaseColor(color: Int) {
        _uiState.update { it.copy(baseColor = color) }
    }

    fun setPaletteCount(count: Int) {
        _uiState.update { it.copy(paletteCount = count.coerceIn(3, 10)) }
    }

    fun generatePalette() {
        val state = _uiState.value
        val results = generatorUseCases.generatePalette(state.baseColor, state.paletteCount)
        _uiState.update { it.copy(paletteColors = results, gradientSteps = emptyList()) }
    }

    fun generateRandomPalette() {
        val state = _uiState.value
        val results = generatorUseCases.generateRandomPalette(state.paletteCount)
        _uiState.update { it.copy(paletteColors = results, gradientSteps = emptyList()) }
    }

    fun setGradientStartColor(color: Int) {
        _uiState.update { it.copy(gradientStartColor = color) }
    }

    fun setGradientEndColor(color: Int) {
        _uiState.update { it.copy(gradientEndColor = color) }
    }

    fun setGradientSteps(steps: Int) {
        _uiState.update { it.copy(gradientStepsCount = steps.coerceIn(2, 20)) }
    }

    fun generateGradient() {
        val state = _uiState.value
        val results = generatorUseCases.generateGradient(
            state.gradientStartColor,
            state.gradientEndColor,
            state.gradientStepsCount
        )
        _uiState.update { it.copy(gradientSteps = results, paletteColors = emptyList()) }
    }

    // ==================== 随机数据 ====================

    fun generateChineseName() {
        val name = generatorUseCases.generateChineseName()
        _uiState.update { it.copy(randomNameResult = name) }
    }

    fun generateChineseNames() {
        val state = _uiState.value
        val names = generatorUseCases.generateChineseNames(state.randomNameCount)
        _uiState.update { it.copy(randomNameBatchResults = names) }
    }

    fun setRandomNameCount(count: Int) {
        _uiState.update { it.copy(randomNameCount = count.coerceIn(1, 100)) }
    }

    fun setPhoneCarrier(carrier: String) {
        _uiState.update { it.copy(phoneCarrier = carrier) }
    }

    fun generatePhoneNumber() {
        val state = _uiState.value
        val number = generatorUseCases.generatePhoneNumber(state.phoneCarrier)
        _uiState.update { it.copy(randomPhoneResult = number) }
    }

    fun generatePhoneNumbers() {
        val state = _uiState.value
        val numbers = generatorUseCases.generatePhoneNumbers(state.randomPhoneCount, state.phoneCarrier)
        _uiState.update { it.copy(randomPhoneBatchResults = numbers) }
    }

    fun setRandomPhoneCount(count: Int) {
        _uiState.update { it.copy(randomPhoneCount = count.coerceIn(1, 100)) }
    }

    fun generateEmail() {
        val email = generatorUseCases.generateEmail()
        _uiState.update { it.copy(randomEmailResult = email) }
    }

    fun generateRandomIp() {
        val ip = generatorUseCases.generateIpAddress()
        _uiState.update { it.copy(randomIpResult = ip) }
    }

    fun generateRandomMac() {
        val mac = generatorUseCases.generateMacAddress()
        _uiState.update { it.copy(randomMacResult = mac) }
    }

    fun generateRandomUrl() {
        val url = generatorUseCases.generateUrl()
        _uiState.update { it.copy(randomUrlResult = url) }
    }

    fun generateRandomDate() {
        val date = generatorUseCases.generateDate()
        _uiState.update { it.copy(randomDateResult = date) }
    }

    fun generateRandomAddress() {
        val address = generatorUseCases.generateAddress()
        _uiState.update { it.copy(randomAddressResult = address) }
    }

    fun setLoremIpsumWords(words: Int) {
        _uiState.update { it.copy(loremIpsumWords = words.coerceIn(1, 500)) }
    }

    fun generateLoremIpsum() {
        val state = _uiState.value
        val text = generatorUseCases.generateLoremIpsum(state.loremIpsumWords)
        _uiState.update { it.copy(loremIpsumResult = text) }
    }

    fun setChineseTextChars(chars: Int) {
        _uiState.update { it.copy(chineseTextChars = chars.coerceIn(1, 1000)) }
    }

    fun generateChineseText() {
        val state = _uiState.value
        val text = generatorUseCases.generateChineseText(state.chineseTextChars)
        _uiState.update { it.copy(chineseTextResult = text) }
    }

    fun setRandomNumberMin(min: Int) {
        _uiState.update { it.copy(randomNumberMin = min) }
    }

    fun setRandomNumberMax(max: Int) {
        _uiState.update { it.copy(randomNumberMax = max) }
    }

    fun generateRandomNumber() {
        val state = _uiState.value
        val number = generatorUseCases.generateRandomNumber(
            minOf(state.randomNumberMin, state.randomNumberMax),
            maxOf(state.randomNumberMin, state.randomNumberMax)
        )
        _uiState.update { it.copy(randomNumberResult = number.toString()) }
    }

    fun setRandomHexLength(length: Int) {
        _uiState.update { it.copy(randomHexLength = length.coerceIn(1, 256)) }
    }

    fun generateRandomHex() {
        val state = _uiState.value
        val hex = generatorUseCases.generateHexString(state.randomHexLength)
        _uiState.update { it.copy(randomHexResult = hex) }
    }

    fun setRandomBase64Length(length: Int) {
        _uiState.update { it.copy(randomBase64Length = length.coerceIn(1, 256)) }
    }

    fun generateRandomBase64() {
        val state = _uiState.value
        val base64 = generatorUseCases.generateBase64(state.randomBase64Length)
        _uiState.update { it.copy(randomBase64Result = base64) }
    }

    // ==================== 批量生成 ====================

    fun setBatchType(type: BatchDataType) {
        _uiState.update { it.copy(batchType = type) }
    }

    fun setBatchCount(count: Int) {
        _uiState.update { it.copy(batchCount = count.coerceIn(1, 500)) }
    }

    fun toggleBatchItemSelection(index: Int) {
        _uiState.update { state ->
            val newSet = state.selectedBatchIndices.toMutableSet()
            if (newSet.contains(index)) {
                newSet.remove(index)
            } else {
                newSet.add(index)
            }
            state.copy(selectedBatchIndices = newSet)
        }
    }

    fun toggleSelectAllBatch() {
        _uiState.update { state ->
            if (state.selectedBatchIndices.size == state.batchResults.size) {
                state.copy(selectedBatchIndices = emptySet())
            } else {
                state.copy(selectedBatchIndices = state.batchResults.indices.toSet())
            }
        }
    }

    fun generateBatch() {
        val state = _uiState.value
        val results = when (state.batchType) {
            BatchDataType.UUID -> generatorUseCases.generateUUIDs(state.batchCount)
            BatchDataType.CHINESE_NAME -> generatorUseCases.generateChineseNames(state.batchCount)
            BatchDataType.PHONE_NUMBER -> generatorUseCases.generatePhoneNumbers(state.batchCount, state.phoneCarrier)
            BatchDataType.EMAIL -> generatorUseCases.generateEmails(state.batchCount)
            BatchDataType.IP -> (1..state.batchCount).map { generatorUseCases.generateIpAddress() }
            BatchDataType.MAC -> (1..state.batchCount).map { generatorUseCases.generateMacAddress() }
            BatchDataType.URL -> (1..state.batchCount).map { generatorUseCases.generateUrl() }
            BatchDataType.DATE -> (1..state.batchCount).map { generatorUseCases.generateDate() }
            BatchDataType.ADDRESS -> (1..state.batchCount).map { generatorUseCases.generateAddress() }
        }
        _uiState.update { it.copy(batchResults = results, selectedBatchIndices = emptySet()) }
    }

    fun getSelectedBatchText(): String {
        val state = _uiState.value
        return state.selectedBatchIndices.sorted()
            .mapNotNull { state.batchResults.getOrNull(it) }
            .joinToString("\n")
    }

    fun getAllBatchText(): String {
        return _uiState.value.batchResults.joinToString("\n")
    }

    // ==================== 通用 ====================

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

// ==================== UI 状态 ====================

/**
 * 生成工具页面 UI 状态
 */
data class GeneratorUiState(
    // 通用
    val activeTab: GeneratorTab = GeneratorTab.UUID,
    val error: String? = null,

    // UUID 生成
    val uuidVersion: GeneratorUseCases.UuidVersion = GeneratorUseCases.UuidVersion.V4,
    val uuidCount: Int = 1,
    val uuidResults: List<String> = emptyList(),

    // 条形码
    val barcodeContent: String = "",
    val barcodeType: GeneratorUseCases.BarcodeType = GeneratorUseCases.BarcodeType.CODE128,
    val barcodeData: GeneratorUseCases.BarcodeData? = null,

    // 调色板
    val baseColor: Int = 0xFF4CAF50.toInt(),
    val paletteCount: Int = 5,
    val paletteColors: List<GeneratorUseCases.PaletteColor> = emptyList(),
    val gradientStartColor: Int = 0xFF2196F3.toInt(),
    val gradientEndColor: Int = 0xFFFF9800.toInt(),
    val gradientStepsCount: Int = 10,
    val gradientSteps: List<GeneratorUseCases.GradientStep> = emptyList(),

    // 随机数据 - 姓名
    val randomNameResult: String = "",
    val randomNameCount: Int = 10,
    val randomNameBatchResults: List<String> = emptyList(),

    // 随机数据 - 手机号
    val phoneCarrier: String = "",
    val randomPhoneResult: String = "",
    val randomPhoneCount: Int = 10,
    val randomPhoneBatchResults: List<String> = emptyList(),

    // 随机数据 - 邮箱
    val randomEmailResult: String = "",

    // 随机数据 - IP/MAC/URL
    val randomIpResult: String = "",
    val randomMacResult: String = "",
    val randomUrlResult: String = "",

    // 随机数据 - 日期/地址
    val randomDateResult: String = "",
    val randomAddressResult: String = "",

    // 随机数据 - 文本
    val loremIpsumWords: Int = 50,
    val loremIpsumResult: String = "",
    val chineseTextChars: Int = 100,
    val chineseTextResult: String = "",

    // 随机数据 - 数字/Hex/Base64
    val randomNumberMin: Int = 1,
    val randomNumberMax: Int = 100,
    val randomNumberResult: String = "",
    val randomHexLength: Int = 16,
    val randomHexResult: String = "",
    val randomBase64Length: Int = 16,
    val randomBase64Result: String = "",

    // 批量生成
    val batchType: BatchDataType = BatchDataType.UUID,
    val batchCount: Int = 10,
    val batchResults: List<String> = emptyList(),
    val selectedBatchIndices: Set<Int> = emptySet()
)

/**
 * 生成工具 Tab 枚举
 */
enum class GeneratorTab(val label: String) {
    UUID("UUID生成"),
    BARCODE("条形码"),
    PALETTE("调色板"),
    RANDOM_DATA("随机数据"),
    BATCH("批量生成")
}

/**
 * 批量生成数据类型
 */
enum class BatchDataType(val label: String) {
    UUID("UUID"),
    CHINESE_NAME("随机姓名"),
    PHONE_NUMBER("随机手机号"),
    EMAIL("随机邮箱"),
    IP("随机IP"),
    MAC("随机MAC"),
    URL("随机URL"),
    DATE("随机日期"),
    ADDRESS("随机地址")
}
