package com.example.spottio

object SettingsMenuProvider {

    fun getMenuItems(): List<SettingsAdapter.SettingItem> {
        // In Kotlin usiamo listOf() per creare liste immutabili in modo ultra-compatto
        return listOf(
            SettingsAdapter.SettingItem("Cambio Password", SettingsAdapter.SettingType.PASSWORD),
            SettingsAdapter.SettingItem("Cambio Email", SettingsAdapter.SettingType.CAMBIO_EMAIL),
            SettingsAdapter.SettingItem("Colore di Sfondo", SettingsAdapter.SettingType.COLORE_SFONDO),
            SettingsAdapter.SettingItem("Scarica App (APK)", SettingsAdapter.SettingType.SCARICA_APK),
            SettingsAdapter.SettingItem("Privacy Profilo", SettingsAdapter.SettingType.PRIVACY_PROFILO),
            SettingsAdapter.SettingItem("Esporta Dati", SettingsAdapter.SettingType.ESPORTA_DATI),
            SettingsAdapter.SettingItem("Storia", SettingsAdapter.SettingType.STORIA),
            SettingsAdapter.SettingItem("Informativa Privacy", SettingsAdapter.SettingType.INFORMATIVA_PRIVACY),
            SettingsAdapter.SettingItem("Policy della Community", SettingsAdapter.SettingType.POLICY_COMMUNITY),
            SettingsAdapter.SettingItem("Elimina Account", SettingsAdapter.SettingType.ELIMINA_ACCOUNT)
        )
    }
}