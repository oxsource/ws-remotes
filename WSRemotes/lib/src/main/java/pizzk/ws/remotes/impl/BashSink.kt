package pizzk.ws.remotes.impl

import pizzk.ws.remotes.SockSink
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class BashSink : SockSink {
    companion object {
        private const val PROGRAM = "/system/bin/sh"
        private const val PARAMS = "-c"
    }

    @Volatile
    private var process: Process? = null

    override fun identify(): String = "sh"

    override fun process(params: String, source: (String) -> Unit) {
        //write adb shell cmd
        if (params.isEmpty()) throw Exception("write cmd is empty")
        val pb: ProcessBuilder = ProcessBuilder()
            .command(PROGRAM, PARAMS, params)
            .redirectErrorStream(true)
        val ps = pb.start()
        process = ps
        //read adb shell results
        val ins: InputStream = ps.inputStream
        val reader = BufferedReader(InputStreamReader(ins))
        var line: String?
        while (null != process) {
            line = reader.readLine()
            source(line ?: "")
            if (null == line) break
        }
    }

    override fun close() {
        val ps: Process = process ?: return
        kotlin.runCatching { ps.inputStream.close() }
        kotlin.runCatching { ps.destroy() }
        process = null
    }
}