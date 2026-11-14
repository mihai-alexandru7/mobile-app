package com.florilegia.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import androidx.core.text.HtmlCompat
import com.florilegia.data.model.BookFileFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

data class PickedBookFile(
    val uri: Uri,
    val displayName: String,
    val format: BookFileFormat,
    val sizeBytes: Long,
    val suggestedSampleText: String?,
)

data class PickedCoverImage(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
)

object FilePicker {

    private const val MAX_BOOK_BYTES = 100L * 1024L * 1024L
    private const val MAX_COVER_BYTES = 10L * 1024L * 1024L

    suspend fun inspectCoverImage(context: Context, uri: Uri): Result<PickedCoverImage> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val (rawName, rawSize) = queryMeta(context, uri)
                val mime = resolver.getType(uri)?.lowercase().orEmpty()
                val ext = rawName.substringAfterLast('.', "").lowercase()

                val resolvedMime = when {
                    mime.startsWith("image/") -> mime
                    ext == "png" -> "image/png"
                    ext == "webp" -> "image/webp"
                    ext in listOf("jpg", "jpeg") -> "image/jpeg"
                    else -> throw IllegalArgumentException("Please select a valid image file (JPG, PNG, or WebP).")
                }

                val size = if (rawSize > 0L) rawSize else measureStreamBytes(context, uri)
                require(size > 0L) { "Selected image file is empty." }
                require(size <= MAX_COVER_BYTES) { "Cover image must be smaller than 10 MB." }

