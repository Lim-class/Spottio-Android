package com.example.spottio

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

object FileExportHelper {

    fun salvaInCartellaDownloadPubblica(ctx: Context?, nomeFile: String, mimeType: String, contenuto: String) {
        if (ctx == null) return

        try {
            var tempOutputStream: OutputStream? = null

            // 1. Creazione del file in base alla versione di Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = ctx.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, nomeFile)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (fileUri != null) {
                    tempOutputStream = resolver.openOutputStream(fileUri)
                }
            } else {
                @Suppress("DEPRECATION")
                val cartellaDownload = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val fileFisico = File(cartellaDownload, nomeFile)
                tempOutputStream = FileOutputStream(fileFisico)
            }

            // 2. Scrittura del file con .use() che chiude automaticamente lo stream
            tempOutputStream?.use { outputStream ->
                outputStream.write(contenuto.toByteArray())
                Toast.makeText(ctx, "File salvato con successo nella cartella 'Download'!", Toast.LENGTH_LONG).show()
            } ?: run {
                Toast.makeText(ctx, "Impossibile creare il file di esportazione.", Toast.LENGTH_SHORT).show()
            }

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(ctx, "Errore di scrittura durante l'esportazione.", Toast.LENGTH_SHORT).show()
        }
    }
}