package com.example.spottio;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;

public class LoginUIHelper {

    private final MainActivity activity;

    // 1. IL COSTRUTTORE MANCANTE (Risolve l'errore: "constructor cannot be applied...")
    public LoginUIHelper(MainActivity activity) {
        this.activity = activity;
    }

    // 2. L'UNICO METODO TOGGLE (Risolve l'errore: "method is already defined...")
    public void toggleLoginMode(boolean isLoginMode, String langCode) {
        Context locCtx = LanguageHelper.getLocalizedContext(activity, langCode);

        // L'email serve sempre (sia per accedere che per registrarsi)
        activity.getLayoutEmail().setVisibility(View.VISIBLE);

        // L'username serve SOLO nella registrazione
        activity.getLayoutUsername().setVisibility(isLoginMode ? View.GONE : View.VISIBLE);

        activity.getBtnLogin().setVisibility(isLoginMode ? View.VISIBLE : View.GONE);
        activity.getBtnRegister().setVisibility(isLoginMode ? View.GONE : View.VISIBLE);
        activity.getTvForgotPassword().setVisibility(isLoginMode ? View.VISIBLE : View.GONE);
        activity.getCbTerms().setVisibility(isLoginMode ? View.GONE : View.VISIBLE);

        activity.getTvSwitch().setText(locCtx.getString(
                isLoginMode ? R.string.switch_to_register : R.string.switch_to_login));
    }

    // Mostra il popup (Dialog) per il recupero della password
    public void showResetPasswordDialog(String currentLangCode, AuthManager authManager) {
        Context locCtx = LanguageHelper.getLocalizedContext(activity, currentLangCode);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(locCtx.getString(R.string.dialog_reset_title));
        builder.setMessage(locCtx.getString(R.string.dialog_reset_msg));

        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        builder.setPositiveButton(locCtx.getString(R.string.btn_send), (dialog, which) -> {
            String text = input.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                authManager.gestisciRecuperoPassword(text);
            }
        });

        builder.setNegativeButton(locCtx.getString(R.string.btn_cancel), (dialog, which) -> dialog.cancel());
        builder.show();
    }
}