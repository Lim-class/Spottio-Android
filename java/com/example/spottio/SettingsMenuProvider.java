package com.example.spottio;

import java.util.ArrayList;
import java.util.List;

public class SettingsMenuProvider {

    public static List<SettingsAdapter.SettingItem> getMenuItems() {
        List<SettingsAdapter.SettingItem> menuItems = new ArrayList<>();
        menuItems.add(new SettingsAdapter.SettingItem("Cambio Password", SettingsAdapter.SettingType.PASSWORD));
        menuItems.add(new SettingsAdapter.SettingItem("Cambio Email", SettingsAdapter.SettingType.CAMBIO_EMAIL));
        menuItems.add(new SettingsAdapter.SettingItem("Colore di Sfondo", SettingsAdapter.SettingType.COLORE_SFONDO));
        menuItems.add(new SettingsAdapter.SettingItem("Scarica App (APK)", SettingsAdapter.SettingType.SCARICA_APK));
        menuItems.add(new SettingsAdapter.SettingItem("Privacy Profilo", SettingsAdapter.SettingType.PRIVACY_PROFILO));
        menuItems.add(new SettingsAdapter.SettingItem("Esporta Dati", SettingsAdapter.SettingType.ESPORTA_DATI));
        menuItems.add(new SettingsAdapter.SettingItem("Storia", SettingsAdapter.SettingType.STORIA));
        menuItems.add(new SettingsAdapter.SettingItem("Informativa Privacy", SettingsAdapter.SettingType.INFORMATIVA_PRIVACY));
        menuItems.add(new SettingsAdapter.SettingItem("Policy della Community", SettingsAdapter.SettingType.POLICY_COMMUNITY));
        menuItems.add(new SettingsAdapter.SettingItem("Elimina Account", SettingsAdapter.SettingType.ELIMINA_ACCOUNT));

        return menuItems;
    }
}