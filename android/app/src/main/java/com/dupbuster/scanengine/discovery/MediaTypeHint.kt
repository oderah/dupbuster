package com.dupbuster.scanengine.discovery

/**
 * Pipeline media hint (architecture §4.1 stage 1). Wire values are lowercase
 * strings stored in SQLite / used by HashPipeline (e.g. `video` → VIDEO_CONTENT_V1).
 */
enum class MediaTypeHint(val wireValue: String) {
  IMAGE("image"),
  VIDEO("video"),
  AUDIO("audio"),
  DOCUMENT("document"),
  TEXT("text"),
  OTHER("other"),
  ;

  companion object {
    fun fromMimeType(mimeType: String?): MediaTypeHint {
      if (mimeType.isNullOrBlank()) {
        return OTHER
      }
      val normalized = mimeType.lowercase()
      return when {
        normalized == "text/plain" || normalized.startsWith("text/") -> TEXT
        normalized.startsWith("image/") -> IMAGE
        normalized.startsWith("video/") -> VIDEO
        normalized.startsWith("audio/") -> AUDIO
        normalized == "application/pdf" ||
            normalized.startsWith("application/vnd.") ||
            normalized.contains("document") ||
            normalized.contains("msword") ||
            normalized.contains("spreadsheet") ||
            normalized.contains("presentation") -> DOCUMENT
        else -> OTHER
      }
    }

    fun fromFileName(displayName: String): MediaTypeHint {
      val dot = displayName.lastIndexOf('.')
      if (dot < 0 || dot == displayName.length - 1) {
        return OTHER
      }
      val ext = displayName.substring(dot + 1).lowercase()
      return when (ext) {
        "jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp", "tif", "tiff" -> IMAGE
        "mp4", "mov", "mkv", "webm", "avi", "m4v", "3gp" -> VIDEO
        "mp3", "m4a", "aac", "wav", "flac", "ogg", "opus" -> AUDIO
        "txt", "md", "csv", "json", "xml", "log" -> TEXT
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp" -> DOCUMENT
        else -> OTHER
      }
    }
  }
}
