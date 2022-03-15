package pizzk.ws.remotes.impl

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.luaj.vm2.Globals
import org.luaj.vm2.LoadState
import org.luaj.vm2.LuaValue
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.*
import org.luaj.vm2.lib.jse.*
import pizzk.ws.remotes.SockSink
import java.io.File
import java.io.StringReader
import java.net.URL


//http://www.luaj.org/luaj/3.0/README.html#1
class LuaSink : SockSink {
    companion object {
        const val LUA_MAIN_METHOD = "main"
    }

    override fun identify(): String = "lua"

    override fun process(params: String, source: (String) -> Unit) {
        val name = File(params).name
        val scripts = URL(params).readText()
        val globals = newGlobals()
        globals.load(StringReader(scripts), name).call()
        val main = globals.get(LUA_MAIN_METHOD)
        if (main == LuaValue.NIL) throw Exception("not found method $LUA_MAIN_METHOD")
        val value: String = main.call().tojstring()
        source.invoke(value)
    }

    private fun newGlobals(): Globals {
        val globals = Globals()
        globals.load(JseBaseLib())
        globals.load(PackageLib())
        globals.load(Bit32Lib())
        globals.load(TableLib())
        globals.load(StringLib())
        globals.load(CoroutineLib())
        globals.load(JseMathLib())
        globals.load(JseIoLib())
        globals.load(JseOsLib())
        globals.load(LuaAndroidLib())
        LoadState.install(globals)
        LuaC.install(globals)
        return globals
    }

    //https://github.com/luaj/luaj/issues/65
    class LuaAndroidLib : LuajavaLib() {
        override fun classForName(name: String): Class<*> {
            val loader = this.javaClass.classLoader
            return Class.forName(name, true, loader)
        }
    }

    //local lua script test code
    fun local(context: Context) {
        val callback: (Result<String>) -> Unit = { result ->
            val msg = if (result.isFailure) {
                result.exceptionOrNull()?.message ?: "Exception"
            } else {
                result.getOrNull() ?: ""
            }
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
        val execute: () -> String = process@{
            val name = "index.lua"
            val scripts = context.assets.open(name).use { it.reader().readText() }
            val globals = newGlobals()
            globals.load(StringReader(scripts), name).call()
            val main = globals.get(LUA_MAIN_METHOD)
            if (main == LuaValue.NIL) throw Exception("not found method $LUA_MAIN_METHOD")
            val value = main.call()
            return@process value.tojstring()
        }
        Thread {
            kotlin.runCatching {
                callback(Result.success(execute()))
            }.onFailure { callback(Result.failure(it)) }
        }.start()
    }
}