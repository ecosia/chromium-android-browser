package org.ecosia.referrals;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import org.chromium.base.TimeUtils;
import org.chromium.base.task.AsyncTask;
import org.ecosia.api.ApiProvider;
import org.ecosia.utils.DateHolder;
import org.ecosia.utils.Requests;
import org.ecosia.utils.RetrieveDataTask;
import org.ecosia.utils.RetrieveDelegate;
import org.ecosia.utils.SettingsHelpers;
import org.ecosia.utils.SharedPreferencesHelpers;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

public class Referrals {
    public enum Error {
        FAILED_CREATING_REFERRAL_CODE,
        FAILED_FETCHING_REFERRALS_COUNT,
    }

    public enum ClaimError {
        ALREADY_CLAIMED,
        INVALID_LINK,
        NO_CURRENT_REFERRAL_CODE,
        NO_INSTALL_REFERRER,
        NETWORK_ERROR,
        ILLEGALLY_FORMATTED_REFERRAL_CODE
    }

    public interface ReferralCodeCallback {
        void onReady(@Nullable String referralCode, int currentClaimsCount, int previousClaimsCount);
        void onError(Error error);
    }

    public interface ClaimReferralCallback {
        void onClaimed();
        void onError(ClaimError error);
    }

    private class ClaimsCountResult {
        Boolean needsReferralCodeCreation;
        @Nullable Pair<String, Map<String, List<String>>> result;

        ClaimsCountResult(Boolean needsReferralCodeCreation, @Nullable Pair<String, Map<String, List<String>>> result) {
            this.needsReferralCodeCreation = needsReferralCodeCreation;
            this.result = result;
        }
    }

    private static final String TAG = "Referrals";
    private Context mContext;
    private static volatile Referrals sInstance;

    private static final String REFERRAL_CODE_PREFIX = "friends-";

    private static final String PREF_REFERRALS_CURRENT_REFERRAL_CODE = "com.ecosia.PREF_REFERRALS_CURRENT_REFERRAL_CODE";
    private static final String PREF_REFERRALS_MOST_CURRENTLY_KNOWN_CLAIMS_COUNT = "com.ecosia.PREF_REFERRALS_MOST_CURRENTLY_KNOWN_CLAIMS_COUNT";
    private static final String PREF_REFERRALS_MOST_RECENT_INSTALL_REFERRER = "com.ecosia.PREF_REFERRALS_MOST_RECENT_INSTALL_REFERRER";
    private static final String PREF_REFERRALS_DID_CLAIM_INSTALL_REFERRER = "com.ecosia.PREF_REFERRALS_DID_CLAIM_INSTALL_REFERRER";
    private static final String PREF_REFERRALS_LAST_CLAIMS_COUNT_FETCH_TIMESTAMP = "com.ecosia.PREF_REFERRALS_LAST_CLAIMS_COUNT_FETCH_TIMESTAMP";

    private static final String CLAIM_ERROR_MESSAGE_ALREADY_CLAIMED = "Referral code has been claimed already!";
    private static final String CLAIM_ERROR_MESSAGE_INVALID_LINK = "Invalid Referral Code";

    public static final int NO_CLAIMS_COUNT = -1;

    private int mClaimsCount = NO_CLAIMS_COUNT;

    private Referrals(Context context) {
        mContext = context;
    }

    public synchronized static Referrals getInstance(Context context) {
        if (sInstance == null) {
            synchronized (Referrals.class) {
                if (sInstance == null) {
                    sInstance = new Referrals(context);
                }
            }
        }
        return sInstance;
    }

    private HashMap<String, String> getApiRequestHeaders() {
        return new HashMap<>() {{
            if (ApiProvider.getEnvironment() == ApiProvider.Environment.STAGING) {
                put("CF-Access-Client-Id", SettingsHelpers.getCloudflareClientId(mContext));
                put("CF-Access-Client-Secret", SettingsHelpers.getCloudflareClientSecret(mContext));
            }
        }};
    }