                PickedCoverImage(
                    uri = uri,
                    displayName = rawName.ifBlank { "cover.jpg" },
                    mimeType = resolvedMime,
                    sizeBytes = size,
                )
            }
        }

    suspend fun inspectBookFile(context: Context, uri: Uri): Result<PickedBookFile> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val (rawName, rawSize) = queryMeta(context, uri)
                val mime = resolver.getType(uri)?.lowercase().orEmpty()
                val ext = rawName.substringAfterLast('.', "").lowercase()

                val header = ByteArray(5)
                val headerRead = resolver.openInputStream(uri)?.use { it.read(header) } ?: 0
                val isPdfMagic = headerRead >= 4 &&
                        header[0] == '%'.code.toByte() &&
                        header[1] == 'P'.code.toByte() &&
                        header[2] == 'D'.code.toByte() &&
                        header[3] == 'F'.code.toByte()
                val isZipMagic = headerRead >= 2 &&
                        header[0] == 0x50.toByte() &&
                        header[1] == 0x4B.toByte()

                val format = when {
                    ext == "pdf" || mime == "application/pdf" || isPdfMagic -> {
                        require(isPdfMagic || ext == "pdf") { "Invalid PDF file." }
                        BookFileFormat.PDF
                    }
                    ext == "epub" || mime == "application/epub+zip" || isZipMagic -> {
                        require(isZipMagic) { "Selected file is not a valid EPUB archive." }
                        BookFileFormat.EPUB
                    }
                    else -> throw IllegalArgumentException("Unsupported file type. Please choose an .epub or .pdf file.")
                }

                val size = if (rawSize > 0L) rawSize else measureStreamBytes(context, uri)
                require(size > 0L) { "Selected file is empty." }
                require(size <= MAX_BOOK_BYTES) { "Book file must be smaller than 100 MB." }

                val sample = if (format == BookFileFormat.EPUB) {
                    extractEpubSample(context, uri)
                } else {
                    null
                }

                PickedBookFile(
                    uri = uri,
                    displayName = rawName.ifBlank { "book.${format.value}" },
                    format = format,
                    sizeBytes = size,
                    suggestedSampleText = sample,
                )
            }
        }

    private fun queryMeta(context: Context, uri: Uri): Pair<String, Long> {
        var name = ""
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) name = cursor.getString(nameIdx).orEmpty()
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
            }
        }
        return name to size
    }

    private fun measureStreamBytes(context: Context, uri: Uri): Long {
        val buf = ByteArray(8192)
        var total = 0L
        context.contentResolver.openInputStream(uri)?.use { input ->
            while (true) {
                val read = input.read(buf)
                if (read <= 0) break
                total += read
            }
        }
        return total
    }

    private fun extractEpubSample(context: Context, uri: Uri): String? {
        val tempFile = File.createTempFile("florilegia_sample_", ".epub", context.cacheDir)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            ZipFile(tempFile).use { zip ->
                val opfPath = findOpfPath(zip)
                val orderedChapterPaths = if (opfPath != null) {
                    readSpinePaths(zip, opfPath)
                } else {
                    emptyList()
                }.ifEmpty {
                    zip.entries().asSequence()
                        .filter { !it.isDirectory }
                        .map { it.name }
                        .filter {
                            val lower = it.lowercase()
                            (lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm")) &&
                                    !lower.contains("nav.") && !lower.contains("toc.")
                        }
                        .sorted()
                        .toList()
                }

                val builder = StringBuilder()
                for (entryPath in orderedChapterPaths) {
                    val entry = zip.getEntry(entryPath) ?: continue
                    val rawHtml = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val plain = htmlToReadableText(rawHtml)
                    if (plain.length < 80 && builder.isEmpty()) {
                        // Skip cover / short title page
                        continue
                    }
                    if (plain.isNotBlank()) {
                        if (builder.isNotEmpty()) builder.append("\n\n")
                        builder.append(plain)
                    }
                    if (builder.length >= 1600) break
                }

                val full = builder.toString().trim()
                if (full.isBlank()) null
                else if (full.length <= 2000) full
                else full.take(2000).substringBeforeLast(' ').ifBlank { full.take(2000) } + "..."
            }
        } catch (_: Exception) {
            null
        } finally {
            tempFile.delete()
        }
    }

    private fun findOpfPath(zip: ZipFile): String? {
        val container = zip.getEntry("META-INF/container.xml") ?: return null
        return zip.getInputStream(container).use { stream ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(stream, "UTF-8")
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name.equals("rootfile", ignoreCase = true)) {
                    val path = parser.getAttributeValue(null, "full-path")
                    if (!path.isNullOrBlank()) return@use path
                }
                event = parser.next()
            }
            null
        }
    }

    private fun readSpinePaths(zip: ZipFile, opfPath: String): List<String> {
        val opfEntry = zip.getEntry(opfPath) ?: return emptyList()
        val baseDir = opfPath.substringBeforeLast('/', "")
        val manifest = mutableMapOf<String, Pair<String, String>>() // id -> (href, mediaType)
        val spineIds = mutableListOf<String>()

        zip.getInputStream(opfEntry).use { stream ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(stream, "UTF-8")
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when {
                        parser.name.equals("item", ignoreCase = true) -> {
                            val id = parser.getAttributeValue(null, "id")
                            val href = parser.getAttributeValue(null, "href")
                            val media = parser.getAttributeValue(null, "media-type").orEmpty()
                            if (!id.isNullOrBlank() && !href.isNullOrBlank()) {
                                manifest[id] = href to media
                            }
                        }
                        parser.name.equals("itemref", ignoreCase = true) -> {
                            val idref = parser.getAttributeValue(null, "idref")
                            if (!idref.isNullOrBlank()) spineIds.add(idref)
                        }
                    }
                }
                event = parser.next()
            }
        }

        return spineIds.mapNotNull { id ->
            val (href, media) = manifest[id] ?: return@mapNotNull null
            val cleanHref = href.substringBefore('#')
            val isHtml = media.contains("html", ignoreCase = true) ||
                    cleanHref.endsWith(".xhtml", ignoreCase = true) ||
                    cleanHref.endsWith(".html", ignoreCase = true) ||
                    cleanHref.endsWith(".htm", ignoreCase = true)
            if (!isHtml) return@mapNotNull null
            if (baseDir.isBlank()) cleanHref else "$baseDir/$cleanHref"
        }
    }

    private fun htmlToReadableText(rawHtml: String): String {
        val cleaned = rawHtml
            .replace(Regex("(?is)<head[^>]*>.*?</head>"), " ")
            .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
            .replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
        return HtmlCompat.fromHtml(cleaned, HtmlCompat.FROM_HTML_MODE_COMPACT)
            .toString()
            .replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }
}