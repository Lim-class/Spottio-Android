package com.example.spottio;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;

import java.util.Locale;

public class LanguageHelper {

    // CHIAVI UNIVOCHE CENTRALIZZATE
    public static final String PREFS_NAME = "SpottioPrefs";
    public static final String KEY_LANG_CODE = "app_lang_code";
    public static final String KEY_LANG_FLAG = "app_lang_flag";
    public static final String KEY_LANG_NAME = "app_lang_name";

    // Metodo unico per salvare la lingua
    public static void setLanguage(Context context, String code, String flag, String name) {
        // 1. Salva comunque in locale (per averla subito pronta all'avvio prima del login)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_LANG_CODE, code)
                .putString(KEY_LANG_FLAG, flag)
                .putString(KEY_LANG_NAME, name)
                .apply();

        // 2. Se l'utente è loggato, aggiorna anche il suo profilo su Firebase Firestore
        String uid = AuthManager.getCurrentUserUid(context);
        if (uid != null && !uid.isEmpty() && !uid.equals("Guest")) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("language", code)
                    .addOnFailureListener(e -> {
                        // Logga l'errore o gestisci silenziosamente
                    });
        }
    }

    // Metodi unici per leggere i dati della lingua salvata
    public static String getCurrentLangCode(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANG_CODE, "it");
    }

    public static String getCurrentLangFlag(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANG_FLAG, "🇮🇹");
    }

    public static String getCurrentLangName(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANG_NAME, "Italiano");
    }

    // Genera un contesto tradotto al volo
    public static Context getLocalizedContext(Context context, String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

    // Crea i link cliccabili nella Checkbox in base alla lingua
    public static void configuraCheckboxLink(CheckBox cbTerms, Context locCtx, Runnable onPrivacyClick, Runnable onPolicyClick) {
        String htmlText = locCtx.getString(R.string.cb_terms);
        CharSequence sequence = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY);
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);

        for (URLSpan span : urls) {
            int start = strBuilder.getSpanStart(span);
            int end = strBuilder.getSpanEnd(span);
            int flags = strBuilder.getSpanFlags(span);

            ClickableSpan clickable = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View view) {
                    if (span.getURL().equals("privacy") && onPrivacyClick != null) {
                        onPrivacyClick.run();
                    } else if (span.getURL().equals("policy") && onPolicyClick != null) {
                        onPolicyClick.run();
                    }
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(Color.parseColor("#66CCB3"));
                }
            };
            strBuilder.setSpan(clickable, start, end, flags);
            strBuilder.removeSpan(span);
        }
        cbTerms.setText(strBuilder);
        cbTerms.setMovementMethod(LinkMovementMethod.getInstance());
    }
}