    private boolean shouldFetchNewClaimsCount() {
        long lastFetchTimestampMillis = SharedPreferencesHelpers.getLong(mContext, PREF_REFERRALS_LAST_CLAIMS_COUNT_FETCH_TIMESTAMP, 0L);
        DateHolder now = new DateHolder(Calendar.getInstance());

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(lastFetchTimestampMillis);
        DateHolder lastFetchDate = new DateHolder(calendar);

        return now.isAtLeastOneDayLaterAs(lastFetchDate);
    }

    public void getReferralCode(boolean ignoreRateLimit, ReferralCodeCallback callback) {
        createAndPersistReferralCodeIfRequired(ignoreRateLimit, callback);
    }

    public void getCurrentClaimsCount(boolean ignoreRateLimit, ReferralCodeCallback callback) {
        if (getCurrentReferralCode() == null) {
            callback.onReady(null, storedComputedClaimsCount(), storedComputedClaimsCount());
        } else {
           doRetrieveClaimsCount(ignoreRateLimit, callback);
        }
    }

    public void setInstallReferrer(@Nullable String referrer) {
        SharedPreferencesHelpers.putString(mContext, PREF_REFERRALS_MOST_RECENT_INSTALL_REFERRER, referrer);
    }

    private int storedComputedClaimsCount() {
        int storedCount = SharedPreferencesHelpers.getInt(mContext, PREF_REFERRALS_MOST_CURRENTLY_KNOWN_CLAIMS_COUNT, 0);
        int computedCount = didClaimReferral() ? 1 : 0;
        return storedCount + computedCount;
    }

    private boolean didClaimReferral() {
        return SharedPreferencesHelpers.getBoolean(mContext, PREF_REFERRALS_DID_CLAIM_INSTALL_REFERRER, false);
    }

    @Nullable
    private String getInstallReferrer() {
        return SharedPreferencesHelpers.getString(mContext, PREF_REFERRALS_MOST_RECENT_INSTALL_REFERRER, null);
    }

    @Nullable
    private String getCurrentReferralCode() {
        String existingReferralCode = SharedPreferencesHelpers.getString(mContext, PREF_REFERRALS_CURRENT_REFERRAL_CODE, "");
        if (existingReferralCode.isBlank()) {
            return null;
        }
        return existingReferralCode;
    }

    @Nullable
    public String getReferralShareLinkUriString() {
        return "https://ecosia.co/app?referrer=" + getCurrentReferralCode();
    }

    @Nullable
    public String getIosReferralUriString() {
        return "ecosia://invite/" + getCurrentReferralCode();
    }

    public void claimInstallationFromReferrer(ClaimReferralCallback callback) {
        String referrer = getInstallReferrer();

        if (referrer == null || referrer.isBlank()) {
            callback.onError(ClaimError.NO_INSTALL_REFERRER);
            return;
        }

        if (!isValidReferralCode(referrer)) {
            setInstallReferrer(null);
            callback.onError(ClaimError.ILLEGALLY_FORMATTED_REFERRAL_CODE);
            return;
        }

        String currentReferralCode = getCurrentReferralCode();
        if (currentReferralCode != null) {
            // immediately fetch new claim as code already exists
            doClaimReferralCode(referrer, currentReferralCode, callback);
            return;
        }

        // Create new referral code and do claim once code exists
        createNewReferralCode(new ReferralCodeCallback() {
            @Override
            public void onReady(@Nullable String referralCode, int currentClaimsCount, int previousClaimsCount) {
                doClaimReferralCode(referrer, referralCode, callback);
            }

            @Override
            public void onError(Error error) {
                callback.onError(ClaimError.NO_CURRENT_REFERRAL_CODE);
            }
        });
    }

    private boolean isValidReferralCode(@Nullable String code) {
        if (code == null) return false;
        return code.startsWith(REFERRAL_CODE_PREFIX);
    }

    private void createAndPersistReferralCodeIfRequired(boolean ignoreRateLimit, ReferralCodeCallback callback) {
        String existingReferralCode = getCurrentReferralCode();
        if (existingReferralCode != null && !existingReferralCode.isBlank()) {
            doRetrieveClaimsCount(ignoreRateLimit, new ReferralCodeCallback() {
                @Override
                public void onReady(@Nullable String referralCode, int currentClaimsCount, int previousClaimsCount) {
                    callback.onReady(referralCode, storedComputedClaimsCount(), previousClaimsCount);
                }

                @Override
                public void onError(Error error) {
                    callback.onError(error);
                }
            });
            return;
        }

        createNewReferralCode(new ReferralCodeCallback() {
            @Override
            public void onReady(@androidx.annotation.Nullable String referralCode, int currentClaimsCount, int previousClaimsCount) {
                doRetrieveClaimsCount(ignoreRateLimit, callback);
            }

            @Override
            public void onError(Error error) {
                callback.onError(error);
            }
        });
    }

