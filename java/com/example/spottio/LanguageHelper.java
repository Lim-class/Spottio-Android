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