package com.example.spottio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout

class MainActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etEmail: EditText

    // Dichiarandoli pubblici, Kotlin genererà automaticamente i metodi getter
    // (es. getLayoutEmail()) in modo che LoginUIHelper.java continui a funzionare!
    lateinit var layoutEmail: TextInputLayout
    lateinit var layoutUsername: TextInputLayout
    lateinit var layoutPassword: TextInputLayout
    lateinit var btnLogin: Button
    lateinit var btnRegister: Button
    lateinit var tvForgotPassword: TextView
    lateinit var tvSwitch: TextView
    lateinit var cbTerms: CheckBox

    private lateinit var btnStoria: Button
    private lateinit var btnPolicy: Button
    private lateinit var btnPrivacy: Button

    private var isLoginMode = true
    private var currentLangCode = "it"

    private lateinit var authManager: AuthManager
    private lateinit var uiHelper: LoginUIHelper
    private lateinit var languageSelectorManager: LanguageSelectorManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = AuthManager(this)
        authManager.checkSessionAndRedirect()

        setContentView(R.layout.activity_main)

        uiHelper = LoginUIHelper(this)
        initViews()

        currentLangCode = LanguageHelper.getCurrentLangCode(this)

        languageSelectorManager = LanguageSelectorManager(this)
        languageSelectorManager.init(object : LanguageSelectorManager.LanguageSelectionListener {
            override fun onLanguageSelected(code: String, flag: String, name: String) {
                LanguageHelper.setLanguage(this@MainActivity, code, flag, name)
                languageSelectorManager.updateLanguageUI(flag, name)
                currentLangCode = code
                aggiornaInterfacciaAlVolo(code)
            }
        })

        aggiornaInterfacciaAlVolo(currentLangCode)
        setupListeners()

        uiHelper.toggleLoginMode(isLoginMode, currentLangCode)
    }

    private fun initViews() {
        btnStoria = findViewById(R.id.btnStoria)
        btnPolicy = findViewById(R.id.btnPolicy)
        btnPrivacy = findViewById(R.id.btnPrivacy)
        cbTerms = findViewById(R.id.cbTerms)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etEmail = findViewById(R.id.etEmail)
        layoutEmail = findViewById(R.id.layoutEmail)
        layoutUsername = findViewById(R.id.layoutUsername)
        layoutPassword = findViewById(R.id.layoutPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        tvSwitch = findViewById(R.id.tvSwitch)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
    }

    private fun setupListeners() {
        btnStoria.setOnClickListener { apriInfo("storia") }
        btnPolicy.setOnClickListener { apriInfo("policy") }
        btnPrivacy.setOnClickListener { apriInfo("privacy") }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            authManager.handleLogin(email, password)
        }

        btnRegister.setOnClickListener {
            if (!cbTerms.isChecked) {
                val locCtx = LanguageHelper.getLocalizedContext(this, currentLangCode)
                Toast.makeText(this, locCtx.getString(R.string.error_terms), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            authManager.handleRegister(
                etEmail.text.toString().trim(),
                etPassword.text.toString().trim(),
                etUsername.text.toString().trim(),
                etUsername
            )
        }

        tvForgotPassword.setOnClickListener {
            uiHelper.showResetPasswordDialog(currentLangCode, authManager)
        }

        tvSwitch.setOnClickListener {
            isLoginMode = !isLoginMode
            uiHelper.toggleLoginMode(isLoginMode, currentLangCode)
        }
    }

    fun aggiornaInterfacciaAlVolo(langCode: String) {
        val locCtx = LanguageHelper.getLocalizedContext(this, langCode)

        layoutEmail.hint = locCtx.getString(R.string.hint_email)
        layoutUsername.hint = locCtx.getString(R.string.hint_username)
        layoutPassword.hint = locCtx.getString(R.string.hint_password)

        tvForgotPassword.text = locCtx.getString(R.string.forgot_password)
        btnLogin.text = locCtx.getString(R.string.btn_login)
        btnRegister.text = locCtx.getString(R.string.btn_register)
        btnStoria.text = locCtx.getString(R.string.btn_storia)
        btnPolicy.text = locCtx.getString(R.string.btn_policy)
        btnPrivacy.text = locCtx.getString(R.string.btn_privacy)

        tvSwitch.text = if (isLoginMode) locCtx.getString(R.string.switch_to_register) else locCtx.getString(R.string.switch_to_login)

        LanguageHelper.configuraCheckboxLink(
            cbTerms, locCtx,
            { apriInfo("privacy") },
            { apriInfo("policy") }
        )

        uiHelper.toggleLoginMode(isLoginMode, langCode)
    }

    fun apriInfo(tipo: String) {
        val intent = Intent(this, InfoActivity::class.java).apply {
            putExtra("info_type", tipo)
        }
        startActivity(intent)
    }
}