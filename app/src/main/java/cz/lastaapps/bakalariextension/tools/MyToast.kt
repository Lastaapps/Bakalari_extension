package cz.lastaapps.bakalariextension.tools

import android.content.Context
import android.content.Intent
import android.os.Looper

/**Shows toast from not Main thread*/
class MyToast {

    companion object {
        const val LENGTH_SHORT = 0
        const val LENGTH_LONG = 1

    }
    class makeText {
        private var context: Context
        private var res: Int = 0
        private var string: String = ""
        private var length: Int = 0

        constructor(context: Context, res: Int, length: Int) {
            this.context = context
            this.res = res
            this.length = length
        }

        constructor(context: Context, string: String, length: Int) {
            this.context = context
            this.string = string
            this.length = length
        }

        fun show() {
            if (string == "")
                android.os.Handler(Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, res, length).show()
                }
            else
                android.os.Handler(Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, string, length).show()
                }
        }
    }
}