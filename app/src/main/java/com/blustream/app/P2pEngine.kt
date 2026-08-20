package com.blustream.app

import android.content.Context
import com.frostwire.jlibtorrent.Priority
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.swig.torrent_flags_t
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.min

object P2pEngine {
    private val startLock = Any()
    private val session = SessionManager(false)
    private var started = false
    private val httpServer = P2pHttpServer()

    suspend fun prepare(context: Context, source: BluStreamSource): P2pPreparedStream = withContext(Dispatchers.IO) {
        val magnet = source.playableTarget?.takeIf { it.startsWith("magnet:", ignoreCase = true) }
            ?: error("This P2P source does not contain a magnet link or info hash.")
        ensureStarted()
        val root = File(context.cacheDir, "blustream-p2p").apply { mkdirs() }
        val metadataDir = File(root, "metadata").apply { mkdirs() }
        val torrentBytes = session.fetchMagnet(magnet, 45, metadataDir)
            ?: error("Torrent metadata was not received. Check peers and trackers, then try again.")
        val info = TorrentInfo(torrentBytes)
        val files = info.files()
        val fileIndex = chooseFile(files.numFiles(), source.fileIdx) { index -> files.fileName(index) to files.fileSize(index) }
        val priorities = Priority.array(Priority.IGNORE, files.numFiles())
        priorities[fileIndex] = Priority.SEVEN
        session.download(info, root, null, priorities, null, torrent_flags_t())
        val handle = waitForHandle(info)
        handle.prioritizeFiles(priorities)
        handle.resume()
        val path = File(files.filePath(fileIndex, root.absolutePath))
        path.parentFile?.mkdirs()
        val media = TorrentMedia(info, handle, fileIndex, path, files.fileSize(fileIndex), files.fileName(fileIndex))
        val localUrl = httpServer.register(media)
        P2pPreparedStream(localUrl, media.displayName, media.size)
    }

    private fun ensureStarted() {
        synchronized(startLock) {
            if (started) return
            session.start()
            httpServer.start()
            started = true
        }
    }

    private suspend fun waitForHandle(info: TorrentInfo): TorrentHandle {
        repeat(100) {
            session.find(info)?.let { return it }
            delay(100)
        }
        error("The torrent engine did not start the transfer.")
    }

    private fun chooseFile(count: Int, requested: Int?, info: (Int) -> Pair<String, Long>): Int {
        requested?.takeIf { it in 0 until count }?.let { return it }
        val videoExtensions = setOf("mp4", "mkv", "webm", "avi", "mov", "m4v", "ts", "m2ts")
        return (0 until count)
            .map { index -> Triple(index, info(index).first, info(index).second) }
            .filter { (_, name, _) -> name.substringAfterLast('.', "").lowercase() in videoExtensions }
            .maxByOrNull { it.third }?.first
            ?: (0 until count).maxByOrNull { info(it).second }
            ?: error("The torrent contains no files.")
    }
}

data class P2pPreparedStream(val url: String, val title: String, val size: Long)

private data class TorrentMedia(
    val info: TorrentInfo,
    val handle: TorrentHandle,
    val fileIndex: Int,
    val file: File,
    val size: Long,
    val displayName: String
) {
    fun waitForRange(start: Long, endInclusive: Long) {
        if (size <= 0) return
        val safeStart = start.coerceIn(0, size - 1)
        val safeEnd = endInclusive.coerceIn(safeStart, size - 1)
        val firstPiece = info.mapFile(fileIndex, safeStart, 1).piece()
        val lastPiece = info.mapFile(fileIndex, safeEnd, 1).piece()
        for (piece in firstPiece..lastPiece) {
            var loops = 0
            while (!handle.havePiece(piece)) {
                if (++loops > 1200) error("Timed out waiting for torrent piece $piece")
                Thread.sleep(100)
            }
        }
        var loops = 0
        while (!file.exists()) {
            if (++loops > 100) error("Torrent file was not created on disk.")
            Thread.sleep(50)
        }
    }
}

private class P2pHttpServer {
    private val streams = ConcurrentHashMap<String, TorrentMedia>()
    private val workers = Executors.newCachedThreadPool { runnable -> Thread(runnable, "BluStream-P2P-HTTP").apply { isDaemon = true } }
    @Volatile private var serverSocket: ServerSocket? = null

    fun start() {
        if (serverSocket != null) return
        val socket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        serverSocket = socket
        Thread({
            while (!socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                workers.execute { serve(client) }
            }
        }, "BluStream-P2P-Accept").apply { isDaemon = true; start() }
    }

    fun register(media: TorrentMedia): String {
        if (serverSocket == null) start()
        val token = UUID.randomUUID().toString().replace("-", "")
        streams[token] = media
        val port = serverSocket?.localPort ?: error("P2P HTTP server is not running.")
        return "http://127.0.0.1:$port/stream/$token/${encodePath(media.displayName)}"
    }

