package pizzk.ws.remotes

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import org.json.JSONObject
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Sockets(private val url: String) {
    companion object {
        private const val EVENT_SINK = "sink"
        private const val EVENT_SOURCE = "source"
        private const val KEY_ID = "id"
        private const val KEY_CONTENT = "content"
        private const val WHAT_SOURCE = 1000
        private const val WHAT_SINK = 1001
    }

    private val state: MutableLiveData<Result<String>> = MutableLiveData()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val sinks: MutableList<SockSink> = ArrayList()
    private var socket: Socket? = null

    private val handler: Handler = Handler(Looper.getMainLooper()) { msg ->
        val socket = this.socket ?: return@Handler true
        if (!socket.connected()) return@Handler true
        val what = msg.what
        if (WHAT_SINK == what) {
            val json = msg.obj as? JSONObject ?: return@Handler true
            val id = json.optString(KEY_ID, "")
            val content = json.optString(KEY_CONTENT, "")
            if (id.isEmpty() || content.isEmpty()) return@Handler true
            val handler = msg.target
            val source: (String) -> Unit = source@{ e ->
                val reply = handler.obtainMessage(WHAT_SOURCE)
                reply.data.putString(KEY_ID, id)
                reply.data.putString(KEY_CONTENT, e)
                reply.sendToTarget()
            }
            //利用单线程池特性，任务进入队列
            executor.execute { sinks.forEach { s -> if (s.open(content, source)) return@execute } }
        } else if (WHAT_SOURCE == what) {
            val json = JSONObject()
            json.put(KEY_ID, msg.data.getString(KEY_ID))
            json.put(KEY_CONTENT, msg.data.getString(KEY_CONTENT))
            socket.emit(EVENT_SOURCE, json)
        }
        return@Handler true
    }

    fun connect() {
        kotlin.runCatching {
            val s = socket
            if (null != s && s.connected()) return@runCatching
            disconnect()
            val options = IO.Options.builder()
                .setTransports(arrayOf(WebSocket.NAME))
                .build()
            val socket = IO.socket(url, options)
            socket.on(EVENT_SINK) { args ->
                val obj = args.firstOrNull() ?: return@on
                val msg = handler.obtainMessage(WHAT_SINK)
                msg.obj = obj
                msg.sendToTarget()
            }
            socket.on(Socket.EVENT_CONNECT) { state.postValue(Result.success(socket.id())) }
            socket.on(Socket.EVENT_DISCONNECT) { args ->
                val value = args.firstOrNull() as? String ?: "unknown"
                state.postValue(Result.failure(Exception(value)))
            }
            socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val exp = when (val value = args.firstOrNull() ?: "") {
                    is String -> Exception(value)
                    is Exception -> value
                    else -> Exception("unknown")
                }
                state.postValue(Result.failure(exp))
            }
            socket.connect()
            this.socket = socket
            return@runCatching
        }.onFailure { state.postValue(Result.failure(it)) }
    }

    fun sinks(values: List<SockSink>, clean: Boolean = true) {
        if (clean) sinks.clear()
        sinks.addAll(values)
    }

    fun disconnect() {
        handler.removeMessages(WHAT_SINK)
        handler.removeMessages(WHAT_SOURCE)
        val socket = socket ?: return
        this.socket = null
        sinks.forEach(SockSink::close)
        socket.off(EVENT_SINK)
            .off(Socket.EVENT_CONNECT)
            .off(Socket.EVENT_DISCONNECT)
            .off(Socket.EVENT_CONNECT_ERROR)
        if (socket.connected()) socket.disconnect()
    }

    fun state(): LiveData<Result<String>> = state
}