    private void createNewReferralCode(ReferralCodeCallback callback) {
        RetrieveDelegate<Optional<String>> retrieveDelegate = new RetrieveDelegate<>() {
            @Override
            public Optional<String> doInBackground() {
                try {
                    String url = ApiProvider.getApiUrl(ApiProvider.UseCase.REFERRALS) + "/referral/";
                    String result = new Requests().postEmptyBody(url, getApiRequestHeaders());
                    return Optional.of(result);

                } catch (IOException | RuntimeException e) {
                    if (e.getMessage() != null ) {
                        Log.d(TAG, "Fetch failed: " + e.getMessage());
                    }
                    return Optional.empty();
                }
            }

            @Override
            public void onPostExecute(Optional<String> result) {
                Log.d(TAG, "Result:" + result);

                if (result != null && result.isPresent())  {
                    try {
                        JSONObject jsonObject = new JSONObject(result.get());
                        String referralCode = jsonObject.getString("code");
                        Log.d(TAG, "Received new Referral Code:" + referralCode);
                        SharedPreferencesHelpers.putString(mContext, PREF_REFERRALS_CURRENT_REFERRAL_CODE, referralCode);
                        callback.onReady(referralCode, storedComputedClaimsCount(), storedComputedClaimsCount());
                    } catch (JSONException e) {
                        Log.e(TAG, "Caught exception when trying to decode Referrals response:" + e);
                        callback.onError(Error.FAILED_CREATING_REFERRAL_CODE);
                    }
                } else {
                    Log.d(TAG, "Received empty result. Bailing out.");
                    callback.onError(Error.FAILED_CREATING_REFERRAL_CODE);
                }
            }
        };

        new RetrieveDataTask(retrieveDelegate).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void doRetrieveClaimsCount(boolean ignoreRateLimit, ReferralCodeCallback callback) {
        String currentReferralCode = getCurrentReferralCode();
        if (currentReferralCode == null) {
            Log.e(TAG, "No existing Referral Code, not trying to fetch Claims count.");
            return;
        }
        if (!shouldFetchNewClaimsCount() && !ignoreRateLimit) {
            callback.onReady(currentReferralCode, storedComputedClaimsCount(), storedComputedClaimsCount());
            return;
        }
        RetrieveDelegate<ClaimsCountResult> retrieveDelegate = new RetrieveDelegate<>() {
            @Override
            public ClaimsCountResult doInBackground() {
                try {
                    String url = ApiProvider.getApiUrl(ApiProvider.UseCase.REFERRALS) + "/referral/" + getCurrentReferralCode();
                    Pair<String, Map<String, List<String>>> result = new Requests().downloadContentAndHeaders(url, getApiRequestHeaders());
                    return new ClaimsCountResult(false, result);
                } catch (FileNotFoundException e) {
                    return new ClaimsCountResult(true, null);
                } catch (IOException | RuntimeException e) {
                    e.printStackTrace();
                    if (e.getMessage() != null) {
                        Log.d(TAG, "Fetch failed: " + e.getMessage());
                    }
                    return new ClaimsCountResult(false, null);
                }
            }

            @Override
            public void onPostExecute(ClaimsCountResult retrieveResult) {
                Log.d(TAG, "Result:" + retrieveResult.result);
                if (retrieveResult.needsReferralCodeCreation) {
                    createNewReferralCode(callback);
                } else if (retrieveResult.result != null && retrieveResult.result.first != null)  {
                    try {
                        int previouslyStoredComputedClaimsCount = storedComputedClaimsCount();
                        JSONObject jsonObject = new JSONObject(retrieveResult.result.first);
                        String referralCode = jsonObject.getString("code");
                        int claimsCount = jsonObject.getInt("claims_count");
                        mClaimsCount = claimsCount;
                        Log.d(TAG, "Received Claims count: " + claimsCount  + " for existing Referral Code:" + referralCode);
                        SharedPreferencesHelpers.putInt(mContext, PREF_REFERRALS_MOST_CURRENTLY_KNOWN_CLAIMS_COUNT, claimsCount);
                        SharedPreferencesHelpers.putLong(mContext, PREF_REFERRALS_LAST_CLAIMS_COUNT_FETCH_TIMESTAMP, TimeUtils.currentTimeMillis());
                        callback.onReady(referralCode, storedComputedClaimsCount(), previouslyStoredComputedClaimsCount);
                    } catch (JSONException e) {
                        Log.e(TAG, "Caught exception when trying to decode Claims count response:" + e);
                        callback.onError(Error.FAILED_FETCHING_REFERRALS_COUNT);
                    }
                } else {
                    Log.d(TAG, "Received empty result. Bailing out.");
                    callback.onError(Error.FAILED_FETCHING_REFERRALS_COUNT);
                }
            }
        };

        new RetrieveDataTask(retrieveDelegate).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void doClaimReferralCode(String claimCode, String currentReferralCode, ClaimReferralCallback callback) {
        if (currentReferralCode == null) {
            // This should not happen
            Log.e(TAG, "No existing Referral Code, not trying to claim code.");
            callback.onError(ClaimError.NO_CURRENT_REFERRAL_CODE);
            return;
        }
        RetrieveDelegate<Optional<String>> retrieveDelegate = new RetrieveDelegate<>() {
            @Override
            public Optional<String> doInBackground() {
                try {
                    String url = ApiProvider.getApiUrl(ApiProvider.UseCase.REFERRALS) + "/claim/";
                    String jsonString = "{\"claim_code\":\"" + currentReferralCode + "\",\"referral_code\":\"" + claimCode + "\"}";
                    String result = new Requests().postStringBody(url, jsonString, getApiRequestHeaders());
                    return Optional.of(result);
                } catch (IOException | RuntimeException e) {
                    if (e.getMessage() != null ) {
                        Log.d(TAG, "Fetch failed: " + e.getMessage());
                    }
                    return Optional.empty();
                }
            }

            @Override
            public void onPostExecute(Optional<String> result) {
                Log.d(TAG, "Result:" + result);

                if (result != null && result.isPresent())  {
                    try {
                        JSONObject jsonObject = new JSONObject(result.get());

                        String errorMessage = jsonObject.optString("error");
                        if (!errorMessage.isEmpty()) {
                            // Handle error
                            switch (errorMessage) {
                                case CLAIM_ERROR_MESSAGE_ALREADY_CLAIMED:
                                    callback.onError(ClaimError.ALREADY_CLAIMED);
                                    break;
                                case CLAIM_ERROR_MESSAGE_INVALID_LINK:
                                    // Delete stored code as it's considered invalid
                                    setInstallReferrer(null);
                                    callback.onError(ClaimError.INVALID_LINK);
                                    break;
                            }

                            // Do not try to claim code as an error has occurred
                            return;
                        }

                        String claimCode = jsonObject.getString("claim_code");
                        Log.d(TAG, "Successfully Claimed " + claimCode);

                        // Remember claiming of referral code
                        SharedPreferencesHelpers.putBoolean(mContext, PREF_REFERRALS_DID_CLAIM_INSTALL_REFERRER, true);
                        doRetrieveClaimsCount(true, new ReferralCodeCallback() {
                            @Override
                            public void onReady(@Nullable String referralCode, int currentClaimsCount, int previousClaimsCount) {
                                setInstallReferrer(null);
                                callback.onClaimed();
                            }

                            @Override
                            public void onError(Error error) {
                                callback.onError(ClaimError.NETWORK_ERROR);
                            }
                        });
                    } catch (JSONException e) {
                        Log.e(TAG, "Caught exception when trying to decode Claims count response:" + e);
                        callback.onError(ClaimError.NETWORK_ERROR);
                    }
                } else {
                    Log.d(TAG, "Received empty result. Bailing out.");
                    callback.onError(ClaimError.NETWORK_ERROR);
                }
            }
        };

        new RetrieveDataTask(retrieveDelegate).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