    private fun serve(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 30_000
            val input = BufferedInputStream(client.getInputStream())
            val output = BufferedOutputStream(client.getOutputStream())
            try {
                val headerBytes = readHeaders(input)
                if (headerBytes.isEmpty()) return
                val headerText = String(headerBytes, StandardCharsets.ISO_8859_1)
                val lines = headerText.split("\r\n")
                val request = lines.firstOrNull()?.split(' ') ?: return
                val method = request.getOrNull(0)?.uppercase() ?: return
                val path = request.getOrNull(1) ?: return
                val token = path.substringAfter("/stream/", "").substringBefore('/')
                val media = streams[token] ?: return writeSimple(output, 404, "Not Found")
                val rangeHeader = lines.firstOrNull { it.startsWith("Range:", ignoreCase = true) }?.substringAfter(':')?.trim()
                val range = parseRange(rangeHeader, media.size)
                val start = range?.first ?: 0L
                val end = range?.second ?: (media.size - 1).coerceAtLeast(0)
                val length = if (media.size == 0L) 0L else end - start + 1
                if (method != "HEAD" && length > 0) media.waitForRange(start, min(end, start + 512 * 1024 - 1))
                val status = if (range != null) "206 Partial Content" else "200 OK"
                val headers = buildString {
                    append("HTTP/1.1 $status\r\n")
                    append("Accept-Ranges: bytes\r\n")
                    append("Content-Type: ${mimeFor(media.displayName)}\r\n")
                    append("Content-Length: $length\r\n")
                    if (range != null) append("Content-Range: bytes $start-$end/${media.size}\r\n")
                    append("Connection: close\r\n\r\n")
                }
                output.write(headers.toByteArray(StandardCharsets.ISO_8859_1)); output.flush()
                if (method == "HEAD" || length <= 0) return
                RandomAccessFile(media.file, "r").use { file ->
                    file.seek(start)
                    val buffer = ByteArray(128 * 1024)
                    var position = start
                    var remaining = length
                    while (remaining > 0) {
                        val chunk = min(buffer.size.toLong(), remaining).toInt()
                        media.waitForRange(position, position + chunk - 1)
                        val read = file.read(buffer, 0, chunk)
                        if (read <= 0) break
                        output.write(buffer, 0, read); output.flush()
                        position += read; remaining -= read
                    }
                }
            } catch (_: Throwable) {
                runCatching { writeSimple(output, 500, "P2P stream error") }
            }
        }
    }

    private fun readHeaders(input: BufferedInputStream): ByteArray {
        val bytes = ArrayList<Byte>(4096)
        var matched = 0
        val marker = byteArrayOf(13, 10, 13, 10)
        while (bytes.size < 64 * 1024) {
            val value = input.read(); if (value < 0) break
            val b = value.toByte(); bytes.add(b)
            matched = if (b == marker[matched]) matched + 1 else if (b == marker[0]) 1 else 0
            if (matched == marker.size) break
        }
        return ByteArray(bytes.size) { bytes[it] }
    }

    private fun parseRange(header: String?, size: Long): Pair<Long, Long>? {
        if (header == null || !header.startsWith("bytes=", ignoreCase = true) || size <= 0) return null
        val value = header.substringAfter('=').substringBefore(',').trim()
        val first = value.substringBefore('-').trim()
        val last = value.substringAfter('-', "").trim()
        return if (first.isEmpty()) {
            val suffix = last.toLongOrNull()?.takeIf { it > 0 }?.coerceAtMost(size) ?: return null
            (size - suffix) to (size - 1)
        } else {
            val start = first.toLongOrNull()?.coerceIn(0, size - 1) ?: return null
            val end = last.toLongOrNull()?.coerceIn(start, size - 1) ?: (size - 1)
            start to end
        }
    }

    private fun writeSimple(output: BufferedOutputStream, code: Int, message: String) {
        val body = message.toByteArray(StandardCharsets.UTF_8)
        val status = if (code == 404) "404 Not Found" else "500 Internal Server Error"
        output.write("HTTP/1.1 $status\r\nContent-Type: text/plain\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray(StandardCharsets.ISO_8859_1))
        output.write(body); output.flush()
    }

    private fun mimeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "mp4", "m4v" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "ts", "m2ts" -> "video/mp2t"
        "mov" -> "video/quicktime"
        "avi" -> "video/x-msvideo"
        else -> "application/octet-stream"
    }

    private fun encodePath(value: String): String = value.replace("%", "%25").replace(" ", "%20").replace("/", "%2F").replace("?", "%3F").replace("#", "%23")
}
