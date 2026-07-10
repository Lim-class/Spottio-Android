package com.example.spottio

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.CheckBox
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

object LanguageHelper {

    const val PREFS_NAME = "SpottioPrefs"
    const val KEY_LANG_CODE = "app_lang_code"
    const val KEY_LANG_FLAG = "app_lang_flag"
    const val KEY_LANG_NAME = "app_lang_name"

    @JvmStatic
    fun setLanguage(context: Context, code: String, flag: String, name: String) {
        // 1. Salva in locale
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(KEY_LANG_CODE, code)
            putString(KEY_LANG_FLAG, flag)
            putString(KEY_LANG_NAME, name)
            apply()
        }

        // 2. Aggiorna su Firebase
        val uid = AuthManager.getCurrentUserUid(context)
        if (!uid.isNullOrEmpty() && uid != "Guest") {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("language", code)
                .addOnFailureListener { /* Errore ignorato silenziosamente */ }
        }
    }

    @JvmStatic
    fun getCurrentLangCode(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANG_CODE, "it") ?: "it"
    }

    @JvmStatic
    fun getCurrentLangFlag(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANG_FLAG, "🇮🇹") ?: "🇮🇹"
    }

    @JvmStatic
    fun getCurrentLangName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANG_NAME, "Italiano") ?: "Italiano"
    }

    @JvmStatic
    fun getLocalizedContext(context: Context, langCode: String): Context {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    @JvmStatic
    fun configuraCheckboxLink(cbTerms: CheckBox, locCtx: Context, onPrivacyClick: Runnable?, onPolicyClick: Runnable?) {
        val htmlText = locCtx.getString(R.string.cb_terms)
        val sequence = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY)
        val strBuilder = SpannableStringBuilder(sequence)
        val urls = strBuilder.getSpans(0, sequence.length, URLSpan::class.java)

        for (span in urls) {
            val start = strBuilder.getSpanStart(span)
            val end = strBuilder.getSpanEnd(span)
            val flags = strBuilder.getSpanFlags(span)

            val clickable = object : ClickableSpan() {
                override fun onClick(view: View) {
                    if (span.url == "privacy") onPrivacyClick?.run()
                    else if (span.url == "policy") onPolicyClick?.run()
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = true
                    ds.color = Color.parseColor("#66CCB3")
                }
            }
            strBuilder.setSpan(clickable, start, end, flags)
            strBuilder.removeSpan(span)
        }
        cbTerms.text = strBuilder
        cbTerms.movementMethod = LinkMovementMethod.getInstance()
    }
}