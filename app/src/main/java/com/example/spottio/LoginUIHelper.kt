package com.example.spottio

import android.text.InputType
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog

class LoginUIHelper(private val activity: MainActivity) {

    fun toggleLoginMode(isLoginMode: Boolean, langCode: String) {
        val locCtx = LanguageHelper.getLocalizedContext(activity, langCode)

        // Assumiamo che getter come `getLayoutEmail` siano diventati property pubbliche in Kotlin nel `MainActivity`
        // Se in MainActivity.kt li hai mantenuti privati con getter personalizzati, potresti dover usare i loro nomi specifici
        activity.layoutEmail.visibility = View.VISIBLE
        activity.layoutUsername.visibility = if (isLoginMode) View.GONE else View.VISIBLE

        activity.btnLogin.visibility = if (isLoginMode) View.VISIBLE else View.GONE
        activity.btnRegister.visibility = if (isLoginMode) View.GONE else View.VISIBLE
        activity.tvForgotPassword.visibility = if (isLoginMode) View.VISIBLE else View.GONE
        activity.cbTerms.visibility = if (isLoginMode) View.GONE else View.VISIBLE

        activity.tvSwitch.text = locCtx.getString(
            if (isLoginMode) R.string.switch_to_register else R.string.switch_to_login
        )
    }

    // Mostra il popup (Dialog) per il recupero della password
    fun showResetPasswordDialog(currentLangCode: String, authManager: AuthManager) {
        val locCtx = LanguageHelper.getLocalizedContext(activity, currentLangCode)
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(locCtx.getString(R.string.dialog_reset_title))
        builder.setMessage(locCtx.getString(R.string.dialog_reset_msg))

        val input = EditText(activity)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        builder.setView(input)

        builder.setPositiveButton(locCtx.getString(R.string.btn_send)) { _, _ ->
            val text = input.text.toString().trim()
            if (!TextUtils.isEmpty(text)) {
                authManager.gestisciRecuperoPassword(text)
            }
        }

        builder.setNegativeButton(locCtx.getString(R.string.btn_cancel)) { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }
}