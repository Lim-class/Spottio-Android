package com.example.spottio;

import java.util.List;

public class SettingsMenuProvider {

    public static List<SettingsAdapter.SettingItem> getMenuItems() {
        // Novità: List.of() crea una lista immutabile in modo ultra-compatto
        return List.of(
                new SettingsAdapter.SettingItem("Cambio Password", SettingsAdapter.SettingType.PASSWORD),
                new SettingsAdapter.SettingItem("Cambio Email", SettingsAdapter.SettingType.CAMBIO_EMAIL),
                new SettingsAdapter.SettingItem("Colore di Sfondo", SettingsAdapter.SettingType.COLORE_SFONDO),
                new SettingsAdapter.SettingItem("Scarica App (APK)", SettingsAdapter.SettingType.SCARICA_APK),
                new SettingsAdapter.SettingItem("Privacy Profilo", SettingsAdapter.SettingType.PRIVACY_PROFILO),
                new SettingsAdapter.SettingItem("Esporta Dati", SettingsAdapter.SettingType.ESPORTA_DATI),
                new SettingsAdapter.SettingItem("Storia", SettingsAdapter.SettingType.STORIA),
                new SettingsAdapter.SettingItem("Informativa Privacy", SettingsAdapter.SettingType.INFORMATIVA_PRIVACY),
                new SettingsAdapter.SettingItem("Policy della Community", SettingsAdapter.SettingType.POLICY_COMMUNITY),
                new SettingsAdapter.SettingItem("Elimina Account", SettingsAdapter.SettingType.ELIMINA_ACCOUNT)
        );
    }
}