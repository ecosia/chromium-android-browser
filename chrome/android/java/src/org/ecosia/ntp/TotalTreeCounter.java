package org.ecosia.ntp;

import android.content.Context;
import android.content.Intent;

import org.chromium.base.Log;
import org.ecosia.utils.SharedPreferencesHelpers;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import static org.ecosia.ntp.EcosiaStatisticsManager.EXTRA_TREE_COUNTER_LAST_UPDATE;
import static org.ecosia.ntp.EcosiaStatisticsManager.EXTRA_TREE_COUNTER_RATE;
import static org.ecosia.ntp.EcosiaStatisticsManager.EXTRA_TREE_COUNTER_VALUE;

public class TotalTreeCounter {

    private static final String TAG = TotalTreeCounter.class.getSimpleName();
    private static final String PREF_TREE_COUNTER = "PREF_TREE_COUNTER";
    private static final String TREE_COUNTER_VALUE = "value";
    private static final String TREE_COUNTER_RATE = "rate";
    private static final String TREE_COUNTER_LAST_UPDATE = "last_update";
    private static final String TREE_COUNTER_COMPUTED_VALUE = "computed_value";
    private static final int DEFAULT_TREE_COUNT = 113016418;
    private static final float DEFAULT_TREE_COUNTER_RATE = 1.3f;
    private static final long DEFAULT_TREE_TIMESTAMP = 1604671200000L;

    private final Context mContext;

    private long mServerTreeCounter;
    private long mComputedCount;
    private float mSecondsPerTree;
    private long mLastUpdatedTimestamp;

    public TotalTreeCounter(Context context) {
        mContext = context;
    }

    public void updateFromIntent(Intent intent) {
        mServerTreeCounter = intent.getIntExtra(EXTRA_TREE_COUNTER_VALUE, 0);
        mSecondsPerTree = intent.getFloatExtra(EXTRA_TREE_COUNTER_RATE, 0);
        mLastUpdatedTimestamp = intent.getLongExtra(EXTRA_TREE_COUNTER_LAST_UPDATE, 0);
        mComputedCount = calculateNumberOfTreesPlanted();
        saveTreeCounterData();
    }

    public void loadStoredValues() {
        String serialized = SharedPreferencesHelpers.getString(mContext, PREF_TREE_COUNTER, "");

        if (serialized.equals("")) {
            mServerTreeCounter = DEFAULT_TREE_TIMESTAMP;
            mSecondsPerTree = DEFAULT_TREE_COUNTER_RATE;
            mLastUpdatedTimestamp = DEFAULT_TREE_TIMESTAMP;
            mComputedCount = calculateDefaultNumberOfTreesPlanted();
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(serialized);
            mServerTreeCounter = jsonObject.getLong(TREE_COUNTER_VALUE);
            mSecondsPerTree = (float) jsonObject.getDouble(TREE_COUNTER_RATE);
            mLastUpdatedTimestamp = jsonObject.getLong(TREE_COUNTER_LAST_UPDATE);
            mComputedCount = jsonObject.getInt(TREE_COUNTER_COMPUTED_VALUE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void saveTreeCounterData() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(TREE_COUNTER_VALUE, mServerTreeCounter);
            jsonObject.put(TREE_COUNTER_RATE, mSecondsPerTree);
            jsonObject.put(TREE_COUNTER_LAST_UPDATE, mLastUpdatedTimestamp);
            jsonObject.put(TREE_COUNTER_COMPUTED_VALUE, mComputedCount);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Serialized tree counter data: " + jsonObject.toString());
        SharedPreferencesHelpers.putString(mContext, PREF_TREE_COUNTER, jsonObject.toString());
    }

    public void updateComputedCount() {
        mComputedCount = calculateNumberOfTreesPlanted();
    }

    private long calculateNumberOfTreesPlanted() {
        return calculateNumberOfTreesPlantedRelativeTo(System.currentTimeMillis(),
                mLastUpdatedTimestamp, mSecondsPerTree, mServerTreeCounter);
    }

    private long calculateNumberOfTreesPlantedRelativeTo(long pointInTime, long lastUpdated,
                                                               float secondsPerTree, long treeCount) {
        if (secondsPerTree == 0) {
            return treeCount;
        }
        long timeDifference = pointInTime - lastUpdated;
        float treesSinceLastUpdate =  (float) timeDifference / 1000 / secondsPerTree;
        return (long) treesSinceLastUpdate + treeCount;
    }

    private long calculateDefaultNumberOfTreesPlanted() {
        long now = System.currentTimeMillis();
        return calculateNumberOfTreesPlantedRelativeTo(now, DEFAULT_TREE_TIMESTAMP, DEFAULT_TREE_COUNTER_RATE, DEFAULT_TREE_COUNT);
    }

    public String getFormattedTreeCount(long value) {
        String format = value != 0 ? "%,d" : "> %,d";
        long counterValue = value > 0 ? value : calculateDefaultNumberOfTreesPlanted();
        return String.format(Locale.getDefault(), format, counterValue);
    }

    public float getSecondsPerTree() {
        return mSecondsPerTree;
    }

    public long getComputedCount() {
        return mComputedCount;
    }

    public long getLastUpdatedTimestamp() {
        return mLastUpdatedTimestamp;
    }
}
