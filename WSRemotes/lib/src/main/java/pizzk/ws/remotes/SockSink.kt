package pizzk.ws.remotes

import java.util.*

interface SockSink {
    fun identify(): String

    fun open(s: String, source: (String) -> Unit): Boolean {
        val symbol = identify()
        if (!s.lowercase(Locale.getDefault()).startsWith(symbol)) return false
        val params = s.substring(symbol.length).trim()
        close()
        kotlin.runCatching {
            process(params, source)
        }.onFailure { exp ->
            source("${identify()} failed via ${exp.message}")
            close()
        }
        return true
    }

    fun close() = Unit

    fun process(params: String, source: (String) -> Unit)
}