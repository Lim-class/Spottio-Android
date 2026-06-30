package com.example.spottio;

import java.util.HashMap;
import java.util.Map;

public class DeviceInfoHelper {

    /**
     * Raccoglie i dati tecnici del dispositivo, la versione dell'app e il fuso orario.
     * @return Una Mappa contenente i campi pronti per essere inviati a Firestore.
     */
    public static Map<String, Object> getDeviceSpecs() {
        Map<String, Object> map = new HashMap<>();
        map.put("last_device", android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL +
                " (Android " + android.os.Build.VERSION.RELEASE + ")");
        map.put("app_version", BuildConfig.VERSION_NAME);
        map.put("timezone", java.util.TimeZone.getDefault().getID());
        return map;
    }
}