package com.example.spottio

import android.content.Context
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LanguageSelectorManager {

    private val context: Context
    private val layoutLanguage: View?
    private val tvFlag: TextView?
    private val tvLangName: TextView?

    // Costruttore per Activity
    constructor(activity: AppCompatActivity) {
        this.context = activity
        this.layoutLanguage = activity.findViewById(R.id.layoutLanguage)
        this.tvFlag = activity.findViewById(R.id.tvFlag)
        this.tvLangName = activity.findViewById(R.id.tvLangName)
    }

    // Costruttore per Fragment (USA LA ROOT VIEW)
    constructor(context: Context, rootView: View) {
        this.context = context
        this.layoutLanguage = rootView.findViewById(R.id.layoutLanguage)
        this.tvFlag = rootView.findViewById(R.id.tvFlag)
        this.tvLangName = rootView.findViewById(R.id.tvLangName)
    }

    fun init(selectionListener: LanguageSelectionListener?) {
        tvFlag?.text = LanguageHelper.getCurrentLangFlag(context)
        tvLangName?.text = LanguageHelper.getCurrentLangName(context)

        layoutLanguage?.setOnClickListener { v ->
            if (selectionListener != null) {
                showLanguageMenu(v, selectionListener)
            }
        }
    }

    private fun showLanguageMenu(v: View, listener: LanguageSelectionListener) {
        val popup = PopupMenu(context, v)
        val languages = arrayOf(
            "🇸🇦 العربية", "🇨🇳 中文", "🇰🇵 조선말", "🇩🇪 Deutsch",
            "🇬🇧 English", "🇪🇸 Español", "🇫🇷 Français", "🇮🇳 हिन्दी",
            "🇮🇹 Italiano", "🇯🇵 日本語", "🇷🇺 Русский"
        )

        for (i in languages.indices) {
            popup.menu.add(Menu.NONE, i, i, languages[i])
        }

        popup.setOnMenuItemClickListener { item ->
            var langTag = "it"
            var flag = "🇮🇹"
            var name = "Italiano"

            when (item.itemId) {
                0 -> { langTag = "ar"; flag = "🇸🇦"; name = "العربية" }
                1 -> { langTag = "zh"; flag = "🇨🇳"; name = "中文" }
                2 -> { langTag = "ko"; flag = "🇰🇵"; name = "조선말" }
                3 -> { langTag = "de"; flag = "🇩🇪"; name = "Deutsch" }
                4 -> { langTag = "en"; flag = "🇬🇧"; name = "English" }
                5 -> { langTag = "es"; flag = "🇪🇸"; name = "Español" }
                6 -> { langTag = "fr"; flag = "🇫🇷"; name = "Français" }
                7 -> { langTag = "hi"; flag = "🇮🇳"; name = "हिन्दी" }
                8 -> { langTag = "it"; flag = "🇮🇹"; name = "Italiano" }
                9 -> { langTag = "ja"; flag = "🇯🇵"; name = "日本語" }
                10 -> { langTag = "ru"; flag = "🇷🇺"; name = "Русский" }
            }

            listener.onLanguageSelected(langTag, flag, name)
            true
        }
        popup.show()
    }

    fun updateLanguageUI(flag: String, name: String) {
        tvFlag?.text = flag
        tvLangName?.text = name
    }

    interface LanguageSelectionListener {
        fun onLanguageSelected(code: String, flag: String, name: String)
    }
}