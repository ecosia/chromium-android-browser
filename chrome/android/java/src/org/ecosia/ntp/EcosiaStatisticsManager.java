package org.ecosia.ntp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import org.chromium.base.Log;
import androidx.annotation.VisibleForTesting;
import org.chromium.base.task.AsyncTask;
import org.ecosia.utils.Requests;
import org.ecosia.utils.RetrieveDataTask;
import org.ecosia.utils.RetrieveDelegate;
import org.ecosia.utils.SharedPreferencesHelpers;
import org.ecosia.utils.TimestampHelpers;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EcosiaStatisticsManager {

    private static final String TAG = "EcosiaStatisticsManager";
    private static final int FETCH_INTERVAL_DEFAULT = 24 * 60 * 60 * 1000;
    private static final int RETRY_DELAY_DEFAULT = 1000;
    private static final long MAX_REFRESH_FREQUENCY = 2000;
    private static final String PREF_ECOSIA_STATS_RETRIEVE_TIMESTAMP = "PREF_ECOSIA_STATS_RETRIEVE_TIMESTAMP";
    public static final String ACTION_ECOSIA_STATS_FETCH = "com.ecosia.ECOSIA_STATS_FETCH";
    public static final String ACTION_ECOSIA_STATS_UPDATE = "com.ecosia.ECOSIA_STATS_UPDATE";

    public static final String EXTRA_TREE_COUNTER_VALUE = "treeCounterValue";
    public static final String EXTRA_TREE_COUNTER_RATE = "treeCounterSecondsPerTree";
    public static final String EXTRA_TREE_COUNTER_LAST_UPDATE = "treeCounterLastUpdate";

    public static final String EXTRA_INVESTMENTS_AMOUNT_VALUE = "investmentsAmountValue";
    public static final String EXTRA_INVESTMENTS_AMOUNT_RATE = "investmentsAmountPerSecond";
    public static final String EXTRA_INVESTMENTS_AMOUNT_LAST_UPDATE = "investmentsAmountLastUpdate";

    public static final String EXTRA_ECOSIA_STATS_FETCH_SUCCESS = "fetchSuccess";
    private static final String TOTAL_TREES_PLANTED = "Total Trees Planted";
    private static final String SECONDS_PER_TREE = "Time per tree (seconds)";
    private static final String TOTAL_INVESTMENTS_AMOUNT = "Total investments amount";
    private static final String INVESTMENTS_AMOUNT_PER_SECOND = "Investments amount per second";
    private static final String OPTION_NAME = "name";
    private static final String OPTION_VALUE = "value";
    private static final String OPTION_LAST_UPDATED = "last_updated";

    private final Context mContext;
    private ScheduledExecutorService mTreeCounterUpdateScheduler;
    private ScheduledExecutorService mInvestmentsAmountUpdateScheduler;
    private final BroadcastReceiver mStatisticsReceiver;
    private final IntentFilter mStatisticsFilter;
    private final TotalTreeCounter mTotalTreeCounter;
    private final InvestmentsAmount mInvestmentsAmount;
    private final Runnable mRunnable;
    private final Handler mHandler;
    private int mRetryTime;

    public EcosiaStatisticsManager(Context context) {
        mContext = context;
        mRetryTime = RETRY_DELAY_DEFAULT;
        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                retrieve();
            }
        };

        if (isRetrieveTimeElapsed()) {
            mHandler.post(mRunnable);
        }

        mTotalTreeCounter = new TotalTreeCounter(context);
        mInvestmentsAmount = new InvestmentsAmount(context);
        mStatisticsReceiver = new EcosiaStatisticsReceiver();
        mStatisticsFilter = new IntentFilter(ACTION_ECOSIA_STATS_FETCH);
    }

    private boolean isRetrieveTimeElapsed() {
        long nextRetrieveTimestamp = SharedPreferencesHelpers.getLong(mContext,
                PREF_ECOSIA_STATS_RETRIEVE_TIMESTAMP, 0);
        return System.currentTimeMillis() > nextRetrieveTimestamp;
    }

    private void setNextRetrieveTimestamp() {
        long timestamp = System.currentTimeMillis();
        long nextTimestamp = timestamp + FETCH_INTERVAL_DEFAULT;
        SharedPreferencesHelpers.putLong(mContext, PREF_ECOSIA_STATS_RETRIEVE_TIMESTAMP,
                nextTimestamp);
    }

    private void restartScheduler() {
        mTotalTreeCounter.loadStoredValues();
        mInvestmentsAmount.loadStoredValues();
        sendUpdateBroadcast();
        stopTreeCounterScheduler();
        stopInvestmentsAmountScheduler();
        startTreeCounterSchedulerIfAvailable();
        startInvestmentsAmountSchedulerIfAvailable();
    }

    public void start() {
        mContext.registerReceiver(mStatisticsReceiver, mStatisticsFilter);
        restartScheduler();
    }

    private void retrieve() {
        RetrieveDelegate<String> retrieveDelegate = new RetrieveDelegate<>() {
            @Override
            public String doInBackground() {
                try {
                    String url = mContext.getResources().getString(org.chromium.chrome.R.string.cards_option_url);
                    return new Requests().downloadContent(url);
                } catch (IOException |RuntimeException e) {
                    if (e.getMessage() != null ) {
                        Log.d(TAG, "Fetch failed: " + e.getMessage());
                    }
                    return "";
                }
            }

            @Override
            public void onPostExecute(String result) {
                Intent intent = new Intent(ACTION_ECOSIA_STATS_FETCH);
                intent.putExtra(EXTRA_ECOSIA_STATS_FETCH_SUCCESS, false);

                if (!result.isEmpty()) {
                    try {
                        intent = fillIntentFromOptionsResponse(result, intent);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                mContext.sendBroadcast(intent);
            }
        };

        new RetrieveDataTask(retrieveDelegate).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @VisibleForTesting
    public Intent fillIntentFromOptionsResponse(String response, Intent intent) throws JSONException {
        JSONObject jsonResult = new JSONObject(response);
        JSONArray jsonArray = jsonResult.getJSONArray("results");

        boolean hasTreesPlanted = false;
        boolean hasSecondsPerTree = false;
        boolean hasInvestmentsAmount = false;
        boolean hasInvestmentsAmountPerSecond = false;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject option = jsonArray.getJSONObject(i);
            String name = option.getString(OPTION_NAME);
            String value = option.getString(OPTION_VALUE);
            if (name.equals(TOTAL_TREES_PLANTED)) {
                String timeString = option.getString(OPTION_LAST_UPDATED);
                int treeCounter = (int) Math.round(Double.parseDouble(value));
                long lastUpdated = TimestampHelpers.parseTimestamp(timeString);
                intent.putExtra(EXTRA_TREE_COUNTER_VALUE, treeCounter);
                intent.putExtra(EXTRA_TREE_COUNTER_LAST_UPDATE, lastUpdated);
                hasTreesPlanted = true;
            }
            if (name.equals(SECONDS_PER_TREE)) {
                float secondsPerTree = Float.parseFloat(value);
                intent.putExtra(EXTRA_TREE_COUNTER_RATE, secondsPerTree);
                hasSecondsPerTree = true;
            }
            if (name.equals(TOTAL_INVESTMENTS_AMOUNT)) {
                String timeString = option.getString(OPTION_LAST_UPDATED);
                int investmentsAmount = (int) Math.round(Double.parseDouble(value));
                long lastUpdated = TimestampHelpers.parseTimestamp(timeString);
                intent.putExtra(EXTRA_INVESTMENTS_AMOUNT_VALUE, investmentsAmount);
                intent.putExtra(EXTRA_INVESTMENTS_AMOUNT_LAST_UPDATE, lastUpdated);
                hasInvestmentsAmount = true;
            }
            if (name.equals(INVESTMENTS_AMOUNT_PER_SECOND)) {
                float investmentsAmountPerSecond = Float.parseFloat(value);
                intent.putExtra(EXTRA_INVESTMENTS_AMOUNT_RATE, investmentsAmountPerSecond);
                hasInvestmentsAmountPerSecond = true;
            }
        }
        boolean isFetchSuccessful = hasTreesPlanted && hasSecondsPerTree && hasInvestmentsAmount &&
                hasInvestmentsAmountPerSecond;
        intent.putExtra(EXTRA_ECOSIA_STATS_FETCH_SUCCESS, isFetchSuccessful);
        return intent;
    }

    private void startTreeCounterSchedulerIfAvailable() {
        float secondsPerTree = mTotalTreeCounter.getSecondsPerTree();
        if (secondsPerTree > 0) {
            long refreshFrequency = Math.max((long) secondsPerTree * 1000, MAX_REFRESH_FREQUENCY);
            mTreeCounterUpdateScheduler = Executors.newScheduledThreadPool(1);
            mTreeCounterUpdateScheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    mTotalTreeCounter.updateComputedCount();
                    sendUpdateBroadcast();
                }
            }, 0, refreshFrequency, TimeUnit.MILLISECONDS);
            Log.d(TAG, "Tree counter scheduler started at " + refreshFrequency + " ms rate");
        }
    }

    private void startInvestmentsAmountSchedulerIfAvailable() {
        float investmentsAmountPerSecond = mInvestmentsAmount.getInvestmentsAmountPerSecond();
        if (investmentsAmountPerSecond > 0) {
            long refreshFrequency = MAX_REFRESH_FREQUENCY;
            mInvestmentsAmountUpdateScheduler = Executors.newScheduledThreadPool(1);
            mInvestmentsAmountUpdateScheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    mInvestmentsAmount.updateComputedCount();
                    sendUpdateBroadcast();
                }
            }, 0, refreshFrequency, TimeUnit.MILLISECONDS);
            Log.d(TAG, "Investments value scheduler started at " + refreshFrequency + " ms rate");
        }
    }

    public void stop() {
        try {
            mContext.unregisterReceiver(mStatisticsReceiver);
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
        stopTreeCounterScheduler();
        stopInvestmentsAmountScheduler();
    }

    private void stopTreeCounterScheduler() {
        if (mTreeCounterUpdateScheduler != null) {
            mTreeCounterUpdateScheduler.shutdown();
            Log.d(TAG, "Tree counter scheduler stopped");
        }
    }

    private void stopInvestmentsAmountScheduler() {
        if (mInvestmentsAmountUpdateScheduler != null) {
            mInvestmentsAmountUpdateScheduler.shutdown();
            Log.d(TAG, "Investments value scheduler stopped");
        }
    }

    private void sendUpdateBroadcast() {
        Intent intent = new Intent(ACTION_ECOSIA_STATS_UPDATE);
        mContext.sendBroadcast(intent);
    }

    public String getFormattedTreeCount(final long value) {
        return mTotalTreeCounter.getFormattedTreeCount(value);
    }

    public TotalTreeCounter getTotalTreeCounter() {
        return mTotalTreeCounter;
    }

    public String getFormattedInvestmentsAmount(final long value) {
        return mInvestmentsAmount.getFormattedInvestmentsAmount(value);
    }

    public InvestmentsAmount getInvestmentsAmount() {
        return mInvestmentsAmount;
    }

    class EcosiaStatisticsReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean fetchSuccess = intent.getBooleanExtra(EXTRA_ECOSIA_STATS_FETCH_SUCCESS, false);
            if (fetchSuccess) {
                handleFetchSuccess(intent);
            } else {
                handleFetchFailure();
            }
        }

        private void handleFetchSuccess(Intent intent) {
            resetRetrieveToDefault();
            mTotalTreeCounter.updateFromIntent(intent);
            mInvestmentsAmount.updateFromIntent(intent);
            restartScheduler();
        }

        private void resetRetrieveToDefault() {
            mHandler.postDelayed(mRunnable, FETCH_INTERVAL_DEFAULT);
            mRetryTime = RETRY_DELAY_DEFAULT;
            setNextRetrieveTimestamp();
        }

        private void handleFetchFailure() {
            mHandler.postDelayed(mRunnable, mRetryTime);
            mRetryTime = mRetryTime * 2;
            if (mRetryTime >= FETCH_INTERVAL_DEFAULT) {
                mRetryTime = FETCH_INTERVAL_DEFAULT;
            }
        }
    }
}
