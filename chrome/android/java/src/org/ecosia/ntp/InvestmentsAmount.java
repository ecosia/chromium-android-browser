package org.ecosia.ntp;

import static org.ecosia.ntp.EcosiaStatisticsManager.EXTRA_INVESTMENTS_AMOUNT_LAST_UPDATE;
import static org.ecosia.ntp.EcosiaStatisticsManager.EXTRA_INVESTMENTS_AMOUNT_RATE;
import static org.ecosia.ntp.EcosiaStatisticsManager.EXTRA_INVESTMENTS_AMOUNT_VALUE;

import android.content.Context;
import android.content.Intent;

import org.chromium.base.Log;
import org.ecosia.utils.SharedPreferencesHelpers;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class InvestmentsAmount {

    private static final String TAG = InvestmentsAmount.class.getSimpleName();
    private static final String PREF_INVESTMENTS_AMOUNT = "PREF_INVESTMENTS_AMOUNT";
    private static final String INVESTMENTS_AMOUNT_VALUE = "value";
    private static final String INVESTMENTS_AMOUNT_RATE = "rate";
    private static final String INVESTMENTS_AMOUNT_LAST_UPDATE = "last_update";
    private static final String INVESTMENTS_AMOUNT_COMPUTED_VALUE = "computed_value";
    private static final int DEFAULT_INVESTMENTS_AMOUNT = 78500000;
    private static final float DEFAULT_INVESTMENTS_AMOUNT_RATE = 0.35f;
    private static final long DEFAULT_INVESTMENTS_AMOUNT_TIMESTAMP = 1697673600000L;

    private final Context mContext;
    private long mServerInvestmentsAmount;
    private long mComputedCount;
    private float mInvestmentsAmountPerSecond;
    private long mLastUpdatedTimestamp;

    public InvestmentsAmount(Context context) {
        mContext = context;
    }

    public void updateFromIntent(Intent intent) {
        mServerInvestmentsAmount = intent.getIntExtra(EXTRA_INVESTMENTS_AMOUNT_VALUE, 0);
        mInvestmentsAmountPerSecond = intent.getFloatExtra(EXTRA_INVESTMENTS_AMOUNT_RATE, 0);
        mLastUpdatedTimestamp = intent.getLongExtra(EXTRA_INVESTMENTS_AMOUNT_LAST_UPDATE, 0);
        mComputedCount = calculateInvestmentsAmount();
        saveInvestmentsAmountData();
    }

    public void loadStoredValues() {
        String serialized = SharedPreferencesHelpers.getString(mContext, PREF_INVESTMENTS_AMOUNT, "");

        if (serialized.equals("")) {
            mServerInvestmentsAmount = DEFAULT_INVESTMENTS_AMOUNT_TIMESTAMP;
            mInvestmentsAmountPerSecond = DEFAULT_INVESTMENTS_AMOUNT_RATE;
            mLastUpdatedTimestamp = DEFAULT_INVESTMENTS_AMOUNT_TIMESTAMP;
            mComputedCount = calculateDefaultInvestmentsAmount();
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(serialized);
            mServerInvestmentsAmount = jsonObject.getLong(INVESTMENTS_AMOUNT_VALUE);
            mInvestmentsAmountPerSecond = (float) jsonObject.getDouble(INVESTMENTS_AMOUNT_RATE);
            mLastUpdatedTimestamp = jsonObject.getLong(INVESTMENTS_AMOUNT_LAST_UPDATE);
            mComputedCount = jsonObject.getInt(INVESTMENTS_AMOUNT_COMPUTED_VALUE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void saveInvestmentsAmountData() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(INVESTMENTS_AMOUNT_VALUE, mServerInvestmentsAmount);
            jsonObject.put(INVESTMENTS_AMOUNT_RATE, mInvestmentsAmountPerSecond);
            jsonObject.put(INVESTMENTS_AMOUNT_LAST_UPDATE, mLastUpdatedTimestamp);
            jsonObject.put(INVESTMENTS_AMOUNT_COMPUTED_VALUE, mComputedCount);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Serialized investments value data: " + jsonObject);
        SharedPreferencesHelpers.putString(mContext, PREF_INVESTMENTS_AMOUNT, jsonObject.toString());
    }

    public void updateComputedCount() {
        mComputedCount = calculateInvestmentsAmount();
    }

    private long calculateInvestmentsAmount() {
        return calculateInvestmentsAmountRelativeTo(System.currentTimeMillis(),
                mLastUpdatedTimestamp, mInvestmentsAmountPerSecond, mServerInvestmentsAmount);
    }

    private long calculateInvestmentsAmountRelativeTo(long pointInTime, long lastUpdated,
                                                      float amountPerSecond, long investmentsAmount) {
        if (amountPerSecond == 0) {
            return investmentsAmount;
        }
        long timeDifference = pointInTime - lastUpdated;
        float investmentsAmountSinceLastUpdate =  amountPerSecond * timeDifference / 1000;
        return (long) investmentsAmountSinceLastUpdate + investmentsAmount;
    }

    private long calculateDefaultInvestmentsAmount() {
        long now = System.currentTimeMillis();
        return calculateInvestmentsAmountRelativeTo(now, DEFAULT_INVESTMENTS_AMOUNT_TIMESTAMP,
                DEFAULT_INVESTMENTS_AMOUNT_RATE, DEFAULT_INVESTMENTS_AMOUNT);
    }

    public String getFormattedInvestmentsAmount(long value) {
        String format = value != 0 ? "€%,d" : "> €%,d";
        long counterValue = value > 0 ? value : calculateDefaultInvestmentsAmount();
        return String.format(Locale.getDefault(), format, counterValue);
    }

    public float getInvestmentsAmountPerSecond() {
        return mInvestmentsAmountPerSecond;
    }

    public long getComputedCount() {
        return mComputedCount;
    }

    public long getLastUpdatedTimestamp() {
        return mLastUpdatedTimestamp;
    }
}
