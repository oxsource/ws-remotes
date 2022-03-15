package pizzk.ws.remotes.app

import android.util.Log

object Defaults {
    private const val TAG = "Defaults"

    fun version(): String = "fsp-1.9.0"

    fun echo() = Log.d(TAG, "echo")
}