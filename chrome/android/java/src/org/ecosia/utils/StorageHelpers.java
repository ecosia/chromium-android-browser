package org.ecosia.utils;

import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.chromium.base.ContextUtils.getApplicationContext;

public class StorageHelpers {

    public static void saveToInternalStorage(Bitmap image, String name){
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File filesDir = contextWrapper.getFilesDir();
        File file = new File(filesDir, name);

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Bitmap loadImageFromStorage(String name) {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File filesDir = contextWrapper.getFilesDir();
        File file = new File(filesDir, name);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    public static String getPathForImage(String name) {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File filesDir = contextWrapper.getFilesDir();
        File file = new File(filesDir, name);
        return file.getAbsolutePath();
    }

    public static boolean deleteImageFromStorage(String name) {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File filesDir = contextWrapper.getFilesDir();
        File file = new File(filesDir, name);
        return file.exists() && file.delete();
    }
}
