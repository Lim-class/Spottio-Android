package com.example.spottio

import android.os.Build
import java.util.TimeZone

object DeviceInfoHelper {

    /**
     * Raccoglie i dati tecnici del dispositivo, la versione dell'app e il fuso orario.
     * @return Una Mappa contenente i campi pronti per essere inviati a Firestore.
     */
    @JvmStatic
    fun getDeviceSpecs(): Map<String, Any> {
        return mapOf(
            "last_device" to "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})",
            "app_version" to BuildConfig.VERSION_NAME,
            "timezone" to TimeZone.getDefault().id
        )
    }
}