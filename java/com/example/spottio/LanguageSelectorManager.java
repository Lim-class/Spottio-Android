package com.example.spottio;

import android.content.Context;
import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Gestisce esclusivamente il componente visivo del selettore della lingua e il suo menu.
 */
public class LanguageSelectorManager {

    private final Context context;
    private final View layoutLanguage;
    private final TextView tvFlag;
    private final TextView tvLangName;

    // Costruttore per Activity
    public LanguageSelectorManager(AppCompatActivity activity) {
        this.context = activity;
        this.layoutLanguage = activity.findViewById(R.id.layoutLanguage);
        this.tvFlag = activity.findViewById(R.id.tvFlag);
        this.tvLangName = activity.findViewById(R.id.tvLangName);
    }

    // Costruttore per Fragment (USA LA ROOT VIEW)
    public LanguageSelectorManager(Context context, View rootView) {
        this.context = context;
        this.layoutLanguage = rootView.findViewById(R.id.layoutLanguage);
        this.tvFlag = rootView.findViewById(R.id.tvFlag);
        this.tvLangName = rootView.findViewById(R.id.tvLangName);
    }

    public void init(LanguageSelectionListener selectionListener) {
        if (tvFlag != null) {
            tvFlag.setText(LanguageHelper.getCurrentLangFlag(context));
        }
        if (tvLangName != null) {
            tvLangName.setText(LanguageHelper.getCurrentLangName(context));
        }

        if (layoutLanguage != null) {
            layoutLanguage.setOnClickListener(v -> {
                if (selectionListener != null) {
                    showLanguageMenu(v, selectionListener);
                }
            });
        }
    }

    private void showLanguageMenu(View v, LanguageSelectionListener listener) {
        PopupMenu popup = new PopupMenu(context, v);
        String[] languages = {
                "🇸🇦 العربية", "🇨🇳 中文", "🇰🇵 조선말", "🇩🇪 Deutsch",
                "🇬🇧 English", "🇪🇸 Español", "🇫🇷 Français", "🇮🇳 हिन्दी",
                "🇮🇹 Italiano", "🇯🇵 日本語", "🇷🇺 Русский"
        };

        for (int i = 0; i < languages.length; i++) {
            popup.getMenu().add(Menu.NONE, i, i, languages[i]);
        }

        popup.setOnMenuItemClickListener(item -> {
            String langTag = "it", flag = "🇮🇹", name = "Italiano";

            // JAVA 17: Switch expression senza 'break'
            switch (item.getItemId()) {
                case 0 -> { langTag = "ar"; flag = "🇸🇦"; name = "العربية"; }
                case 1 -> { langTag = "zh"; flag = "🇨🇳"; name = "中文"; }
                case 2 -> { langTag = "ko"; flag = "🇰🇵"; name = "조선말"; }
                case 3 -> { langTag = "de"; flag = "🇩🇪"; name = "Deutsch"; }
                case 4 -> { langTag = "en"; flag = "🇬🇧"; name = "English"; }
                case 5 -> { langTag = "es"; flag = "🇪🇸"; name = "Español"; }
                case 6 -> { langTag = "fr"; flag = "🇫🇷"; name = "Français"; }
                case 7 -> { langTag = "hi"; flag = "🇮🇳"; name = "हिन्दी"; }
                case 8 -> { langTag = "it"; flag = "🇮🇹"; name = "Italiano"; }
                case 9 -> { langTag = "ja"; flag = "🇯🇵"; name = "日本語"; }
                case 10 -> { langTag = "ru"; flag = "🇷🇺"; name = "Русский"; }
            }

            if (listener != null) {
                listener.onLanguageSelected(langTag, flag, name);
            }
            return true;
        });
        popup.show();
    }

    public void updateLanguageUI(String flag, String name) {
        if (tvFlag != null) tvFlag.setText(flag);
        if (tvLangName != null) tvLangName.setText(name);
    }

    public interface LanguageSelectionListener {
        void onLanguageSelected(String code, String flag, String name);
    }
}