package com.banana.toolbox.domain.usecase.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 加密解密工具用例
 */
@Singleton
class CryptoUseCases @Inject constructor() {
    
    private val secureRandom = SecureRandom()
    
    // ==================== 文件加密/解密 ====================
    
    /**
     * AES 加密文件
     */
    suspend fun encryptFile(
        inputPath: String,
        outputPath: String,
        password: String,
        algorithm: CryptoAlgorithm = CryptoAlgorithm.AES_256
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val key = deriveKey(password, algorithm)
                val iv = ByteArray(16).also { secureRandom.nextBytes(it) }
                
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
                
                val inputFile = File(inputPath)
                val outputFile = File(outputPath)
                
                FileOutputStream(outputFile).use { fos ->
                    // 写入 IV（用于解密）
                    fos.write(iv)
                    
                    // 加密文件内容
                    FileInputStream(inputFile).use { fis ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (fis.read(buffer).also { bytesRead = it } != -1) {
                            val encrypted = cipher.update(buffer, 0, bytesRead)
                            if (encrypted != null) fos.write(encrypted)
                        }
                        val finalBlock = cipher.doFinal()
                        if (finalBlock != null) fos.write(finalBlock)
                    }
                }
                
                Result.success(outputFile.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * AES 解密文件
     */
    suspend fun decryptFile(
        inputPath: String,
        outputPath: String,
        password: String,
        algorithm: CryptoAlgorithm = CryptoAlgorithm.AES_256
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val key = deriveKey(password, algorithm)
                
                val inputFile = File(inputPath)
                FileInputStream(inputFile).use { fis ->
                    // 读取 IV
                    val iv = ByteArray(16)
                    fis.read(iv)
                    
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
                    
                    val outputFile = File(outputPath)
                    FileOutputStream(outputFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (fis.read(buffer).also { bytesRead = it } != -1) {
                            val decrypted = cipher.update(buffer, 0, bytesRead)
                            if (decrypted != null) fos.write(decrypted)
                        }
                        val finalBlock = cipher.doFinal()
                        if (finalBlock != null) fos.write(finalBlock)
                    }
                    
                    Result.success(outputFile.absolutePath)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 从密码派生密钥
     */
    private fun deriveKey(password: String, algorithm: CryptoAlgorithm): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(password.toByteArray())
        return keyBytes.copyOf(algorithm.keySize / 8)
    }
    
    // ==================== 文本加密/解密 ====================
    
    /**
     * AES 加密文本
     */
    fun encryptText(text: String, password: String): Result<String> {
        return try {
            val key = deriveKey(password, CryptoAlgorithm.AES_256)
            val iv = ByteArray(16).also { secureRandom.nextBytes(it) }
            
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            
            val encrypted = cipher.doFinal(text.toByteArray())
            val combined = iv + encrypted
            
            Result.success(Base64.getEncoder().encodeToString(combined))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * AES 解密文本
     */
    fun decryptText(encryptedText: String, password: String): Result<String> {
        return try {
            val combined = Base64.getDecoder().decode(encryptedText)
            val iv = combined.copyOfRange(0, 16)
            val encrypted = combined.copyOfRange(16, combined.size)
            
            val key = deriveKey(password, CryptoAlgorithm.AES_256)
            
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            
            val decrypted = cipher.doFinal(encrypted)
            Result.success(String(decrypted))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== 密码生成器 ====================
    
    /**
     * 生成随机密码
     */
    fun generatePassword(
        length: Int = 16,
        includeUppercase: Boolean = true,
        includeLowercase: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true,
        excludeAmbiguous: Boolean = false
    ): String {
        val charPool = StringBuilder()
        
        if (includeUppercase) {
            val upper = if (excludeAmbiguous) "ABCDEFGHJKMNPQRSTUVWXYZ" else "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            charPool.append(upper)
        }
        if (includeLowercase) {
            val lower = if (excludeAmbiguous) "abcdefghjkmnpqrstuvwxyz" else "abcdefghijklmnopqrstuvwxyz"
            charPool.append(lower)
        }
        if (includeNumbers) {
            val nums = if (excludeAmbiguous) "23456789" else "0123456789"
            charPool.append(nums)
        }
        if (includeSymbols) {
            charPool.append("!@#$%^&*()_+-=[]{}|;:,.<>?")
        }
        
        return (1..length)
            .map { charPool.random() }
            .joinToString("")
    }
    
    /**
     * 生成密码短语
     */
    fun generatePassphrase(wordCount: Int = 4, separator: String = "-"): String {
        val words = listOf(
            "apple", "banana", "cherry", "dragon", "eagle", "forest", "garden", "hammer",
            "island", "jungle", "knight", "lemon", "mountain", "night", "ocean", "piano",
            "queen", "river", "stone", "thunder", "umbrella", "village", "water", "yellow",
            "zebra", "anchor", "bridge", "castle", "diamond", "ember", "falcon", "globe",
            "harbor", "iceberg", "jade", "kite", "lunar", "maple", "nova", "orbit",
            "prism", "quartz", "raven", "solar", "tiger", "unity", "vortex", "wonder",
            "xenon", "yarn", "zenith", "alpha", "brave", "coral", "delta", "echo"
        )
        
        return (1..wordCount)
            .map { words.random() }
            .joinToString(separator)
    }
    
    /**
     * 评估密码强度
     */
    fun evaluatePasswordStrength(password: String): PasswordStrength {
        var score = 0
        
        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.length >= 16) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        if (password.any { it in "!@#$%^&*" }) score++
        
        // 检查常见密码
        val commonPasswords = setOf("123456", "password", "12345678", "qwerty", "abc123", "111111")
        if (password.lowercase() in commonPasswords) score = 0
        
        // 检查连续字符
        if (hasConsecutiveChars(password)) score--
        
        return when {
            score <= 2 -> PasswordStrength.WEAK
            score <= 4 -> PasswordStrength.MEDIUM
            score <= 6 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }
    }
    
    private fun hasConsecutiveChars(password: String): Boolean {
        for (i in 0 until password.length - 2) {
            if (password[i] + 1 == password[i + 1] && password[i + 1] + 1 == password[i + 2]) {
                return true
            }
        }
        return false
    }
    
    // ==================== 暴力破解检测 ====================
    
    /**
     * 字典攻击检测（检测密码是否在常见密码列表中）
     */
    fun dictionaryAttackCheck(password: String): DictionaryAttackResult {
        val commonPasswords = setOf(
            "123456", "password", "12345678", "qwerty", "123456789", "12345", "1234",
            "111111", "1234567", "dragon", "123123", "baseball", "abc123", "football",
            "monkey", "letmein", "shadow", "master", "666666", "qwertyuiop", "123321",
            "mustang", "1234567890", "michael", "654321", "superman", "1qaz2wsx",
            "7777777", "121212", "000000", "qazwsx", "123qwe", "killer", "trustno1",
            "jordan", "jennifer", "zxcvbnm", "asdfgh", "hunter", "buster", "soccer",
            "harley", "batman", "andrew", "tigger", "sunshine", "iloveyou", "2000",
            "charlie", "robert", "thomas", "hockey", "ranger", "daniel", "starwars",
            "klaster", "112233", "george", "computer", "michelle", "jessica", "pepper",
            "1111", "zxcvbn", "555555", "11111111", "131313", "freedom", "777777",
            "pass", "maggie", "159753", "aaaaaa", "ginger", "princess", "joshua",
            "cheese", "amanda", "summer", "love", "ashley", "nicole", "chelsea",
            "biteme", "matthew", "access", "yankees", "987654321", "dallas", "austin",
            "thunder", "taylor", "matrix", "admin", "admin123", "root", "toor"
        )
        
        val isCommon = password.lowercase() in commonPasswords
        
        // 估算暴力破解时间
        val charsetSize = when {
            password.any { it.isUpperCase() } && password.any { it.isLowerCase() } && password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() } -> 94
            password.any { it.isUpperCase() } && password.any { it.isLowerCase() } && password.any { it.isDigit() } -> 62
            password.any { it.isLetter() } && password.any { it.isDigit() } -> 36
            password.any { it.isUpperCase() } && password.any { it.isLowerCase() } -> 52
            else -> 26
        }
        
        val combinations = charsetSize.toDouble().pow(password.length)
        val guessesPerSecond = 1_000_000_000.0 // 10亿次/秒
        val secondsToCrack = combinations / guessesPerSecond / 2 // 平均情况
        
        return DictionaryAttackResult(
            isCommonPassword = isCommon,
            estimatedCrackTime = formatCrackTime(secondsToCrack),
            crackTimeSeconds = secondsToCrack,
            characterSetSize = charsetSize,
            totalCombinations = combinations
        )
    }
    
    private fun formatCrackTime(seconds: Double): String {
        return when {
            seconds < 1 -> "瞬间"
            seconds < 60 -> "%.0f 秒".format(seconds)
            seconds < 3600 -> "%.0f 分钟".format(seconds / 60)
            seconds < 86400 -> "%.0f 小时".format(seconds / 3600)
            seconds < 86400 * 365 -> "%.0f 天".format(seconds / 86400)
            seconds < 86400 * 365 * 1000 -> "%.0f 年".format(seconds / 86400 / 365)
            seconds < 86400 * 365 * 1000000 -> "%.0f 千年".format(seconds / 86400 / 365 / 1000)
            else -> "数百万年+"
        }
    }
    
    // ==================== 哈希计算 ====================
    
    /**
     * 计算文件哈希
     */
    suspend fun hashFile(path: String, algorithm: HashAlgorithm = HashAlgorithm.MD5): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val digest = MessageDigest.getInstance(algorithm.name)
                val file = File(path)
                
                file.inputStream().use { fis ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                }
                
                val hash = digest.digest().joinToString("") { "%02x".format(it) }
                Result.success(hash)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 计算文本哈希
     */
    fun hashText(text: String, algorithm: HashAlgorithm = HashAlgorithm.MD5): String {
        val digest = MessageDigest.getInstance(algorithm.name)
        val hash = digest.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
        return hash
    }
    
    /**
     * 验证文件哈希
     */
    suspend fun verifyFileHash(path: String, expectedHash: String, algorithm: HashAlgorithm = HashAlgorithm.MD5): Boolean {
        return hashFile(path, algorithm).getOrDefault("") == expectedHash.lowercase()
    }
}

// ==================== 枚举和数据类 ====================

enum class CryptoAlgorithm(val displayName: String, val keySize: Int) {
    AES_128("AES-128", 128),
    AES_256("AES-256", 256)
}

enum class HashAlgorithm(val name: String) {
    MD5("MD5"),
    SHA1("SHA-1"),
    SHA256("SHA-256"),
    SHA384("SHA-384"),
    SHA512("SHA-512")
}

enum class PasswordStrength(val displayName: String, val color: String) {
    WEAK("弱", "#F44336"),
    MEDIUM("中等", "#FF9800"),
    STRONG("强", "#4CAF50"),
    VERY_STRONG("非常强", "#2196F3")
}

data class DictionaryAttackResult(
    val isCommonPassword: Boolean,
    val estimatedCrackTime: String,
    val crackTimeSeconds: Double,
    val characterSetSize: Int,
    val totalCombinations: Double
)
