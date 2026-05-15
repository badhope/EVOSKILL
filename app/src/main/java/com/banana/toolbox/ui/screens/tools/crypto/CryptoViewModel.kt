package com.banana.toolbox.ui.screens.tools.crypto

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banana.toolbox.domain.usecase.tools.CryptoAlgorithm
import com.banana.toolbox.domain.usecase.tools.CryptoUseCases
import com.banana.toolbox.domain.usecase.tools.DictionaryAttackResult
import com.banana.toolbox.domain.usecase.tools.HashAlgorithm
import com.banana.toolbox.domain.usecase.tools.PasswordStrength
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * 加密解密工具 ViewModel
 *
 * 封装 [CryptoUseCases]，管理文件加密/解密、文本加密/解密、
 * 密码生成、哈希计算、密码强度检测的 UI 状态。
 */
@HiltViewModel
class CryptoViewModel @Inject constructor(
    private val cryptoUseCases: CryptoUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(CryptoUiState())
    val uiState: StateFlow<CryptoUiState> = _uiState.asStateFlow()

    // ==================== Tab 切换 ====================

    fun setActiveTab(tab: CryptoTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    // ==================== 文件加密/解密 ====================

    /**
     * 选择待加密/解密的文件
     */
    fun selectFile(uri: Uri, filePath: String) {
        _uiState.update {
            it.copy(
                selectedFileUri = uri,
                selectedFilePath = filePath,
                selectedFileName = filePath.substringAfterLast("/"),
                fileResult = null,
                error = null
            )
        }
    }

    /**
     * 设置文件加密密码
     */
    fun setFilePassword(password: String) {
        _uiState.update { it.copy(filePassword = password) }
    }

    /**
     * 设置加密算法
     */
    fun setCryptoAlgorithm(algorithm: CryptoAlgorithm) {
        _uiState.update { it.copy(cryptoAlgorithm = algorithm) }
    }

    /**
     * 加密文件
     */
    fun encryptFile() {
        val state = _uiState.value
        val inputPath = state.selectedFilePath ?: return
        if (state.filePassword.isBlank()) {
            _uiState.update { it.copy(error = "请输入密码") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isFileProcessing = true, error = null, fileResult = null) }

            val outputFile = File(inputPath)
            val outputPath = "${outputFile.parent}/${outputFile.nameWithoutExtension}.enc"

            cryptoUseCases.encryptFile(
                inputPath = inputPath,
                outputPath = outputPath,
                password = state.filePassword,
                algorithm = state.cryptoAlgorithm
            ).onSuccess { resultPath ->
                _uiState.update {
                    it.copy(
                        isFileProcessing = false,
                        fileResult = resultPath,
                        successMessage = "文件加密成功"
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isFileProcessing = false,
                        error = throwable.message ?: "文件加密失败"
                    )
                }
            }
        }
    }

    /**
     * 解密文件
     */
    fun decryptFile() {
        val state = _uiState.value
        val inputPath = state.selectedFilePath ?: return
        if (state.filePassword.isBlank()) {
            _uiState.update { it.copy(error = "请输入密码") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isFileProcessing = true, error = null, fileResult = null) }

            val outputFile = File(inputPath)
            // 去掉 .enc 后缀作为输出文件名
            val outputName = if (outputFile.extension.equals("enc", ignoreCase = true)) {
                outputFile.nameWithoutExtension
            } else {
                "${outputFile.nameWithoutExtension}_decrypted"
            }
            val outputPath = "${outputFile.parent}/$outputName"

            cryptoUseCases.decryptFile(
                inputPath = inputPath,
                outputPath = outputPath,
                password = state.filePassword,
                algorithm = state.cryptoAlgorithm
            ).onSuccess { resultPath ->
                _uiState.update {
                    it.copy(
                        isFileProcessing = false,
                        fileResult = resultPath,
                        successMessage = "文件解密成功"
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isFileProcessing = false,
                        error = throwable.message ?: "文件解密失败，请检查密码是否正确"
                    )
                }
            }
        }
    }

    // ==================== 文本加密/解密 ====================

    /**
     * 设置待加密/解密的文本
     */
    fun setTextInput(text: String) {
        _uiState.update { it.copy(textInput = text) }
    }

    /**
     * 设置文本加密密码
     */
    fun setTextPassword(password: String) {
        _uiState.update { it.copy(textPassword = password) }
    }

    /**
     * 加密文本
     */
    fun encryptText() {
        val state = _uiState.value
        if (state.textInput.isBlank()) {
            _uiState.update { it.copy(error = "请输入待加密文本") }
            return
        }
        if (state.textPassword.isBlank()) {
            _uiState.update { it.copy(error = "请输入密码") }
            return
        }

        cryptoUseCases.encryptText(state.textInput, state.textPassword)
            .onSuccess { encrypted ->
                _uiState.update {
                    it.copy(
                        textOutput = encrypted,
                        successMessage = "文本加密成功"
                    )
                }
            }
            .onFailure { throwable ->
                _uiState.update {
                    it.copy(error = throwable.message ?: "文本加密失败")
                }
            }
    }

    /**
     * 解密文本
     */
    fun decryptText() {
        val state = _uiState.value
        if (state.textInput.isBlank()) {
            _uiState.update { it.copy(error = "请输入待解密文本") }
            return
        }
        if (state.textPassword.isBlank()) {
            _uiState.update { it.copy(error = "请输入密码") }
            return
        }

        cryptoUseCases.decryptText(state.textInput, state.textPassword)
            .onSuccess { decrypted ->
                _uiState.update {
                    it.copy(
                        textOutput = decrypted,
                        successMessage = "文本解密成功"
                    )
                }
            }
            .onFailure { throwable ->
                _uiState.update {
                    it.copy(error = throwable.message ?: "文本解密失败，请检查密码是否正确")
                }
            }
    }

    // ==================== 密码生成 ====================

    /**
     * 设置密码长度
     */
    fun setPasswordLength(length: Int) {
        _uiState.update { it.copy(passwordLength = length.coerceIn(8, 64)) }
    }

    /**
     * 设置是否包含大写字母
     */
    fun setIncludeUppercase(include: Boolean) {
        _uiState.update { it.copy(includeUppercase = include) }
    }

    /**
     * 设置是否包含小写字母
     */
    fun setIncludeLowercase(include: Boolean) {
        _uiState.update { it.copy(includeLowercase = include) }
    }

    /**
     * 设置是否包含数字
     */
    fun setIncludeDigits(include: Boolean) {
        _uiState.update { it.copy(includeDigits = include) }
    }

    /**
     * 设置是否包含特殊字符
     */
    fun setIncludeSpecial(include: Boolean) {
        _uiState.update { it.copy(includeSpecial = include) }
    }

    /**
     * 设置是否排除歧义字符
     */
    fun setExcludeAmbiguous(exclude: Boolean) {
        _uiState.update { it.copy(excludeAmbiguous = exclude) }
    }

    /**
     * 生成密码
     */
    fun generatePassword() {
        val state = _uiState.value
        if (!state.includeUppercase && !state.includeLowercase && !state.includeDigits && !state.includeSpecial) {
            _uiState.update { it.copy(error = "请至少选择一种字符类型") }
            return
        }

        val password = cryptoUseCases.generatePassword(
            length = state.passwordLength,
            includeUppercase = state.includeUppercase,
            includeLowercase = state.includeLowercase,
            includeNumbers = state.includeDigits,
            includeSymbols = state.includeSpecial,
            excludeAmbiguous = state.excludeAmbiguous
        )

        val strength = cryptoUseCases.evaluatePasswordStrength(password)

        _uiState.update {
            it.copy(
                generatedPassword = password,
                generatedPasswordStrength = strength,
                successMessage = "密码已生成"
            )
        }
    }

    // ==================== 哈希计算 ====================

    /**
     * 设置哈希输入文本
     */
    fun setHashInputText(text: String) {
        _uiState.update { it.copy(hashInputText = text) }
    }

    /**
     * 设置哈希算法
     */
    fun setHashAlgorithm(algorithm: HashAlgorithm) {
        _uiState.update { it.copy(hashAlgorithm = algorithm) }
    }

    /**
     * 设置哈希输入模式（文本/文件）
     */
    fun setHashInputMode(mode: HashInputMode) {
        _uiState.update {
            it.copy(
                hashInputMode = mode,
                hashResult = null,
                hashFilePath = null,
                hashFileName = null,
                hashFileUri = null
            )
        }
    }

    /**
     * 选择哈希计算文件
     */
    fun selectHashFile(uri: Uri, filePath: String) {
        _uiState.update {
            it.copy(
                hashFileUri = uri,
                hashFilePath = filePath,
                hashFileName = filePath.substringAfterLast("/")
            )
        }
    }

    /**
     * 计算文本哈希
     */
    fun computeTextHash() {
        val state = _uiState.value
        if (state.hashInputText.isBlank()) {
            _uiState.update { it.copy(error = "请输入待计算文本") }
            return
        }

        val hash = cryptoUseCases.hashText(state.hashInputText, state.hashAlgorithm)
        _uiState.update {
            it.copy(
                hashResult = hash,
                successMessage = "哈希计算完成"
            )
        }
    }

    /**
     * 计算文件哈希
     */
    fun computeFileHash() {
        val state = _uiState.value
        val filePath = state.hashFilePath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isHashComputing = true, error = null, hashResult = null) }

            cryptoUseCases.hashFile(filePath, state.hashAlgorithm)
                .onSuccess { hash ->
                    _uiState.update {
                        it.copy(
                            isHashComputing = false,
                            hashResult = hash,
                            successMessage = "文件哈希计算完成"
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isHashComputing = false,
                            error = throwable.message ?: "哈希计算失败"
                        )
                    }
                }
        }
    }

    // ==================== 密码强度检测 ====================

    /**
     * 设置待检测密码
     */
    fun setStrengthCheckPassword(password: String) {
        _uiState.update { it.copy(strengthCheckPassword = password) }
    }

    /**
     * 执行密码强度检测
     */
    fun checkPasswordStrength() {
        val state = _uiState.value
        if (state.strengthCheckPassword.isBlank()) {
            _uiState.update { it.copy(error = "请输入待检测密码") }
            return
        }

        val strength = cryptoUseCases.evaluatePasswordStrength(state.strengthCheckPassword)
        val dictionaryResult = cryptoUseCases.dictionaryAttackCheck(state.strengthCheckPassword)

        _uiState.update {
            it.copy(
                passwordStrength = strength,
                dictionaryAttackResult = dictionaryResult,
                successMessage = "密码强度检测完成"
            )
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
     * 清除成功信息
     */
    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * 重置当前 Tab 状态
     */
    fun resetCurrentTab() {
        _uiState.update {
            when (it.activeTab) {
                CryptoTab.FILE_ENCRYPT -> it.copy(
                    selectedFileUri = null,
                    selectedFilePath = null,
                    selectedFileName = null,
                    filePassword = "",
                    fileResult = null,
                    isFileProcessing = false,
                    error = null,
                    successMessage = null
                )
                CryptoTab.TEXT_ENCRYPT -> it.copy(
                    textInput = "",
                    textOutput = "",
                    textPassword = "",
                    error = null,
                    successMessage = null
                )
                CryptoTab.PASSWORD_GENERATOR -> it.copy(
                    generatedPassword = "",
                    generatedPasswordStrength = null,
                    error = null,
                    successMessage = null
                )
                CryptoTab.HASH_COMPUTE -> it.copy(
                    hashInputText = "",
                    hashResult = null,
                    hashFilePath = null,
                    hashFileName = null,
                    hashFileUri = null,
                    isHashComputing = false,
                    error = null,
                    successMessage = null
                )
                CryptoTab.PASSWORD_STRENGTH -> it.copy(
                    strengthCheckPassword = "",
                    passwordStrength = null,
                    dictionaryAttackResult = null,
                    error = null,
                    successMessage = null
                )
            }
        }
    }
}

// ==================== UI 状态 ====================

/**
 * 加密解密页面 UI 状态
 */
data class CryptoUiState(
    // 通用
    val activeTab: CryptoTab = CryptoTab.FILE_ENCRYPT,
    val error: String? = null,
    val successMessage: String? = null,

    // 文件加密/解密
    val selectedFileUri: Uri? = null,
    val selectedFilePath: String? = null,
    val selectedFileName: String? = null,
    val filePassword: String = "",
    val cryptoAlgorithm: CryptoAlgorithm = CryptoAlgorithm.AES_256,
    val isFileProcessing: Boolean = false,
    val fileResult: String? = null,

    // 文本加密/解密
    val textInput: String = "",
    val textOutput: String = "",
    val textPassword: String = "",

    // 密码生成
    val passwordLength: Int = 16,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeDigits: Boolean = true,
    val includeSpecial: Boolean = true,
    val excludeAmbiguous: Boolean = false,
    val generatedPassword: String = "",
    val generatedPasswordStrength: PasswordStrength? = null,

    // 哈希计算
    val hashInputMode: HashInputMode = HashInputMode.TEXT,
    val hashInputText: String = "",
    val hashAlgorithm: HashAlgorithm = HashAlgorithm.SHA256,
    val hashFilePath: String? = null,
    val hashFileName: String? = null,
    val hashFileUri: Uri? = null,
    val isHashComputing: Boolean = false,
    val hashResult: String? = null,

    // 密码强度检测
    val strengthCheckPassword: String = "",
    val passwordStrength: PasswordStrength? = null,
    val dictionaryAttackResult: DictionaryAttackResult? = null
)

/**
 * 加密解密 Tab 枚举
 */
enum class CryptoTab(val label: String) {
    FILE_ENCRYPT("文件加密"),
    TEXT_ENCRYPT("文本加密"),
    PASSWORD_GENERATOR("密码生成"),
    HASH_COMPUTE("哈希计算"),
    PASSWORD_STRENGTH("密码强度")
}

/**
 * 哈希输入模式
 */
enum class HashInputMode(val label: String) {
    TEXT("文本"),
    FILE("文件")
}
