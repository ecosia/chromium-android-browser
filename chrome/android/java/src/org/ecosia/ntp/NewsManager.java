package org.ecosia.ntp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;

import org.chromium.base.Log;
import org.chromium.base.task.AsyncTask;
import org.chromium.components.version_info.VersionInfo;
import org.ecosia.utils.DownloadImageTask;
import org.ecosia.utils.Requests;
import org.ecosia.utils.RetrieveDataTask;
import org.ecosia.utils.RetrieveDelegate;
import org.ecosia.utils.SharedPreferencesHelpers;
import org.ecosia.utils.TimestampHelpers;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NewsManager {

    private static final int FETCH_INTERVAL = 24 * 60 * 60 * 1000;
    private static final int RETRY_DELAY = 60 * 1000;
    private static final int NEWS_LIMIT = 3;
    public static final String ACTION_NEWS_UPDATE = "com.ecosia.NEWS_UPDATE";
    public static final String ACTION_IMAGE_DOWNLOADED = "com.ecosia.IMAGE_DOWNLOADED";
    private static final String PREF_ECOSIA_NEWS = "PREF_ECOSIA_NEWS";
    private static final String PREF_ECOSIA_NEWS_RETRIEVE_TIMESTAMP = "PREF_ECOSIA_NEWS_RETRIEVE_TIMESTAMP";
    private final String TAG = getClass().getSimpleName();
    private static final String LANGUAGE_KEY = "language";
    private static final String LIMIT_KEY = "limit";

    private Runnable mRunnable;
    private Handler mHandler;
    private int mExpectedImages;
    private int mDownloadedImages;
    private int mRetryTime;
    private Context mContext;

    public NewsManager(Context context) {
        mContext = context.getApplicationContext();
        mRetryTime = RETRY_DELAY;
        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                retrieve();
            }
        };

        BroadcastReceiver imageDownloadReceiver = new ImageDownloadReceiver();
        IntentFilter mImageDownloadFilter = new IntentFilter(ACTION_IMAGE_DOWNLOADED);
        mContext.registerReceiver(imageDownloadReceiver, mImageDownloadFilter);
    }

    private boolean isRetrieveTimeElapsed() {
        long nextRetrieveTimestamp = SharedPreferencesHelpers.getLong(mContext,
                PREF_ECOSIA_NEWS_RETRIEVE_TIMESTAMP, 0);
        return System.currentTimeMillis() > nextRetrieveTimestamp;
    }

    private void setNextRetrieveTimestamp() {
        long timestamp = System.currentTimeMillis();
        long nextTimestamp = timestamp + FETCH_INTERVAL;
        SharedPreferencesHelpers.putLong(mContext, PREF_ECOSIA_NEWS_RETRIEVE_TIMESTAMP, nextTimestamp);
    }

    public void scheduleNewsDownload() {
        if (isRetrieveTimeElapsed()) {
            mHandler.post(mRunnable);
        }
    }

    private void onNewsReceived(List<News> news) {
        Log.d(TAG, "Number of news received: " + news.size());
        if (news.isEmpty()) {
            mHandler.postDelayed(mRunnable, mRetryTime);
            if (mRetryTime * 2 < FETCH_INTERVAL) {
                mRetryTime = mRetryTime * 2;
            } else {
                mRetryTime = FETCH_INTERVAL;
            }
        } else {
            mHandler.postDelayed(mRunnable, FETCH_INTERVAL);
            mRetryTime = RETRY_DELAY;
            setNextRetrieveTimestamp();
            if (shouldUpdateNews(news)) {
                updateNewsData(news);
            }
        }
    }

    private void retrieve() {
        RetrieveDelegate retrieveDelegate = new RetrieveDelegate<String>() {
            @Override
            public String doInBackground() {
                try {
                    String url = createNewsUrl();
                    return new Requests().downloadContent(url);
                } catch (IOException | RuntimeException e) {
                    if (e.getMessage() != null) {
                        Log.d(TAG, "Fetch failed: " + e.getMessage());
                    }
                    return "";
                }
            }

            @Override
            public void onPostExecute(String result) {
                List<News> news = new ArrayList<>();
                try {
                    JSONArray jsonArray = new JSONArray(result);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonNews = jsonArray.getJSONObject(i);
                        news.add(new News(jsonNews));
                    }
                } catch (JSONException | MalformedURLException e) {
                    news.clear();
                }
                onNewsReceived(news);
            }
        };
        new RetrieveDataTask(retrieveDelegate).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private String createNewsUrl() {
        int urlResource = org.chromium.chrome.R.string.cards_news_url;
        String urlBase = mContext.getResources().getString(urlResource);
        Uri.Builder uriBuilder = Uri.parse(urlBase).buildUpon();
        String language = Locale.getDefault().getLanguage();
        uriBuilder.appendQueryParameter(LANGUAGE_KEY, language);
        uriBuilder.appendQueryParameter(LIMIT_KEY, String.valueOf(NEWS_LIMIT));
        return uriBuilder.toString();
    }

    private boolean shouldUpdateNews(List<News> news) {
        if (didLanguageChange(news)) {
            return true;
        }
        int topPosition = 0;
        News topNews = news.get(topPosition);
        long topNewsTimestamp = topNews.getTimestamp();
        long storedNewsTimestamp = getStoredNewsTimestamp();
        return topNewsTimestamp != storedNewsTimestamp;
    }

    private long getStoredNewsTimestamp() {
        String serialized = SharedPreferencesHelpers.getString(mContext, PREF_ECOSIA_NEWS, "");
        try {
            JSONArray jsonArray = new JSONArray(serialized);
            List<News> newsList = getNewsListFromJsonArray(jsonArray);
            return newsList.get(0).getTimestamp();
        } catch (JSONException | MalformedURLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String getStoredNewsLanguage() {
        String serialized = SharedPreferencesHelpers.getString(mContext, PREF_ECOSIA_NEWS, "");
        try {
            JSONArray jsonArray = new JSONArray(serialized);
            List<News> newsList = getNewsListFromJsonArray(jsonArray);
            if (newsList.size() > 0) {
                return newsList.get(0).getLanguage();
            }
        } catch (JSONException | MalformedURLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private boolean didLanguageChange(List<News> news) {
        String topNewsLanguage = news.get(0).getLanguage();
        String storedNewsLanguage = getStoredNewsLanguage();
        return !topNewsLanguage.equals(storedNewsLanguage);
    }

    private void updateNewsData(List<News> news) {
        List<News> localizedNews = getDisplayableItems(news);
        int newsCount = localizedNews.size();
        try {
            String serialized = toJsonArray(localizedNews).toString();
            SharedPreferencesHelpers.putString(mContext, PREF_ECOSIA_NEWS, serialized);
            prepareImageDownload(newsCount);
            for (int i = 0; i < newsCount; i++) {
                News newsItem = localizedNews.get(i);
                String imageUrl = newsItem.getImageUrl().toString();
                new DownloadImageTask(i, imageUrl).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private List<News> getDisplayableItems(List<News> news) {
        List<News> displayableItems = new ArrayList<>();
        long systemTimestamp = System.currentTimeMillis();
        for (News newsItem : news) {
            long newsTimestamp = newsItem.getTimestamp();
            if (systemTimestamp > newsTimestamp) {
                displayableItems.add(newsItem);
            }
        }
        int newsCount = displayableItems.size() >= NEWS_LIMIT ? NEWS_LIMIT : displayableItems.size();
        return displayableItems.subList(0, newsCount);
    }

    private void prepareImageDownload(int expectedImages) {
        mExpectedImages = expectedImages;
        mDownloadedImages = 0;
    }

    public List<News> getStoredNews() {
        String serialized = SharedPreferencesHelpers.getString(mContext, PREF_ECOSIA_NEWS, "");
        try {
            JSONArray jsonArray = new JSONArray(serialized);
            return getNewsListFromJsonArray(jsonArray);
        } catch (JSONException | MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject toJsonObject(News news) throws JSONException {
        JSONObject jsonNews = new JSONObject();
        jsonNews.put("text", news.getText());
        jsonNews.put("tracking_name", news.getTrackingName());
        String publishDate = TimestampHelpers.getCompleteFormattedDate(news.getTimestamp());
        jsonNews.put("publish_date", publishDate);
        jsonNews.put("target_url", news.getTargetUrl().toString());
        jsonNews.put("image_url", news.getImageUrl().toString());
        jsonNews.put("language", news.getLanguage());
        jsonNews.put("level", news.getLevel());
        return jsonNews;
    }

    private JSONArray toJsonArray(List<News> news) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (News newsItem : news) {
            JSONObject jsonObject = toJsonObject(newsItem);
            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }

    private List<News> getNewsListFromJsonArray(JSONArray jsonArray) throws JSONException,
            MalformedURLException {
        List<News> newsList = new ArrayList<>();
        int newsCount = jsonArray.length() >= NEWS_LIMIT ? NEWS_LIMIT : jsonArray.length();
        for (int i = 0; i < newsCount; i++) {
            JSONObject jsonNews = jsonArray.getJSONObject(i);
            News newsItem = new News(jsonNews);
            newsList.add(newsItem);
        }
        return newsList;
    }

    class ImageDownloadReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mDownloadedImages++;
            if (mDownloadedImages >= mExpectedImages) {
                Intent broadcastIntent = new Intent(ACTION_NEWS_UPDATE);
                mContext.sendBroadcast(broadcastIntent);
            }
        }
    }
}
