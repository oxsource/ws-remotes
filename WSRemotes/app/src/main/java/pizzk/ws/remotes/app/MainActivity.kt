package pizzk.ws.remotes.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import pizzk.ws.remotes.Sockets
import pizzk.ws.remotes.impl.BashSink
import pizzk.ws.remotes.impl.LuaSink

class MainActivity : AppCompatActivity() {
    private val socket = Sockets("ws://192.168.31.120:9000/devices")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val vStatus = findViewById<TextView>(R.id.vStatus)
        socket.state().observe(this) { result ->
            val status = if (result.isSuccess) {
                "connected(${result.getOrNull()})"
            } else {
                "disconnect(${result.exceptionOrNull()?.cause?.message})"
            }
            vStatus.text = status
        }
        socket.sinks(listOf(BashSink(), LuaSink()), clean = true)
        vStatus.setOnClickListener { socket.connect() }
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.disconnect()
    }
}