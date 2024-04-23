package org.ecosia.utils;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.chromium.base.task.AsyncTask;
import org.chromium.net.ChromiumNetworkAdapter;
import org.chromium.net.NetworkTrafficAnnotationTag;

import java.io.IOException;
import java.io.InputStream;

import static org.chromium.base.ContextUtils.getApplicationContext;
import static org.ecosia.ntp.NewsManager.ACTION_IMAGE_DOWNLOADED;

public class DownloadImageTask extends AsyncTask<Bitmap> {
    private static final String IMAGE_NAME = "image_";
    private static final String TAG = DownloadImageTask.class.getSimpleName();
    private final int mPosition;
    private final String mUrl;

    public DownloadImageTask(int position, String url) {
        mPosition = position;
        mUrl = url;
    }

    @Override
    protected Bitmap doInBackground() {
        Bitmap image = null;
        InputStream inputStream = null;
        try {
            inputStream = ChromiumNetworkAdapter.openStream(new java.net.URL(mUrl), NetworkTrafficAnnotationTag.NO_TRAFFIC_ANNOTATION_YET);
            image = BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return image;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (result != null) {
            StorageHelpers.saveToInternalStorage(result, IMAGE_NAME + mPosition);
        } else {
            StorageHelpers.deleteImageFromStorage(IMAGE_NAME + mPosition);
        }

        Intent intent = new Intent(ACTION_IMAGE_DOWNLOADED);
        getApplicationContext().sendBroadcast(intent);
    }
}
