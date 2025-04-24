package com.example.bgwf.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SharedPreferencesHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(
            "user_prefs",
            Context.MODE_PRIVATE
        )

    fun saveToken(token: String) {
        prefs.edit { putString("access_token", token) }
    }

    fun getToken(): String? {
        return prefs.getString("access_token", null)
    }

    fun clearToken() {
        prefs.edit { remove("access_token") }
    }

    /**
     * Проверяет, следует ли показывать подсказку при оценке игры (по умолчанию true)
     */
    fun shouldShowRatingHint(): Boolean {
        return prefs.getBoolean("show_rating_hint", true)
    }

    /**
     * Устанавливает, показывать ли подсказку при оценке игры в будущем
     */
    fun setShowRatingHint(show: Boolean) {
        prefs.edit { putBoolean("show_rating_hint", show) }
    }
}
