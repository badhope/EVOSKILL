package com.banana.toolbox.domain.model

/**
 * 文件项领域模型
 */
data class FileItem(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val mimeType: String? = null,
    val extension: String? = null
) {
    /**
     * 获取文件类型图标
     */
    fun getFileType(): FileType = when {
        isDirectory -> FileType.DIRECTORY
        extension == null -> FileType.UNKNOWN
        else -> when (extension.lowercase()) {
            in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg") -> FileType.IMAGE
            in listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm") -> FileType.VIDEO
            in listOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma") -> FileType.AUDIO
            in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md") -> FileType.DOCUMENT
            in listOf("zip", "rar", "7z", "tar", "gz", "bz2") -> FileType.ARCHIVE
            "apk" -> FileType.APK
            in listOf("kt", "java", "py", "js", "ts", "cpp", "c", "h", "go", "rs") -> FileType.CODE
            else -> FileType.UNKNOWN
        }
    }
}

/**
 * 文件类型枚举
 */
enum class FileType {
    DIRECTORY,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    ARCHIVE,
    APK,
    CODE,
    UNKNOWN
}

/**
 * 排序方式
 */
enum class SortBy {
    NAME,
    SIZE,
    DATE,
    TYPE
}

/**
 * 视图模式
 */
enum class ViewMode {
    LIST,
    GRID
}
