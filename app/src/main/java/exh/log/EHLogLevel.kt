package exh.log

import android.content.Context
import androidx.preference.PreferenceManager

enum class EHLogLevel(val nameRes: String, val description: String) {
    MINIMAL("Minimal", "Critical errors only"),
    EXTRA("Extra", "Log everything"),
    EXTREME("Extreme", "Network inspection mode"),
    ;

    companion object {
        private var curLogLevel: Int? = null

        val currentLogLevel get() = entries[curLogLevel!!]

        fun init(context: Context) {
            curLogLevel = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt("eh_log_level", MINIMAL.ordinal)
        }

        fun shouldLog(requiredLogLevel: EHLogLevel): Boolean {
            return (curLogLevel ?: MINIMAL.ordinal) >= requiredLogLevel.ordinal
        }
    }
}
