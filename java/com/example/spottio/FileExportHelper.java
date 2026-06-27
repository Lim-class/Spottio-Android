package com.example.spottio;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FileExportHelper {

    public static void salvaInCartellaDownloadPubblica(Context ctx, String nomeFile, String mimeType, String contenuto) {
        if (ctx == null) return;

        try {
            OutputStream tempOutputStream = null;

            // 1. Creazione del file in base alla versione di Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = ctx.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, nomeFile);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (fileUri != null) {
                    tempOutputStream = resolver.openOutputStream(fileUri);
                }
            } else {
                File cartellaDownload = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File fileFisico = new File(cartellaDownload, nomeFile);
                tempOutputStream = new FileOutputStream(fileFisico);
            }

            // 2. Scrittura del file con Try-With-Resources (chiude automaticamente lo stream)
            if (tempOutputStream != null) {
                try (OutputStream outputStream = tempOutputStream) {
                    outputStream.write(contenuto.getBytes());
                    Toast.makeText(ctx, "File salvato con successo nella cartella 'Download'!", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(ctx, "Impossibile creare il file di esportazione.", Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(ctx, "Errore di scrittura durante l'esportazione.", Toast.LENGTH_SHORT).show();
        }
    }
}